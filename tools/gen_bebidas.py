# -*- coding: utf-8 -*-
"""Gera os assets da DESTILARIA (v1.2.50) — modelos 3D, texturas, blockstates,
loot tables e item definitions.

Filosofia dos modelos (mesma das armas 3D do projeto):
- JSON elements nativos do Minecraft (sem OBJ/Blender);
- curvas simuladas com caixas sobrepostas;
- cada barril tem IDENTIDADE VISUAL própria (spec 8) — nada de recolorir o
  barril vanilla;
- barris: texturas 64x64 com atlas de materiais;
- máquinas de matéria-prima: atlas 128x128 (células de material de 64px) — a
  densidade de pixels acompanha o nível de detalhe do modelo 3D.

Uso: python tools/gen_bebidas.py  (a partir da raiz do projeto do mod)
"""
import json
import os
import struct
import zlib

from random import Random

ASSETS = os.path.join("src", "main", "resources", "assets", "intoxicantes")
MODELS = os.path.join(ASSETS, "models", "block")
TEX = os.path.join(ASSETS, "textures", "block")
TEX_ITEM = os.path.join(ASSETS, "textures", "item")
DATA = os.path.join("src", "main", "resources", "data", "intoxicantes")


def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=1, ensure_ascii=False)
        f.write("\n")


# ============================================================ TEXTURAS (64x64)

def _png(path, pixels):
    """Grava um PNG RGBA de width x height a partir de uma lista de linhas de
    tuplas (r,g,b,a). Sem dependência do Pillow (chunk IHDR/IDAT/IEND à mão)."""
    h = len(pixels)
    w = len(pixels[0])
    raw = b"".join(b"\x00" + b"".join(struct.pack("4B", *px) for px in row)
                   for row in pixels)

    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)

    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    body = chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b"")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + body)


def _mistura(c1, c2, t):
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(len(c1)))


def _hex(s, a=255):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), a)


