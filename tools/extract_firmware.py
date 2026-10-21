#!/usr/bin/env python3
"""Safely unpack container formats found in update files, into a new directory.

Handles the containers the related Honda platforms are known to use
(ZIP for Android OTA / SwUpdate.mdt, see docs/firmware.md), plus tar and
gzip. Anything else: use binwalk, and record what it found.

Safety rules:
  * the input is opened read-only and never modified
  * the output directory must not exist yet (no silent overwrite)
  * members with absolute paths, '..' components or links are skipped
  * a total-size cap guards against decompression bombs
  * nothing extracted is executed or made executable

Usage:
  tools/extract_firmware.py FILE OUTDIR [--max-total-mb 4096] [--dry-run]
"""
from __future__ import annotations

import argparse
import gzip
import logging
import shutil
import sys
import tarfile
import zipfile
from pathlib import Path, PurePosixPath

log = logging.getLogger("extract_firmware")


def safe_member(name: str) -> PurePosixPath | None:
    p = PurePosixPath(name.replace("\\", "/"))
    if p.is_absolute() or ".." in p.parts or not p.parts:
        return None
    return p


def detect(path: Path) -> str | None:
    if zipfile.is_zipfile(path):
        return "zip"
    if tarfile.is_tarfile(path):
        return "tar"
    with path.open("rb") as fh:
        if fh.read(2) == b"\x1f\x8b":
            return "gzip"
    return None


def extract_zip(src: Path, out: Path, cap: int, dry: bool) -> list[str]:
    done, total = [], 0
    with zipfile.ZipFile(src) as zf:
        for info in zf.infolist():
            rel = safe_member(info.filename)
            if rel is None:
                log.warning("skipping unsafe member %r", info.filename)
                continue
            if info.is_dir():
                continue
            if info.flag_bits & 0x1:
                log.warning("skipping encrypted member %s", rel)
                continue
            total += info.file_size
            if total > cap:
                raise RuntimeError("size cap exceeded; raise --max-total-mb if expected")
            done.append(str(rel))
            if dry:
                continue
            dest = out / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            with zf.open(info) as s, dest.open("wb") as d:
                shutil.copyfileobj(s, d)
    return done


def extract_tar(src: Path, out: Path, cap: int, dry: bool) -> list[str]:
    done, total = [], 0
    with tarfile.open(src) as tf:
        for m in tf.getmembers():
            rel = safe_member(m.name)
            if rel is None or not m.isfile():
                if not m.isdir():
                    log.warning("skipping unsafe or non-regular member %r", m.name)
                continue
            total += m.size
            if total > cap:
                raise RuntimeError("size cap exceeded; raise --max-total-mb if expected")
            done.append(str(rel))
            if dry:
                continue
            dest = out / rel
            dest.parent.mkdir(parents=True, exist_ok=True)
            with tf.extractfile(m) as s, dest.open("wb") as d:
                shutil.copyfileobj(s, d)
    return done


def extract_gzip(src: Path, out: Path, cap: int, dry: bool) -> list[str]:
    name = src.name[:-3] if src.name.endswith(".gz") else src.name + ".out"
    if dry:
        return [name]
    out.mkdir(parents=True, exist_ok=True)
    written = 0
    with gzip.open(src, "rb") as s, (out / name).open("wb") as d:
        while buf := s.read(1 << 20):
            written += len(buf)
            if written > cap:
                raise RuntimeError("size cap exceeded; raise --max-total-mb if expected")
            d.write(buf)
    return [name]


HANDLERS = {"zip": extract_zip, "tar": extract_tar, "gzip": extract_gzip}


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("file", type=Path)
    ap.add_argument("outdir", type=Path)
    ap.add_argument("--max-total-mb", type=int, default=4096)
    ap.add_argument("--dry-run", action="store_true", help="list, do not write")
    a = ap.parse_args(argv)
    logging.basicConfig(level=logging.INFO, format="%(levelname)s %(message)s")

    if not a.file.is_file():
        log.error("not a regular file: %s", a.file)
        return 2
    if a.outdir.exists() and not a.dry_run:
        log.error("output directory already exists: %s", a.outdir)
        return 2
    if a.outdir.resolve() in a.file.resolve().parents:
        log.error("output directory must not contain the input file")
        return 2

    kind = detect(a.file)
    if kind is None:
        log.error("no supported container at offset 0; try: binwalk %s", a.file)
        return 1
    log.info("detected %s", kind)
    if not a.dry_run:
        a.outdir.mkdir(parents=True)
    try:
        members = HANDLERS[kind](a.file, a.outdir, a.max_total_mb << 20, a.dry_run)
    except (RuntimeError, zipfile.BadZipFile, tarfile.TarError, OSError) as e:
        log.error("%s", e)
        return 1
    for m in members:
        print(m)
    log.info("%s %d members", "would extract" if a.dry_run else "extracted", len(members))
    return 0


if __name__ == "__main__":
    sys.exit(main())
