#!/usr/bin/env python3
"""Static port-scoping report for an Android Kotlin/Java source tree.

Answers "how much of this Android app can run on a plain desktop JVM?" without
building or running any of it. For every .kt/.java file under SRC_ROOT it records:

  * direct   - imports android.* / androidx.* / com.android.*, or declares JNI
               (`external fun` / `native`) code
  * tainted  - no direct Android use, but depends (transitively) on a direct file
  * pure     - neither; candidate to compile unchanged on desktop JVM

Dependencies come from explicit imports of in-tree classes, plus same-package
references found by matching other files' top-level declaration names as whole
words. That second step is a heuristic: it over-reports (a common name can match
by accident), so "pure" is a conservative lower bound.

The tree is only read as text. Nothing is compiled or executed. Stdlib only.

Usage:
  tools/port_scope.py SRC_ROOT [SRC_ROOT ...] [--json OUT.json] [--depth 5] [--shim]
                      [--seam-dir DIR]
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from pathlib import Path

NATIVE_DECL = re.compile(r"\bexternal\s+fun\b|\bnative\s+\w[\w<>\[\], ]*\s+\w+\s*\(")
PACKAGE = re.compile(r"^\s*package\s+([\w.]+)", re.M)
IMPORT = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)(?:\.\*)?", re.M)
# Top-level declarations: start of line, optional modifiers, then a keyword + name.
TOP_DECL = re.compile(
    r"^(?:(?:public|internal|private|protected|abstract|open|sealed|data|enum|"
    r"annotation|inline|value|final|static|const|expect|actual)\s+)*"
    r"(?:fun\s+interface|enum\s+class|class|object|interface|fun|val|var|typealias)\s+"
    r"(?:<[^>]*>\s*)?(?:[\w.]+\.)?([A-Za-z_]\w*)",
    re.M,
)
FQ_ANDROID = re.compile(r"(?<![\w.\"])(?:android|androidx|dalvik)\.[a-z]\w*(?:\.[A-Za-z_]\w*)+")
FQ_NAME = re.compile(r"(?<![\w.])[a-z]\w*(?:\.[a-z]\w*)*\.[A-Z]\w*")
KEYWORDS = {"interface", "class", "object", "fun", "val", "var", "companion", "data", "enum"}
# Android APIs a ~50-line desktop stub can stand in for (logging, clocks, build
# info, Base64, the XML pull parser). With --shim they stop counting as "direct".
DEFAULT_SHIM = ("android.util.Log", "android.os.Build", "android.os.SystemClock",
                "android.util.Base64", "android.util.Xml", "android.os.Process", "android.os.Process.", "android.annotation.",
                "androidx.annotation.")
MAX_FILE_BYTES = 4 * 1024 * 1024


class SourceFile:
    def __init__(self, path: Path, root: Path, text: str, shim: tuple[str, ...] = ()):
        self.path = path
        self.rel = str(path.relative_to(root))
        self.package = (PACKAGE.search(text) or [None, ""])[1]
        self.imports = IMPORT.findall(text)
        self.names = set(TOP_DECL.findall(text)) - KEYWORDS
        # Imports plus fully-qualified uses in code (e.g. android.os.Process.setThreadPriority).
        code = "\n".join(l for l in text.splitlines()
                         if not l.lstrip().startswith(("import ", "package ", "//", "*", "/*")))
        self.android_imports = sorted(
            {m.group(1) for m in re.finditer(
                r"^\s*import\s+((?:android|androidx|com\.android|dalvik)\.[\w.]+)", text, re.M)}
            | {m.group(0) for m in FQ_ANDROID.finditer(code)})
        self.native = bool(NATIVE_DECL.search(text))
        self.unshimmed = [a for a in self.android_imports if not a.startswith(shim)] if shim \
            else list(self.android_imports)
        self.direct = bool(self.unshimmed) or self.native
        self.lines = text.count("\n") + 1
        self.body = text
        self.deps: set[str] = set()
        self.replaced = False


def load(roots: list[Path], shim: tuple[str, ...] = ()) -> dict[str, SourceFile]:
    files: dict[str, SourceFile] = {}
    for root in roots:
        for p in sorted(root.rglob("*")):
            if p.suffix not in (".kt", ".java") or not p.is_file() or p.is_symlink():
                continue
            if p.stat().st_size > MAX_FILE_BYTES:
                continue
            sf = SourceFile(p, root, p.read_text(encoding="utf-8", errors="replace"), shim)
            files[f"{root.name}:{sf.rel}"] = sf
    return files


def link(files: dict[str, SourceFile]) -> None:
    by_fqn: dict[str, set[str]] = defaultdict(set)
    by_pkg: dict[str, list[str]] = defaultdict(list)
    for key, sf in files.items():
        by_pkg[sf.package].append(key)
        for n in sf.names:
            by_fqn[f"{sf.package}.{n}"].add(key)
    for key, sf in files.items():
        refs = list(sf.imports) + FQ_NAME.findall(sf.body)
        for imp in refs:
            # Nested imports (pkg.Outer.Inner) resolve to the file declaring pkg.Outer.
            parts = imp.split(".")
            for n in range(len(parts), 1, -1):
                hit = by_fqn.get(".".join(parts[:n]))
                if hit:
                    sf.deps.update(t for t in hit if t != key)
                    break
            # wildcard / package import
            for target in by_pkg.get(imp, ()):
                if target != key:
                    sf.deps.add(target)
        words = set(re.findall(r"\b[A-Za-z_]\w*\b", sf.body))
        for other in by_pkg[sf.package]:
            if other != key and files[other].names & words:
                sf.deps.add(other)


def classify(files: dict[str, SourceFile]) -> dict[str, str]:
    state = {k: ("direct" if f.direct else "pure" if f.replaced else None) for k, f in files.items()}
    changed = True
    while changed:  # fixed point: taint flows from dependency to dependant
        changed = False
        for k, f in files.items():
            if state[k] is None and not f.replaced and any(state[d] in ("direct", "tainted") for d in f.deps):
                state[k] = "tainted"
                changed = True
    return {k: (v or "pure") for k, v in state.items()}


def package_key(sf: SourceFile, depth: int) -> str:
    return ".".join(sf.package.split(".")[:depth]) or "(default)"


def report(files: dict[str, SourceFile], state: dict[str, str], depth: int) -> dict:
    pkgs: dict[str, dict] = defaultdict(lambda: {"pure": 0, "tainted": 0, "direct": 0,
                                                 "loc_pure": 0, "loc_total": 0})
    android_api: dict[str, int] = defaultdict(int)
    for k, sf in files.items():
        p = pkgs[package_key(sf, depth)]
        p[state[k]] += 1
        p["loc_total"] += sf.lines
        if state[k] == "pure":
            p["loc_pure"] += sf.lines
        for a in sf.android_imports:
            android_api[".".join(a.split(".")[:3])] += 1
    totals = {s: sum(1 for v in state.values() if v == s) for s in ("pure", "tainted", "direct")}
    return {
        "files": len(files),
        "totals": totals,
        "loc_total": sum(f.lines for f in files.values()),
        "loc_pure": sum(f.lines for k, f in files.items() if state[k] == "pure"),
        "packages": dict(sorted(pkgs.items())),
        "android_api_by_prefix": dict(sorted(android_api.items(), key=lambda kv: -kv[1])),
        "per_file": {k: {"state": state[k], "lines": files[k].lines, "native": files[k].native,
                         "android_imports": files[k].android_imports,
                         "blocking_imports": files[k].unshimmed}
                     for k in sorted(files)},
    }


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    ap.add_argument("roots", nargs="+", type=Path)
    ap.add_argument("--json", type=Path, help="write the full report here")
    ap.add_argument("--depth", type=int, default=5, help="package grouping depth")
    ap.add_argument("--seam-dir", type=Path,
                    help="dir of desktop rewrites; files at the same relative path count as pure")
    ap.add_argument("--shim", action="store_true",
                    help="assume a desktop stub for trivial APIs (Log, Build, SystemClock, Base64...)")
    a = ap.parse_args(argv)
    for r in a.roots:
        if not r.is_dir():
            print(f"not a directory: {r}", file=sys.stderr)
            return 2
    files = load(a.roots, DEFAULT_SHIM if a.shim else ())
    if a.seam_dir:
        seams = {str(p.relative_to(a.seam_dir)) for p in a.seam_dir.rglob("*.kt")}
        for sf in files.values():
            if sf.rel in seams:
                sf.direct = False
                sf.unshimmed = []
                sf.replaced = True
    link(files)
    state = classify(files)
    rep = report(files, state, a.depth)
    print(f"{rep['files']} files, {rep['loc_total']} lines; "
          f"pure={rep['totals']['pure']} tainted={rep['totals']['tainted']} "
          f"direct={rep['totals']['direct']} (pure lines {rep['loc_pure']})")
    print(f"{'package':60} pure taint direct  pureLOC/LOC")
    for name, p in rep["packages"].items():
        print(f"{name:60} {p['pure']:4} {p['tainted']:5} {p['direct']:6}  {p['loc_pure']}/{p['loc_total']}")
    if a.json:
        a.json.parent.mkdir(parents=True, exist_ok=True)
        a.json.write_text(json.dumps(rep, indent=1))
    return 0


if __name__ == "__main__":
    sys.exit(main())