def textura_barril(nome, madeira_clara, madeira_media, madeira_escura,
                   aro, aro_escuro, detalhe, carbonizada=False):
    """Atlas 64x64 v2 do barril:
    [0..15]x[0..15]  LADO — tábuas verticais (4 tábuas de 4px com frestas),
                     grão por pixel e sombra de barriga (linhas 0-3 e 12-15
                     mais escuras: a curva lê na textura);
    [16..31]x[0..15] TOPO/CABEÇA — anéis concêntricos + frestas radiais;
    [32..47]x[0..15] FUNDO — versão escura da cabeça;
    [48..63]x[0..15] AROS/ROLHA — aro (2 tons) e rolha/cano.
    """
    from random import Random
    rng = Random(hash(nome) & 0xFFFF)
    claro, medio, escuro = _hex(madeira_clara), _hex(madeira_media), _hex(madeira_escura)
    met, met_esc = _hex(aro), _hex(aro_escuro)

    def grao(cor, forca=10):
        d = rng.randint(-forca, forca // 2)
        return (max(0, min(255, cor[0] + d)), max(0, min(255, cor[1] + d)),
                max(0, min(255, cor[2] + d)), 255)

    px = [[(0, 0, 0, 0)] * 64 for _ in range(64)]

    # ---------- célula 0: LADO (tábuas verticais + sombra de barriga)
    for y in range(16):
        # sombreamento vertical: a barriga (meio) reflete mais luz
        if y < 3:
            luz = 0.78 + y * 0.06
        elif y > 12:
            luz = 0.72 + (15 - y) * 0.05
        else:
            luz = 1.0 - abs(y - 8) * 0.012
        for x in range(16):
            fresta = x % 4 == 3          # junta entre tábuas
            borda = x % 4 == 0           # lado iluminado da tábua
            base = claro if x % 8 < 4 else medio  # alterna tom por tábua
            c = grao(base, 12)
            if fresta:
                c = grao(escuro, 6)
            elif borda:
                c = tuple(min(255, int(v * 1.10)) for v in c[:3]) + (255,)
            c = tuple(max(0, min(255, int(v * luz))) for v in c[:3]) + (255,)
            px[y][x] = c
    if carbonizada:
        # rum: carbonização — manchas escuras irregulares sobre a madeira
        for _ in range(90):
            x, y = rng.randint(0, 15), rng.randint(0, 15)
            prof = rng.choice((0.45, 0.6, 0.75))
            px[y][x] = tuple(int(v * prof) for v in px[y][x][:3]) + (255,)
        for _ in range(26):  # brasas vivas discretas no grão queimado
            x, y = rng.randint(0, 15), rng.randint(0, 15)
            px[y][x] = grao((92, 52, 28), 8)

    # ---------- células 1/2: CABEÇA (topo) e FUNDO (anéis concêntricos)
    centro = 7.5
    for cx, fator in ((16, 1.0), (32, 0.55)):
        for y in range(16):
            for x in range(16):
                dx, dy = x - centro, y - centro
                r = (dx * dx + dy * dy) ** 0.5
                if r > 7.6:
                    px[y][cx + x] = (0, 0, 0, 0)          # canto transparente
                    continue
                anel = int(r)
                base = claro if anel % 2 == 0 else medio   # anéis alternados
                c = grao(base, 9)
                if abs(dx) > 6.2 or abs(dy) > 6.2:          # aro externo da cabeça
                    c = grao(escuro, 6)
                if int(dx) == int(dy) and abs(dx) > 3:      # fresta radial (tábua diagonal)
                    c = grao(escuro, 4)
                if fator < 1.0:
                    c = tuple(int(v * fator) for v in c[:3]) + (255,)
                px[y][cx + x] = c

    # ---------- célula 3: AROS + ROLHA (colunas 48..63)
    for y in range(16):
        for x in range(16):
            if x < 8:
                base = met if x < 4 else tuple(int(v * 0.78) for v in met)
                c = grao(base, 7)
                if y in (0, 15):
                    c = grao(met_esc, 5)
            else:
                # rolha = BUJÃO DE MADEIRA (cortiça), não a cor de identidade
                c = grao(_escurecer(medio, 0.12) if y % 5 else _escurecer(medio, 0.38), 8)
            px[y][48 + x] = c

    _png(os.path.join(TEX, "barril_%s.png" % nome), px)
    return "barril_%s" % nome


def _escurecer(cor, fator):
    return (int(cor[0] * (1 - fator)), int(cor[1] * (1 - fator)),
            int(cor[2] * (1 - fator)), cor[3])


# ============================================================ MODELOS 3D

def _faces(uv, tex):
    return {
        "north": {"uv": uv, "texture": "#%s" % tex},
        "south": {"uv": uv, "texture": "#%s" % tex},
        "east": {"uv": uv, "texture": "#%s" % tex},
        "west": {"uv": uv, "texture": "#%s" % tex},
        "up": {"uv": uv, "texture": "#%s" % tex},
        "down": {"uv": uv, "texture": "#%s" % tex},
    }


def barril_modelo(nome, tex, detalhe_altura=1.0, torneira=False):
    """Barril v2 — barriga REAL (spec 7): 7 bandas de raio graduado formando
    o bojudo (chime → barriga → chime), 5 aros GEOMÉTRICOS que acompanham a
    curva (o central mais grosso, como nos barris de carvalho reais), cabeças
    finas com anéis concêntricos, rolha em degraus e torneira opcional.
    UVs mapeiam a ALTURA REAL de cada banda: as tábuas correm contínuas.
    ~50 elements, todos legíveis no preview.
    """
    els = []
    # -------- perfil do corpo: (y0, y1, inset) — inset a partir de 0.9px
    # chimes (bordas) estreitos, barriga larga no meio (y 6..10)
    bandas = [
        (1.2, 3.0, 2.9),   # chime inferior
        (3.0, 4.6, 2.15),  # barriga baixa
        (4.6, 6.0, 1.5),
        (6.0, 10.0, 1.05), # BARRIGA (quatro px de ventre pleno)
        (10.0, 11.4, 1.5),
        (11.4, 13.0, 2.15),# barriga alta
        (13.0, 14.8, 2.9), # chime superior
    ]
    total_y = 14.8 - 1.2   # altura pintada do corpo (13.6)
    for i, (y0, y1, inset) in enumerate(bandas):
        x0, x1 = 0.9 + inset, 15.1 - inset
        # texture_size 64: 1 unidade de UV = 4 px do atlas. A célula LADO é
        # 16x16 px => 4x4 unidades. UV x 0..1 = a linha de tábuas inteira;
        # v0..v1 = a fração da altura da banda (tábua contínua, sem dobra).
        v0 = (y0 - 1.2) / total_y * 4
        v1 = (y1 - 1.2) / total_y * 4
        uv_lado = (0.0, v0, 4.0, v1)
        els.append({
            "from": [x0, y0, x0], "to": [x1, y1, x1],
            "faces": {
                "north": {"uv": uv_lado, "texture": "#lado"},
                "south": {"uv": uv_lado, "texture": "#lado"},
                "east": {"uv": uv_lado, "texture": "#lado"},
                "west": {"uv": uv_lado, "texture": "#lado"},
                # o degrau entre bandas é VINCO (linha escura da fresta),
                # não prateleira: faixa fina da célula LADO (fundo escuro)
                "up": {"uv": (0.0, 3.7, 4.0, 3.85), "texture": "#lado"},
                "down": {"uv": (0.0, 3.85, 4.0, 4.0), "texture": "#lado"},
            },
        })
    # -------- cabeças: tampa e fundo (célula topo/fundo com anéis)
    # cabeça RECESSADA (afundada dentro do chime) — anéis concêntricos de cima
    els.append({"from": [3.9, 14.8, 3.9], "to": [12.1, 15.6, 12.1],
                "faces": {"up": {"uv": (4.0, 0.0, 8.0, 4.0), "texture": "#topo"},
                          "down": {"uv": (4.0, 0.0, 8.0, 4.0), "texture": "#lado"},
                          "north": {"uv": (12.0, 0.0, 13.0, 2.0), "texture": "#aro"},
                          "south": {"uv": (12.0, 0.0, 13.0, 2.0), "texture": "#aro"},
                          "east": {"uv": (12.0, 0.0, 13.0, 2.0), "texture": "#aro"},
                          "west": {"uv": (12.0, 0.0, 13.0, 2.0), "texture": "#aro"}}})
    els.append({"from": [3.9, 0.4, 3.9], "to": [12.1, 1.2, 12.1],
                "faces": {"up": {"uv": (0.0, 0.0, 4.0, 4.0), "texture": "#lado"},
                          "down": {"uv": (8.0, 0.0, 12.0, 4.0), "texture": "#fundo"},
                          "north": {"uv": (12.0, 0.0, 13.0, 2.0), "texture": "#aro"},
                          "south": {"uv": (12.0, 0.0, 13.0, 2.0), "texture": "#aro"},
                          "east": {"uv": (12.0, 0.0, 13.0, 2.0), "texture": "#aro"},
                          "west": {"uv": (12.0, 0.0, 13.0, 2.0), "texture": "#aro"}}})
    # -------- 5 AROS GEOMÉTRICOS que seguem a curva (o central mais grosso)
    # (y, espessura) posicionados nas juntas das bandas; raio = banda + 0.35
    aros = [
        (2.0, 0.9),    # chime inferior
        (4.2, 0.8),    # quartos
        (7.1, 1.4),    # CENTRAL (bilge hoop, grosso como no barril real)
        (10.0, 0.8),   # quartos
        (13.6, 0.9),   # chime superior
    ]
    for y, esp in aros:
        # raio do aro: acompanha o corpo (interpolado do perfil)
        if y < 3.0:
            inset = 2.9
        elif y < 4.6:
            inset = 2.15
        elif y < 6.0:
            inset = 1.5
        elif y < 10.0:
            inset = 1.05
        elif y < 11.4:
            inset = 1.5
        elif y < 13.0:
            inset = 2.15
        else:
            inset = 2.9
        x0, x1 = 0.9 + inset - 0.35, 15.1 - inset + 0.35
        # célula ARO = colunas 48..55 px => unidades 12..14; altura do aro
        # na textura proporcional à espessura (célula tem 4 unidades)
        uv_aro = (12.0, 0.0, 14.0, esp / 1.4 * 4.0)
        els.append({
            "from": [x0, y - esp / 2, x0], "to": [x1, y + esp / 2, x1],
            "faces": {
                "north": {"uv": uv_aro, "texture": "#aro"},
                "south": {"uv": uv_aro, "texture": "#aro"},
                "east": {"uv": uv_aro, "texture": "#aro"},
                "west": {"uv": uv_aro, "texture": "#aro"},
            },
        })
    # -------- rolha (bung) em degraus no topo — cresce com a fase
    h = max(0.6, detalhe_altura)
    uv_rolha = (14.0, 0.0, 16.0, 4.0)  # célula ROLHA (cortiça)
    els.append({"from": [6.9, 15.6, 6.9], "to": [9.1, 15.6 + h * 0.55, 9.1],
                "faces": {"up": {"uv": uv_rolha, "texture": "#aro"},
                          "down": {"uv": uv_rolha, "texture": "#aro"},
                          "north": {"uv": uv_rolha, "texture": "#aro"},
                          "south": {"uv": uv_rolha, "texture": "#aro"},
                          "east": {"uv": uv_rolha, "texture": "#aro"},
                          "west": {"uv": uv_rolha, "texture": "#aro"}}})
    els.append({"from": [7.4, 15.6 + h * 0.55, 7.4], "to": [8.6, 15.6 + h, 8.6],
                "faces": {"up": {"uv": uv_rolha, "texture": "#aro"},
                          "north": {"uv": uv_rolha, "texture": "#aro"},
                          "south": {"uv": uv_rolha, "texture": "#aro"},
                          "east": {"uv": uv_rolha, "texture": "#aro"},
                          "west": {"uv": uv_rolha, "texture": "#aro"}}})
    # -------- TORNEIRA (cerveja): bico de metal na barriga frontal + almofada
    if torneira:
        uv_t = (12.0, 0.0, 14.0, 2.0)   # metal do aro
        els.append({"from": [10.9, 5.4, -1.2], "to": [13.1, 7.6, 0.95],
                    "faces": {"north": {"uv": uv_t, "texture": "#aro"},
                              "south": {"uv": uv_t, "texture": "#aro"},
                              "east": {"uv": uv_t, "texture": "#aro"},
                              "west": {"uv": uv_t, "texture": "#aro"},
                              "up": {"uv": uv_t, "texture": "#aro"},
                              "down": {"uv": uv_t, "texture": "#aro"}}})
        els.append({"from": [11.5, 5.0, -1.6], "to": [12.5, 5.4, 0.2],
                    "faces": {"north": {"uv": uv_t, "texture": "#aro"},
                              "south": {"uv": uv_t, "texture": "#aro"},
                              "east": {"uv": uv_t, "texture": "#aro"},
                              "west": {"uv": uv_t, "texture": "#aro"},
                              "down": {"uv": uv_t, "texture": "#aro"}}})
    modelo = {
        "texture_size": [64, 64],
        "textures": {
            "lado": "intoxicantes:block/" + tex,
            "topo": "intoxicantes:block/" + tex,
            "fundo": "intoxicantes:block/" + tex,
            "aro": "intoxicantes:block/" + tex,
            "particle": "intoxicantes:block/" + tex,
        },
        "elements": els,
    }
    # display do item (barril no inventário: iso padrão de bloco, mantém tamanho)
    modelo["display"] = {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0],
                 "scale": [0.85, 0.85, 0.85]},
        "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"rotation": [0, 0, 0], "scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                   "scale": [0.5, 0.5, 0.5]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                   "scale": [0.5, 0.5, 0.5]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0],
                                   "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 1, 0],
                                   "scale": [0.6, 0.6, 0.6]},
    }
    wjson(os.path.join(MODELS, "barril_%s.json" % nome), modelo)
    return modelo


