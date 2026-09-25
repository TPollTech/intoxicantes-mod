# -*- coding: utf-8 -*-
"""PORTA-GRADE DO ESQUINÃO (v1.2.55) — gerador completo, fonte única.

Gera em src/main/resources/assets/intoxicantes/:
  blockstates/porta_grade.json     — facing × half × fechada (SEM lit: o bloco
                                     não tem a propriedade, e variante que não
                                     casa = xadrez rosa no jogo)
  models/block/porta_grade*.json   — 4 modelos 3D HD (aberta/fechada × baixo/topo)
  textures/block/porta_grade*.png  — 5 texturas 128×128 (uma por material)
  models/item/porta_grade.json     — item com display (porta inteira)
  items/porta_grade.json           — definição de item da 26.3 (SEM ela o
                                     inventário mostra o cubo "desconhecido")

v1.2.55 — conserto da estética: os modelos agora declaram "texture_size"
(128×128) e cada material tem textura PRÓPRIA (madeira, ferro, soleira, verga,
grade). Antes: um atlas só e faces sem UV explícito varriam o atlas inteiro —
madeira/pedra/ferro misturados na mesma face (o "esticado" do playtest).

Anatomia (2 blocos de altura, vãos reais):
  BAIXO (aberta): moldura de madeira com painel rebaixo + soleira + o VÃO da
                  janela do guichê (de dia o freguês fala por aqui).
  BAIXO (fechada): painel de madeira inteiro (colisão plena vem do VoxelShape).
  TOPO  (aberto): verga de madeira + vão com grade de ferro (barras 1px).
  TOPO  (fechado): verga + grade completa + travessa reforçada + cadeado.

Uso: python tools/gen_porta_grade.py  (da raiz do projeto do mod)
"""
import json
import os
import random

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "intoxicantes")
MB = os.path.join(ASSETS, "models", "block")
TB = os.path.join(ASSETS, "textures", "block")
MI = os.path.join(ASSETS, "models", "item")
IT = os.path.join(ASSETS, "items")
BS = os.path.join(ASSETS, "blockstates")

Y = {"north": 0, "east": 90, "south": 180, "west": 270}
HALF = {"lower": ("porta_grade", "porta_grade_fechada"),
        "upper": ("porta_grade_topo", "porta_grade_topo_fechado")}

T = 128          # cada textura de material em 128×128
TEXTURE_SIZE = [T, T]


# ============================================================ TEXTURAS HD
def _grao(g, x0, y0, x1, y1, tons, qtd, rnd):
    for _ in range(qtd):
        x, y = rnd.randint(x0, x1 - 1), rnd.randint(y0, y1 - 1)
        g.point((x, y), fill=rnd.choice(tons))


def textura_madeira():
    """porta_grade.png — spruce envernizado, tábuas verticais + grão + nós."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    g = ImageDraw.Draw(img)
    rnd = random.Random(512)
    for i, x in enumerate(range(0, T, 16)):          # 8 tábuas de 16px
        g.rectangle([x, 0, x + 15, T - 1],
                    fill="#84643f" if i % 2 else "#7a5b3a")
        g.line([x, 0, x, T - 1], fill="#5f4629", width=1)   # fresta
        g.line([x + 15, 0, x + 15, T - 1], fill="#6b5030", width=1)
        for _ in range(46):                           # grão vertical
            gx = x + 2 + rnd.randint(0, 12)
            g.point((gx, rnd.randint(0, T - 1)),
                    fill=rnd.choice(["#6d5233", "#8d6c46", "#735635"]))
        # nó de madeira (anel + centro)
        ny = rnd.randint(16, T - 20)
        g.ellipse([x + 6, ny, x + 10, ny + 5], outline="#4f3a20", fill="#5d4527")
        g.point((x + 8, ny + 2), fill="#3f2e18")
    # brilho de verniz (faixa diagonal sutil)
    for d in range(T):
        if d % 7 < 3:
            g.point((d, (d * 2) % T), fill="#96744c")
    img.save(os.path.join(TB, "porta_grade.png"))


def textura_ferro():
    """porta_grade_ferro.png — ferro forjado escuro com brilho de topo."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    g = ImageDraw.Draw(img)
    rnd = random.Random(256)
    g.rectangle([0, 0, T - 1, T - 1], fill="#4a4d52")
    _grao(g, 0, 0, T, T, ["#43464b", "#54575d", "#3d4045"], 1600, rnd)
    g.rectangle([0, 0, T - 1, 4], fill="#6a6e74")     # luz de topo
    g.rectangle([0, T - 5, T - 1, T - 1], fill="#33363a")  # sombra de base
    # vincos de forja (colunas suaves a cada 32px)
    for x in range(0, T, 32):
        g.line([x, 0, x, T - 1], fill="#40434a", width=2)
        g.line([x + 2, 0, x + 2, T - 1], fill="#565a61", width=1)
    img.save(os.path.join(TB, "porta_grade_ferro.png"))


