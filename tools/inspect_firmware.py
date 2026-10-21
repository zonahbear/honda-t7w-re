#!/usr/bin/env python3
"""Read-only first-pass inspector for untrusted firmware / update files.

Produces a JSON (or short text) report with:
  * size and hashes (MD5, SHA-1, SHA-256)
  * known magic signatures found anywhere in the file (offset + type)
  * Shannon entropy per block, to spot compressed/encrypted regions
  * printable strings matching Honda / Android / bootloader identifier patterns
  * a ZIP member listing if the file is a ZIP (Android OTA, Honda SwUpdate.mdt)

The input file is opened read-only and never modified. Nothing is executed.
Standard library only, so it runs before the venv exists.

Usage:
  tools/inspect_firmware.py FILE [--json OUT.json] [--block-size 65536]
                                 [--max-hits 200] [--no-strings]
"""
from __future__ import annotations

import argparse
import hashlib
import json
import logging
import math
import re
import sys
import zipfile
from pathlib import Path

log = logging.getLogger("inspect_firmware")

# (magic bytes, label). Kept to signatures that matter for Honda/Android/Tegra
# head units; binwalk is the broad tool, this is the fast, dependency-free one.
SIGNATURES: list[tuple[bytes, str]] = [
    (b"PK\x03\x04", "zip local file header"),
    (b"PK\x05\x06", "zip end of central directory"),
    (b"ANDROID!", "Android boot/recovery image"),
    (b"\x3a\xff\x26\xed", "Android sparse image"),
    (b"\x7fELF", "ELF executable/object"),
    (b"hsqs", "SquashFS (little-endian)"),
    (b"sqsh", "SquashFS (big-endian)"),
    (b"\x27\x05\x19\x56", "U-Boot uImage"),
    (b"UBI#", "UBI erase-counter header"),
    (b"\x1f\x8b\x08", "gzip"),
    (b"\xfd7zXZ\x00", "xz"),
    (b"BZh", "bzip2"),
    (b"\x5d\x00\x00", "lzma (heuristic)"),
    (b"\x28\xb5\x2f\xfd", "zstd"),
    (b"\x89LZO", "lzop"),
    (b"070701", "cpio newc"),
    (b"070707", "cpio odc"),
    (b"MZ", "DOS/PE header (heuristic)"),
    (b"-----BEGIN", "PEM block"),
    (b"\x30\x82", "DER/ASN.1 SEQUENCE (heuristic)"),
    (b"CE\x00\x00", "WinCE/NK image (heuristic)"),
    (b"B000FF\n", "WinCE B000FF image"),
    (b"META-INF/com/google/android", "Android OTA updater path"),
]
# ext2/3/4 superblock magic lives at offset 0x438 from the filesystem start.
EXT_MAGIC = b"\x53\xef"
EXT_MAGIC_OFFSET = 0x438

# Heuristic signatures are noisy; they are reported only at offset 0.
HEURISTIC = {"lzma (heuristic)", "DOS/PE header (heuristic)",
             "DER/ASN.1 SEQUENCE (heuristic)", "WinCE/NK image (heuristic)",
             "bzip2"}

# Patterns for identifiers worth flagging. The T7W ones come from the vehicle
# diagnostic menu (see docs/hardware.md).
INTERESTING = re.compile(
    rb"(H14M[ -A-Za-z0-9.]*|E200[0-9.]*|H42F10[0-9. ]*|35ZB[0-9]+|T7W[-A-Z0-9]*"
    rb"|DBOOT|SH-BOOT|daupdater|SwUpdate[0-9]?|HondaAppCenter[_A-Za-z0-9.]*"
    rb"|fujitsu[_-]?ten[._a-z]*|MY1[0-9][A-Z]{2,4}|tegra[0-9a-z_]*|nvidia"
    rb"|ro\.build\.[a-z.]+=?[^\x00\n]*|Android [0-9.]+|Linux version [^\x00\n]+"
    rb"|U-Boot [^\x00\n]+|QNX[^\x00\n]{0,40}|Windows CE[^\x00\n]{0,40}"
    rb"|BEGIN [A-Z ]*KEY)",
    re.IGNORECASE,
)
PRINTABLE = re.compile(rb"[\x20-\x7e]{6,}")


def file_hashes(path: Path, chunk: int = 1 << 20) -> dict[str, str]:
    hs = {"md5": hashlib.md5(), "sha1": hashlib.sha1(), "sha256": hashlib.sha256()}
    with path.open("rb") as fh:
        while buf := fh.read(chunk):
            for h in hs.values():
                h.update(buf)
    return {k: h.hexdigest() for k, h in hs.items()}