def dorna_modelo():
    """Tina aberta de fermentação: aro de tábuas + líquido visível + pernas.
    Atlas 128x128: C0 tábuas, C1 mosto, C2 aro/pernas, C3 base (sobras)."""
    C0, C1, C2 = (0.0, 0.0, 8.0, 8.0), (8.0, 0.0, 16.0, 8.0), (0.0, 8.0, 8.0, 16.0)
    els = []
    # parede da tina (4 lados como caixa oca aproximada: 4 elementos finos)
    els.append({"from": [1.0, 2.0, 1.0], "to": [15.0, 9.0, 2.4], "faces": _faces(C0, "lado")})
    els.append({"from": [1.0, 2.0, 13.6], "to": [15.0, 9.0, 15.0], "faces": _faces(C0, "lado")})
    els.append({"from": [1.0, 2.0, 2.4], "to": [2.4, 9.0, 13.6], "faces": _faces(C0, "lado")})
    els.append({"from": [13.6, 2.0, 2.4], "to": [15.0, 9.0, 13.6], "faces": _faces(C0, "lado")})
    # FUNDO da tina (revisão v1.2.50): sem ele a boca aberta vira buraco —
    # backfaces internos são culled no jogo e dá pra ver através do bloco
    els.append({"from": [1.0, 2.0, 1.0], "to": [15.0, 3.2, 15.0], "faces": _faces(C2, "lado")})
    # líquido (a cor do mosto; o brilho é a fermentação viva)
    els.append({"from": [2.4, 6.8, 2.4], "to": [13.6, 7.4, 13.6],
                "faces": _faces(C1, "liquido")})
    # aro superior (madeira escura — no atlas 64 ele usava a cor do mosto, bug)
    els.append({"from": [0.6, 9.0, 0.6], "to": [15.4, 10.0, 15.4],
                "faces": _faces(C2, "aro")})
    # pernas (4 cantos)
    for (x, z) in ((1.6, 1.6), (12.8, 1.6), (1.6, 12.8), (12.8, 12.8)):
        els.append({"from": [x, 0.0, z], "to": [x + 1.6, 2.0, z + 1.6],
                    "faces": _faces(C2, "lado")})
    modelo = {
        "texture_size": [128, 128],
        "textures": {
            "lado": "intoxicantes:block/dorna",
            "liquido": "intoxicantes:block/dorna",
            "aro": "intoxicantes:block/dorna",
            "particle": "intoxicantes:block/dorna",
        },
        "elements": els,
        "display": _display_padrao(),
    }
    wjson(os.path.join(MODELS, "dorna_bebida.json"), modelo)


def alambique_modelo():
    """Alambique de cobre: caldeira redonda + pescoço + serpentina em espiral
    (simulada por segmentos escalonados) + bica de saída. ~22 elements.
    Atlas 128x128: C0 cobre, C1 tampa/pescoço, C2 serpentina com pátina,
    C3 madeira do balde (no atlas 64 ele usava a célula do cobre, bug)."""
    C0, C1, C2, C3 = ((0.0, 0.0, 8.0, 8.0), (8.0, 0.0, 16.0, 8.0),
                      (0.0, 8.0, 8.0, 16.0), (8.0, 8.0, 16.0, 16.0))
    PESCOCO = (10.0, 1.0, 14.0, 7.0)   # faixa vertical da célula C1
    SERP = (1.0, 9.0, 7.0, 15.0)       # faixa da célula C2 (tubo fino)
    BICA = (1.0, 1.0, 5.0, 5.0)        # quadrante da célula C0 (cobre polido)
    els = []
    # caldeira: 3 fatias (curva do pote de cobre)
    els.append({"from": [2.4, 0.0, 2.4], "to": [13.6, 2.0, 13.6], "faces": _faces(C0, "cobre")})
    els.append({"from": [1.6, 2.0, 1.6], "to": [14.4, 8.0, 14.4], "faces": _faces(C0, "cobre")})
    els.append({"from": [2.4, 8.0, 2.4], "to": [13.6, 9.6, 13.6], "faces": _faces(C1, "cobre")})
    # tampa abaulada (degraus com recuo crescente = leitura de domo)
    els.append({"from": [4.4, 9.6, 4.4], "to": [11.6, 10.8, 11.6], "faces": _faces(C1, "cobre")})
    els.append({"from": [6.4, 10.8, 6.4], "to": [9.6, 11.6, 9.6], "faces": _faces(C1, "cobre")})
    # pescoço/vapor sobe da tampa
    els.append({"from": [7.0, 11.6, 7.0], "to": [9.0, 13.6, 9.0], "faces": _faces(PESCOCO, "cobre")})
    # REVISÃO v1.2.50 do circuito: a serpentina antiga atravessava a tampa e
    # terminava no ar (nunca entrava no balde). Agora: braço dobrado sobre a
    # tampa leva o vapor pra FORA da caldeira; o tubo desce na franja entre
    # tampa e parede e MERGULHA no balde condensador; anéis marcam o enrolamento.
    els.append({"from": [8.8, 12.8, 7.4], "to": [15.3, 13.6, 8.6], "faces": _faces(SERP, "cobre")})
    els.append({"from": [14.5, 2.6, 10.8], "to": [15.3, 12.8, 11.6], "faces": _faces(SERP, "cobre")})
    for y in (11.2, 8.6, 6.8):   # anéis do enrolamento descendo pro balde
        els.append({"from": [14.3, y, 10.6], "to": [15.5, y + 0.6, 12.0],
                    "faces": _faces(PESCOCO, "cobre")})
    # balde condensador (o tubo mergulha nele pela boca)
    els.append({"from": [9.6, 0.0, 8.0], "to": [15.6, 6.0, 13.4],
                "faces": _faces(C3, "madeira")})
    # bica de saída no balde
    els.append({"from": [13.6, 2.6, 10.2], "to": [15.8, 3.8, 11.4],
                "faces": _faces(BICA, "cobre")})
    modelo = {
        "texture_size": [128, 128],
        "textures": {
            "cobre": "intoxicantes:block/alambique",
            "madeira": "intoxicantes:block/alambique",
            "particle": "intoxicantes:block/alambique",
        },
        "elements": els,
        "display": _display_padrao(1.05),
    }
    wjson(os.path.join(MODELS, "alambique.json"), modelo)