def textura_soleira():
    """porta_grade_soleira.png — pedra polida (o degrau do balcão)."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    g = ImageDraw.Draw(img)
    rnd = random.Random(1024)
    g.rectangle([0, 0, T - 1, T - 1], fill="#8f8f8f")
    _grao(g, 0, 0, T, T, ["#848484", "#9a9a9a", "#7c7c7c", "#909090"], 1300, rnd)
    # juntas de pedra (blocos 32×32 com fratura central)
    for x in range(0, T, 32):
        g.line([x, 0, x, T - 1], fill="#6f6f6f", width=2)
    for y in range(0, T, 32):
        g.line([0, y, T - 1, y], fill="#6f6f6f", width=2)
    for _ in range(24):                               # lascas
        x, y = rnd.randint(2, T - 6), rnd.randint(2, T - 6)
        g.point((x, y), fill="#a5a5a5")
        g.point((x + 1, y + 1), fill="#777777")
    img.save(os.path.join(TB, "porta_grade_soleira.png"))


def textura_verga():
    """porta_grade_verga.png — madeira escura (verga/travessa de cima)."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    g = ImageDraw.Draw(img)
    rnd = random.Random(64)
    g.rectangle([0, 0, T - 1, T - 1], fill="#4c3822")
    _grao(g, 0, 0, T, T, ["#42311d", "#574127", "#3a2a18", "#4f3b24"], 1400, rnd)
    for x in range(0, T, 16):                         # tábuas mais escuras
        g.line([x, 0, x, T - 1], fill="#33250f", width=1)
    g.rectangle([0, 0, T - 1, 3], fill="#5f4728")     # luz de topo
    img.save(os.path.join(TB, "porta_grade_verga.png"))


def textura_grade():
    """porta_grade_grade.png — barras de ferro sobre alfa (cutout)."""
    img = Image.new("RGBA", (T, T), (0, 0, 0, 0))
    g = ImageDraw.Draw(img)
    rnd = random.Random(777)
    for bx in range(16, T, 32):                       # barras verticais
        g.rectangle([bx - 6, 0, bx + 6, T - 1], fill="#3f4247")
        g.line([bx - 3, 0, bx - 3, T - 1], fill="#585c62")
        g.line([bx + 3, 0, bx + 3, T - 1], fill="#2e3135")
    for by in (16, 64, 112):                          # travessas horizontais
        g.rectangle([0, by - 7, T - 1, by + 7], fill="#45484d")
        g.line([0, by - 4, T - 1, by - 4], fill="#5d6167")
        g.line([0, by + 4, T - 1, by + 4], fill="#2e3135")
    for bx in range(16, T, 32):                       # remaches nas cruzetas
        for by in (16, 64, 112):
            g.ellipse([bx - 10, by - 10, bx + 10, by + 10], fill="#6b6f75")
            g.ellipse([bx - 6, by - 6, bx + 2, by + 2], fill="#83878d")
    for _ in range(500):                              # desgaste
        x, y = rnd.randint(0, T - 1), rnd.randint(0, T - 1)
        if img.getpixel((x, y))[3] > 0:
            g.point((x, y), fill="#383b3f")
    img.save(os.path.join(TB, "porta_grade_grade.png"))


