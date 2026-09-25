"""Gera os JSONs de worldgen do Mercado Esquinão + o template NBT da estrutura.

O NBT segue o formato REAL do 26.3 (decifrado do igloo/top.nbt do jar vanilla):
  size     = TAG_List de 3 TAG_Int
  entities = TAG_List de TAG_Compound {blockPos: [i,i,i], pos: [d,d,d], nbt: {...}}
  blocks   = TAG_List de TAG_Compound {pos: [i,i,i], state: TAG_Int, nbt?}
  palette  = TAG_List de TAG_Compound {id: str, properties?: {str:str}, nbt?}
  DataVersion = TAG_Int

Layout (v1.2.18 — 15x8x19): o PRÉDIO (15x5x11, z0..z10) igual à foto da SUL
DISTRIBUIDORA & MERCADO ESQUINÃO e, na frente, o PÁTIO DO ESQUINÃO (z11..z18):
calçada, meio-fio, ESTACIONAMENTO demarcado com postes de luz. Duas garantias
de "espaço seguro":
  1. TODO o volume do template é colocado, inclusive o AR — mata árvore,
     cana e folhagem invasora, e desalaga o box (água de lago/raso some);
  2. terrain_adaptation BEARD_BOX no JSON de estrutura: o terreno sob a
     fundação inteira (prédio + pátio) é aterrado em caixa — nada de loja
     no penhasco ou flutuando sobre o vale.

O LETREIRO da fachada agora é a placa CUSTOM (intoxicantes:placa_esquinao):
painel preto 3 blocos com texto verde de LED renderizado por código
(PlacaEsquinaoRenderer). O quadro de CARDÁPIO interno continua placa vanilla
(vitrine "de papel" da bodega, o texto real fica na tela do menu).

Uso: python tools/gen_mercado.py  (a partir da raiz do projeto do mod)
"""
import gzip
import json
import os
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__))) + os.sep

DATA_VERSION = 5023  # 26.3

