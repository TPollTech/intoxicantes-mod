#!/usr/bin/env python3
"""v1.2.58 — Gera as texturas novas em ALTA RESOLUÇÃO (1254×1254), o mesmo
padrão do pack HD que o mod já embute (asfalto, lsd, lúpulo, hidrante...).
Tudo pintado direto em alta resolução com sombreamento (nada de upscale)."""
from PIL import Image, ImageDraw, ImageFilter
import math
import os

RAIZ = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                    "assets", "intoxicantes", "textures")
T = 1254  # resolução padrão do pack HD do mod

def novo():
    return Image.new("RGBA", (T, T), (0, 0, 0, 0))

def elipse(d, cx, cy, rx, ry, fill, outline=None, w=6):
    d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=fill,
              outline=outline, width=w if outline else 0)

def poligono(d, pts, fill, outline=None, w=6):
    d.polygon(pts, fill=fill, outline=outline, width=w if outline else 0)

def salvar(img, pasta, nome):
    img.save(os.path.join(RAIZ, pasta, nome + ".png"))
    print(f"  {pasta}/{nome}.png ({T}x{T})")

# luz vindo do canto superior-esquerdo: brilho no topo-esq, sombra no baixo-dir
def brilho_sombra(d, cx, cy, rx, ry, tom, k=0.22):
    tom_brilho = tuple(min(255, int(c + (255 - c) * k)) for c in tom[:3]) + (255,)
    tom_sombra = tuple(int(c * (1 - k)) for c in tom[:3]) + (255,)
    d.ellipse([cx - rx * 0.72, cy - ry * 0.72, cx + rx * 0.25, cy + ry * 0.1],
              fill=tom_brilho)
    d.ellipse([cx + rx * 0.1, cy + ry * 0.2, cx + rx * 0.95, cy + ry * 0.98],
              fill=tom_sombra)

# ============================================================ SUCO DETOX
img = novo(); d = ImageDraw.Draw(img)
MARROM = (74, 48, 22, 255)
VERDE = (56, 142, 68, 255)
VERDE_CLARO = (108, 190, 112, 255)
# corpo da garrafa (com contorno)
elipse(d, 627, 900, 330, 260, VERDE, MARROM, 14)
d.rectangle([297, 520, 957, 930], fill=VERDE, outline=MARROM, width=14)
# líquido mais claro no topo (brilho)
d.rectangle([320, 545, 934, 660], fill=VERDE_CLARO)
d.rounded_rectangle([470, 180, 784, 540], 60, fill=VERDE, outline=MARROM, width=14)
d.rounded_rectangle([505, 215, 748, 500], 40, fill=(180, 226, 186, 255))
# bolhas
for (bx, by, br) in [(520, 760, 26), (700, 820, 34), (830, 730, 20), (620, 1010, 24)]:
    d.ellipse([bx - br, by - br, bx + br, by + br], fill=(198, 238, 202, 220))
# tampa de metal
d.rounded_rectangle([440, 90, 814, 210], 40, fill=(148, 108, 52, 255),
                    outline=MARROM, width=12)
d.line([470, 130, 784, 130], fill=(196, 152, 88, 255), width=14)
# brilho de vidro lateral
d.line([360, 560, 360, 1000], fill=(235, 250, 236, 120), width=40)
salvar(img, "item", "suco_detox")

# ============================================================ ÁGUA DE COCO
img = novo(); d = ImageDraw.Draw(img)
CASCA = (58, 122, 48, 255)
CASCA_ESCURA = (36, 86, 32, 255)
AGUA = (240, 246, 228, 255)
AGUA_CLARA = (252, 253, 248, 255)
# coco verde deitado (corte pra cima)
elipse(d, 627, 760, 400, 330, CASCA, CASCA_ESCURA, 18)
# faixa de casca mais clara
d.arc([247, 450, 1007, 1070], 190, 330, fill=(96, 168, 82, 255), width=40)
# corte superior (tampa aberta)
elipse(d, 627, 430, 330, 150, (44, 100, 40, 255), CASCA_ESCURA, 14)
elipse(d, 627, 430, 290, 118, AGUA)
elipse(d, 560, 400, 110, 40, AGUA_CLARA)
# água transbordando
poligono(d, [(590, 520), (664, 520), (700, 640), (554, 640)], AGUA)
# folhinhas de coqueiro atrás
for (x1, y1, x2, y2, x3, y3) in [(180, 260, 420, 200, 360, 340),
                                 (1074, 260, 834, 200, 894, 340)]:
    poligono(d, [(x1, y1), (x2, y2), (x3, y3)], (74, 148, 62, 255),
             (44, 104, 40, 255), 10)
