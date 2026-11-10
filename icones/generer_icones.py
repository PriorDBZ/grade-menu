#!/usr/bin/env python3
"""Génère les icônes 16x16 du mod Grade Menu et une planche d'aperçu.

Chaque icône est dessinée à la main sous forme de grille de caractères ;
une seule palette est partagée par toutes les icônes pour garder un style
cohérent : contour sombre, remplissage gris clair, une touche dorée.

Usage : python3 generer_icones.py   (écrit png/*.png et apercu.png)
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ICI = Path(__file__).resolve().parent
TAILLE = 16

PALETTE = {
    ".": None,
    "K": (31, 31, 36),     # contour
    "D": (75, 75, 85),     # ombre foncée
    "M": (142, 142, 154),  # ombre moyenne
    "L": (228, 228, 234),  # remplissage clair
    "W": (255, 255, 255),  # reflet
    "Y": (247, 214, 120),  # accent doré clair
    "A": (232, 178, 58),   # accent doré
    "a": (169, 119, 28),   # accent doré ombré
    "R": (196, 72, 60),    # rouge (suppression uniquement)
    "r": (138, 43, 36),    # rouge ombré
}

# --- Grade Confirmé·e ------------------------------------------------------

HAT = """
....KKKKKKKK....
....KLLLLLLK....
....KLLLLLMK....
....KLLLLLMK....
....KLLLLLMK....
....KLLLLLMK....
....KAAAAAaK....
....KaaaaaaK....
.KKKKMMMMMMKKKK.
.KLLLLLLLLLLLMK.
..KKKKKKKKKKKK..
"""

CRAFT = """
.KKKKKKKKKKK.
KLLLKLLLKLLLK
KLLLKLLLKLLLK
KLLMKLLMKLLMK
KKKKKKKKKKKKK
KLLLKAAAKLLLK
KLLLKAAAKLLLK
KLLMKAAaKLLMK
KKKKKKKKKKKKK
KLLLKLLLKLLLK
KLLLKLLLKLLLK
KLLMKLLMKLLMK
.KKKKKKKKKKK.
"""

BED = """
KKK...........
KDK...........
KDKKKKK.......
KDKLLLKKKKKKKK
KDKLLLKAAAAAAK
KDKKKKKaaaaaaK
KDKLLLLLLLLLLK
KDKMMMMMMMMMMK
KKKKKKKKKKKKKK
KK..........KK
"""

# --- Grade Premium -----------------------------------------------------------

BLOCKS = """
......KK......
....KKYYKK....
..KKYYYYYYKK..
KKYYYYYYYYYYKK
KAKKYYYYYYKKaK
KAAAKKYYKKaaaK
KAAAAAKKaaaaaK
KAAAAAAaaaaaaK
KAAAAAAaaaaaaK
KAAAAAAaaaaaaK
KKAAAAAaaaaaKK
..KKAAAaaaKK..
....KKAaKK....
......KK......
"""

MAP = """
KKKKK....KKKKK
KLLLLKKKKLLLLK
KLLLLMMMMLLLLK
KLLLLMMMMLaaLK
KLLLLMMMMaAAaK
KLLLLMMMMaAAaK
KLLLLMMDDLaaLK
KLLLLMMMMLLLLK
KLLLLDDMMLLLLK
KLLLLMMMMLLLLK
KLDDLMMMMLLLLK
KKKKKMMMMKKKKK
.....KKKK.....
"""

BANNER = """
KKKKKKKKKKKKKK
KDDDDDDDDDDDDK
KKKKKKKKKKKKKK
..KLLLLLLLMK..
..KLLLLLLLMK..
..KLLLLLLLMK..
..KAAAAAAAaK..
..KaaaaaaaaK..
..KLLLLLLLMK..
..KLLLLLLLMK..
..KLLLLLLLMK..
..KLLLKKLLMK..
..KLLK..KLMK..
..KKK....KKK..
"""

ANVIL = """
..KKKKKKKKKKKK
KKLLLLLLLLLLMK
.KKMMMMMMMMMDK
...KKKMMMKKKKK
.....KMMMK....
.....KMMMK....
....KKMMMKK...
..KKKLLLLLKKK.
..KLLLLLLLLLK.
..KMMMMMMMMMK.
..KKKKKKKKKKK.
"""

SAW = """
......KK......
...KK.KLK.....
..KLKKLLKKKK..
..KLLLLLLLLK..
.KKLLLLLLLLKKK
KLLLLKKKKLLLMK
.KLLLKAAKLLLK.
.KLLLKAaKLLLK.
KMLLLKKKKLLLLK
KKKLLLLLLLLMKK.
..KMLLLLLLMK..
..KKKKMMKKMK..
.....KMK.KK...
......KK......
"""

HOUSE = """
......KK......
.....KDDK.....
....KDDDDK....
...KDDDDDDK...
..KDDDDDDDDK..
.KKKKKKKKKKKK.
..KLLLLLLLLK..
..KLLLLLLLMK..
..KLKKKKDDMK..
..KLKAAKDDMK..
..KLKAAKLLMK..
..KLKAaKLLMK..
..KKKKKKKKKK..
"""

HEAD = """
KKKKKKKKKKKK
KDDDDDDDDDDK
KDDLLLLLLDDK
KLLLLLLLLLMK
KLKKLLLLKKMK
KLKKLLLLKKMK
KLLLLLLLLLMK
KLLLKKKKLLMK
KLLLLLLLLLMK
KMMMMMMMMMMK
KKKKKKKKKKKK
"""

SPAWN = """
..KKKKKK..
.KYYAAAAK.
KYAAKKAAaK
KAAKLLKAaK
KAAKLLKAaK
KAAAKKAAaK
.KAAAAAaK.
.KAAAAaaK.
..KAAaaK..
...KAaK...
....KK....
..........
.KKMMMMKK.
...KKKK...
"""

# --- Menu des homes ----------------------------------------------------------

PLUS = """
....KKKK....
....KAAK....
....KAAK....
....KAAK....
KKKKKAAKKKKK
KAAAAAAAAAAK
KaaaaAAaaaaK
KKKKKAaKKKKK
....KAaK....
....KAaK....
....KaaK....
....KKKK....
"""

PENCIL = """
.........KKK.
........KAAAK
.......KDAAaK
......KLLDDK.
.....KLLMDK..
....KLLMMK...
...KLLMMK....
..KLLMMK.....
.KKLMMK......
.KDKMK.......
KDDKK........
KKK..........
"""

CROSS = """
KKK......KKK
KRRK....KRrK
KRRRK..KRrrK
.KRRRKKRRrK.
..KRRRRRrK..
...KRRRrK...
...KRRRrK...
..KRRRKRrrK.
.KRRrKKKRrrK
KRRrK..KRrrK
KRrK....KrrK
KKK......KKK
"""


PREV = """
....KKKK
...KDDK.
..KDDK..
.KDDK...
KDDK....
KDDK....
.KDDK...
..KDDK..
...KDDK.
....KKKK
"""

NEXT = "\n".join(ligne[::-1] for ligne in PREV.strip("\n").splitlines())


# --- Icônes générées par formule ---------------------------------------------

def _contourer(remplissage, n):
    """remplissage(x, y) -> caractère ou None ; le bord de la forme devient 'K'."""
    def dedans(x, y):
        return 0 <= x < n and 0 <= y < n and remplissage(x, y) is not None
    rangs = []
    for y in range(n):
        rang = ""
        for x in range(n):
            if not dedans(x, y):
                rang += "."
            elif not all(dedans(x + dx, y + dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                rang += "K"
            else:
                rang += remplissage(x, y)
        rangs.append(rang)
    return "\n".join(rangs)


def _scie():
    """Lame de scie circulaire à 8 dents crochues (suréchantillonnée 4x4)."""
    import math
    n, c, ss = 14, 7.0, 4
    def couverture(x, y):
        k = 0
        for i in range(ss):
            for j in range(ss):
                dx, dy = x + (i + .5) / ss - c, y + (j + .5) / ss - c
                angle = (math.atan2(dy, dx) / (2 * math.pi)) % 1
                k += math.hypot(dx, dy) <= 4.4 + 2.6 * ((angle * 8) % 1)
        return k / ss / ss
    def f(x, y):
        if couverture(x, y) < 0.5:
            return None
        dx, dy = x + .5 - c, y + .5 - c
        dist = math.hypot(dx, dy)
        if dist < 1.0:
            return "A"
        if dist < 2.2:
            return "K"
        return "M" if dist > 3.4 and dx + dy > 1.5 else "L"
    return _contourer(f, n)


def _croix():
    """Croix rouge : deux barres diagonales, ombre sur le bas de la seconde."""
    n = 12
    def f(x, y):
        if abs(x + y - (n - 1)) <= 2 and x + y - (n - 1) == 1 and abs(x - y) > 1:
            return "r"
        if abs(x - y) <= 2 or abs(x + y - (n - 1)) <= 2:
            return "R"
        return None
    return _contourer(f, n)


SAW = _scie()
CROSS = _croix()


ICONES = [
    # (nom de fichier, grille, libellé, groupe)
    ("hat", HAT, "/hat", "Confirmé·e"),
    ("craft", CRAFT, "/craft", "Confirmé·e"),
    ("home", BED, "/home", "Confirmé·e"),
    ("blocks", BLOCKS, "/blocks", "Premium"),
    ("cartographytable", MAP, "/cartographytable", "Premium"),
    ("loom", BANNER, "/loom", "Premium"),
    ("smithingtable", ANVIL, "/smithingtable", "Premium"),
    ("stonecutter", SAW, "/stonecutter", "Premium"),
    ("homes", HOUSE, "Menu des homes", "Premium"),
    ("skull", HEAD, "/skull", "Premium"),
    ("spawn", SPAWN, "/spawn", "Premium"),
    ("home_add", PLUS, "Nouveau home", "Menu des homes"),
    ("home_rename", PENCIL, "Renommer", "Menu des homes"),
    ("home_delete", CROSS, "Supprimer", "Menu des homes"),
    ("page_prev", PREV, "Page préc.", "Menu des homes"),
    ("page_next", NEXT, "Page suiv.", "Menu des homes"),
]


def lignes(grille):
    rangs = [l for l in grille.strip("\n").splitlines()]
    largeur = max(len(l) for l in rangs)
    return [l.ljust(largeur, ".") for l in rangs]


def dessiner(grille):
    """Rend une grille en image 16x16, centrée sur sa boîte englobante."""
    rangs = lignes(grille)
    pts = [(x, y, c) for y, l in enumerate(rangs) for x, c in enumerate(l) if PALETTE[c]]
    x0 = min(p[0] for p in pts); x1 = max(p[0] for p in pts)
    y0 = min(p[1] for p in pts); y1 = max(p[1] for p in pts)
    w, h = x1 - x0 + 1, y1 - y0 + 1
    assert w <= TAILLE and h <= TAILLE, f"icône trop grande : {w}x{h}"
    dx = (TAILLE - w) // 2 - x0
    dy = (TAILLE - h + 1) // 2 - y0
    img = Image.new("RGBA", (TAILLE, TAILLE), (0, 0, 0, 0))
    for x, y, c in pts:
        img.putpixel((x + dx, y + dy), PALETTE[c] + (255,))
    return img


# --- Planche d'aperçu --------------------------------------------------------

FOND_INV = (198, 198, 198)


def bouton(taille, enfonce=False):
    """Bouton façon interface Minecraft (relief clair / sombre, contour noir)."""
    b = Image.new("RGBA", (taille, taille), (0, 0, 0, 0))
    d = ImageDraw.Draw(b)
    d.rectangle([0, 0, taille - 1, taille - 1], fill=(0, 0, 0))
    haut, bas = ((85, 85, 85), (255, 255, 255)) if enfonce else ((255, 255, 255), (85, 85, 85))
    d.rectangle([1, 1, taille - 2, taille - 2], fill=bas)
    d.rectangle([1, 1, taille - 3, taille - 3], fill=haut)
    d.rectangle([2, 2, taille - 3, taille - 3], fill=FOND_INV if not enfonce else (170, 170, 170))
    for x, y in [(0, 0), (taille - 1, 0), (0, taille - 1), (taille - 1, taille - 1)]:
        b.putpixel((x, y), (0, 0, 0, 0))
    return b


def _inventaire():
    """Fond d'inventaire simplifié façon Minecraft (176x166), 3 rangées + barre."""
    inv_w, inv_h = 176, 166
    inv = Image.new("RGBA", (inv_w, inv_h), (0, 0, 0, 0))
    di = ImageDraw.Draw(inv)
    di.rounded_rectangle([0, 0, inv_w - 1, inv_h - 1], radius=3, fill=(0, 0, 0))
    di.rectangle([1, 1, inv_w - 2, inv_h - 2], fill=(85, 85, 85))
    di.rectangle([1, 1, inv_w - 3, inv_h - 3], fill=(255, 255, 255))
    di.rectangle([3, 3, inv_w - 4, inv_h - 4], fill=FOND_INV)
    def case(sx, sy, taille=18):
        di.rectangle([sx, sy, sx + taille - 1, sy + taille - 1], fill=(255, 255, 255))
        di.rectangle([sx, sy, sx + taille - 2, sy + taille - 2], fill=(55, 55, 55))
        di.rectangle([sx + 1, sy + 1, sx + taille - 2, sy + taille - 2], fill=(139, 139, 139))
    for r in range(4):
        case(7, 7 + r * 18)                       # armure
    di.rectangle([25, 7, 75, 78], fill=(0, 0, 0))  # cadre du joueur
    case(76, 61)                                  # main secondaire
    for r in range(2):
        for c in range(2):
            case(97 + c * 18, 17 + r * 18)        # artisanat 2x2
    case(153, 27)
    for r in range(3):
        for c in range(9):
            case(7 + c * 18, 83 + r * 18)
    for c in range(9):
        case(7 + c * 18, 141)
    return inv


