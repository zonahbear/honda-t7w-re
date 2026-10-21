# analysis/

Working output from analysis runs. **Not committed** except this file.

Layout convention: `analysis/<first 12 chars of input sha256>/`
- `inspect.json` from `tools/inspect_firmware.py`
- `extracted/` from `tools/extract_firmware.py`
- `binwalk.txt`, `notes.md`, Ghidra project paths, etc.

Durable conclusions belong in `docs/` (with the sha256 they came from), not here.
