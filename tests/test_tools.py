"""Unit tests for tools/ using synthetic inputs (no real firmware needed).

Run: python3 -m unittest discover -s tests -v
"""
import io
import json
import sys
import tarfile
import tempfile
import unittest
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "tools"))

import extract_firmware as ex  # noqa: E402
import inspect_firmware as insp  # noqa: E402


class InspectTests(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())

    def test_entropy_bounds(self):
        self.assertEqual(insp.entropy(b"\x00" * 1000), 0.0)
        self.assertAlmostEqual(insp.entropy(bytes(range(256)) * 4), 8.0, places=3)

    def test_signatures_and_strings(self):
        blob = (b"\x00" * 16 + b"ANDROID!" + b"\x00" * 32 + b"\x7fELF"
                + b"\x00DBOOT H14M DA US 3.26\x00" + b"Linux version 3.1.10\x00")
        f = self.tmp / "img.bin"
        f.write_bytes(blob)
        r = insp.inspect(f, block=256)
        types = {s["type"]: s["offset"] for s in r["signatures"]}
        self.assertEqual(types["Android boot/recovery image"], 16)
        self.assertIn("ELF executable/object", types)
        texts = " ".join(s["text"] for s in r["strings"])
        self.assertIn("H14M", texts)
        self.assertIn("Linux version", texts)

    def test_heuristic_only_at_offset_zero(self):
        f = self.tmp / "x.bin"
        f.write_bytes(b"\x00\x00MZ\x00\x00")
        labels = [s["type"] for s in insp.inspect(f, block=256)["signatures"]]
        self.assertNotIn("DOS/PE header (heuristic)", labels)

    def test_zip_listing_and_input_untouched(self):
        f = self.tmp / "update.zip"
        with zipfile.ZipFile(f, "w") as z:
            z.writestr("META-INF/com/google/android/updater-script", "ui_print(\"x\");")
        before = f.read_bytes()
        r = insp.inspect(f, block=256)
        self.assertEqual(r["zip"][0]["name"], "META-INF/com/google/android/updater-script")
        self.assertEqual(before, f.read_bytes())

    def test_cli_refuses_json_over_input(self):
        f = self.tmp / "a.bin"
        f.write_bytes(b"abc")
        self.assertEqual(insp.main([str(f), "--json", str(f)]), 2)

    def test_cli_writes_json(self):
        f = self.tmp / "a.bin"
        f.write_bytes(b"\x1f\x8b\x08" + b"\x00" * 300)
        out = self.tmp / "r" / "a.json"
        self.assertEqual(insp.main([str(f), "--json", str(out)]), 0)
        self.assertEqual(json.loads(out.read_text())["size"], 303)


class ExtractTests(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())

    def test_zip_skips_traversal(self):
        f = self.tmp / "evil.zip"
        with zipfile.ZipFile(f, "w") as z:
            z.writestr("ok/a.txt", "a")
            z.writestr("../escape.txt", "x")
            z.writestr("/abs.txt", "x")
        out = self.tmp / "out"
        self.assertEqual(ex.main([str(f), str(out)]), 0)
        self.assertTrue((out / "ok/a.txt").is_file())
        self.assertFalse((self.tmp / "escape.txt").exists())

    def test_refuses_existing_outdir(self):
        f = self.tmp / "a.zip"
        with zipfile.ZipFile(f, "w") as z:
            z.writestr("a", "a")
        (self.tmp / "out").mkdir()
        self.assertEqual(ex.main([str(f), str(self.tmp / "out")]), 2)

    def test_size_cap(self):
        f = self.tmp / "big.zip"
        with zipfile.ZipFile(f, "w", zipfile.ZIP_DEFLATED) as z:
            z.writestr("zeros", b"\x00" * (2 << 20))
        self.assertEqual(ex.main([str(f), str(self.tmp / "o"), "--max-total-mb", "1"]), 1)

    def test_tar_skips_symlink(self):
        f = self.tmp / "t.tar"
        with tarfile.open(f, "w") as t:
            data = b"hello"
            ti = tarfile.TarInfo("d/file")
            ti.size = len(data)
            t.addfile(ti, io.BytesIO(data))
            link = tarfile.TarInfo("d/link")
            link.type = tarfile.SYMTYPE
            link.linkname = "/etc/passwd"
            t.addfile(link)
        out = self.tmp / "o"
        self.assertEqual(ex.main([str(f), str(out)]), 0)
        self.assertTrue((out / "d/file").is_file())
        self.assertFalse((out / "d/link").exists())

    def test_unknown_format(self):
        f = self.tmp / "raw.bin"
        f.write_bytes(b"\x00" * 64)
        self.assertEqual(ex.main([str(f), str(self.tmp / "o")]), 1)


if __name__ == "__main__":
    unittest.main()
