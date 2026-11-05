#!/usr/bin/env python3
"""Rebuild the Talmud knowledge bundles used as ChatGPT plugin reference files.

Merges every page under content/ into a handful of token-balanced Markdown
bundles, plus three CSV manifests. Originally done ad hoc inside ChatGPT's code
interpreter in Sep 2025; this script reproduces that output byte-for-byte and
makes it repeatable whenever content/ changes.

Usage:
    ./CustomGPT/make_bundles.py                    # rebuild in place
    ./CustomGPT/make_bundles.py --out-dir /tmp/x   # build elsewhere
    ./CustomGPT/make_bundles.py --content-dir DIR --budget 200000
"""

import argparse
import csv
import pathlib
import sys

NOTE = "> Merged pages for Custom GPT knowledge (token-balanced)."
SEP = "\n\n---\n\n"
DEFAULT_BUDGET = 200_000  # est. tokens per bundle; keeps each file well under
                          # the 2M-token-per-file cap and the ~2MB size at
                          # which the GPT->plugin migrator has been seen to balk


def est_tokens(n_chars: int) -> int:
    """Token estimate used by the original bundler: 4 chars per token.

    Floors at 1 so the ~375 placeholder pages for dapim that have not been
    written yet (a single newline each) still count as one token, matching the
    Sep 2025 manifests.
    """
    return max(1, n_chars // 4)


def collect_pages(content_dir: pathlib.Path, repo_root: pathlib.Path) -> list[dict]:
    """Every page under content/, in lexicographic rel_path order."""
    pages = []
    for path in sorted(content_dir.rglob("*.txt"), key=lambda p: str(p)):
        text = path.read_text(encoding="utf-8", errors="replace")
        try:
            rel = path.relative_to(repo_root).as_posix()
        except ValueError:
            rel = pathlib.Path("content", path.relative_to(content_dir)).as_posix()
        pages.append({
            "path": str(path),
            "rel_path": rel,
            "size_bytes": path.stat().st_size,
            "n_chars": len(text),
            "est_tokens": est_tokens(len(text)),
            "tractate": path.parent.name,
            "daf_num": "",
            "daf_side": "",
            "text": text,
        })
    return pages


def pack(pages: list[dict], budget: int) -> list[list[dict]]:
    """Greedy fill: start a new bundle when the next page would exceed budget."""
    bundles: list[list[dict]] = []
    current: list[dict] = []
    total = 0
    for page in pages:
        if current and total + page["est_tokens"] > budget:
            bundles.append(current)
            current, total = [], 0
        current.append(page)
        total += page["est_tokens"]
    if current:
        bundles.append(current)
    return bundles


def render(index: int, pages: list[dict]) -> str:
    toc = "\n".join(
        f"- [{p['rel_path']}](#src-{i:05d})" for i, p in enumerate(pages, 1)
    )
    head = (
        f"# Talmud Bundle {index:02d}\n\n{NOTE}\n\n"
        f"## Table of Contents\n\n{toc}"
    )
    blocks = [
        f"### {p['rel_path']}\n"
        f'<a id="src-{i:05d}"></a>\n\n'
        f"**Source file:** `{p['rel_path']}`\n\n"
        f"{p['text'].strip()}"
        for i, p in enumerate(pages, 1)
    ]
    return head + SEP + SEP.join(blocks) + SEP


def write_csv(path: pathlib.Path, fields: list[str], rows: list[dict]) -> None:
    with path.open("w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=fields, extrasaction="ignore",
                                lineterminator="\n")
        writer.writeheader()
        writer.writerows(rows)


def main() -> int:
    repo_root = pathlib.Path(__file__).resolve().parent.parent
    ap = argparse.ArgumentParser(description=__doc__,
                                 formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--content-dir", type=pathlib.Path,
                    default=repo_root / "content",
                    help="directory of page .txt files (default: ./content)")
    ap.add_argument("--out-dir", type=pathlib.Path,
                    default=repo_root / "CustomGPT" / "talmud_merged_for_custom_gpt",
                    help="where to write bundles and manifests")
    ap.add_argument("--budget", type=int, default=DEFAULT_BUDGET,
                    help=f"est. tokens per bundle (default: {DEFAULT_BUDGET})")
    args = ap.parse_args()

    if not args.content_dir.is_dir():
        print(f"error: no such content dir: {args.content_dir}", file=sys.stderr)
        return 1

    pages = collect_pages(args.content_dir, repo_root)
    if not pages:
        print(f"error: no .txt pages found under {args.content_dir}", file=sys.stderr)
        return 1

    bundles = pack(pages, args.budget)
    args.out_dir.mkdir(parents=True, exist_ok=True)

    mapping, summary = [], []
    for index, group in enumerate(bundles, 1):
        name = f"talmud_bundle_{index:02d}.md"
        text = render(index, group)
        out = args.out_dir / name
        out.write_text(text, encoding="utf-8")
        for order, page in enumerate(group, 1):
            mapping.append({
                "bundle": name,
                "order_in_bundle": order,
                "source_rel_path": page["rel_path"],
                "est_tokens": page["est_tokens"],
            })
        summary.append({
            "bundle": name,
            "n_pages": len(group),
            "approx_tokens": sum(p["est_tokens"] for p in group),
            # size_mb is the bundle file as written -- that is what the upload
            # size check sees. The Sep 2025 manifest put the source pages' own
            # bytes here instead, understating each bundle by ~9% because it
            # ignored the table of contents and per-page headers; that figure
            # is kept alongside as source_mb.
            "size_mb": round(out.stat().st_size / 1_048_576, 2),
            "source_mb": round(sum(p["size_bytes"] for p in group) / 1_048_576, 2),
        })

    write_csv(args.out_dir / "all_files_inventory.csv",
              ["path", "rel_path", "size_bytes", "n_chars", "est_tokens",
               "tractate", "daf_num", "daf_side"], pages)
    write_csv(args.out_dir / "bundle_mapping.csv",
              ["bundle", "order_in_bundle", "source_rel_path", "est_tokens"], mapping)
    write_csv(args.out_dir / "bundle_summary.csv",
              ["bundle", "n_pages", "approx_tokens", "size_mb", "source_mb"], summary)

    print(f"{len(pages)} pages -> {len(bundles)} bundles in {args.out_dir}")
    for row in summary:
        print(f"  {row['bundle']}: {row['n_pages']:>4} pages, "
              f"~{row['approx_tokens']:>7,} tokens, {row['size_mb']} MB")
    return 0


if __name__ == "__main__":
    sys.exit(main())