def maquina_modelo(nome, tex, tipo):
    """Moenda (rolos + manivela), prensa (fuso + cesto) e caldeirão (pote de
    ferro com alça). ~14-18 elements cada.
    Atlas 128x128: C0 material principal, C1 metal claro, C2 escuro/base,
    C3 detalhe (metal escuro ou líquido). Tubos finos usam faixas da célula."""
    C0, C1, C2, C3 = ((0.0, 0.0, 8.0, 8.0), (8.0, 0.0, 16.0, 8.0),
                      (0.0, 8.0, 8.0, 16.0), (8.0, 8.0, 16.0, 16.0))
    EIXO = (8.0, 2.0, 16.0, 6.0)       # faixa horizontal do metal C1
    MANIVELA = (8.0, 9.0, 16.0, 15.0)  # faixa do metal escuro C3
    FUSO = (10.0, 0.0, 14.0, 8.0)      # faixa vertical do metal C1
    BORDA = (8.0, 1.0, 16.0, 5.0)      # faixa do metal claro C1
    ALCAS = (0.0, 10.0, 8.0, 14.0)     # faixa do escuro C2
    els = []
    if tipo == "moenda":
        # base + dois rolos verticais + manivela
        els.append({"from": [1.0, 0.0, 1.0], "to": [15.0, 3.0, 15.0], "faces": _faces(C2, "madeira")})
        els.append({"from": [3.0, 3.0, 5.0], "to": [7.0, 12.0, 11.0], "faces": _faces(C0, "madeira")})
        els.append({"from": [9.0, 3.0, 5.0], "to": [13.0, 12.0, 11.0], "faces": _faces(C0, "madeira")})
        els.append({"from": [6.4, 12.0, 7.2], "to": [9.6, 13.0, 8.8], "faces": _faces(EIXO, "ferro")})
        els.append({"from": [13.0, 10.0, 7.4], "to": [15.0, 12.6, 8.6], "faces": _faces(MANIVELA, "ferro")})
    elif tipo == "prensa":
        # cesto aberto + fuso central + viga superior
        els.append({"from": [2.0, 0.0, 2.0], "to": [14.0, 2.0, 14.0], "faces": _faces(C2, "madeira")})
        for (x0, z0, x1, z1) in ((2.0, 2.0, 3.4, 14.0), (12.6, 2.0, 14.0, 14.0),
                                  (3.4, 2.0, 12.6, 3.4), (3.4, 12.6, 12.6, 14.0)):
            els.append({"from": [x0, 2.0, z0], "to": [x1, 9.0, z1], "faces": _faces(C0, "madeira")})
        els.append({"from": [3.4, 6.8, 3.4], "to": [12.6, 7.6, 12.6], "faces": _faces(C3, "liquido")})
        els.append({"from": [7.2, 9.0, 7.2], "to": [8.8, 13.4, 8.8], "faces": _faces(FUSO, "ferro")})
        els.append({"from": [1.0, 13.4, 1.0], "to": [15.0, 15.0, 15.0], "faces": _faces(C2, "madeira")})
    else:  # caldeirão
        # REVISÃO v1.2.50: o corpo era SÓLIDO com o líquido ENTERRADO dentro
        # (invisível no jogo) e a borda flutuava sobre a boca. Agora é um pote
        # ABERTO de verdade: fundo + 4 paredes + líquido visível + aro na boca.
        els.append({"from": [2.0, 0.0, 2.0], "to": [14.0, 2.0, 14.0], "faces": _faces(C2, "ferro")})
        els.append({"from": [2.6, 2.0, 2.6], "to": [13.4, 3.2, 13.4], "faces": _faces(C2, "ferro")})
        for (x0, z0, x1, z1) in ((1.4, 1.4, 14.6, 2.6), (1.4, 13.4, 14.6, 14.6),
                                  (1.4, 2.6, 2.6, 13.4), (13.4, 2.6, 14.6, 13.4)):
            els.append({"from": [x0, 2.0, z0], "to": [x1, 8.6, z1], "faces": _faces(C0, "ferro")})
        # superfície do mosto visível pela boca (perto da borda)
        els.append({"from": [2.6, 7.4, 2.6], "to": [13.4, 8.2, 13.4], "faces": _faces(C3, "liquido")})
        # aro da boca: 4 faixas acompanhando as paredes (não flutua)
        for (x0, z0, x1, z1) in ((1.4, 1.4, 14.6, 2.6), (1.4, 13.4, 14.6, 14.6),
                                  (1.4, 2.6, 2.6, 13.4), (13.4, 2.6, 14.6, 13.4)):
            els.append({"from": [x0, 8.6, z0], "to": [x1, 9.4, z1], "faces": _faces(BORDA, "ferro")})
        # alças laterais (grudadas nas paredes)
        els.append({"from": [0.2, 5.4, 6.8], "to": [1.4, 6.6, 9.2], "faces": _faces(ALCAS, "ferro")})
        els.append({"from": [14.6, 5.4, 6.8], "to": [15.8, 6.6, 9.2], "faces": _faces(ALCAS, "ferro")})
    modelo = {
        "texture_size": [128, 128],
        "textures": {
            "madeira": "intoxicantes:block/" + tex,
            "ferro": "intoxicantes:block/" + tex,
            "liquido": "intoxicantes:block/" + tex,
            "particle": "intoxicantes:block/" + tex,
        },
        "elements": els,
        "display": _display_padrao(),
    }
    wjson(os.path.join(MODELS, "%s.json" % nome), modelo)


