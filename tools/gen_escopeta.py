"""Gera a escopeta do Gago: textura de tons + MODELO 3D com elementos (pump-action).

Modelo construido com cano no +Y (vertical, igual ao sprite da besta vanilla):
o display de 1a/3a pessoa da besta/tridente gira X=-90 derrubando o +Y pra -Z
(pra onde o player mira) — com o cano no +X a arma ficava DEITADA na mao.
Uso: python tools/gen_escopeta.py  (a partir da raiz do projeto do mod)
"""
import json
import os

from PIL import Image, ImageDraw

ASSETS = os.path.join("src", "main", "resources", "assets", "intoxicantes")

# ------------------------------------------------------------------ textura (32x32)
# Cada bloco de 4x4 tem degradê vertical (claro em cima, escuro embaixo)
# pra dar volume mesmo com UV esticado nas faces.
BASES = {
    "metal":       ((168, 174, 182), (108, 114, 122), (120, 128, 136)),
    "metal_dark":  ((96, 100, 106),  (56, 58, 63),     (70, 73, 79)),
    "metal_light": ((220, 226, 232), (166, 172, 180), (188, 195, 203)),
    "steel_blue":  ((150, 165, 185), (88, 102, 126),  (112, 128, 152)),
    "muzzle":      ((52, 52, 58),    (22, 22, 26),    (32, 32, 38)),
    "brass":       ((240, 214, 140), (170, 138, 62),  (205, 178, 100)),
    "leather":     ((120, 84, 52),   (72, 48, 26),    (94, 64, 38)),
    "ponto_mira":  ((235, 42, 42),   (120, 10, 10),   (200, 30, 30)),
    "wood":        ((150, 100, 48),  (92, 58, 26),    (120, 78, 36)),
    "wood_dark":   ((106, 66, 30),   (60, 36, 14),    (82, 50, 22)),
    "wood_light":  ((196, 140, 76),  (136, 90, 42),   (166, 114, 58)),
}

img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
POS = {"metal": (0, 0), "metal_dark": (4, 0), "metal_light": (8, 0),
       "steel_blue": (12, 0), "muzzle": (16, 0), "brass": (20, 0),
       "leather": (24, 0), "ponto_mira": (28, 0),
       "wood": (0, 8), "wood_dark": (4, 8), "wood_light": (8, 8)}
for nome, (px, py) in POS.items():
    claro, escuro, meio = BASES[nome]
    d.rectangle([px, py, px + 3, py], fill=claro)
    d.rectangle([px, py + 1, px + 3, py + 2], fill=meio)
    d.rectangle([px, py + 3, px + 3, py + 3], fill=escuro)
# grão da madeira: pontinhos alternados
for px, py, cor in [(1, 9, (104, 66, 30)), (2, 10, (168, 118, 62)), (5, 9, (72, 44, 20)),
                    (6, 10, (136, 90, 40)), (9, 10, (182, 128, 68)), (10, 9, (100, 62, 28))]:
    d.point((px, py), fill=(*cor, 255))
# pontinhos de desgaste no metal e no latão
d.point((1, 1), fill=(130, 136, 144, 255))
d.point((2, 2), fill=(140, 146, 154, 255))
d.point((21, 2), fill=(224, 196, 116, 255))
d.point((22, 1), fill=(150, 120, 52, 255))
# ponto de mira: anel escuro com centro vermelho (o "dot" na ponta do cano)
px, py = POS["ponto_mira"]
d.rectangle([px, py, px + 3, py + 3], fill=(30, 30, 34, 255))
d.rectangle([px + 1, py + 1, px + 2, py + 2], fill=(235, 42, 42, 255))

caminho = os.path.join(ASSETS, "textures", "item", "escopeta.png")
os.makedirs(os.path.dirname(caminho), exist_ok=True)
img.save(caminho)


# ------------------------------------------------------------------ modelo 3D
def uv(tonename):
    px, py = POS[tonename]
    # UV de model e NORMALIZADO 0-16, independente do tamanho da textura.
    # Textura 32x32: cada bloco de 4px = 2 unidades de UV (divide pixel por 2).
    return [px / 2, py / 2, (px + 4) / 2, (py + 4) / 2]