# ============================================================ MODELOS 3D
def _box(f, t, x0, y0, z0, x1, y1, z1, cull=None):
    """Elemento com todas as faces na textura `t` (UV padrão por bounding)."""
    el = {
        "from": [x0, y0, z0], "to": [x1, y1, z1],
        "faces": {
            "down": {"texture": f"#{t}"}, "up": {"texture": f"#{t}"},
            "north": {"texture": f"#{t}"}, "south": {"texture": f"#{t}"},
            "west": {"texture": f"#{t}"}, "east": {"texture": f"#{t}"},
        },
    }
    if cull:
        for face in cull:
            el["faces"][face]["cullface"] = face
    return el


def _base(texturas):
    # texture_size: a 26.3 interpreta UV em unidades de 16 sobre ESTE tamanho —
    # sem ele, textura 128 é lida como 16 e cada face esparrama o atlas.
    return {"parent": "block/block", "texture_size": TEXTURE_SIZE,
            "textures": texturas}


def modelo_baixo(fechada):
    """Metade de baixo: moldura + painel (fechada) ou balcão com vão (aberta)."""
    tex = {"particle": "#madeira",
           "madeira": "intoxicantes:block/porta_grade",
           "ferro": "intoxicantes:block/porta_grade_ferro",
           "soleira": "intoxicantes:block/porta_grade_soleira",
           "verga": "intoxicantes:block/porta_grade_verga"}
    el = [
        # pilares laterais (batentes de porta real)
        _box(None, "madeira", 0, 0, 0, 2, 16, 4),
        _box(None, "madeira", 14, 0, 0, 16, 16, 4),
        # travessa superior do vão (verga escura = contraste)
        _box(None, "verga", 2, 13, 0, 14, 16, 4, cull=["up"]),
        # soleira de pedra na frente (o degrau do balcão)
        _box(None, "soleira", 1, 0, 4, 15, 1, 5),
        # canaleta de ferro lateral (guias do guichê)
        _box(None, "ferro", 1, 1, 4, 2, 16, 5),
        _box(None, "ferro", 14, 1, 4, 15, 16, 5),
    ]
    if fechada:
        # painel cheio rebaixo com moldura saliente (colisão plena é do bloco)
        el += [
            _box(None, "madeira", 2, 1, 0, 14, 13, 3),
            _box(None, "verga", 2, 6, 3, 14, 8, 4),    # travessa do guichê
            _box(None, "ferro", 6, 6, 4, 10, 8, 5),    # plaquinha "FECHADO"
        ]
    else:
        # aberta: meia-parede (balcão) + vão de atendimento por cima
        el += [
            _box(None, "madeira", 2, 1, 0, 14, 7, 3),
            _box(None, "verga", 2, 7, 0, 14, 9, 4),    # borda do balcão
            _box(None, "ferro", 3, 7, 4, 13, 8, 5),    # peitoril de ferro
        ]
    m = _base(tex)
    m["elements"] = el
    return m


def modelo_topo(fechado):
    """Metade de cima: verga + grade de ferro (fechado) ou vão gradeado (aberto).
    Cutout: a textura da grade tem alfa (barras sobre transparente)."""
    tex = {"particle": "#madeira",
           "madeira": "intoxicantes:block/porta_grade",
           "ferro": "intoxicantes:block/porta_grade_ferro",
           "verga": "intoxicantes:block/porta_grade_verga",
           "grade": "intoxicantes:block/porta_grade_grade"}
    el = [
        # verga de madeira (continuação da travessa)
        _box(None, "verga", 0, 12, 0, 16, 16, 4, cull=["up"]),
        # montantes de ferro nas bordas
        _box(None, "ferro", 0, 0, 0, 2, 12, 4),
        _box(None, "ferro", 14, 0, 0, 16, 12, 4),
    ]
    if fechado:
        # grade cheia + travessa reforçada no meio (o "tranca" da madrugada)
        el += [
            _box(None, "grade", 2, 0, 1, 14, 12, 3),
            _box(None, "ferro", 2, 5, 0, 14, 7, 4),
            _box(None, "ferro", 6, 0, 3, 10, 12, 4),   # cadeado central
        ]
    else:
        # aberto: vão com grade só na metade superior (olho mágico do guichê)
        el += [
            _box(None, "grade", 2, 6, 1, 14, 12, 3),
            _box(None, "madeira", 2, 0, 0, 14, 5, 3),  # arremato inferior
        ]
    m = _base(tex)
    m["render_type"] = "minecraft:cutout"
    m["elements"] = el
    return m