def _display_padrao(escala=1.0):
    s = escala
    return {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0],
                 "scale": [0.85 * s, 0.85 * s, 0.85 * s]},
        "ground": {"translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
        "fixed": {"scale": [1, 1, 1]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                   "scale": [0.5, 0.5, 0.5]},
        "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                  "scale": [0.5, 0.5, 0.5]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 1, 0],
                                   "scale": [0.6, 0.6, 0.6]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 1, 0],
                                  "scale": [0.6, 0.6, 0.6]},
    }


# ============================================================ TEXTURAS 64x64 DOS BLOCOS

def gerar_texturas_blocos():
    # barris — paletas por identidade (spec 8)
    textura_barril("cachaca", "C8A86A", "A8874C", "7A5C30", "B8B0A4", "6E6A60", "D9C27E")
    textura_barril("cerveja", "D8B978", "BC9C58", "8A6C38", "3E3830", "241F1A", "9BC44D")
    textura_barril("rum", "5A4028", "452F1C", "2E1F12", "2A2420", "151210", "3A2A18", carbonizada=True)
    textura_barril("vinho", "8A6A48", "6E5436", "4A3620", "C0B4A4", "6E6A60", "7A2E52")

    # dorna, alambique, moenda, prensa e caldeirão: geradas em
    # gerar_texturas_maquinas() — atlas 128x128 com células de material de 64px.


def _clarear(cor, t):
    return (int(cor[0] + (255 - cor[0]) * t), int(cor[1] + (255 - cor[1]) * t),
            int(cor[2] + (255 - cor[2]) * t), cor[3])


# ============================================================ MÁQUINAS 128x128
# Atlas 2x2 de células de 64px (um material por célula): a densidade de pixels
# acompanha o modelo 3D — mesma filosofia das armas 3D (regra §8 do AGENTS.md).
# Células em pixels: 0=(0,0) 1=(64,0) 2=(0,64) 3=(64,64); em unidades de UV
# (texture_size 128): C0=(0,0,8,8) C1=(8,0,16,8) C2=(0,8,8,16) C3=(8,8,16,16).

_CEL = 64


def _atlas_maquina(nome, c0, c1, c2, c3):
    px = [[(0, 0, 0, 0)] * (_CEL * 2) for _ in range(_CEL * 2)]
    for y in range(_CEL):
        for x in range(_CEL):
            px[y][x] = c0[y][x]
            px[y][_CEL + x] = c1[y][x]
            px[_CEL + y][x] = c2[y][x]
            px[_CEL + y][_CEL + x] = c3[y][x]
    _png(os.path.join(TEX, nome + ".png"), px)
    print("máquina 128x128:", nome)