# olhos do coco na base
elipse(d, 500, 950, 34, 28, (26, 62, 24, 255))
elipse(d, 627, 985, 34, 28, (26, 62, 24, 255))
elipse(d, 754, 950, 34, 28, (26, 62, 24, 255))
salvar(img, "item", "agua_de_coco")

# ============================================================ COCO (fruto)
img = novo(); d = ImageDraw.Draw(img)
COCO = (116, 82, 46, 255)
COCO_CLARO = (168, 130, 84, 255)
COCO_ESCURO = (74, 50, 26, 255)
# fibra externa (fios na diagonal)
for i in range(-T, T * 2, 90):
    d.line([i, -40, i + T + 80, T + 40], fill=(94, 66, 36, 255), width=26)
elipse(d, 627, 640, 400, 400, COCO, COCO_ESCURO, 16)
brilho_sombra(d, 627, 640, 390, 390, COCO, k=0.25)
d.ellipse([330, 300, 640, 560], fill=COCO_CLARO)
# os 3 olhos (triângulo)
for (ex, ey) in [(540, 700), (714, 700), (627, 830)]:
    elipse(d, ex, ey, 42, 40, (34, 22, 10, 255))
    d.ellipse([ex - 14, ey - 14, ex + 4, ey + 2], fill=(86, 60, 34, 255))
# sombra de contato na base
d.ellipse([400, 1010, 854, 1080], fill=(52, 34, 16, 255))
salvar(img, "item", "coco")

# ============================================================ CHÁ DE LÚPULO
img = novo(); d = ImageDraw.Draw(img)
CHA = (198, 138, 62, 255)
CHA_CLARO = (232, 180, 104, 255)
VIDRO = (226, 240, 244, 235)
# caneca reta de vidro
d.rounded_rectangle([320, 340, 934, 1080], 70, fill=VIDRO, outline=(60, 46, 30, 255), width=14)
# chá dentro (com espaço do topo)
d.rounded_rectangle([356, 430, 898, 1046], 50, fill=CHA)
d.rounded_rectangle([356, 430, 898, 560], 50, fill=CHA_CLARO)
# espuma/borda
d.rounded_rectangle([320, 300, 934, 380], 40, fill=(244, 240, 228, 255),
                    outline=(60, 46, 30, 255), width=12)
# alça
d.arc([900, 480, 1150, 880], 300, 60, fill=(60, 46, 30, 255), width=36)
d.arc([908, 500, 1130, 860], 300, 60, fill=VIDRO, width=18)
# flor de lúpulo caindo no chá (cone verde-claro com pétalas)
elipse(d, 560, 640, 96, 116, (150, 190, 96, 255), (94, 132, 56, 255), 10)
for i, ang in enumerate(range(-60, 61, 30)):
    a = math.radians(ang)
    px = 560 + 60 * math.sin(a)
    py = 640 + 80 * math.cos(a) - 40
    elipse(d, px, py, 34, 44, (170, 210, 112, 255), (94, 132, 56, 255), 8)
# vapor subindo
for (vx, vy, vr) in [(560, 250, 60), (700, 180, 76), (640, 110, 52)]:
    d.ellipse([vx - vr, vy - vr * 1.6, vx + vr, vy + vr * 1.6],
              fill=(250, 250, 250, 70))
# brilho do vidro
d.line([390, 420, 390, 1000], fill=(250, 254, 255, 140), width=36)
salvar(img, "item", "cha_lupulo")

# ============================================================ PÃO DE CEVADA
img = novo(); d = ImageDraw.Draw(img)
PAO = (188, 132, 66, 255)
PAO_CLARO = (222, 172, 100, 255)
PAO_ESCURO = (128, 86, 40, 255)
# pão redondo achatado
elipse(d, 627, 760, 470, 320, PAO, PAO_ESCURO, 18)
elipse(d, 627, 640, 440, 260, PAO_CLARO)
elipse(d, 560, 560, 220, 120, (240, 202, 134, 255))
# cortes diagonais da crosta
for x0 in (430, 620, 810):
    d.line([x0, 560, x0 + 90, 780], fill=PAO_ESCURO, width=24)
# grãos de cevada salpicados
for (gx, gy) in [(420, 640), (560, 700), (700, 620), (820, 700), (520, 860),
                 (760, 880), (640, 940)]:
    poligono(d, [(gx - 36, gy), (gx, gy - 22), (gx + 36, gy), (gx, gy + 22)],
             (150, 106, 48, 255), (100, 68, 30, 255), 8)
