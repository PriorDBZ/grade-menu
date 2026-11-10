#!/usr/bin/env python3
"""Emballe la version Horizon dans grade_menu.hnmod : horizon-native.json et les
fichiers .rs / .toml / LICENSE de mixins/grade_menu/, comme les .hnmod du registre."""
import pathlib, sys, zipfile

root = pathlib.Path(__file__).resolve().parent
out = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else root / "grade_menu.hnmod"
files = [root / "horizon-native.json"] + sorted(
    p for p in (root / "mixins" / "grade_menu").iterdir()
    if p.suffix in (".rs", ".toml") or p.name.startswith("LICENSE")
)
with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
    for p in files:
        info = zipfile.ZipInfo(p.relative_to(root).as_posix(), (2026, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        info.external_attr = 0o644 << 16
        z.writestr(info, p.read_bytes())
print(out)
