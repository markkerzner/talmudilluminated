# MosesAI — Custom GPT knowledge bundles

The knowledge base behind [MosesAI](https://chatgpt.com/g/g-68d6ee5a5c008191a1919c20e0777157-moses-ai):
every page under `content/` merged into six token-balanced Markdown bundles.

## Migrating to a plugin before 11 December 2026

OpenAI is retiring custom GPTs on **11 December 2026**. The GPT stops working and its
share link dies on that date; past conversations stay readable. Only Enterprise and
FedRAMP workspaces can request a deferral to 11 February 2027, which does not apply here.

MosesAI is the straightforward case: it has **no custom actions**, and custom actions are
the one thing migration cannot carry over. Instructions migrate as a skill and knowledge
files migrate as plugin reference files.

### Checklist

1. **Rebuild the bundles first** (see below). Migration copies whatever files the GPT
   currently holds, so refresh them before migrating rather than after — once migrated,
   the original GPT becomes read-only.
2. Upload the six rebuilt bundles to MosesAI, replacing the old ones.
3. In ChatGPT, open MosesAI and use the migrate prompt. It scans for anything that will
   not carry over, then takes about a minute.
4. **Test retrieval** in a new chat before 11 December, while the original GPT is still
   there to compare against. Ask something that can only be answered from a bundle — a
   specific daf, e.g. "What does Arachin 10 say about trumpets in the Temple?" Some
   builders report instructions and knowledge files being ignored after migration, so
   confirm it actually reads the files rather than answering from general knowledge.
5. **Re-share it.** Sharing does not carry over — the new plugin starts private, and
   people who used MosesAI get no access automatically. Making a personal plugin public
   requires a separate plugin submission, so start this early if public access matters.
6. Update the link in the root `README.md` once the plugin has a stable URL.

### If the migrator rejects the knowledge files

Builders have seen the migration tool reject files as "too large" even at ~2 MB, while the
same GPT happily held files over 10 MB. These bundles are ~0.83 MB and ~200k tokens each,
comfortably under the documented limits (20 files per GPT, 2M tokens per file), so they
should pass. If they are rejected anyway:

- Build the plugin manually and attach the bundles as reference files — that path has
  worked where the migrator refused.
- Or split them smaller: `./CustomGPT/make_bundles.py --budget 100000` yields roughly
  twice as many bundles at half the size. Staying at or under 20 files keeps within the
  documented per-GPT file cap.

## Rebuilding the bundles

```sh
./CustomGPT/make_bundles.py            # rebuild in place from ./content
./CustomGPT/make_bundles.py --help     # options: --content-dir, --out-dir, --budget
```

Run this after any crawl that changes `content/`. Outputs, all in
`talmud_merged_for_custom_gpt/`:

| File | Contents |
| --- | --- |
| `talmud_bundle_01..06.md` | the pages themselves, with a per-bundle table of contents |
| `all_files_inventory.csv` | every source page: size, char count, token estimate, tractate |
| `bundle_mapping.csv` | which bundle each page landed in, and in what order |
| `bundle_summary.csv` | per-bundle page count, token estimate, and size |

Pages are taken in lexicographic path order and packed greedily into bundles of at most
200,000 estimated tokens (4 characters per token, floored at 1). The script reproduces the
original September 2025 bundles byte-for-byte when run against the content of that commit,
so the only changes between rebuilds come from `content/` itself.

Two notes on the manifests. `size_mb` is the bundle file as written, which is what an
upload size check sees; the September 2025 manifest put the source pages' own bytes in
that column instead, understating each bundle by about 9% because it ignored the table of
contents and per-page headers. That older figure is kept alongside as `source_mb`. The
`daf_num` and `daf_side` columns in the inventory have never been populated.

## Current state

2,711 pages across 6 bundles, ~1.2M estimated tokens. 367 of those pages are placeholders
for dapim not yet written (see `doc/missing_pages.md` and `missing_pages.py`); they carry
no text and contribute nothing but a filename to the bundles.