# sombra de contato
d.ellipse([300, 1010, 954, 1090], fill=(96, 64, 28, 255))
salvar(img, "item", "pao_cevada")

# ============================================================ BLOCOS DO COQUEIRO
# tronco lateral: fibra vertical com anéis
img = Image.new("RGBA", (T, T), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
BASE_TRONCO = (150, 112, 64, 255)
for y in range(0, T, 36):  # fibras verticais levemente onduladas
    pts = []
    for yy in range(-20, T + 40, 60):
        pts.append((y + 18 * math.sin(yy / 140.0 + y), yy))
    d.line(pts, fill=(120, 88, 48, 255), width=16)
d.rectangle([0, 0, T, T], fill=None)
elipse(d, T // 2, T // 2, 900, 900, (0, 0, 0, 0))  # noop de forma segura
img = Image.new("RGBA", (T, T), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
d.rectangle([0, 0, T, T], fill=BASE_TRONCO)
for y in range(0, T, 36):
    pts = [(y + 18 * math.sin(yy / 140.0 + y), yy) for yy in range(-20, T + 40, 60)]
    d.line(pts, fill=(122, 90, 50, 255), width=16)
for y in range(0, T, 36):
    pts = [(y + 18 * math.sin(yy / 140.0 + y) + 14, yy) for yy in range(-20, T + 40, 60)]
    d.line(pts, fill=(178, 138, 86, 255), width=10)
# anéis horizontais das marcas dos galhos
for ay in (300, 640, 980):
    d.rectangle([0, ay - 24, T, ay + 24], fill=(104, 76, 42, 255))
    d.line([0, ay - 24, T, ay - 24], fill=(190, 152, 100, 255), width=10)
salvar(img, "block", "coqueiro_tronco")

# tronco topo: anéis concêntricos
img = novo(); d = ImageDraw.Draw(img)
d.rectangle([0, 0, T, T], fill=(104, 76, 42, 255))
for raio, cor in [(600, (150, 112, 64, 255)), (460, (178, 138, 86, 255)),
                  (320, (150, 112, 64, 255)), (180, (178, 138, 86, 255)),
                  (80, (122, 90, 50, 255))]:
    elipse(d, T // 2, T // 2, raio, raio, cor)
# fibras radiais
for a in range(0, 360, 15):
    rad = math.radians(a)
    d.line([T // 2 + 90 * math.cos(rad), T // 2 + 90 * math.sin(rad),
            T // 2 + 600 * math.cos(rad), T // 2 + 600 * math.sin(rad)],
           fill=(122, 90, 50, 120), width=6)
salvar(img, "block", "coqueiro_tronco_topo")

# folhas: verde tropical com folíolos
img = novo(); d = ImageDraw.Draw(img)
d.rectangle([0, 0, T, T], fill=(52, 116, 48, 255))
# folíolos diagonais claros e escuros (como folha de coqueiro vista de cima)
for i in range(-T * 2, T * 2, 110):
    d.line([i, 0, i + T, T], fill=(88, 160, 74, 255), width=34)
    d.line([i + 55, 0, i + 55 + T, T], fill=(30, 78, 30, 255), width=30)
# nervura central
d.line([-40, -40, T + 40, T + 40], fill=(22, 58, 22, 200), width=26)
# furos de transparência espalhados (estilo folha vanilla)
furos = [(180, 240), (760, 160), (420, 700), (1020, 560), (240, 980), (880, 1020)]
for (fx, fy) in furos:
    elipse(d, fx, fy, 46, 36, (0, 0, 0, 0))
salvar(img, "block", "coqueiro_folhas")

# coco bloco: casca marrom fibrosa com os 3 olhos
img = novo(); d = ImageDraw.Draw(img)
d.rectangle([0, 0, T, T], fill=COCO)
# fibra em linhas onduladas
for y in range(0, T, 80):
    pts = [(x, y + 22 * math.sin(x / 120.0 + y)) for x in range(-20, T + 40, 40)]
    d.line(pts, fill=(94, 66, 36, 255), width=20)
for y in range(40, T, 80):
    pts = [(x, y + 22 * math.sin(x / 120.0 + y)) for x in range(-20, T + 40, 40)]
    d.line(pts, fill=(150, 112, 66, 255), width=14)
d.rectangle([0, 0, T, T], outline=(58, 40, 20, 255), width=24)
# os 3 olhos
for (ex, ey) in [(470, 560), (784, 560), (627, 800)]:
    elipse(d, ex, ey, 64, 60, (30, 20, 10, 255), (20, 14, 8, 255), 10)
    d.ellipse([ex - 20, ey - 20, ex + 8, ey + 4], fill=(96, 68, 38, 255))
salvar(img, "block", "coco")

# ============================================================ ÍCONES DOS EFEITOS
def icone_fundo(cor_base, cor_clara):
    i = novo(); dd = ImageDraw.Draw(i)
    elipse(dd, T // 2, T // 2, 610, 610, cor_base)
    elipse(dd, T // 2 - 180, T // 2 - 220, 330, 260, cor_clara)
    return i

def icone_salva(img, nome):
    salvar(img, "mob_effect", nome)

# TRANQUILO: folha relaxada
img = icone_fundo((72, 118, 62, 255), (88, 138, 76, 255)); d = ImageDraw.Draw(img)
poligono(d, [(627, 250), (980, 500), (760, 900), (500, 900), (280, 500)],
         (46, 84, 40, 255), (28, 56, 26, 255), 14)
poligono(d, [(627, 330), (860, 520), (700, 800), (560, 800), (400, 520)],
         (110, 170, 96, 255))
d.line([627, 300, 627, 880], fill=(28, 56, 26, 255), width=18)
d.line([627, 880, 627, 1010], fill=(46, 84, 40, 255), width=26)
icone_salva(img, "tranquilo")

# MORNO: sol quente
img = icone_fundo((150, 98, 48, 255), (170, 118, 62, 255)); d = ImageDraw.Draw(img)
elipse(d, 627, 627, 240, 240, (232, 168, 74, 255), (180, 120, 48, 255), 12)
elipse(d, 627, 627, 150, 150, (248, 204, 122, 255))
for a in range(0, 360, 30):
    rad = math.radians(a)
    x1 = 627 + 300 * math.cos(rad); y1 = 627 + 300 * math.sin(rad)
    x2 = 627 + 420 * math.cos(rad); y2 = 627 + 420 * math.sin(rad)
    d.line([x1, y1, x2, y2], fill=(240, 184, 92, 255), width=40)
icone_salva(img, "morno")

# SONHO: lua crescente
img = icone_fundo((98, 76, 132, 255), (114, 92, 150, 255)); d = ImageDraw.Draw(img)
elipse(d, 570, 600, 280, 280, (222, 214, 244, 255), (150, 140, 190, 255), 12)
elipse(d, 700, 540, 250, 250, (114, 92, 150, 255))
for (sx, sy) in [(300, 300), (940, 950), (280, 940)]:
    poligono(d, [(sx, sy - 50), (sx + 14, sy - 14), (sx + 50, sy),
                 (sx + 14, sy + 14), (sx, sy + 50), (sx - 14, sy + 14),
                 (sx - 50, sy), (sx - 14, sy - 14)], (230, 224, 250, 255))
icone_salva(img, "sonho")

# OVERDRIVE: raio branco
img = icone_fundo((196, 196, 200, 255), (216, 216, 220, 255)); d = ImageDraw.Draw(img)
poligono(d, [(700, 180), (460, 660), (620, 660), (520, 1080),
             (860, 560), (680, 560), (820, 180)],
         (255, 255, 255, 255), (150, 150, 158, 255), 12)
icone_salva(img, "overdrive")

# VIAGEM: olho psicodélico arco-íris
img = icone_fundo((132, 48, 168, 255), (152, 68, 188, 255)); d = ImageDraw.Draw(img)
for raio, cor in [(520, (240, 80, 80, 255)), (420, (240, 180, 60, 255)),
                  (320, (120, 220, 100, 255)), (220, (80, 160, 240, 255))]:
    elipse(d, 627, 627, raio, raio * 0.86, cor)
elipse(d, 627, 627, 130, 130, (24, 12, 28, 255))
elipse(d, 580, 580, 40, 40, (255, 255, 255, 255))
icone_salva(img, "viagem")

# ABSTINENCIA: caveira
img = icone_fundo((70, 70, 86, 255), (86, 86, 104, 255)); d = ImageDraw.Draw(img)
elipse(d, 627, 560, 320, 300, (206, 206, 214, 255), (120, 120, 134, 255), 12)
d.rounded_rectangle([500, 780, 754, 960], 60, fill=(206, 206, 214, 255),
                    outline=(120, 120, 134, 255), width=12)
elipse(d, 510, 560, 90, 100, (48, 48, 60, 255))
elipse(d, 744, 560, 90, 100, (48, 48, 60, 255))
poligono(d, [(627, 640), (664, 740), (590, 740)], (48, 48, 60, 255))
for tx in (540, 627, 714):
    d.line([tx, 830, tx, 940], fill=(120, 120, 134, 255), width=14)
icone_salva(img, "abstinencia")

print("Texturas da 1.2.58 geradas em HD (1254x1254).")
