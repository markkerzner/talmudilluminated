#!/usr/bin/env bash
#
# publish.sh -- crawl the Talmud Illuminated blog, build the static site,
# and deploy it to talmudilluminated.com.
#
# Replaces collect_all.sh (an IntelliJ "copy run command" capture with
# hard-coded ~/.m2 jar paths). The classpath here comes from Maven, every
# stage aborts on failure, and the QA stage gates the deploy.
#
#   ./publish.sh                 full run: crawl, QA, build, copy, deploy
#   ./publish.sh --skip-crawl    rebuild and deploy from existing content/
#   ./publish.sh --skip-deploy   crawl and build, but publish nothing
#   ./publish.sh bava_kamma      crawl only that masechet, then build/deploy
#   ./publish.sh -y              don't ask before deploying
#   ./publish.sh --update-baseline   record this run's QA result as the
#                                    new doc/current_qa.txt baseline
#
# The build also emits the PWA files (manifest, service worker, icons) that make
# the site installable and let an opened daf be re-read offline; the deploy is
# gated on them being present and well formed.
#
# Environment:
#   GOOGLE_API_KEY       required for the crawl (Blogger API)
#   CONTENT_REPO         target repo for the built site
#                        (default: ../TalmudIlluminatedContent)
#   MAX_NEW_MISSING      abort before deploy if QA reports this many more
#                        missing pages than the recorded baseline in
#                        doc/current_qa.txt (default: 10)

set -euo pipefail

PROJECT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"   # BloggerPuller, QA and MakeSite all use relative paths

CONTENT_REPO="${CONTENT_REPO:-$PROJECT_DIR/../TalmudIlluminatedContent}"
MAX_NEW_MISSING="${MAX_NEW_MISSING:-10}"
QA_BASELINE="doc/current_qa.txt"
MISSING_LIST="doc/missing_pages.md"
CLASSPATH_CACHE="target/classpath.txt"
QA_LOG="target/qa.txt"

skip_crawl=false
skip_deploy=false
assume_yes=false
update_baseline=false
masechet=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-crawl)  skip_crawl=true ;;
    --update-baseline) update_baseline=true ;;
    --skip-deploy) skip_deploy=true ;;
    -y|--yes)      assume_yes=true ;;
    -h|--help)     awk 'NR>1 && /^#/ {sub(/^# ?/, ""); print; next} NR>1 {exit}' "${BASH_SOURCE[0]}"; exit 0 ;;
    -*)            echo "publish.sh: unknown option $1" >&2; exit 2 ;;
    *)             masechet="$1" ;;
  esac
  shift
done

step() { printf '\n\033[1m==> %s\033[0m\n' "$1"; }
die()  { printf '\033[31mpublish.sh: %s\033[0m\n' "$1" >&2; exit 1; }

# --- preflight ---------------------------------------------------------------

if ! $skip_crawl && [[ -z "${GOOGLE_API_KEY:-}" ]]; then
  die "GOOGLE_API_KEY is not set; the crawl needs it for the Blogger API."
fi

if ! $skip_deploy; then
  [[ -d "$CONTENT_REPO" ]]          || die "content repo not found: $CONTENT_REPO"
  [[ -x "$CONTENT_REPO/deploy.sh" ]] || die "no executable deploy.sh in $CONTENT_REPO"
fi

# --- compile and resolve the classpath --------------------------------------

step "Compiling"
mvn -q compile

if [[ ! -s "$CLASSPATH_CACHE" || pom.xml -nt "$CLASSPATH_CACHE" ]]; then
  step "Resolving dependency classpath"
  mvn -q dependency:build-classpath -Dmdep.outputFile="$CLASSPATH_CACHE" -Dmdep.includeScope=runtime
fi
CP="target/classes:$(<"$CLASSPATH_CACHE")"

run_java() { java -cp "$CP" "$@"; }

# --- 1. crawl ----------------------------------------------------------------

if $skip_crawl; then
  step "Skipping crawl (using existing content/)"
else
  step "Crawling the blog${masechet:+ ($masechet only)}"
  run_java com.shmsoft.t_i.BloggerPuller ${masechet:+"$masechet"}
fi

# --- 2. QA -------------------------------------------------------------------

step "QA"
run_java com.shmsoft.t_i.QA | tee "$QA_LOG"

# Some pages are legitimately absent from the blog, so an absolute threshold is
# meaningless here. Compare against the recorded baseline instead: a jump in
# missing pages means the crawl broke (expired key, Blogger change), not that
# the blog changed.
missing_count() { grep -oP 'Missing pages count:\s*\K[0-9]+' "$1" | tail -1; }

missing="$(missing_count "$QA_LOG" || true)"
[[ -n "$missing" ]] || die "QA produced no 'Missing pages count' line; see $PROJECT_DIR/$QA_LOG"

