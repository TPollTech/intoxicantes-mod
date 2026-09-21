"""Gera texturas pixel-art dos itens de plantacao e da lampada UV.

v2: sementes = pacote de sementes (sache); plantacoes = plantas construidas
por codigo (caule + folhas + cachos, identidade por cultura); lampada UV =
luminaria industrial com tubos brilhantes (item) e painel de luz (bloco).

Complementa o gen_textures.py (que mantem as texturas dos itens/bebidas).
Uso: python tools/gen_farm_textures.py  (a partir da raiz do projeto do mod)
"""
import os
import struct
import zlib

ASSETS = os.path.join("src", "main", "resources", "assets", "intoxicantes")

def write_png(path, rows):
    h = len(rows)
    w = len(rows[0])
    def chunk(tag, data):
        c = tag + data
        return struct.pack(">I", len(data)) + c + struct.pack(">I", zlib.crc32(c) & 0xFFFFFFFF)
    ihdr = struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0)
    raw = b"".join(b"\x00" + b"".join(bytes(px) for px in row) for row in rows)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))

def rgba(s, alpha=255):
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), alpha)

def clareia(hexs, t):
    """Mistura a cor com branco (t=0..1) — highlight de fruto/folha."""
    r, g, b, a = rgba(hexs)
    return (r + int((255 - r) * t), g + int((255 - g) * t), b + int((255 - b) * t), a)

def escurece(hexs, t):
    """Mistura a cor com preto (t=0..1) — sombra de volume."""
    r, g, b, a = rgba(hexs)
    return (int(r * (1 - t)), int(g * (1 - t)), int(b * (1 - t)), a)

def render(map_lines, palette):
    rows = []
    for line in map_lines:
        row = []
        for ch in line:
            if ch == ".":
                row.append((0, 0, 0, 0))
            else:
                v = palette[ch]
                row.append(v if isinstance(v, tuple) else rgba(v))
        rows.append(row)
    return rows

