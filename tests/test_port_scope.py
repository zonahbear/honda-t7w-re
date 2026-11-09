"""Unit tests for tools/port_scope.py on a synthetic Kotlin tree.

Run: python3 -m unittest discover -s tests -v
"""
import json
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent / "tools"))

import port_scope as ps  # noqa: E402

TREE = {
    "a/Pure.kt": "package a\n\nclass Pure { fun x() = 1 }\n",
    "a/UsesPure.kt": "package a\n\nobject UsesPure { val p = Pure() }\n",
    "a/Droid.kt": "package a\n\nimport android.content.Context\n\nclass Droid(val c: Context)\n",
    "b/NeedsDroid.kt": "package b\n\nimport a.Droid\n\nfun interface NeedsDroid { fun d(): Droid }\n",
    "b/Logs.kt": "package b\n\nimport android.util.Log\n\nobject Logs { fun l() = Log.i(\"t\", \"m\") }\n",
    "b/FqProcess.kt": "package b\n\nobject FqProcess { fun p() = android.os.Process.setThreadPriority(0) }\n",
    "b/Native.kt": "package b\n\nobject Native { external fun open(): Int }\n",
    "c/Nested.kt": "package c\n\nimport b.Outer.Inner\n\nclass Nested(val i: Inner)\n",
    "b/Outer.kt": "package b\n\nimport android.os.Looper\n\nclass Outer { class Inner }\n",
    "d/Fq.kt": "package d\n\nclass Fq { val x = a.Droid::class }\n",
}


class PortScopeTests(unittest.TestCase):
    def setUp(self):
        self.root = Path(tempfile.mkdtemp()) / "src"
        for rel, text in TREE.items():
            p = self.root / rel
            p.parent.mkdir(parents=True, exist_ok=True)
            p.write_text(text)

    def states(self, shim=(), seams=()):
        files = ps.load([self.root], shim)
        for sf in files.values():
            if sf.rel in seams:
                sf.direct, sf.unshimmed, sf.replaced = False, [], True
        ps.link(files)
        st = ps.classify(files)
        return {files[k].rel: v for k, v in st.items()}

    def test_classification_without_shim(self):
        st = self.states()
        self.assertEqual(st["a/Pure.kt"], "pure")
        self.assertEqual(st["a/UsesPure.kt"], "pure")
        self.assertEqual(st["a/Droid.kt"], "direct")
        self.assertEqual(st["b/NeedsDroid.kt"], "tainted")
        self.assertEqual(st["b/Logs.kt"], "direct")
        self.assertEqual(st["b/FqProcess.kt"], "direct")  # fully-qualified use, no import
        self.assertEqual(st["b/Native.kt"], "direct")     # JNI
        self.assertEqual(st["c/Nested.kt"], "tainted")    # nested import resolves to Outer.kt
        self.assertEqual(st["d/Fq.kt"], "tainted")        # fully-qualified in-tree reference

    def test_fun_interface_is_not_a_name(self):
        files = ps.load([self.root])
        names = {sf.rel: sf.names for sf in files.values()}
        self.assertIn("NeedsDroid", names["b/NeedsDroid.kt"])
        self.assertNotIn("interface", names["b/NeedsDroid.kt"])

    def test_shim_clears_trivial_apis(self):
        st = self.states(shim=ps.DEFAULT_SHIM)
        self.assertEqual(st["b/Logs.kt"], "pure")
        self.assertEqual(st["b/FqProcess.kt"], "pure")
        self.assertEqual(st["a/Droid.kt"], "direct")  # Context is not shimmable

    def test_seam_replacement_untaints_dependants(self):
        st = self.states(seams={"a/Droid.kt"})
        self.assertEqual(st["a/Droid.kt"], "pure")
        self.assertEqual(st["b/NeedsDroid.kt"], "pure")

    def test_cli_json(self):
        out = self.root.parent / "r.json"
        self.assertEqual(ps.main([str(self.root), "--shim", "--json", str(out)]), 0)
        rep = json.loads(out.read_text())
        self.assertEqual(rep["files"], len(TREE))
        self.assertEqual(sum(rep["totals"].values()), len(TREE))
        self.assertEqual(ps.main([str(self.root / "nope")]), 2)


if __name__ == "__main__":
    unittest.main()