def entropy(buf: bytes) -> float:
    if not buf:
        return 0.0
    counts = [0] * 256
    for b in buf:
        counts[b] += 1
    n = len(buf)
    return -sum(c / n * math.log2(c / n) for c in counts if c)


def scan_signatures(data: bytes, max_hits: int) -> list[dict]:
    hits: list[dict] = []
    for magic, label in SIGNATURES:
        if label in HEURISTIC:
            if data.startswith(magic):
                hits.append({"offset": 0, "type": label})
            continue
        start = 0
        while len(hits) < max_hits:
            idx = data.find(magic, start)
            if idx < 0:
                break
            hits.append({"offset": idx, "type": label})
            start = idx + 1
    if data[EXT_MAGIC_OFFSET:EXT_MAGIC_OFFSET + 2] == EXT_MAGIC:
        hits.append({"offset": 0, "type": "ext2/3/4 filesystem"})
    hits.sort(key=lambda h: h["offset"])
    return hits[:max_hits]


def entropy_profile(data: bytes, block: int) -> dict:
    blocks = [round(entropy(data[i:i + block]), 3) for i in range(0, len(data), block)]
    high = sum(1 for e in blocks if e > 7.9)
    return {
        "block_size": block,
        "blocks": len(blocks),
        "mean": round(sum(blocks) / len(blocks), 3) if blocks else 0.0,
        "high_entropy_blocks": high,  # >7.9 bits/byte: compressed or encrypted
        "per_block": blocks,
    }


def interesting_strings(data: bytes, max_hits: int) -> list[dict]:
    out, seen = [], set()
    for m in PRINTABLE.finditer(data):
        s = m.group()
        if INTERESTING.search(s) and s not in seen:
            seen.add(s)
            out.append({"offset": m.start(), "text": s[:200].decode("ascii")})
            if len(out) >= max_hits:
                break
    return out


def zip_listing(path: Path, limit: int = 500) -> list[dict] | None:
    if not zipfile.is_zipfile(path):
        return None
    with zipfile.ZipFile(path) as zf:
        return [{"name": i.filename, "size": i.file_size, "compressed": i.compress_size,
                 "encrypted": bool(i.flag_bits & 0x1)} for i in zf.infolist()[:limit]]


def inspect(path: Path, block: int = 65536, max_hits: int = 200,
            strings: bool = True) -> dict:
    data = path.read_bytes()
    report = {
        "file": str(path),
        "size": len(data),
        "hashes": file_hashes(path),
        "signatures": scan_signatures(data, max_hits),
        "entropy": entropy_profile(data, block),
        "zip": zip_listing(path),
    }
    if strings:
        report["strings"] = interesting_strings(data, max_hits)
    return report


def summary(r: dict) -> str:
    lines = [f"{r['file']}  {r['size']} bytes",
             f"sha256 {r['hashes']['sha256']}",
             f"entropy mean {r['entropy']['mean']} bits/byte, "
             f"{r['entropy']['high_entropy_blocks']}/{r['entropy']['blocks']} blocks > 7.9"]
    sigs = r["signatures"]
    lines.append(f"signatures: {len(sigs)}")
    lines += [f"  0x{s['offset']:08x}  {s['type']}" for s in sigs[:25]]
    if r["zip"] is not None:
        lines.append(f"zip members: {len(r['zip'])}")
        lines += [f"  {m['name']}  {m['size']}" for m in r["zip"][:25]]
    for s in r.get("strings", [])[:25]:
        lines.append(f"  str 0x{s['offset']:08x}  {s['text']}")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("file", type=Path)
    ap.add_argument("--json", type=Path, help="write the full report here")
    ap.add_argument("--block-size", type=int, default=65536)
    ap.add_argument("--max-hits", type=int, default=200)
    ap.add_argument("--no-strings", action="store_true")
    ap.add_argument("-v", "--verbose", action="store_true")
    a = ap.parse_args(argv)
    logging.basicConfig(level=logging.DEBUG if a.verbose else logging.INFO,
                        format="%(levelname)s %(message)s")

    if not a.file.is_file():
        log.error("not a regular file: %s", a.file)
        return 2
    if a.block_size < 256:
        log.error("--block-size must be >= 256")
        return 2
    if a.json and a.json.resolve() == a.file.resolve():
        log.error("refusing to overwrite the input file")
        return 2

    report = inspect(a.file, a.block_size, a.max_hits, not a.no_strings)
    print(summary(report))
    if a.json:
        a.json.parent.mkdir(parents=True, exist_ok=True)
        a.json.write_text(json.dumps(report, indent=1))
        log.info("wrote %s", a.json)
    return 0


if __name__ == "__main__":
    sys.exit(main())