def outline(rows, color_hex, alpha=255):
    col = rgba(color_hex, alpha)
    h, w = len(rows), len(rows[0])
    out = [list(r) for r in rows]
    for y in range(h):
        for x in range(w):
            if rows[y][x][3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                ny, nx = y + dy, x + dx
                if 0 <= ny < h and 0 <= nx < w and rows[ny][nx][3] >= 200:
                    out[y][x] = col
                    break
    return [tuple(p) for p in out]

# ---------------------------------------------------------------- culturas
# nome -> (caule escuro, caule claro, fruto, fruto maduro,
#          (semente escura, semente clara))
CROPS = {
    "maconha": ("3E2812", "4C8C3A", "6FBF4A", "B4EFA0", ("2E6B22", "8FD96A")),
    "lupulo": ("3E5A28", "5A8C3A", "8CC63F", "D4F07A", ("3E5A28", "9BD95A")),
    "uva": ("3E4A28", "5A7A3A", "8B5CF6", "D8B4FE", ("3E4A28", "B08CF0")),
    "cafe": ("4A3826", "6B5A3A", "C0392B", "E74C3C", ("4A3826", "D07050")),
    "papoula": ("2E4A20", "4A7A30", "D42A2A", "FF6B6B", ("2E4A20", "E86A6A")),
}

# ---------------------------------------------------------------- semente: pacote
def pacote_semente(semente_escura, semente_clara, acento):
    """Pacote de sementes (sache de papel kraft): costura no topo, broto
    estampado e etiqueta com a cor da cultura. MUITO mais legivel que
    graozinhos soltos no inventario."""
    return (
        [
            "................",
            "..oooooooooooo..",
            "..o.k.k.k.k..o..",  # costura do topo
            "..okkkkkkkkkko..",
            "..okkWWkkkkkko..",  # brilho do papel
            "..okkWggkkkkko..",  # broto estampado (folhinhas)
            "..okgggggkkkko..",  # broto estampado (corpo)
            "..okkWggkkkkko..",
            "..okkkkkkkkkko..",
            "..oaaaaaaaaaao..",  # etiqueta (faixa da cultura)
            "..oaF.sF.sF.ao..",  # graozinhos na etiqueta
            "..oaaaaaaaaaao..",
            "..okkkkkkkkkko..",
            "..okkkkkkkkkko..",
            "..oooooooooooo..",
            "................",
        ],
        {
            "o": "5A4326",                                  # borda do papel
            "k": "B8965C",                                  # kraft
            "W": "D4B784",                                  # kraft claro (brilho)
            "g": "4C8C3A",                                  # broto verde
            "a": clareia(acento, 0.25),                     # faixa da cultura
            "F": rgba(semente_escura),                      # grao escuro
            "s": rgba(semente_clara),                       # grao claro
        },
    )

# ---------------------------------------------------------------- planta (blocos)
def _grid():
    return [["." for _ in range(16)] for _ in range(16)]

def _caule(g, topo, base=14, x=7):
    """Caule reto: claro em cima, escuro na base + torrao de terra."""
    for y in range(topo, base + 1):
        g[y][x] = "g"
    g[base][x] = "d"
    g[15][x - 1] = "d"
    g[15][x] = "d"
    g[15][x + 1] = "d"

def _folha(g, y, lado, n, ch="g", cai=True):
    """Folha em DIAGONAL saindo do caule: sobe/desce 1px a cada 2 de distancia,
    lendo como folha caida (nao como barra solida). Ponta com 1px extra."""
    for i in range(1, n + 1):
        x = 7 + lado * i
        yy = y + ((i + 1) // 2 if cai else -((i + 1) // 2))
        if 0 <= x < 16 and 0 <= yy < 16:
            g[yy][x] = ch
    ponta_x, ponta_y = 7 + lado * (n + 1), y + ((n + 2) // 2 if cai else -((n + 2) // 2))
    if 0 <= ponta_x < 16 and 0 <= ponta_y < 16:
        g[ponta_y][ponta_x] = "d"  # ponta na sombra

def _cacho(g, pontos, ch="f"):
    for x, y in pontos:
        if 0 <= x < 16 and 0 <= y < 16:
            g[y][x] = ch

def _spec_maconha():
    """Maconha: BUDA (cola) redondinho no topo + folhas diagonais caidas."""
    g = _grid()
    _caule(g, topo=4)
    # buda: cluster 5x4 com topo arredondado (nao e' cruz, e' flor)
    _cacho(g, [(7, 0), (6, 1), (7, 1), (8, 1),
               (5, 2), (6, 2), (7, 2), (8, 2), (9, 2),
               (5, 3), (6, 3), (7, 3), (8, 3), (9, 3),
               (6, 4), (7, 4), (8, 4)])
    _folha(g, 6, +1, 4); _folha(g, 6, -1, 4)
    _folha(g, 9, +1, 3); _folha(g, 9, -1, 3)
    _folha(g, 11, +1, 2); _folha(g, 11, -1, 2)
    return g

def _spec_lupulo():
    """Lupulo: cones pendurados (ovais 2x3) descendo de conectores."""
    g = _grid()
    _caule(g, topo=3)
    # cone esquerdo: conecta em (5,3), oval em x4-5, y4-6
    g[3][5] = "d"
    _cacho(g, [(4, 4), (5, 4), (4, 5), (5, 5), (4, 6), (5, 6)])
    # cone direito: conecta em (9,4), oval em x9-10, y5-7
    g[4][9] = "d"
    _cacho(g, [(9, 5), (10, 5), (9, 6), (10, 6), (9, 7), (10, 7)])
    _folha(g, 6, +1, 3); _folha(g, 6, -1, 3)
    _folha(g, 9, +1, 2); _folha(g, 9, -1, 2)
    return g

def _spec_uva():
    """Uva: copa de folhas caidas em cima + cacho PENDURADO em losango."""
    g = _grid()
    _caule(g, topo=5)
    g[4][7] = "d"  # haste da copa
    _folha(g, 3, +1, 4); _folha(g, 3, -1, 4)
    _folha(g, 5, +1, 3); _folha(g, 5, -1, 3)
    # cacho em losango (5-4-3): largura maxima no meio
    _cacho(g, [(7, 7), (6, 8), (7, 8), (8, 8),
               (5, 9), (6, 9), (7, 9), (8, 9), (9, 9),
               (6, 10), (7, 10), (8, 10), (7, 11)])
    _folha(g, 10, +1, 2); _folha(g, 10, -1, 2)
    return g

def _spec_cafe():
    """Cafe: arbusto frondoso com BAGAS 2x2 em pares colados no caule."""
    g = _grid()
    _caule(g, topo=4)
    # copa: folhas caindo no topo
    _folha(g, 4, +1, 3); _folha(g, 4, -1, 3)
    _folha(g, 6, +1, 4); _folha(g, 6, -1, 4)
    # pares de bagas 2x2 (cafe cereja vem em pares no galho)
    _cacho(g, [(5, 7), (6, 7), (5, 8), (6, 8), (8, 7), (9, 7), (8, 8), (9, 8)])
    _folha(g, 9, +1, 3); _folha(g, 9, -1, 3)
    _cacho(g, [(5, 11), (6, 11), (5, 12), (6, 12), (8, 11), (9, 11), (8, 12), (9, 12)])
    _folha(g, 12, +1, 2); _folha(g, 12, -1, 2)
    return g

def _spec_papoula():
    """Papoula: haste fina + FLOR redonda grande no topo (capsula 5x4)."""
    g = _grid()
    _caule(g, topo=5)
    # flor: capsula redondinha 5x4
    _cacho(g, [(6, 1), (7, 1), (8, 1),
               (5, 2), (6, 2), (7, 2), (8, 2), (9, 2),
               (5, 3), (6, 3), (7, 3), (8, 3), (9, 3),
               (6, 4), (7, 4), (8, 4)])
    _folha(g, 7, +1, 3); _folha(g, 7, -1, 3)
    _folha(g, 10, +1, 2); _folha(g, 10, -1, 2)
    return g

SPEC_CULTURA = {
    "maconha": _spec_maconha,
    "lupulo": _spec_lupulo,
    "uva": _spec_uva,
    "cafe": _spec_cafe,
    "papoula": _spec_papoula,
}

def _broto(sd, sc):
    """Estagio 1 compartilhado: mudinha com dois cotiledones."""
    g = _grid()
    g[13][7] = "g"
    g[14][7] = "d"
    g[15][6] = "d"; g[15][7] = "d"; g[15][8] = "d"
    g[12][5] = "g"; g[12][9] = "g"   # cotiledones
    g[11][6] = "g"; g[11][8] = "g"   # abrindo
    return g

def _corta(g, y_min):
    """Crescimento = revelar a planta de baixo pra cima (estilos vanilla)."""
    out = _grid()
    for y in range(y_min, 16):
        out[y] = list(g[y])
    return out

def _para_ascii(g):
    return ["".join(linha) for linha in g]

def mistura(c1, c2, t):
    """Mistura duas cores RGBA (t=0..1 pra c2)."""
    return tuple(int(c1[i] + (c2[i] - c1[i]) * t) for i in range(4))

def planta_estagio(crop, estagio, sd, sc, fr, rf, brilho=None):
    """Grid de caracteres + paleta pra um estagio da cultura.
    brilho=None -> automatico (xadrez claro a partir do estagio 3)."""
    if estagio == 1:
        g = _broto(sd, sc)
    elif estagio == 2:
        # v1.2.16: corta mais fundo (era 8) — o gap visual 2->3 era minúsculo
        # ("será que ela só tem 2 estágios?")
        g = _corta(SPEC_CULTURA[crop](), 9)
        # fruto novo e' VERDE (ainda nao e' fruto)
        for y in range(16):
            for x in range(16):
                if g[y][x] == "f":
                    g[y][x] = "g"
    elif estagio == 3:
        # v1.2.16: revela mais planta (era 5) — e o fruto continua VERDE: a
        # cor do fruto é sinal de MATURAÇÃO, não de crescimento
        g = _corta(SPEC_CULTURA[crop](), 4)
        for y in range(16):
            for x in range(16):
                if g[y][x] == "f":
                    g[y][x] = "g"
    else:
        g = SPEC_CULTURA[crop]()
    if brilho is None:
        brilho = estagio >= 4
    if brilho:
        for y in range(16):
            for x in range(16):
                if g[y][x] == "f" and (x + y) % 2 == 0:
                    g[y][x] = "F"
    paleta = {
        "g": rgba(sc),
        "d": rgba(sd),
        "f": rgba(fr),
        "F": clareia(fr, 0.4),   # fruto com brilho (alternado)
    }
    return _para_ascii(g), paleta

def stage4_ripe(crop, sd, sc, fr, rf):
    """Planta MADURA: frutos na cor 'pronta' + HALO DOURADO em volta do cacho
    inteiro + brilho interno. O dourado e a assinatura visual (mesma familia
    do selo do cartao fidelidade e do letreiro) — da longe o jogador ja sabe
    que aquilo vale 3x. O halo e generico (qualquer pixel '.' vizinho de
    fruto vira ouro), entao funciona pras 5 culturas sem coordenada fixa."""
    ascii_grid, pal = planta_estagio(crop, 4, sd, sc, fr, rf, brilho=False)
    pal = dict(pal)
    pal["f"] = rgba(rf)
    pal["F"] = clareia(rf, 0.45)
    pal["H"] = (255, 236, 150, 255)   # dourado claro (brilho principal)
    pal["h"] = (214, 178, 92, 255)    # dourado medio (halo discreto — nao vira pétala)
    pal["W"] = (255, 255, 255, 235)   # faiscas brancas
    lines = [list(line) for line in ascii_grid]
    frutos = [(y, x) for y in range(16) for x in range(16) if lines[y][x] == "f"]
    halo = set()
    for y, x in frutos:
        for dy, dx in ((-1, 0), (1, 0), (0, -1), (0, 1)):  # 4-dir: contorno colado, sem "asas"
            yy, xx = y + dy, x + dx
            if 0 <= yy < 16 and 0 <= xx < 16 and lines[yy][xx] == ".":
                halo.add((yy, xx))
    for y, x in halo:
        lines[y][x] = "h"
    # brilho: cruz de dourado claro no centro do cacho (nao xadrez, nao borra)
    ys = [y for y, _ in frutos]
    xs = [x for _, x in frutos]
    cy, cx = (min(ys) + max(ys)) // 2, (min(xs) + max(xs)) // 2
    for y, x in frutos:
        if abs(y - cy) + abs(x - cx) <= 1:
            lines[y][x] = "H"
    # SEM faiscas brancas: aglomeram e viram blob
    return ["".join(line) for line in lines], pal

# ---------------------------------------------------------------- lampada UV (item)
LAMPADA_ITEM = (
    [
        "................",
        ".......kk.......",
        ".......kk.......",
        "....KKKKKKKK....",
        "...KxxxxxxxxK...",
        "...KUvvvvvvUK...",  # tubo 1
        "...KUvWvvWvUK...",  # highlight do vidro
        "...KxxxxxxxxK...",
        "...KUvvvvvvUK...",  # tubo 2
        "...KUvWvvWvUK...",
        "...KxxxxxxxxK...",
        "....KKKKKKKK....",
        "................",
        "................",
        "................",
        "................",
    ],
    {
        "k": "6E6E78",      # corrente/suporte
        "K": "2B2B31",      # carcaça escura
        "x": "45454E",      # carcaça clara
        "U": "4A2482",      # borda do tubo (UV escura)
        "v": "8B5CF6",      # tubo aceso
        "W": "DCC8FF",      # highlight do vidro
    },
)

# ---------------------------------------------------------------- lampada UV (bloco)
def lampada_bloco():
    """Painel de luz UV (textura cheia 16x16 pro cube_all): moldura metalica
    + DOIS tubos verticais brilhando + grade de ventilacao no centro. Nada de
    quadrado chapado — parece equipamento."""
    g = _grid()
    for i in range(16):
        g[0][i] = "K"; g[15][i] = "K"; g[i][0] = "K"; g[i][15] = "K"
    for y in range(1, 15):
        for x in range(1, 15):
            g[y][x] = "x"
    # tubos verticais (colunas x=3..4 e x=11..12), full height, com highlight
    for y in range(2, 14):
        g[y][3] = "U"; g[y][4] = "v"
        g[y][11] = "v"; g[y][12] = "U"
        if y % 4 == 1:
            g[y][4] = "W"      # brilho do vidro (esquerdo)
        if y % 4 == 3:
            g[y][11] = "W"     # brilho do vidro (direito)
    # cantos dos tubos (soquete)
    for x in (3, 4, 11, 12):
        g[1][x] = "k"; g[14][x] = "k"
    # grade de ventilacao central
    for y in (4, 7, 10):
        for x in (6, 7, 8, 9):
            g[y][x] = "K"
            g[y + 1][x] = "d"
    ascii_grid = _para_ascii(g)
    paleta = {
        "K": "1E1E24",   # moldura escura
        "x": "3A3A44",   # carcaça
        "k": "6E6E78",   # soquete metal
        "d": "2A2A33",   # sombra da grade
        "U": "4A2482",   # tubo escuro
        "v": "8B5CF6",   # tubo aceso
        "W": "DCC8FF",   # highlight
    }
    return ascii_grid, paleta

# ---------------------------------------------------------------- produtos (itens)
PRODUTOS = {
    "lupulo": (
        [
            "................",
            ".....ccc........",
            "....cCCCcc......",
            "...cCCsCCcc.....",
            "...cCsCCsCc.....",
            "....cCCsCcc.....",
            ".....cccc.......",
            "......cc........",
            "......Ss........",
            "......Ss........",
            "......ss........",
            "................",
            "................",
            "................",
            "................",
            "................",
        ],
        {"c": "7FA83A", "C": "A8CC5A", "s": "D4F07A", "S": "5A8C3A"},
    ),
    "uva": (
        [
            "................",
            ".......ss.......",
            "......s.........",
            "....vVv.vVv.....",
            "...vVVVvVvVv....",
            "...vVVvVVvVv....",
            "....vVvVVvV.....",
            "...vVvVVvVv.....",
            "....vVvVvV......",
            ".....vvvv.......",
            "................",
            "................",
            "................",
            "................",
            "................",
            "................",
        ],
        {"s": "5A7A3A", "v": "7B2FBE", "V": "9B4FD8"},
    ),
    "cafe_verde": (
        [
            "................",
            "................",
            "................",
            "......ccc.......",
            ".....cCCCc......",
            "....cCCsCCc.....",
            "....cCsCsCc.....",
            "....cCCsCCc.....",
            "....cCCCCCC.....",
            ".....cCCCc......",
            "......ccc.......",
            "................",
            "................",
            "................",
            "................",
            "................",
        ],
        {"c": "4A6B2A", "C": "7FA845", "s": "A8CC5A"},
    ),
    "cana_de_acucar": (
        [
            "................",
            "......gg........",
            ".....gGGg.......",
            "......gg........",
            ".....gGGg.......",
            "......gg........",
            ".....gGGg.......",
            "......gg........",
            ".....gGGg.......",
            "......gg........",
            ".....gGGg.......",
            "......gg........",
            ".....gGGg.......",
            "......gg........",
            "................",
            "................",
        ],
        {"g": "6B8C3A", "G": "A8CC7A"},
    ),
}

def main():
    tex_dir = os.path.join(ASSETS, "textures", "item")
    block_tex_dir = os.path.join(ASSETS, "textures", "block")
    count = 0

    # sementes (pacotes, com contorno)
    for crop, (sd, sc, fr, rf, (ge, gc)) in CROPS.items():
        mp, pal = pacote_semente(ge, gc, fr)
        rows = outline(render(mp, pal), "2A1F10")
        write_png(os.path.join(tex_dir, "semente_" + crop + ".png"), rows)
        count += 1

    # lampada UV (item, com contorno)
    rows = outline(render(*LAMPADA_ITEM), "101014")
    write_png(os.path.join(tex_dir, "lampada_uv.png"), rows)
    count += 1

    # lampada UV (BLOCO): painel cheio (cube_all) — ver comentario historico:
    # textura de item com cantos transparentes virava "cubo de vidro" no bloco
    mp, pal = lampada_bloco()
    rows_ligada = render(mp, pal)
    write_png(os.path.join(block_tex_dir, "lampada_uv.png"), rows_ligada)
    # v1.2.17: lampada APAGADA (lit=false) — mesmo painel, tubos mortos:
    # tudo escurecido pra 30% (o painel vira "equipamento desligado")
    rows_apagada = [[mistura((16, 18, 22, 255), px, 0.22) for px in linha]
                    for linha in rows_ligada]
    write_png(os.path.join(block_tex_dir, "lampada_uv_off.png"), rows_apagada)
    count += 2

    # produtos agricolas (itens, com contorno)
    for nome, (mp, pal) in PRODUTOS.items():
        rows = outline(render(mp, pal), "1A2010")
        write_png(os.path.join(tex_dir, nome + ".png"), rows)
        count += 1

    # estagios das culturas (blocos cross, SEM contorno)
    # v1.2.16 — a leitura visual em 6 fases (o report do Jade: "a 75% é
    # idêntica à madura"):
    #   1 broto | 2 meia verde | 3 alta verde | 4 COR SURGINDO (esmaecida)
    #   | dormant cor CHEIA sem halo (madura, esperando UV) | ripe DOURADA
    for crop, (sd, sc, fr, rf, _) in CROPS.items():
        for st in (1, 2, 3, 4):
            mp, pal = planta_estagio(crop, st, sd, sc, fr, rf, brilho=False)
            if st == 4:
                # "encamando" (age3): a cor do fruto COMEÇA a sair — misturada
                # com o verde do caule, sem brilho de "pronto"
                pal = dict(pal)
                pal["f"] = mistura(rgba(fr), rgba(sd), 0.45)
            write_png(os.path.join(block_tex_dir, "%s_stage%d.png" % (crop, st)),
                      render(mp, pal))
            count += 1
        # madura ESPERANDO UV (age4, uv 0..2): cor CHEIA, sem halo — "quase,
        # mas o brilho dourado ainda não veio"
        mp, pal = planta_estagio(crop, 4, sd, sc, fr, rf, brilho=False)
        write_png(os.path.join(block_tex_dir, crop + "_stage4_dormant.png"),
                  render(mp, pal))
        count += 1
        mp, pal = stage4_ripe(crop, sd, sc, fr, rf)
        write_png(os.path.join(block_tex_dir, crop + "_stage4_ripe.png"), render(mp, pal))
        count += 1

    print("OK: %d texturas de farm geradas" % count)

if __name__ == "__main__":
    main()