def _celula_madeira(rng, claro, medio, escuro, vertical=True):
    """Tábuas de 16px com fresta, canto iluminado, grão fibroso e 2 nós."""
    cel = []
    nos = ((18, 22), (46, 44))
    for y in range(_CEL):
        linha = []
        for x in range(_CEL):
            a, b = (x, y) if vertical else (y, x)
            base = _mistura(claro, medio, (a % 32) / 31.0)
            if (a // 16) % 2:
                base = _mistura(base, escuro, 0.18)      # alterna tom por tábua
            borda = a % 16
            if borda == 15:
                base = _mistura(base, escuro, 0.55)      # fresta entre tábuas
            elif borda == 0:
                base = _clarear(base, 0.10)              # canto iluminado
            g = ((b * 7 + (a // 16) * 31) % 9) - 4       # grão ao longo da fibra
            if b % 3 == 0:
                g -= 2
            c = tuple(max(0, min(255, v + g * 2)) for v in base[:3])
            for (nx, ny) in nos:                         # nós de madeira
                r = ((x - nx) ** 2 + (y - ny) ** 2) ** 0.5
                if r < 4.5:
                    c = tuple(int(v * (0.62 + r * 0.09)) for v in c)
            linha.append(c + (255,))
        cel.append(linha)
    return cel


def _celula_metal(rng, claro, escuro, pates=0.0):
    """Chapas metálicas: degradê, vinco a cada 32px, rebites nos encontros,
    estrias de escovado; pates > 0 mistura pátina (cobre envelhecido)."""
    patina = (74, 122, 98)
    cel = []
    for y in range(_CEL):
        linha = []
        for x in range(_CEL):
            base = _mistura(claro, escuro, y / 63.0)
            if y % 32 == 31:
                base = _mistura(base, escuro, 0.5)       # vinco entre chapas
            elif y % 32 == 0:
                base = _clarear(base, 0.12)
            s = ((x * 5 + y * 13) % 11) - 5              # escovado
            c = tuple(max(0, min(255, v + s * 2)) for v in base[:3])
            if pates and ((x * 7 + y * 11) % 23) < pates * 23:
                c = _mistura(c, patina, 0.35)[:3]
            if y % 32 in (30, 31) and x % 32 in (15, 16, 47, 48):
                c = tuple(max(0, v - 32) for v in c)     # sombra do rebite
            if y % 32 == 1 and x % 32 in (15, 16, 47, 48):
                c = tuple(min(255, v + 46) for v in c)   # brilho do rebite
            linha.append(c + (255,))
        cel.append(linha)
    return cel


def _celula_liquido(rng, topo, fundo, brilho=(240, 244, 210)):
    """Líquido vivo: gradiente + bolhas claras com borda (fermentação)."""
    bolhas = [(6, 10, 3), (20, 4, 2), (34, 18, 4), (52, 8, 2), (12, 30, 3),
              (40, 34, 5), (58, 26, 2), (26, 50, 3), (50, 52, 4), (8, 52, 2)]
    cel = []
    for y in range(_CEL):
        linha = []
        for x in range(_CEL):
            c = _mistura(topo, fundo, y / 63.0)[:3]
            for (bx, by, br) in bolhas:
                r = ((x - bx) ** 2 + (y - by) ** 2) ** 0.5
                if r < br:
                    c = _mistura(c, brilho, 0.30 + 0.25 * (1 - r / br))[:3]
                elif r < br + 1:
                    c = tuple(int(v * 0.88) for v in c)
            linha.append(c + (255,))
        cel.append(linha)
    return cel


def gerar_texturas_maquinas():
    """As 5 máquinas em alta resolução: atlas 128x128, células de 64px."""
    # ---- dorna: tábuas claras (C0) + mosto verde vivo (C1) + aro/pernas (C2)
    r = Random(41)
    _atlas_maquina("dorna",
                   _celula_madeira(r, _hex("D2B072"), _hex("A8874C"), _hex("6E5436")),
                   _celula_liquido(r, _hex("C4CE74"), _hex("7E9038")),
                   _celula_madeira(r, _hex("9A7A48"), _hex("6E5436"), _hex("4A3620")),
                   _celula_metal(r, _hex("5A5048"), _hex("38322C")))

    # ---- alambique: cobre polido (C0) + tampa/pescoço (C1) + serpentina com
    #      pátina (C2) + balde de madeira (C3)
    r = Random(42)
    _atlas_maquina("alambique",
                   _celula_metal(r, _hex("D08448"), _hex("A85C2E"), pates=0.10),
                   _celula_metal(r, _hex("B87038"), _hex("8A4E24")),
                   _celula_metal(r, _hex("A86430"), _hex("6E3E1E"), pates=0.45),
                   _celula_madeira(r, _hex("9A7A48"), _hex("6E5436"), _hex("4A3620"), vertical=False))

    # ---- moenda: spruce (C0) + ferro dos rolos (C1) + madeira escura (C2) +
    #      ferro escuro da manivela (C3)
    r = Random(43)
    _atlas_maquina("moenda_cana",
                   _celula_madeira(r, _hex("C09A5E"), _hex("9A7A44"), _hex("6E5430")),
                   _celula_metal(r, _hex("9A9AA0"), _hex("5E5E64")),
                   _celula_madeira(r, _hex("9A7A44"), _hex("6E5430"), _hex("4A3620")),
                   _celula_metal(r, _hex("6E6E74"), _hex("3E3E44")))

    # ---- prensa: bétula clara (C0) + fuso de ferro (C1) + vigas (C2) +
    #      mosto bordô (C3)
    r = Random(44)
    _atlas_maquina("prensa_uvas",
                   _celula_madeira(r, _hex("E0D2A4"), _hex("BCA468"), _hex("8A7440")),
                   _celula_metal(r, _hex("A8A8AE"), _hex("66666C")),
                   _celula_madeira(r, _hex("C8B078"), _hex("9A8248"), _hex("66543A")),
                   _celula_liquido(r, _hex("8E3C60"), _hex("5A1E3C"), brilho=(228, 190, 205)))

    # ---- caldeirão: ferro do corpo (C0) + borda clara (C1) + base/alças (C2) +
    #      mostura dourada (C3)
    r = Random(45)
    _atlas_maquina("caldeirao_mostura",
                   _celula_metal(r, _hex("6A6A70"), _hex("42424A")),
                   _celula_metal(r, _hex("8A8A92"), _hex("56565E")),
                   _celula_metal(r, _hex("4A4A52"), _hex("2C2C32")),
                   _celula_liquido(r, _hex("E8BC50"), _hex("B07E2A")))


# ============================================================ ITENS (16x16)

def _png16(path, pixels, _paleta_removida=None):
    """Grava um item 16x16 a partir de PIXELS RGBA (o que _hex16 produz).

    BUG CORRIGIDO (v1.2.50): antes esta função esperava ASCII-art + paleta,
    mas todas as chamadas já passavam pixels RGBA direto (com paleta `{}`)
    — todo pixel caía no "não achado" e virava transparente: os 12 itens
    intermediários nasceram INVISÍVEIS no jogo. Se um dia a arte em ASCII
    voltar, converta com _hex16 antes de chamar aqui.
    """
    # _hex16 devolve listas de linhas; normalize para grade mutável 16x16
    if len(pixels) == 16 and isinstance(pixels[0], list):
        grade = [list(linha) for linha in pixels]
    else:
        grade = [list(linha) for linha in pixels]
    _png(path, grade)
    # guarda de regressão: item invisível NUNCA mais passa silencioso
    if not any(p[3] > 0 for linha in grade for p in linha):
        raise SystemExit("BUG: %s saiu 100%% transparente — arte/paleta errada" % path)


def _contorno(px):
    """Contorno escuro 1px em volta dos pixels opacos (padrão de item do mod)."""
    h, w = len(px), len(px[0])
    out = [list(l) for l in px]
    for y in range(h):
        for x in range(w):
            if px[y][x][3] != 0:
                continue
            viz = False
            for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):
                yy, xx = y + dy, x + dx
                if 0 <= yy < h and 0 <= xx < w and px[yy][xx][3] != 0:
                    viz = True
                    break
            if viz:
                out[y][x] = (26, 24, 20, 200)
    return out


def gerar_texturas_itens():
    # caldo de cana: garrafinha de vidro com líquido verde-claro
    caldo = [
        "................",
        "......ww........",
        "......ww........",
        ".....w..w.......",
        ".....w..w.......",
        "....w....w......",
        "....wggggw......",
        "....wglggw......",
        "....wgglgw......",
        "....wggggw......",
        "....wggggw......",
        ".....wggw.......",
        "......ww........",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "caldo_de_cana.png"),
           _contorno(_hex16(caldo, {"w": "B8D8D8CC", "g": "B8D060", "l": "D0E888"})),
           {})  # já veio RGBA via _hex16

    # mosto fermentado: mesmo formato, líquido turvo
    mosto_c = [
        "................",
        "......ww........",
        "......ww........",
        ".....w..w.......",
        ".....w..w.......",
        "....w....w......",
        "....wggggw......",
        "....wgsggw......",
        "....wggsgw......",
        "....wsgggw......",
        "....wggggw......",
        ".....wggw.......",
        "......ww........",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "mosto_cana_fermentado.png"),
           _contorno(_hex16(mosto_c, {"w": "B8D8D8CC", "g": "A8B050", "s": "D8DC90"})), {})

    # mosto de rum fermentado: mesma garrafa, melaço escuro
    _png16(os.path.join(TEX_ITEM, "mosto_rum_fermentado.png"),
           _contorno(_hex16(mosto_c, {"w": "B8D8D8CC", "g": "6E4A24", "s": "8F6432"})), {})

    # melaço: pote escuro e grosso
    melaco = [
        "................",
        "................",
        ".....pppppp.....",
        "....p......p....",
        "....pmmmmmmp....",
        "....pmmmmmmp....",
        "....pmhmmmp.....",
        "....pmmmmmp.....",
        "....pmmmmmmp....",
        "....pmmmmmmp....",
        ".....pmmmmp.....",
        "......pppp......",
        "................",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "melaco.png"),
           _contorno(_hex16(melaco, {"p": "4A3620", "m": "3A2A14", "h": "5A4426"})), {})

    # cevada: grãos dourados com aristas
    cevada = [
        "................",
        "................",
        "...h.h.h.h......",
        "..hghghghgh.....",
        "..hghghghgh.....",
        "...h.h.h.h......",
        "....ggggg.......",
        "....ggggg.......",
        "....ggggg.......",
        ".....ggg........",
        ".....ggg........",
        "......g.........",
        "................",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "cevada.png"),
           _contorno(_hex16(cevada, {"g": "D8B858", "h": "E8D078"})), {})

    # malte: grãos torrados
    malte = [
        "................",
        "................",
        "....mm.mm.......",
        "...mMMmMMm......",
        "...mMMmMMm......",
        "....mm.mm.......",
        "....MMMMM.......",
        "....MMMMM.......",
        "....MMMMM.......",
        ".....MMM........",
        ".....MMM........",
        "......M.........",
        "................",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "malte.png"),
           _contorno(_hex16(malte, {"M": "A87838", "m": "C89850"})), {})

    # mosto de uva: jarra roxa
    mosto_u = [
        "................",
        "......ww........",
        "......ww........",
        ".....w..w.......",
        ".....w..w.......",
        "....w....w......",
        "....wggggw......",
        "....wggggw......",
        "....wggggw......",
        "....wggggw......",
        "....wggggw......",
        ".....wggw.......",
        "......ww........",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "mosto_de_uva.png"),
           _contorno(_hex16(mosto_u, {"w": "B8D8D8CC", "g": "7A2E52"})), {})

    # mosto lupulado: âmbar com lúpulo flutuando
    _png16(os.path.join(TEX_ITEM, "mosto_cerveja_lupulado.png"),
           _contorno(_hex16(mosto_c, {"w": "B8D8D8CC", "g": "C88828", "s": "9BC44D"})), {})

    # cachaça jovem / rum jovem: garrafas transparentes com líquido cristalino
    jovem = [
        "................",
        "......ww........",
        "......ww........",
        ".....w..w.......",
        ".....w..w.......",
        "....w....w......",
        "....wllllw......",
        "....wllllw......",
        "....wllllw......",
        "....wllllw......",
        "....wllllw......",
        ".....wllw.......",
        "......ww........",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "cachaca_jovem.png"),
           _contorno(_hex16(jovem, {"w": "B8D8D8CC", "l": "E8ECE8C8"})), {})
    _png16(os.path.join(TEX_ITEM, "rum_jovem.png"),
           _contorno(_hex16(jovem, {"w": "B8D8D8CC", "l": "C8A850C8"})), {})

    # bagaço: fibra esmagada marrom
    bagaco = [
        "................",
        "................",
        "................",
        "....ff..ff......",
        "..ffFFffFFf.....",
        "..fFFffFFff.....",
        "...ffFFffFf.....",
        "..fFffFFffFf....",
        "..ffFFffFFf.....",
        "...ff..ff.......",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "bagaco_de_cana.png"),
           _contorno(_hex16(bagaco, {"f": "8A7A48", "F": "A89860"})), {})


