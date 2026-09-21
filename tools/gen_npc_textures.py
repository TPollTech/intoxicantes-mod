"""Gera as texturas dos NPCs: skins recoloridas (layout vanilla intacto) e ovos.

Traficante: skin do wandering trader recolorida pra hoodie escuro + oculos.
Gago: PELE BEGE + camisa vermelha so no tecido do robe (o Rei do Bar).
Uso: python tools/gen_npc_textures.py  (a partir da raiz do projeto do mod)
"""
import colorsys
import io
import os
import struct
import zipfile

from PIL import Image, ImageDraw

ASSETS = os.path.join("src", "main", "resources", "assets", "intoxicantes")
JAR = os.path.expanduser(
    "~/.gradle/caches/fabric-loom/26.3/minecraft-merged.jar")


def vanilla_png(caminho_no_jar):
    with zipfile.ZipFile(JAR) as z:
        return Image.open(io.BytesIO(z.read(caminho_no_jar))).convert("RGBA")


def salvar(img, rel):
    caminho = os.path.join(ASSETS, rel)
    os.makedirs(os.path.dirname(caminho), exist_ok=True)
    img.save(caminho)


def recolor_por_matiz(img, matiz_min, matiz_max, novo_rgb, manter_luz=True):
    """Substitui pixels cujo matiz esta na faixa, preservando a luminancia."""
    px = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            if matiz_min <= hh <= matiz_max and ss > 0.12:
                if manter_luz:
                    fator = max(0.35, min(1.6, ll / 0.42))
                    nr, ng, nb = (min(255, int(c * fator)) for c in novo_rgb)
                else:
                    nr, ng, nb = novo_rgb
                px[x, y] = (nr, ng, nb, a)
    return img


def oculos(img):
    """Oculos escuro na face do villager (face frontal fica em 8..15 x 8..15)."""
    d = ImageDraw.Draw(img)
    d.rectangle([8, 10, 15, 11], fill=(20, 20, 25, 255))       # lentes
    d.point([(7, 10), (16, 10)], fill=(20, 20, 25, 255))       # hastes
    d.point([(11, 10), (12, 10)], fill=(90, 90, 110, 255))     # brilho
    return img


def corrente(img):
    """Corrente de ouco no peito do traficante (regiao do corpo frontal)."""
    d = ImageDraw.Draw(img)
    d.rectangle([20, 21, 27, 21], fill=(212, 175, 55, 255))
    d.point([(21, 22), (26, 22)], fill=(160, 125, 30, 255))
    return img


def egg(cor_base, cor_mancha):
    """Ovo de spawn 16x16 no estilo vanilla (base clara + manchas)."""
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # corpo do ovo: elipse com sombreamento
    d.ellipse([3, 1, 12, 15], fill=cor_base, outline=(60, 55, 50, 255))
    d.ellipse([4, 2, 8, 7], fill=tuple(min(255, c + 28) for c in cor_base))
    d.ellipse([5, 10, 11, 14], fill=tuple(max(0, c - 22) for c in cor_base))
    # manchas
    for mx, my in [(6, 5), (10, 4), (8, 8), (5, 11), (11, 11), (9, 13)]:
        d.point([(mx, my), (mx + 1, my)], fill=cor_mancha)
    return img


# ------------------------------------------------------------------ Traficante
skin = vanilla_png("assets/minecraft/textures/entity/wandering_trader/wandering_trader.png")
# robe azul/verde-azulado do andarilho -> hoodie cinza-esverdeado escuro
skin = recolor_por_matiz(skin, 0.35, 0.60, (52, 66, 56))
skin = oculos(skin)
skin = corrente(skin)
salvar(skin, "textures/entity/traficante.png")

# ------------------------------------------------------------------ Gago
# 7 FANTASIAS TEMATICAS POR BIOMA (mesma cara bege, muda o tecido do robe):
# identidade mantida, repeticao zero. O bioma do mercado escolhe a variante
# (GagoEntity detecta no 1o tick e grava no save; o renderer troca a textura).
# ORDEM = indice do DATA_VARIANTE em GagoEntity (0..6)
VARIANTE_GAGO = [
    # (arquivo, nome, tecido claro/meio/escuro, detalhe opcional)
    ("gago",            "plains",  (196, 52, 56), (168, 36, 44), (124, 22, 30), None),   # camisa vermelha classica
    ("gago_sertao",     "desert",  (214, 158, 74), (190, 132, 54), (140, 92, 34), "cinto"),  # couro e po do Sertao
    ("gago_mata",       "jungle",  (52, 130, 66), (38, 104, 52), (22, 72, 36), "flor"),    # verde-mata + flor no peito
    ("gago_cerrado",    "savanna", (206, 142, 44), (178, 116, 32), (126, 78, 20), "palha"),  # ocra do capim dourado
    ("gago_sul",        "snowy",   (42, 84, 118), (30, 62, 92), (18, 40, 62), "neve"),     # casaco azul-petroleo da SEDE MATRIZ
    ("gago_serra",      "taiga",   (48, 96, 72), (34, 72, 54), (20, 46, 36), "gorro"),    # verde-pinho de serra
    ("gago_brejo",      "swamp",   (108, 92, 58), (84, 70, 42), (52, 42, 24), "lama"),    # marrom-pantano
]
BEGE = {"claro": (233, 196, 158), "meio": (213, 170, 128), "escuro": (183, 138, 98)}