# ============================================================ SAÍDAS
def blockstate():
    variants = {}
    for facing, y in Y.items():
        for half, (aberto, fechado) in HALF.items():
            for fechada in (False, True):
                modelo = fechado if fechada else aberto
                variants[f"facing={facing},half={half},"
                         f"fechada={'true' if fechada else 'false'}"] = {
                    "model": f"intoxicantes:block/{modelo}",
                    "y": y, "uvlock": False,
                }
    destino = os.path.join(BS, "porta_grade.json")
    os.makedirs(os.path.dirname(destino), exist_ok=True)
    with open(destino, "w", encoding="utf-8") as f:
        json.dump({"variants": variants}, f, indent=2, ensure_ascii=False)
        f.write("\n")
    return len(variants)


def main():
    os.makedirs(MB, exist_ok=True)
    os.makedirs(TB, exist_ok=True)
    os.makedirs(MI, exist_ok=True)
    os.makedirs(IT, exist_ok=True)

    textura_madeira()
    textura_ferro()
    textura_soleira()
    textura_verga()
    textura_grade()

    modelos = {
        "porta_grade": modelo_baixo(False),
        "porta_grade_fechada": modelo_baixo(True),
        "porta_grade_topo": modelo_topo(False),
        "porta_grade_topo_fechado": modelo_topo(True),
    }
    for nome, m in modelos.items():
        with open(os.path.join(MB, nome + ".json"), "w", encoding="utf-8") as f:
            json.dump(m, f, indent=2, ensure_ascii=False)
            f.write("\n")

    n = blockstate()

    # item: mostra a porta INTEIRA (baixo + topo empilhados) com TODOS os
    # aliases dos dois modelos (na 1.2.53 faltou "soleira"/"verga" no item —
    # face roxa no ícone) + texture_size
    aliases = dict(modelos["porta_grade_topo"]["textures"])
    aliases.update(modelos["porta_grade"]["textures"])
    item = {
        "parent": "block/block",
        "texture_size": TEXTURE_SIZE,
        "textures": aliases,
        "elements": modelos["porta_grade"]["elements"]
                    + [{**e, "from": [e["from"][0], e["from"][1] + 16, e["from"][2]],
                        "to": [e["to"][0], e["to"][1] + 16, e["to"][2]]}
                       for e in modelos["porta_grade_topo"]["elements"]],
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, -2, 0],
                    "scale": [0.5, 0.5, 0.5]},
            "ground": {"translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 180, 0], "scale": [0.5, 0.5, 0.5]},
            "thirdperson_righthand": {"rotation": [75, 45, 0],
                                      "translation": [0, 2.5, 0],
                                      "scale": [0.375, 0.375, 0.375]},
        },
    }
    with open(os.path.join(MI, "porta_grade.json"), "w", encoding="utf-8") as f:
        json.dump(item, f, indent=2, ensure_ascii=False)
        f.write("\n")

    # definição de item da 26.3 — SEM ela o ícone é o cubo "desconhecido"
    definicao = {"model": {"type": "minecraft:model",
                           "model": "intoxicantes:item/porta_grade"}}
    with open(os.path.join(IT, "porta_grade.json"), "w", encoding="utf-8") as f:
        json.dump(definicao, f, indent=2, ensure_ascii=False)
        f.write("\n")

    print(f"OK: porta-grade — {len(modelos)} modelos 3D HD + 5 texturas 128x128 "
          f"(1 por material) + blockstate ({n} variantes, sem lit) "
          f"+ item inteiro + items/porta_grade.json")


if __name__ == "__main__":
    main()
