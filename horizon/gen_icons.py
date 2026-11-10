#!/usr/bin/env python3
"""Convertit les icônes de ../icones/png en données Rust pour la version Horizon.

Chaque icône 16x16 devient une liste de segments horizontaux (x, y, largeur, ARGB)
de même couleur, que le mod dessine avec `native_fill` (Horizon n'offre qu'un seul
emplacement d'image par mod, pas assez pour 16 icônes).

Usage : python3 gen_icons.py   (écrit mixins/grade_menu/icons.rs)
"""
from pathlib import Path

from PIL import Image

ICI = Path(__file__).resolve().parent
PNG = ICI.parent / "icones" / "png"
SORTIE = ICI / "mixins" / "grade_menu" / "icons.rs"


def segments(image):
    img = image.convert("RGBA")
    runs = []
    for y in range(img.height):
        x = 0
        while x < img.width:
            r, g, b, a = img.getpixel((x, y))
            if a == 0:
                x += 1
                continue
            start, color = x, (r, g, b, a)
            while x < img.width and img.getpixel((x, y)) == color:
                x += 1
            argb = (a << 24) | (r << 16) | (g << 8) | b
            runs.append((start, y, x - start, argb))
    return runs


def main():
    lignes = [
        "// Généré par horizon/gen_icons.py depuis icones/png : ne pas modifier à la main.",
        "// Les icônes 16x16 du mod, en segments (x, y, largeur, couleur ARGB) dessinés avec `native_fill`.",
        "",
        "pub(crate) fn runs(name: &str) -> &'static [(u8, u8, u8, u32)] {",
        "    match name {",
    ]
    for png in sorted(PNG.glob("*.png")):
        runs = segments(Image.open(png))
        corps = ", ".join(f"({x}, {y}, {w}, 0x{c:08X})" for x, y, w, c in runs)
        lignes.append(f'        "{png.stem}" => &[{corps}],')
    lignes += ["        _ => &[],", "    }", "}", ""]
    SORTIE.write_text("\n".join(lignes))
    print(f"{SORTIE} ({len(list(PNG.glob('*.png')))} icônes)")


if __name__ == "__main__":
    main()