# ============================================================ PALETA (chars -> estado)
PALETTE = {
    "W": {"id": "minecraft:white_concrete"},                     # faixa/telhado/calçada/estacionamento
    "G": {"id": "minecraft:green_concrete"},                     # paredes verdes
    "Q": {"id": "minecraft:smooth_quartz"},                      # piso interno
    # faixa da calçada + linhas do estacionamento
    "y": {"id": "minecraft:gray_concrete"},
    # v1.2.19: ASFALTO do estacionamento (bloco do mod —" tapete denso tom de
    # asfalto; vagas demarcadas pelas linhas "y" em cima)
    "A": {"id": "intoxicantes:asfalto"},
    # v1.2.24: POSTE DE LUZ do mod — POSTE COMPLETO de 3 blocos: BASE (y1,
    # sobre o asfalto) + CORPO (y2) + TOPO (y3). A anatomia antiga (só o topo
    # semeado, esperando base que nunca subia) é o bug das luminárias
    # flutuando sem poste no pátio.
    "N": {"id": "intoxicantes:poste_luz", "properties": {
        "lit": "true", "parte": "base"}},
    "C": {"id": "intoxicantes:poste_luz", "properties": {
        "lit": "true", "parte": "corpo"}},
    "O": {"id": "intoxicantes:poste_luz", "properties": {
        "lit": "true", "parte": "topo"}},
    # v1.2.24: FAIXA DE PEDESTRE (tinta da travessia, 1px de altura) e
    # v1.2.24: FAIXA DE PEDESTRE (tinta da travessia, 1px de altura)
    "f": {"id": "intoxicantes:faixa_pedestre"},
    # HIDRANTE (v1.2.29): UM, na CALÇADA ao pé do meio-fio, ao lado da
    # faixa de pedestre (x4,z12) — hidrante em VAGA é o oposto da vida
    # real (perto de hidrante é onde se PROÍBE estacionar). A 1.2.24~28
    # plantava DOIS dentro das vagas; o zelador recolhe os velhos.
    "H": {"id": "intoxicantes:hidrante"},
    "d": {"id": "minecraft:spruce_door", "properties": {
        "half": "lower", "facing": "south", "hinge": "left", "open": "false"}},
    "D": {"id": "minecraft:spruce_door", "properties": {
        "half": "upper", "facing": "south", "hinge": "left", "open": "false"}},
    "L": {"id": "minecraft:spruce_log", "properties": {"axis": "y"}},  # colunas
    "s": {"id": "minecraft:smooth_stone_slab", "properties": {"type": "bottom"}},  # balcão
    "t": {"id": "minecraft:smooth_stone_slab", "properties": {"type": "top"}},     # prateleira
    "r": {"id": "minecraft:barrel", "properties": {"facing": "up"}},  # estoque
    "k": {"id": "minecraft:lantern", "properties": {"hanging": "true"}},
    "B": {"id": "minecraft:black_stained_glass_pane", "properties": {
        "east": "true", "west": "true", "north": "false", "south": "false"}},
    # LETREIRO DO ESQUINÃO (v1.2.31): DISPLAY DE FACHADA estilo Satisfactory
    # — a faixa de display atravessa TODA a fachada do prédio (os 15 blocos
    # da linha y3, z10), MONTADA NA PAREDE sob o beiral, ACIMA da porta.
    # NADA de torres/pólos na calçada e NADA na frente da entrada (as torres
    # da 1.2.24–1.2.30 viraram legado; mundos velhos migram pelo zelador).
    # Só o PAINEL CENTRAL (J, em x7) tem block entity com o texto; os outros
    # 14 blocos são EXTENSÃO (só a caixa preta contínua).
    "J": {"id": "intoxicantes:placa_esquinao", "properties": {
        "facing": "south", "parte": "painel", "nivel": "coluna",
        "lit": "true"}},
    "X": {"id": "intoxicantes:placa_esquinao", "properties": {
        "facing": "south", "parte": "extensao", "nivel": "coluna",
        "lit": "true"}},
    # quadro de PRECOS: placa de parede interna sobre a parede do fundo (menu
    # de bodega, atras do caixa) — facing=south, suporte = parede z0
    "P": {"id": "minecraft:oak_wall_sign", "properties": {"facing": "south"}},
    # painel de luz embutido no teto (luz de noite, sem precisar de suporte
    # pendurado — o teto E' o proprio bloco)
    "K": {"id": "minecraft:shroomlight"},
    # moldura da porta (cinza escuro): coluna solida ao lado dos vidros pra as
    # panes de vidro conectarem entre si SEM "vazar" pro vão da porta
    "m": {"id": "minecraft:gray_concrete"},
    # v1.2.36: PAINEL DE LED CRAFTÁVEL no PÁTIO — o "display de ofertas" da
    # calçada (V): TV de tela plana de 3px colada na parede da fachada, sob o
    # letreiro, mostrando as ofertas (texto editável pela Central de Comando).
    "V": {"id": "intoxicantes:painel_led", "properties": {
        "facing": "south", "telas": "3", "lit": "true"}},
    ".": {"id": "minecraft:air"},
}

# ============================================================ REGIÕES (v1.2.25)
# O item 3 do TODO, fase 2: o PRÉDIO agora nasce "do lugar". Mesmo layout,
# mesmas fileiras, mesma anatomia — muda a PELE (paleta) por clima, no padrão
# das vilas vanilla: UM structure_set com 3 structures, cada uma presa à
# própria tag de bioma (o chunk sorteado só ergue a que o bioma aceitar).
# Override de char = str (só o id; propriedades herdadas, ex. slabs) ou dict
# (substitui o estado inteiro).
REGIOES = {
    # esquina clássica: concreto branco/verde do SUL DISTRIBUIDORA original
    "classico": {},
    # SERTÃO: adobe de terracota laranja, piso e balcão de arenito lapidado,
    # colunas de acácia — a bodega de encruzilhada do interior
    "sertao": {
        "W": "minecraft:smooth_sandstone",
        "G": "minecraft:orange_terracotta",
        "Q": "minecraft:cut_sandstone",
        "y": "minecraft:light_gray_concrete",
        "s": "minecraft:smooth_sandstone_slab",
        "t": "minecraft:smooth_sandstone_slab",
        "m": "minecraft:gray_terracotta",
        "L": "minecraft:acacia_log",
    },
    # SERRA: pedra fria, paredes de pinho escuro, piso de smooth stone —
    # o mercadinho de serra com chão de freezer antigo
    "serra": {
        "W": "minecraft:stone_bricks",
        "G": "minecraft:spruce_planks",
        "Q": "minecraft:smooth_stone",
        "m": "minecraft:polished_deepslate",
    },
}