def _hex16(linhas, paleta):
    """Converte ASCII-art com cores hex em pixels RGBA (alpha opcional).
    Aceita "B8D8D8CC" (com alpha) ou "B8D060" (opaco)."""
    px = []
    for y in range(16):
        linha = []
        for x in range(16):
            c = linhas[y][x]
            if c in paleta:
                v = paleta[c]
                alpha = int(v[6:8], 16) if len(v) >= 8 else 255
                linha.append(_hex(v[0:6], alpha))
            else:
                linha.append((0, 0, 0, 0))
        px.append(linha)
    return px


# ============================================================ BLOCKSTATE / LOOT / ITEMS

BARRIS = ["cachaca", "cerveja", "rum", "vinho"]
MAQUINAS = [("moenda_cana",), ("prensa_uvas",), ("caldeirao_mostura",)]


def gerar_blockstates():
    for b in BARRIS:
        wjson(os.path.join(ASSETS, "blockstates", "barril_%s.json" % b),
              {"variants": {"": {"model": "intoxicantes:block/barril_" + b}}})
    for (m,) in MAQUINAS:
        wjson(os.path.join(ASSETS, "blockstates", m + ".json"),
              {"variants": {"": {"model": "intoxicantes:block/" + m}}})
    wjson(os.path.join(ASSETS, "blockstates", "dorna_bebida.json"),
          {"variants": {"": {"model": "intoxicantes:block/dorna_bebida"}}})
    wjson(os.path.join(ASSETS, "blockstates", "alambique.json"),
          {"variants": {"": {"model": "intoxicantes:block/alambique"}}})
    # cevada: crop vanilla de 8 ages — um modelo por age (nenhum par sem modelo)
    cevada_variants = {}
    for age in range(8):
        cevada_variants["age=%d" % age] = {
            "model": "intoxicantes:block/cevada_stage%d" % age}
    wjson(os.path.join(ASSETS, "blockstates", "cevada_plant.json"),
          {"variants": cevada_variants})
    # modelos cross dos 8 estágios da cevada (referenciados acima)
    for st in range(8):
        wjson(os.path.join(ASSETS, "models", "block", "cevada_stage%d.json" % st),
              {"parent": "minecraft:block/cross",
               "textures": {"cross": "intoxicantes:block/cevada_stage%d" % st}})


def gerar_item_defs():
    # barris e máquinas: item = o modelo 3D do bloco
    todos = ["barril_" + b for b in BARRIS] + [m for (m,) in MAQUINAS] \
        + ["dorna_bebida", "alambique"]
    for nome in todos:
        wjson(os.path.join(ASSETS, "items", nome + ".json"),
              {"model": {"type": "minecraft:model",
                          "model": "intoxicantes:block/" + nome}})
        wjson(os.path.join(ASSETS, "models", "item", nome + ".json"),
              {"parent": "intoxicantes:block/" + nome})
    # cevada plant (item do bloco = espiga madura)
    wjson(os.path.join(ASSETS, "items", "cevada_plant.json"),
          {"model": {"type": "minecraft:model",
                      "model": "intoxicantes:item/cevada_plant"}})
    wjson(os.path.join(ASSETS, "models", "item", "cevada_plant.json"),
          {"parent": "minecraft:item/generated",
           "textures": {"layer0": "intoxicantes:block/cevada_stage7"}})
    # itens intermediários
    for item in ["caldo_de_cana", "mosto_cana_fermentado", "mosto_rum_fermentado",
                 "melaco", "cevada", "malte", "mosto_de_uva", "mosto_cerveja_lupulado",
                 "cachaca_jovem", "rum_jovem", "bagaco_de_cana"]:
        wjson(os.path.join(ASSETS, "items", item + ".json"),
              {"model": {"type": "minecraft:model",
                          "model": "intoxicantes:item/" + item}})
        wjson(os.path.join(ASSETS, "models", "item", item + ".json"),
              {"parent": "minecraft:item/generated",
               "textures": {"layer0": "intoxicantes:item/" + item}})
    # semente de cevada (pacote kraft padrão das sementes do mod)
    wjson(os.path.join(ASSETS, "items", "semente_cevada.json"),
          {"model": {"type": "minecraft:model",
                      "model": "intoxicantes:item/semente_cevada"}})
    wjson(os.path.join(ASSETS, "models", "item", "semente_cevada.json"),
          {"parent": "minecraft:item/generated",
           "textures": {"layer0": "intoxicantes:item/semente_cevada"}})