# Refresh the human-readable list of what is still unwritten.
if command -v python3 >/dev/null 2>&1; then
  python3 -I missing_pages.py "$QA_LOG" > "$MISSING_LIST"
  echo "Wrote $MISSING_LIST"
fi

if [[ -f "$QA_BASELINE" ]]; then
  baseline="$(missing_count "$QA_BASELINE" || true)"
else
  baseline=""
fi

if [[ -z "$baseline" ]]; then
  echo "QA: $missing pages missing (no baseline in $QA_BASELINE to compare against)."
elif (( missing > baseline + MAX_NEW_MISSING )); then
  die "QA reports $missing missing pages, $((missing - baseline)) more than the
     baseline of $baseline in $QA_BASELINE (tolerance $MAX_NEW_MISSING).
     That usually means the crawl failed rather than that the blog changed.
     Missing pages are listed in $PROJECT_DIR/$QA_LOG.
     Re-run with MAX_NEW_MISSING=$((missing - baseline)) to publish anyway."
else
  echo "QA: $missing pages missing vs. a baseline of $baseline -- within tolerance."
fi

if $update_baseline; then
  cp "$QA_LOG" "$QA_BASELINE"
  echo "Baseline updated: $QA_BASELINE"
fi

# --- 3. build the site -------------------------------------------------------

step "Building site/"
run_java com.shmsoft.t_i.MakeSite

# --- 3b. check the PWA files -------------------------------------------------

# A broken manifest or service worker does not break the site, which is exactly
# why it would go unnoticed: pages keep rendering while installability and
# offline reading quietly stop working. Gate the deploy on it, as QA does.

step "Checking PWA files"

pwa_missing=()
for f in manifest.webmanifest sw.js pwa.js offline.html \
         icon-192.png icon-512.png icon-maskable-512.png; do
  [[ -s "site/$f" ]] || pwa_missing+=("$f")
done
(( ${#pwa_missing[@]} == 0 )) \
  || die "MakeSite did not produce: ${pwa_missing[*]}
     These come from src/main/resources/pwa/; check that mvn copied them to target/classes."

if grep -q '@@CACHE_VERSION@@' site/sw.js; then
  die "site/sw.js still contains the @@CACHE_VERSION@@ placeholder -- every
     reader would share one cache name that never changes, so no one would
     ever see updated pages."
fi

if command -v python3 >/dev/null 2>&1; then
  python3 -I -c 'import json,sys; json.load(open("site/manifest.webmanifest"))' \
    || die "site/manifest.webmanifest is not valid JSON; browsers would ignore it
     and the site would stop being installable."
fi

# The head tags are what make any given page an entry point for installing.
# -print -quit rather than piping to head: under pipefail, head closing the pipe
# kills find with SIGPIPE and takes the whole script down with it.
sample_page="$(find site -name '*.html' -path '*/*' -print -quit)"
if [[ -n "$sample_page" ]] && ! grep -q 'rel="manifest"' "$sample_page"; then
  die "generated pages are missing the PWA head tags (checked $sample_page).
     MakeSite.documentHead() should be emitting them."
fi

echo "PWA: $(grep -oP "const VERSION = '\K[^']+" site/sw.js), manifest and icons present."

# --- 4. copy into the content repo ------------------------------------------

if $skip_deploy; then
  step "Done (--skip-deploy): site built in $PROJECT_DIR/site, nothing published"
  exit 0
fi

step "Copying site/ into $CONTENT_REPO"
cp -r site/. "$CONTENT_REPO/"

# index.html and about.html are hand-maintained in the content repo rather than
# generated, so MakeSite cannot add the PWA tags to them. The home page is the
# one most readers install from, so say something rather than fail: the deploy
# is still correct, it just installs from a daf page instead.
for hand_written in index.html about.html; do
  page="$CONTENT_REPO/$hand_written"
  if [[ -f "$page" ]] && ! grep -q 'rel="manifest"' "$page"; then
    printf '\033[33mnote: %s has no PWA tags; add this inside its <head> to make it installable:\033[0m\n' "$hand_written"
    cat <<'TAGS'
    <link rel="manifest" href="/manifest.webmanifest">
    <meta name="theme-color" content="#6b4420">
    <link rel="apple-touch-icon" href="/icon-192.png">
    <meta name="apple-mobile-web-app-title" content="Talmud">
    <script src="/pwa.js" defer></script>
TAGS
  fi
done

# --- 5. deploy ---------------------------------------------------------------

if ! $assume_yes && [[ -t 0 ]]; then
  printf '\nDeploy to talmudilluminated.com now? [y/N] '
  read -r reply
  [[ "$reply" == [yY]* ]] || { echo "Stopped before deploy; site is staged in $CONTENT_REPO"; exit 0; }
fi

step "Deploying"
( cd "$CONTENT_REPO" && ./deploy.sh )

step "Published to talmudilluminated.com"