def apercu(images):
    police = ImageFont.truetype("/usr/share/fonts/noto/NotoSans-Regular.ttf", 15)
    police_titre = ImageFont.truetype("/usr/share/fonts/noto/NotoSans-Bold.ttf", 20)
    grand = 6          # zoom de la vignette détaillée
    case_l, case_h = 150, 160
    par_ligne = 6

    groupes = []
    for nom, _, libelle, groupe in ICONES:
        if not groupes or groupes[-1][0] != groupe:
            groupes.append((groupe, []))
        groupes[-1][1].append((nom, libelle))

    e = 2  # échelle d'interface pour la mise en situation
    hauteur = 20
    for _, elems in groupes:
        hauteur += 40 + case_h * ((len(elems) + par_ligne - 1) // par_ligne)
    hauteur += 40 + 166 * e + 30
    largeur = 20 + case_l * par_ligne + 20

    planche = Image.new("RGBA", (largeur, hauteur), (44, 44, 50, 255))
    d = ImageDraw.Draw(planche)
    y = 20
    for groupe, elems in groupes:
        d.text((20, y), groupe, font=police_titre, fill=(240, 240, 240))
        y += 40
        for i, (nom, libelle) in enumerate(elems):
            cx = 20 + (i % par_ligne) * case_l
            cy = y + (i // par_ligne) * case_h
            v = 16 * grand + 12
            gx = cx + (case_l - v) // 2
            d.rectangle([gx - 2, cy - 2, gx + v + 1, cy + v + 1], fill=(0, 0, 0))
            d.rectangle([gx, cy, gx + v - 1, cy + v - 1], fill=FOND_INV)
            gros = images[nom].resize((16 * grand, 16 * grand), Image.NEAREST)
            planche.alpha_composite(gros, (gx + 6, cy + 6))
            tw = d.textlength(libelle, font=police)
            d.text((cx + (case_l - tw) / 2, cy + v + 8), libelle, font=police, fill=(225, 225, 225))
        y += case_h * ((len(elems) + par_ligne - 1) // par_ligne)

    # mise en situation : boutons à côté de l'inventaire, taille réelle (échelle 2)
    d.text((20, y), f"En situation, taille réelle (interface échelle {e})", font=police_titre,
           fill=(240, 240, 240))
    y += 40
    inv = _inventaire()
    noms = [n for n, _, _, gr in ICONES if gr != "Menu des homes"]
    col = Image.new("RGBA", (2 * 21, 6 * 21), (0, 0, 0, 0))
    for i, nom in enumerate(noms):
        bt = bouton(20)
        bt.alpha_composite(images[nom], (2, 2))
        col.alpha_composite(bt, ((i // 6) * 21, (i % 6) * 21))
    ligne = Image.new("RGBA", (5 * 21, 20), (0, 0, 0, 0))
    for i, nom in enumerate(n for n, _, _, gr in ICONES if gr == "Menu des homes"):
        bt = bouton(20)
        bt.alpha_composite(images[nom], (2, 2))
        ligne.alpha_composite(bt, (i * 21, 0))
    ox = 30
    planche.alpha_composite(inv.resize((inv.width * e, inv.height * e), Image.NEAREST), (ox, y))
    cx = ox + inv.width * e + 10
    planche.alpha_composite(col.resize((col.width * e, col.height * e), Image.NEAREST), (cx, y))
    lx = cx + col.width * e + 40
    d.text((lx, y), "Menu des homes :", font=police, fill=(225, 225, 225))
    planche.alpha_composite(ligne.resize((ligne.width * e, ligne.height * e), Image.NEAREST),
                            (lx, y + 28))
    return planche


# --- Éléments d'interface (boutons, panneau, lignes) en « nine-slice » --------

def _mcmeta(largeur, hauteur, bord):
    import json
    return json.dumps({"gui": {"scaling": {"type": "nine_slice", "width": largeur,
                                           "height": hauteur, "border": bord}}}, indent=2) + "\n"


def _fond_bouton(remplissage):
    b = bouton(20)
    ImageDraw.Draw(b).rectangle([2, 2, 17, 17], fill=remplissage)
    return b


def _panneau(taille=32):
    p = Image.new("RGBA", (taille, taille), (0, 0, 0, 0))
    d = ImageDraw.Draw(p)
    d.rounded_rectangle([0, 0, taille - 1, taille - 1], radius=3, fill=(0, 0, 0))
    d.rectangle([1, 1, taille - 2, taille - 2], fill=(85, 85, 85))
    d.rectangle([1, 1, taille - 3, taille - 3], fill=(255, 255, 255))
    d.rectangle([3, 3, taille - 4, taille - 4], fill=FOND_INV)
    # coins arrondis : on retire les pixels d'angle laissés par les rectangles
    for x, y in [(1, 1), (taille - 2, 1), (1, taille - 2), (taille - 2, taille - 2)]:
        p.putpixel((x, y), (0, 0, 0, 255))
    return p


def _ligne(remplissage):
    l = Image.new("RGBA", (16, 16), (0, 0, 0, 255))
    ImageDraw.Draw(l).rectangle([1, 1, 14, 14], fill=remplissage)
    return l


def widgets(dossier):
    """Écrit les fonds d'interface du mod et leurs .mcmeta (nine-slice)."""
    dossier.mkdir(exist_ok=True)
    elements = {
        "button": (bouton(20), 3),
        "button_highlighted": (_fond_bouton((222, 222, 228)), 3),
        "button_danger": (_fond_bouton((232, 128, 116)), 3),
        "panel": (_panneau(), 4),
        "row": (_ligne((139, 139, 139)), 1),
        "row_highlighted": (_ligne((222, 222, 228)), 1),
    }
    for nom, (image, bord) in elements.items():
        image.save(dossier / f"{nom}.png")
        (dossier / f"{nom}.png.mcmeta").write_text(_mcmeta(image.width, image.height, bord))
    return list(elements)


def main():
    sortie = ICI / "png"
    sortie.mkdir(exist_ok=True)
    images = {}
    for nom, grille, _, _ in ICONES:
        images[nom] = dessiner(grille)
        images[nom].save(sortie / f"{nom}.png")
    apercu(images).save(ICI / "apercu.png")
    widgets(ICI / "widgets")
    print(f"{len(images)} icônes écrites dans {sortie}, aperçu : {ICI / 'apercu.png'}")


if __name__ == "__main__":
    main()
