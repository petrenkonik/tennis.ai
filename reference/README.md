# reference/ — reverse engineering of the original (read-only)

Reverse-engineering materials for `com.tennis.ai` v2.0.19. This directory **is not built**
and is not part of the deliverable; it is the source of knowledge for porting.

## Contents

| Path | What it is |
|---|---|
| `ANALYSIS_FINDINGS.md` | Main report: stack, backend, analysis pipeline, phases M0–M6, thresholds, corrections, drills, Axel, quotas |
| `DECISIONS.md` | Locked-in rebuild decisions (TMA + Nest.js + Mongo + Stars stack, structure, video pipeline) |
| `apks/com.tennis.ai_v2.0.19.apks.apk` | Original App Bundle (243 MB) |
| `apks/apks_extracted/` | Unpacked bundle: base.apk + split_config.arm64_v8a.apk |
| `extracted_assets/` | Dart AOT analysis artifacts: `libapp_strings.txt` (29,620 lines — the primary source), `long_strings.txt`, `all_identifiers.txt`, translations, YOLO models, `grep_paths.py` |
| `rea-outputs/` | Raw REA JSON outputs (`pkg_evidence.json`, classifications) |

## Key identifiers

- REA Evidence: `ev_7f8b10ab273ceac2442806b5363b4ca749f9d4b8539ad360c34b927ccf5dd516`
- base.apk SHA-256: `bb423559aaf67256638694641d81a279c3e386b6bf805b0cd6de2338bc1478e3`
- Tools: REA CLI 6.5.0 + jadx-headless-mcp 0.7.1 (JAR SHA-256 `6e5eacf500b64292bfb73c49797c1958f6ee44646e43e868039ae7feb573ff75`)

## What to port next

For the list of Dart services → target modules, see [../../docs/PORTING.md](../../docs/PORTING.md).