def paleta_da_regiao(regiao):
    """Paleta da região: a base com os overrides aplicados (ordem preservada —
    os índices de estado do NBT saem da ordem do dict)."""
    paleta = {}
    for ch, estado in PALETTE.items():
        override = REGIOES[regiao].get(ch)
        if override is None:
            paleta[ch] = estado
        elif isinstance(override, str):
            trocado = dict(estado)
            trocado["id"] = override
            paleta[ch] = trocado
        else:
            fundido = dict(estado)
            fundido.update(override)
            paleta[ch] = fundido
    return paleta


# ============================================================ TEMPLATE ROWS[y][z][x]
# z=10 é a FRENTE do prédio (alpendre/colunas); z=0 é o fundo. x=0..14
# (oeste->leste). z=11..18 é o PÁTIO: calçada, meio-fio, estacionamento
# demarcado e postes de luz. y=5..7 é AR EXPLICITO sobre tudo (espaço seguro).
SIZE_X, SIZE_Y, SIZE_Z = 15, 8, 19

# piso do pátio: calçada branca, meio-fio cinza e vagas demarcadas
# (linhas cinza a cada 3 blocos: yAA yAA yAA yAA yAA sobre ASFALTO)
ROWS = [
    # y = 0 — piso: quartz interno, borda branca, calçada com faixa cinza + PÁTIO
    [
        "WWWWWWWWWWWWWWW",  # z0 (fundo)
        "WQQQQQQQQQQQQQW",
        "WQQQQQQQQQQQQQW",
        "WQQQQQQQQQQQQQW",
        "WQQQQQQQQQQQQQW",
        "WQQQQQQQQQQQQQW",
        "WQQQQQQQQQQQQQW",
        "WQQQQyyyyyQQQQW",  # z5: marcador de atendimento (frente do balcão)
        "WQQQQQQQQQQQQQW",
        "WQQQQQQQQQQQQQW",  # z9 (vitrine/entrada)
        "WWWWWyyyyyWWWWW",  # z10 (alpendre + calçada)
        ".....WWWWW.....",  # z11: PISO (concreto branco) — a tinta da faixa é SÓ
                            # na fileira de cima (y1). O template 1.2.36–39 pintava
                            # f AQUI TAMBÉM = crosswalk duplo (print do Skyu).
        "WWWWWWWWWWWWWWW",  # z12
        "yyyyyyyyyyyyyyy",  # z12: meio-fio do estacionamento
        "yAAyAAyAAyAAyAA",  # z13: vagas demarcadas no asfalto
        "yAAyAAyAAyAAyAA",  # z14
        "yAAyAAyAAyAAyAA",  # z15
        "yAAyAAyAAyAAyAA",  # z16
        "yAAyAAyAAyAAyAA",  # z17
        "yyyyyyyyyyyyyyy",  # z18: meio-fio de saída
    ],
    # y = 1 — paredes verdes, vitrines, porta, colunas, balcão e barril + PÁTIO aberto
    [
        "GGGGGGGGGGGGGGG",  # z0
        "G.............G",
        "G.........r...G",  # z2: barril
        "G.............G",
        "G..ssss.......G",  # z4: balcão (x3..6)
        "G.............G",
        "G.............G",
        "G.............G",
        "G.............G",
        "GGmBBBBdBBBBmGG",  # z9: moldura + vitrines + porta (x7)
        ".L...........L.",  # z10: colunas x1 e x13
        ".....fffff.....",  # z11: faixa de pedestre (a placa NÃO mora mais aqui)
        "...H...........",  # z12: HIDRANTE — calçada, pé do meio-fio, ao lado da faixa
        "...............",
        "...............",  # (as vagas ficam LIVRES — nada estacionado nelas)
        "...............",
        "...............",
        "..N.........N..",  # z17: POSTES DE LUZ — BASE (corpo+topo em cima)
        "...............",
    ],
    # y = 2 — vitrines continuam, porta (parte de cima), prateleira + lampiões
    [
        "GGGGGGGGGGGGGGG",  # z0
        "G.............G",
        "G.............G",
        "G.............G",
        "G..tttt.......G",  # z4: prateleira
        "G.............G",
        "G.............G",
        "G.............G",
        "G.............G",
        "GGmBBBBDBBBBmGG",  # z9: moldura + porta superior
        ".LVVV........L.",  # z10: PAINEL DE LED (3 telas) À ESQUERDA da porta (x7)
                            # — NUNCA na frente dela (1.2.36–39 plantava x5..9
                            # cruzando a porta). NBT só no CABEÇA (x2); os outros
                            # 2 são extensão (sem NBT = sem texto duplicado).
        "...............",  # z11: LIVRE (o letreiro subiu pra fachada)
        "...............",
        "...............",
        "...............",
        "...............",
        "...............",
        "..C.........C..",  # z17: POSTES DE LUZ — CORPO (base y1, topo y3)
        "...............",
    ],
    # y = 3 — faixa branca, lanterna, quadro de preços + LETREIRO CUSTOM
    [
        "WWWWWWWWWWWWWWW",  # z0
        "W........P....W",  # z1: QUADRO DE PREÇOS (x9, facing=south, suporte=z0)
        "W.............W",
        "W.............W",
        "W...k.........W",  # z4: lanterna pendurada sobre o balcão
        "W.............W",
        "W.............W",
        "W.............W",
        "W.............W",
        "WWWWWWWWWWWWWWW",  # z9: parede cheia
        "WWWWWWWJWWWWWWW",  # z10: DISPLAY DE FACHADA — a faixa de display atravessa os 15 blocos (texto no x7)
        "...............",  # z11: LIVRE (as torres da placa saíram daqui)
        "...............",
        "...............",
        "...............",
        "...............",
        "...............",
        "..O.........O..",  # z17: POSTES — TOPO (luminária EM CIMA da coluna)
        "...............",
    ],
    # y = 4 — telhado branco plano (cobre o alpendre, igual à foto)
    [
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "WWWWWKKKKKWWWWW",  # z5: fileira de lanternas NO TETO — luz de noite
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "WWWWWWWWWWWWWWW",
        "...............",  # z11: LIVRE (as torres da placa saíram daqui)
        "...............",
        "...............",
        "...............",
        "...............",
        "...............",
        "...............",
        "...............",
    ],
    # y = 5..7 — AR EXPLICITO sobre tudo (prédio + pátio): mata árvore invasora,
    # desalaga o box e garante a "bola de cristal" limpa do esquinão
    # (o AR é COLOCADO, não pulado — ver build_nbt)
    *[["..............." for _ in range(SIZE_Z)] for _ in range(3)],
]