def pintar_gago(tecido):
    """Skin do villager com pele bege (y<16) e robe na cor do tecido (y>=16).
    A pele e o robe do vilarejo tem a MESMA paleta terrosa (por cor e impossivel
    separar — foi assim que o Gago virou vermelho kkkk), entao pintamos POR REGIAO."""
    skin = vanilla_png("assets/minecraft/textures/entity/villager/villager.png")
    px = skin.load()
    w, h = skin.size
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            hh, ll, ss = colorsys.rgb_to_hls(r / 255, g / 255, b / 255)
            # so mexe nos pixels terrosos originais (nao toca olho/sobrancelha/boca)
            if not (0.02 <= hh <= 0.13 and ss > 0.12):
                continue
            if y < 16:
                paleta = BEGE
            else:
                paleta = {"claro": tecido[0], "meio": tecido[1], "escuro": tecido[2]}
            if ll >= 0.60:
                cor = paleta["claro"]
            elif ll <= 0.32:
                cor = paleta["escuro"]
            else:
                cor = paleta["meio"]
            px[x, y] = (cor[0], cor[1], cor[2], a)
    return skin


def detalhe(img, tipo):
    """Detalhe tematico desenhado sobre o robe (regiao do corpo frontal do villager).
    O torso frontal fica em x 20..27, y 20..31 (verificado contra o layout vanilla)."""
    d = ImageDraw.Draw(img)
    if tipo == "cinto":       # cinto de couro com fivela (Sertao)
        d.rectangle([20, 25, 27, 26], fill=(96, 62, 30, 255))
        d.rectangle([23, 25, 24, 26], fill=(212, 175, 55, 255))
    elif tipo == "flor":      # flor vermelha no peito (Selva)
        d.point([(22, 21), (23, 21), (21, 22), (24, 22)], fill=(206, 60, 70, 255))
        d.point([(22, 22), (23, 22)], fill=(255, 214, 112, 255))
    elif tipo == "palha":     # faixa trançada de palha (Cerrado)
        d.rectangle([20, 26, 27, 27], fill=(190, 158, 88, 255))
        for xx in range(20, 28, 2):
            d.point([(xx, 26), (xx + 1, 27)], fill=(150, 120, 60, 255))
    elif tipo == "neve":      # barras do casaco + botões (Sede Sul)
        d.rectangle([20, 20, 27, 20], fill=(210, 224, 236, 255))
        d.point([(23, 23), (24, 25), (23, 27), (24, 29)], fill=(210, 224, 236, 255))
    elif tipo == "gorro":     # faixa de lã no peito (Serra)
        d.rectangle([20, 24, 27, 25], fill=(30, 46, 34, 255))
        d.point([(21, 24), (23, 25), (25, 24), (26, 25)], fill=(196, 60, 60, 255))
    elif tipo == "lama":      # salpicos de lama (Brejo)
        for xx, yy in [(21, 22), (25, 23), (23, 27), (26, 28), (20, 29)]:
            d.point([(xx, yy), (xx + 1, yy)], fill=(64, 52, 30, 255))
    return img


for arquivo, nome, c1, c2, c3, detalhe_tipo in VARIANTE_GAGO:
    skin = pintar_gago((c1, c2, c3))
    if detalhe_tipo:
        skin = detalhe(skin, detalhe_tipo)
    salvar(skin, f"textures/entity/{arquivo}.png")
    print(f"  gago[{nome}] -> {arquivo}.png")

# ------------------------------------------------------------------ Ovos de spawn
salvar(egg((226, 230, 224, 255), (52, 140, 84, 255)), "textures/item/ovo_traficante.png")
salvar(egg((226, 230, 224, 255), (176, 40, 48, 255)), "textures/item/ovo_gago.png")

print("NPCs OK: skins traficante/gago + ovos de spawn.")