def gerar_loot():
    def dropa_si(nome):
        return {
            "type": "minecraft:block",
            "pools": [{"rolls": 1,
                        "entries": [{"type": "minecraft:item",
                                      "name": "intoxicantes:" + nome}]}],
            "random_sequence": "intoxicantes:blocks/" + nome,
        }
    for b in BARRIS:
        wjson(os.path.join(DATA, "loot_table", "blocks", "barril_%s.json" % b),
              dropa_si("barril_" + b))
    for (m,) in MAQUINAS:
        wjson(os.path.join(DATA, "loot_table", "blocks", m + ".json"), dropa_si(m))
    wjson(os.path.join(DATA, "loot_table", "blocks", "dorna_bebida.json"),
          dropa_si("dorna_bebida"))
    wjson(os.path.join(DATA, "loot_table", "blocks", "alambique.json"),
          dropa_si("alambique"))
    # cevada: semente sempre; grão no estágio >= 4 (age 4..7, escalando)
    pools = [
        {"rolls": 1, "entries": [{"type": "minecraft:item",
                                   "name": "intoxicantes:semente_cevada"}]},
    ]
    for age in range(4, 8):
        qtd = age - 3  # age4=1 ... age7=4 grãos
        pools.append({
            "rolls": 1,
            "condition": {"type": "minecraft:match_block",
                           "blocks": "intoxicantes:cevada_plant",
                           "state": {"age": str(age)}},
            "entries": [{"type": "minecraft:item", "name": "intoxicantes:cevada",
                          "functions": [{"function": "minecraft:set_count",
                                          "count": qtd}]}],
        })
    wjson(os.path.join(DATA, "loot_table", "blocks", "cevada_plant.json"),
          {"type": "minecraft:block", "pools": pools,
           "random_sequence": "intoxicantes:blocks/cevada_plant"})


def main():
    gerar_texturas_blocos()
    gerar_texturas_maquinas()
    barril_modelo("cachaca", "barril_cachaca")
    barril_modelo("cerveja", "barril_cerveja", torneira=True)
    barril_modelo("rum", "barril_rum")
    barril_modelo("vinho", "barril_vinho")
    dorna_modelo()
    alambique_modelo()
    maquina_modelo("moenda_cana", "moenda_cana", "moenda")
    maquina_modelo("prensa_uvas", "prensa_uvas", "prensa")
    maquina_modelo("caldeirao_mostura", "caldeirao_mostura", "caldeirao")
    gerar_texturas_itens()
    gerar_cevada_texturas()
    gerar_blockstates()
    gerar_item_defs()
    gerar_loot()
    print("OK: assets da destilaria gerados (8 blocos 3D + 11 itens + cevada)")


def gerar_cevada_texturas():
    """8 estágios da cevada (cross, 1:1 com os ages do blockstate).

    Anatomia por colmo: haste com NÓS (articulações mais claras), folhas
    em arco saindo dos nós, e no fim a ESPIGA dourada com ARESTAS (as
    "barbas" — cerdas finas — da cevada real). Folhas amarelecem quando a
    planta matura, como no campo. O dourado é a assinatura do 'pronto'.
    """
    VERDE, VERDE_MED, VERDE_ESC = "6FA24A", "5A8C3A", "436E2C"
    AMARELO, PALHA, DOURADO, DOURADO_ESC = "C8B45A", "DCC878", "E0B850", "B08830"

    # -------- colmo: x fixo, nós, folhas em arco (interpola por estágio)
    def colmo(n):
        """n = nível 1..7 (altura/tecelagem crescente). Gera o grid 16x16."""
        g = [["."] * 16 for _ in range(16)]
        altura = 4 + n * 2                       # 6..18 -> corta em 15
        xs = [7] if n < 3 else ([5, 8] if n < 5 else [3, 7, 11])
        for x in xs:
            topo = max(15 - altura, 1)
            for y in range(15, topo - 1, -1):
                g[y][x] = VERDE_MED
                if (15 - y) % 4 == 2 and y > topo + 2:
                    g[y][x] = VERDE              # nó mais claro
            # lâminas em arco CAÍDO abrindo pra FORA do centro da planta
            if n >= 2:
                for ny in range(13, topo, -4):
                    lado = -1 if x < 7 else (1 if x > 8 else (1 if x % 2 else -1))
                    for i in range(1, 4):
                        fx = x + lado * i
                        fy = ny + i - 1              # cai conforme abre (lâmina)
                        if 0 <= fx < 16 and 0 <= fy < 16 and g[fy][fx] == ".":
                            g[fy][fx] = VERDE_ESC if i == 3 else VERDE_MED
        # brotinho (nível 0): dois cotilédones em V no topo do broto
        if n == 0:
            g[11][6], g[11][8] = VERDE, VERDE
            g[10][5], g[10][9] = VERDE_ESC, VERDE_ESC
        return g, xs, topo

    def espiga(g, xs, topo):
        """Espiga dourada com arestas (barbas) no topo de cada colmo."""
        for x in xs:
            base = topo
            corpo = 5 if base > 6 else 4
            for i, y in enumerate(range(base, base + corpo)):
                if y > 15:
                    break
                g[y][x] = DOURADO if i % 2 == 0 else DOURADO_ESC
                # grãos laterais (espiga volumosa)
                if 0 < x < 15:
                    g[y][x - 1] = PALHA if i % 2 == 0 else AMARELO
                    g[y][x + 1] = AMARELO if i % 2 == 0 else PALHA
            # arestas/barbas subindo da ponta
            for y in range(max(topo - 3, 0), topo):
                if g[y][x] == ".":
                    g[y][x] = PALHA
                if 0 < x < 15 and g[y][x + 1] == ".":
                    g[y][x + 1] = PALHA

    def amarelecer(g):
        """Folhas e colmo amarelando quando a espiga madura (sGRA→palha)."""
        mapa = {VERDE: AMARELO, VERDE_MED: PALHA, VERDE_ESC: "A89448"}
        for y in range(16):
            for x in range(16):
                if g[y][x] in mapa:
                    g[y][x] = mapa[g[y][x]]

    for stage in range(8):
        n = min(stage, 7)
        g, xs, topo = colmo(n)
        if stage >= 6:                            # espigas aparecem e firmam
            espiga(g, xs, topo)
        if stage == 7:
            amarelecer(g)                         # plantio maduro: palha+dourado
        px = []
        for y in range(16):
            linha = []
            for x in range(16):
                c = g[y][x]
                linha.append(_hex(c) if c != "." else (0, 0, 0, 0))
            px.append(linha)
        _png(os.path.join(TEX, "cevada_stage%d.png" % stage), px)
    print("cevada: 8 estágios regenerados (cross, sem contorno)")
    # semente de cevada: pacote kraft padrão das sementes do mod (mini-16x16)
    semente = [
        "................",
        "................",
        "..pppppppppp....",
        "..p........p....",
        "..p.gh..hg.p....",
        "..p........p....",
        "..p..h..h..p....",
        "..p........p....",
        "..pppppppppp....",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
    ]
    _png16(os.path.join(TEX_ITEM, "semente_cevada.png"),
           _contorno(_hex16(semente, {"p": "B89858", "g": "6B9C44", "h": "D8B858"})), {})


if __name__ == "__main__":
    main()