# O Gago, atrás do balcão (x4, y1, z3), de frente pra porta (+z/sul).
GAGO_BLOCK_POS = [4, 1, 3]
GAGO_POS = [4.5, 1.0, 3.5]  # piso do balcão (a placa nova não interfere no interior)


# ============================================================ NBT writer (big-endian, gzip)
def _payload_string(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def _payload_int(v):
    return struct.pack(">i", v)


def _int_payloads(values):
    """Payloads de TAG_Int pra montar uma TAG_List de ints (pos, size, blockPos...)."""
    return [_payload_int(v) for v in values]


def _int_list(name, values):
    return _tag_list(name, 3, _int_payloads(values))


def build_nbt(paleta):
    estados = list(paleta.values())  # ordem determinística do dict
    estado_para_indice = {ch: i for i, ch in enumerate(paleta)}

    # ---- palette (lista de compounds; elementos de TAG_List são SÓ payload)
    palette_payload = []
    for estado in estados:
        body = _tag_string("id", estado["id"])
        if "properties" in estado:
            props = b""
            for k, v in estado["properties"].items():
                props += _tag_string(k, v)
            body += _tag_compound("properties", props)
        # NAO escrever nbt aqui: o vanilla nunca guarda nbt na paleta (0/1511
        # templates) e o StructureTemplate IGNORA — o texto da placa ia a perder.
        palette_payload.append(_compound_payload(body))

    # ---- blocks (lista de compounds {pos, state, nbt?}).
    # v1.2.18: o AR TAMBEM É COLOCADO — o template é uma "bola de cristal":
    # derruba árvore/cana invasora, seca água rasa do box e garante que um
    # prédio da vila não nasça colado dentro da loja (report: "nasceu engolida
    # por uma vila"). nbt de tile entity vai NA ENTRADA DE BLOCO — padrão do
    # vanilla (5597 ocorrências, ex. baús de shipwreck).
    blocks_payload = []
    for y in range(SIZE_Y):
        for z in range(SIZE_Z):
            linha = ROWS[y][z]
            assert len(linha) == SIZE_X, f"linha y={y} z={z} tem {len(linha)} chars"
            for x in range(SIZE_X):
                ch = linha[x]
                if ch not in PALETTE:
                    raise SystemExit(f"caractere '{ch}' (y={y} z={z} x={x}) nao esta na PALETTE")
                body = _int_list("pos", (x, y, z))
                body += _tag_int("state", estado_para_indice[ch])
                if ch == "J":
                    # o LETREIRO CUSTOM: 4 linhas de LED + vínculo com o mercado
                    body += _tag_compound("nbt", _placa_nbt())
                elif ch == "V" and x == 2:
                    # v1.2.40: NBT SÓ no painel-CABEÇA (x2, a ponta oeste da
                    # linha). O template 1.2.36–39 gravava NBT nos 5 blocos →
                    # 5 block entities com texto próprio = 5 renderers sobre-
                    # postos (o display "bugado" do print). Extensão não tem
                    # texto: quem manda na linha é o cabeça.
                    body += _tag_compound("nbt", _painel_nbt())
                elif ch == "P":
                    # o QUADRO DE PREÇOS interno (a "parede" é a fonte visível;
                    # a tela custom detalha compra/venda/estoque em tempo real)
                    body += _tag_compound("nbt", _sign_nbt(SINAL_PRECOS))
                blocks_payload.append(_compound_payload(body))

    # ---- entities
    entities_payload = [_gago_entity()]

    root_body = (
        _int_list("size", (SIZE_X, SIZE_Y, SIZE_Z))
        + _tag_list("entities", 10, entities_payload)
        + _tag_list("blocks", 10, blocks_payload)
        + _tag_list("palette", 10, palette_payload)
        + _tag_int("DataVersion", DATA_VERSION)
    )
    return gzip.compress(_tag_compound("", root_body))


def _tag_string(name, value):
    return b"\x08" + _payload_string(name) + _payload_string(value)


def _tag_byte(name, value):
    return b"\x01" + _payload_string(name) + struct.pack(">b", value)


def _tag_short(name, value):
    return b"\x02" + _payload_string(name) + struct.pack(">h", value)


def _tag_int(name, value):
    return b"\x03" + _payload_string(name) + struct.pack(">i", value)


def _tag_float(name, value):
    return b"\x05" + _payload_string(name) + struct.pack(">f", value)


def _tag_int_array(name, values):
    out = b"\x0B" + _payload_string(name) + struct.pack(">i", len(values))
    for v in values:
        out += struct.pack(">i", v)
    return out


def _tag_list(name, tag_type, payloads):
    out = b"\x09" + _payload_string(name) + struct.pack(">B", tag_type)
    out += struct.pack(">i", len(payloads))
    return out + b"".join(payloads)


def _tag_compound(name, body):
    return b"\x0A" + _payload_string(name) + body + b"\x00"


def _compound_payload(body):
    """Elemento de TAG_List de compounds: SO o payload, COM o terminador 0x00
    (mas sem byte de tipo e sem nome — igual ao igloo/top.nbt)."""
    return body + b"\x00"


def _double_list(name, values):
    return _tag_list(name, 6, [struct.pack(">d", v) for v in values])


def _float_list(name, values):
    return _tag_list(name, 5, [struct.pack(">f", v) for v in values])


# ---- textos das placas (quadro de preços interno) + nbt da placa custom
# quadro de preços: os valores REAIS mudam por dia (cotação da rua) e por nível
# de fidelidade — a tela custom do cardápio é a fonte verdadeira; esta placa é
# a vitrine "de papel" da loja, com os preços base visíveis de dentro.
SINAL_PRECOS = {
    "front_text": {"mensagens": ["- CARDÁPIO -", "Cerveja .. R$15", "Colheita 8x: R$6-8", "Fidelidade: -15%"],
                   "cor": "black", "glow": 0},
    "back_text": {"mensagens": ["", "", "", ""], "cor": "black", "glow": 0},
    "is_waxed": 1,
}


def _sign_nbt(sign=None):
    if sign is None:
        sign = SINAL_PRECOS

    # formato REAL do vanilla (igloo/bottom.nbt): messages e filtered_messages
    # sao TAG_List de 4 TAG_Compound; cada linha = {text: str, color?: str}.
    # O texto NAO e' string JSON — 'color' e' campo IRMAO de 'text'
    # (igloo: \x08\x00\x04text\x00\x05<----\x08\x00\x05color\x00\x05black).
    # A mensagem crua ({"text":...} como valor de text) renderizaria o JSON literal!
    def linha(txt, cor):
        return _compound_payload(_tag_string("text", txt)
                                 + _tag_string("color", cor))

    def texto(lado):
        mensagens = lado["mensagens"]
        cor = lado.get("cor", "black")
        return _tag_list("messages", 10, [linha(m, cor) for m in mensagens]) \
            + _tag_list("filtered_messages", 10, [linha(m, cor) for m in mensagens]) \
            + _tag_byte("has_glowing_text", lado.get("glow", 0))

    body = _tag_compound("front_text", texto(sign["front_text"]))
    body += _tag_compound("back_text", texto(sign["back_text"]))
    body += _tag_byte("is_waxed", sign["is_waxed"])
    return body


def _placa_nbt():
    """NBT da PlacaEsquinaoBlockEntity gravado pelo template (SÓ no painel
    central): o nome da loja em 1 LINHA que o renderer estica pela largura da
    fachada + vínculo com o mercado + a anatomia atual (versao/largura — o
    zelador não re-migra). O 'id' é incluído como os templates do vanilla
    fazem para block entities."""
    body = _tag_string("id", "intoxicantes:placa_esquinao")
    # v1.2.31: o MESMO LINHAS_PADRAO do código + anatomia do display de fachada.
    body += _tag_string("linha0", "MERCADO ESQUINÃO")
    body += _tag_int("versao", 3)
    body += _tag_int("largura", SIZE_X)
    body += _tag_byte("mercado", 1)
    body += _tag_byte("nova", 1)
    return body


def _painel_nbt():
    """NBT do PainelLedBlockEntity (v1.2.36) — o display de OFERTAS pendurado
    sob o letreiro, na fachada. 2 linhas, tela esticada."""
    body = _tag_string("id", "intoxicantes:painel_led")
    body += _tag_string("linha0", "OFERTAS DO DIA")
    body += _tag_string("linha1", "PROMOCOES!")
    body += _tag_int("cor", 0x39FF6E)
    body += _tag_int("brilho", 15)
    body += _tag_int("modo", 0)
    return body


def _gago_entity():
    nbt = (
        _tag_string("id", "intoxicantes:gago")
        + _double_list("Pos", GAGO_POS)
        + _double_list("Motion", [0.0, 0.0, 0.0])
        + _float_list("Rotation", [0.0, 0.0])
        + _tag_int_array("UUID", [305419896, 1871213665, -1463642571, 121865398])
        + _tag_short("Air", 300)
        + _tag_short("Fire", 0)
        + _tag_byte("Invulnerable", 0)  # NUNCA vulneravel=1: com 1 o escudo de
        # veneno/dano nao remove e o Gago ficaria imortal (a 12 "nao dava dano")
        + _tag_byte("OnGround", 1)
        + _tag_byte("PersistenceRequired", 1)
        + _tag_int("PortalCooldown", 0)
        + _tag_float("fall_distance", 0.0)
    )
    body = _int_list("blockPos", GAGO_BLOCK_POS)
    body += _double_list("pos", GAGO_POS)
    body += _tag_compound("nbt", nbt)
    return _compound_payload(body)


# ============================================================ JSONS de worldgen
def write_json(path, obj):
    full = ROOT + path.replace("/", os.sep)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")
    print(f"  {path}")


def main():
    # ============ v1.2.25: MERCADOS REGIONAIS (item 3 do TODO, fase 2) ============
    # Padrão das vilas vanilla: UM structure_set espalha; CADA estrutura tem a
    # própria tag de bioma (disjuntas — o bioma decide a pele) e o próprio
    # template pool. A espiral do mod localiza por chave e grava a região no
    # .dat — o /gagomarket rebuild e o Zelador já sabem qual pele erguer.
    for regiao in ("classico", "sertao", "serra"):
        paleta = paleta_da_regiao(regiao)
        sufixo = "" if regiao == "classico" else f"_{regiao}"

        # ---- template pool: unico piece, rotação aleatória do jigsaw
        # processors INLINE (como os pools vanilla): a referencia "minecraft:none" NAO existe
        # no 26.3 e quebra o registry inteiro ("Unbound values in processor_list")
        write_json(
            f"src/main/resources/data/intoxicantes/worldgen/template_pool/mercado_gago{sufixo}/start.json", {
            "fallback": "minecraft:empty",
            "elements": [{
                "weight": 1,
                "element": {
                    "projection": "rigid",
                    "element_type": "minecraft:legacy_single_pool_element",
                    "location": f"intoxicantes:mercado_gago{sufixo}",
                    "processors": {"processors": []},
                },
            }],
        })

        # ---- tags de bioma DISJUNTAS: o bioma decide a pele. Clássico é o
        # "clima neutro" (plains/forest/selva/brejo); sertão é o quente;
        # serra é o frio. Nenhum bioma em duas tags — um bioma, um mercado.
        tags_bioma = {
            "classico": [
                "minecraft:plains", "minecraft:sunflower_plains", "minecraft:forest",
                "minecraft:birch_forest", "minecraft:old_growth_birch_forest",
                "minecraft:dark_forest", "minecraft:flower_forest", "minecraft:meadow",
                "minecraft:cherry_grove", "minecraft:jungle", "minecraft:sparse_jungle",
                "minecraft:bamboo_jungle", "minecraft:swamp", "minecraft:mangrove_swamp",
                "minecraft:mushroom_fields", "minecraft:pale_garden", "minecraft:river",
                "minecraft:beach", "minecraft:savanna", "minecraft:savanna_plateau",
                "minecraft:windswept_savanna",
            ],
            "sertao": [
                "minecraft:desert", "minecraft:badlands", "minecraft:wooded_badlands",
                "minecraft:eroded_badlands", "minecraft:stony_shore",
            ],
            "serra": [
                "minecraft:taiga", "minecraft:snowy_taiga", "minecraft:snowy_plains",
                "minecraft:snowy_beach", "minecraft:ice_spikes", "minecraft:grove",
                "minecraft:snowy_slopes", "minecraft:jagged_peaks", "minecraft:frozen_peaks",
                "minecraft:stony_peaks", "minecraft:windswept_hills",
                "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills",
            ],
        }
        write_json(
            f"src/main/resources/data/intoxicantes/tags/worldgen/biome/has_structure/mercado_esquinao{sufixo}.json", {
            "replace": False,
            "values": tags_bioma[regiao],
        })

        # ---- structure: jigsaw em superficie, ATERRAMENTO EM CAIXA (beard_box,
        #      v1.2.18): o terreno sob TODO o footprint (prédio + pátio) é preenchido
        #      sólido — a loja não nasce mais no precipício, sobre buraco ou com a
        #      fundação exposta na encosta. O ar explícito do template cuida do
        #      ACIMA (água/árvore/vila colada).
        write_json(
            f"src/main/resources/data/intoxicantes/worldgen/structure/mercado_gago{sufixo}.json", {
            "type": "minecraft:jigsaw",
            "biomes": f"#intoxicantes:has_structure/mercado_esquinao{sufixo}",
            "spawn_overrides": {},
            "step": "surface_structures",
            "terrain_adaptation": "beard_box",
            "start_pool": f"intoxicantes:mercado_gago{sufixo}/start",
            "size": 1,
            "start_height": {"absolute": 0},
            "project_start_to_heightmap": "WORLD_SURFACE_WG",
            "max_distance_from_center": 80,
            "use_expansion_hack": True,
        })

        # ---- NBT da estrutura (paleta da região, mesmo layout)
        nbt_path = (ROOT
                    + f"src/main/resources/data/intoxicantes/structure/mercado_gago{sufixo}.nbt")
        os.makedirs(os.path.dirname(nbt_path), exist_ok=True)
        with open(nbt_path, "wb") as f:
            f.write(build_nbt(paleta))
        print(f"  src/main/resources/data/intoxicantes/structure/mercado_gago{sufixo}.nbt")

        # ---- sanity: le de volta e confere
        with open(nbt_path, "rb") as f:
            blob = f.read()
        assert blob[:2] == b"\x1f\x8b", "NBT nao esta gzip!"
        print(f"OK [{regiao}] — template {SIZE_X}x{SIZE_Y}x{SIZE_Z} "
              f"({sum(len(linha) for row_rows in ROWS for linha in row_rows)} blocos, "
              f"+ Gago, {len(paleta)} estados na paleta)")

    # ---- structure set: raro, estilo igloo (spacing 40 / separation 22).
    # As 3 estruturas dividem a MESMA distribuição: o random_spread sorteia a
    # célula, e a PRIMEIRA estrutura cuja tag de bioma aceita o terreno ergue.
    write_json("src/main/resources/data/intoxicantes/worldgen/structure_set/mercado_gago.json", {
        "structures": [
            {"structure": "intoxicantes:mercado_gago", "weight": 1},
            {"structure": "intoxicantes:mercado_gago_sertao", "weight": 1},
            {"structure": "intoxicantes:mercado_gago_serra", "weight": 1},
        ],
        "placement": {
            "type": "minecraft:random_spread",
            "spacing": 40,
            "separation": 22,
            "salt": 918273645,
        },
    })

    # ---- receita do opio (2 papoulas -> 1 opio): o loot da papoula ja cobre, mas
    #      quem cultiva em fazenda merece renda fixa
    write_json("src/main/resources/data/intoxicantes/recipe/opio.json", {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "group": "intoxicantes",
        "pattern": ["P", "P"],
        "key": {"P": "minecraft:poppy"},
        "result": {"id": "intoxicantes:opio", "count": 1},
    })


if __name__ == "__main__":
    main()
