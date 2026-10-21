# honda-t7w-re

Research into native Apple CarPlay and Android Auto on the factory **T7W** Display Audio unit of a US 2016 Honda HR-V EX, without replacing the head unit.

**Status:** early research. No firmware acquired, nothing flashed. Start with [CLAUDE.md](CLAUDE.md).

```bash
scripts/setup_cachyos.sh --check     # see what's missing (changes nothing)
scripts/setup_cachyos.sh             # install tools + .venv
python3 -m unittest discover -s tests
tools/inspect_firmware.py <file> --json analysis/<sha12>/inspect.json
```

Safety: this car is a daily driver. Read [docs/recovery.md](docs/recovery.md) before touching it.
Firmware binaries are never committed.