def elemento(nome, de, ate, lados, rot=None):
    """lados: dict com up/down/north/south/east/west -> tonename.

    O modelo e' DESENHADO com a arma deitada (cano no +X, altura em Y) porque fica
    intuitivo, e REMAPEADO aqui pro eixo final: cano no +Y (igual ao sprite da
    besta vanilla). Remap: (x, y, z) -> (z, x, y).

    rot: dict opcional com origin/axis/angle (angle em {-45,-22.5,0,22.5,45}).
    """
    def remap(p):
        x, y, z = p
        return [z, x, y]

    faces = {}
    for lado, tonename in lados.items():
        # as faces EAST/WEST viram as "de perfil" (o comprimento da arma cruza
        # a face) — a UV do tom e horizontal, entao a textura fica orientada
        faces[lado] = {"uv": uv(tonename), "texture": "#escopeta"}
    el = {"name": nome, "from": remap(de), "to": remap(ate), "faces": faces}
    if rot:
        el["rotation"] = {"origin": remap(rot["origin"]),
                          "axis": rot["axis"], "angle": rot["angle"]}
    return el


# inclinaçao da coronha: eixo z, +22.5 graus -> a ponta de tras (coronha) desce
ROT_CORONHA = {"origin": [4.2, 7.0, 8.0], "axis": "z", "angle": 22.5}

E = []
# ---- coronha: núcleo reto junto ao recebedor + corpo inclinado + soleira de couro
E.append(elemento("coronha_recebedor", [4.2, 6.4, 6.4], [7.0, 10.4, 9.6],
                  {"up": "wood_light", "down": "wood_dark", "north": "wood",
                   "south": "wood", "east": "wood_dark", "west": "wood"}))
E.append(elemento("coronha_inclinada", [1.2, 3.8, 6.6], [4.2, 8.0, 9.4],
                  {"up": "wood_light", "down": "wood_dark", "north": "wood",
                   "south": "wood", "east": "wood_dark", "west": "wood"},
                  rot=ROT_CORONHA))
E.append(elemento("placa_couro", [0.2, 3.5, 6.8], [1.2, 7.7, 9.2],
                  {"up": "leather", "down": "leather", "north": "leather",
                   "south": "leather", "east": "leather", "west": "leather"},
                  rot=ROT_CORONHA))
# ---- culatra (recebedor metalico)
E.append(elemento("culatra", [7.0, 6.6, 6.2], [9.6, 11.0, 9.8],
                  {"up": "metal_light", "down": "metal_dark", "north": "metal",
                   "south": "metal", "east": "metal_dark", "west": "metal"}))
# porta de ejeçao (lado sul = lado direito do atirador)
E.append(elemento("ejetor", [7.4, 9.4, 9.8], [8.8, 10.2, 10.3],
                  {"up": "muzzle", "down": "muzzle", "north": "muzzle",
                   "south": "muzzle", "east": "muzzle", "west": "muzzle"}))
# martelho visivel atras
E.append(elemento("martilho", [6.8, 10.6, 7.4], [7.6, 11.6, 8.6],
                  {"up": "metal_light", "down": "metal", "north": "metal_dark",
                   "south": "metal", "east": "metal_dark", "west": "metal_dark"}))
# ---- cano (aço azulado) + aros de latão
E.append(elemento("cano", [9.6, 8.3, 7.15], [15.4, 9.7, 8.85],
                  {"up": "metal_light", "down": "metal", "north": "steel_blue",
                   "south": "steel_blue", "east": "muzzle", "west": "muzzle"}))
E.append(elemento("aro_boca", [14.9, 8.1, 6.9], [15.4, 9.9, 9.1],
                  {"up": "brass", "down": "brass", "north": "brass",
                   "south": "brass", "east": "brass", "west": "brass"}))
E.append(elemento("boca", [15.4, 8.2, 7.0], [16.0, 9.8, 9.0],
                  {"up": "muzzle", "down": "muzzle", "north": "muzzle",
                   "south": "muzzle", "east": "muzzle", "west": "muzzle"}))
# ponto de mira vermelho (dot) na boca — o centro da mira quando apontada
E.append(elemento("ponto_de_mira", [16.0, 8.85, 7.85], [16.4, 9.15, 8.15],
                  {"up": "muzzle", "down": "muzzle", "north": "ponto_mira",
                   "south": "ponto_mira", "east": "muzzle", "west": "muzzle"}))
E.append(elemento("banda_cano", [13.9, 8.2, 7.05], [14.3, 9.8, 8.95],
                  {"up": "brass", "down": "brass", "north": "brass",
                   "south": "brass", "east": "brass", "west": "brass"}))
# ---- tubo do magazine + tampa de latão
E.append(elemento("tubo", [9.6, 7.2, 7.45], [14.9, 8.3, 8.55],
                  {"up": "metal", "down": "metal_dark", "north": "metal_dark",
                   "south": "metal_dark", "east": "muzzle", "west": "muzzle"}))
E.append(elemento("tampa_tubo", [14.5, 7.1, 7.35], [15.0, 8.4, 8.65],
                  {"up": "brass", "down": "brass", "north": "brass",
                   "south": "brass", "east": "brass", "west": "brass"}))
# ---- bomba (forend) de madeira reta, envolvendo o tubo, com estrias laterais
E.append(elemento("bomba", [11.0, 6.5, 6.4], [13.6, 9.4, 9.6],
                  {"up": "wood_light", "down": "wood_dark", "north": "wood",
                   "south": "wood", "east": "wood", "west": "wood"}))
E.append(elemento("bomba_estria_1", [10.9, 6.9, 6.6], [11.1, 9.0, 9.4],
                  {"up": "wood_dark", "down": "wood_dark", "north": "wood_dark",
                   "south": "wood_dark", "east": "wood_dark", "west": "wood_dark"}))
E.append(elemento("bomba_estria_2", [13.5, 6.9, 6.6], [13.7, 9.0, 9.4],
                  {"up": "wood_dark", "down": "wood_dark", "north": "wood_dark",
                   "south": "wood_dark", "east": "wood_dark", "west": "wood_dark"}))
# ---- gatilho, guarda-mato e miras de latão
E.append(elemento("guardamato", [5.6, 5.4, 7.5], [8.4, 5.9, 8.5],
                  {"up": "metal_dark", "down": "metal_dark", "north": "metal_dark",
                   "south": "metal_dark", "east": "metal_dark", "west": "metal_dark"}))
E.append(elemento("guardamato_frente", [8.4, 5.4, 7.5], [8.9, 6.7, 8.5],
                  {"up": "metal_dark", "down": "metal_dark", "north": "metal_dark",
                   "south": "metal_dark", "east": "metal_dark", "west": "metal_dark"}))
E.append(elemento("gatilho", [6.8, 5.9, 7.7], [7.6, 7.0, 8.3],
                  {"up": "metal_light", "down": "metal_light", "north": "metal",
                   "south": "metal", "east": "metal", "west": "metal"}))
E.append(elemento("mira", [15.55, 9.8, 7.7], [15.95, 10.5, 8.3],
                  {"up": "brass", "down": "muzzle", "north": "muzzle",
                   "south": "muzzle", "east": "brass", "west": "brass"}))
E.append(elemento("alma_mira", [9.2, 11.0, 7.7], [9.6, 11.5, 8.3],
                  {"up": "brass", "down": "brass", "north": "brass",
                   "south": "brass", "east": "brass", "west": "brass"}))

modelo = {
    "gui_light": "side",
    "textures": {
        "escopeta": "intoxicantes:item/escopeta",
        "particle": "intoxicantes:item/escopeta",
    },
    "elements": E,
    "display": {
        # DISPLAY DA BESTA VANILLA (verbatim): com o cano no +Y, o X=-90 derruba a
        # arma pra -Z (pra onde o player mira) — a besta faz exatamente isso com
        # bounds x=[4..12] y=[0..16]; nossa arma e' do mesmo shape girado
        "thirdperson_righthand": {
            "rotation": [-90, 0, -60], "translation": [2, 0.1, -3],
            "scale": [0.9, 0.9, 0.9],
        },
        "thirdperson_lefthand": {
            "rotation": [-90, 0, 30], "translation": [2, 0.1, -3],
            "scale": [0.9, 0.9, 0.9],
        },
        "firstperson_righthand": {
            "rotation": [-90, 0, -55], "translation": [1.13, 3.2, 1.13],
            "scale": [0.68, 0.68, 0.68],
        },
        "firstperson_lefthand": {
            "rotation": [-90, 0, 35], "translation": [1.13, 3.2, 1.13],
            "scale": [0.68, 0.68, 0.68],
        },
        # diagonal classica de item: coronha embaixo-esquerda, cano pra cima-direita
        "gui": {
            "rotation": [10, 45, 30], "translation": [0, 0, 0],
            "scale": [0.85, 0.85, 0.85],
        },
        "fixed": {
            "rotation": [0, 0, 0], "translation": [0, 2, 0],
            "scale": [0.5, 0.5, 0.5],
        },
        "ground": {
            "rotation": [0, 0, 0], "translation": [0, 2, 0],
            "scale": [0.35, 0.35, 0.35],
        },
    },
}

caminho = os.path.join(ASSETS, "models", "item", "escopeta.json")
os.makedirs(os.path.dirname(caminho), exist_ok=True)
with open(caminho, "w", encoding="utf-8") as f:
    json.dump(modelo, f, indent=2)
    f.write("\n")

# definition do item
caminho = os.path.join(ASSETS, "items", "escopeta.json")
os.makedirs(os.path.dirname(caminho), exist_ok=True)
with open(caminho, "w", encoding="utf-8") as f:
    json.dump({"model": {"type": "minecraft:model", "model": "intoxicantes:item/escopeta"}},
              f, indent=2)
    f.write("\n")

print(f"Escopeta OK: textura 32x32 com degrades + modelo 3D com {len(E)} elementos.")
