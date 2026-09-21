"""Gera os JSONs de worldgen do Mercado Esquinão + o template NBT da estrutura.

O NBT segue o formato REAL do 26.3 (decifrado do igloo/top.nbt do jar vanilla):
  size     = TAG_List de 3 TAG_Int
  entities = TAG_List de TAG_Compound {blockPos: [i,i,i], pos: [d,d,d], nbt: {...}}
  blocks   = TAG_List de TAG_Compound {pos: [i,i,i], state: TAG_Int, nbt?}
  palette  = TAG_List de TAG_Compound {id: str, properties?: {str:str}, nbt?}
  DataVersion = TAG_Int

Layout do prédio (15x5x11), inspirado na foto da SUL DISTRIBUIDORA & MERCADO
ESQUINÃO: paredes verdes, faixa branca com o nome (placa), vitrines escuras,
porta central de vidro/madeira, colunas marrons no alpendre, telhado branco
plano e calçada clara com faixa cinza. O Gago ja nasce atras do balcao.

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
    "W": {"id": "minecraft:white_concrete"},                     # faixa/telhado/calçada
    "G": {"id": "minecraft:green_concrete"},                     # paredes verdes
    "Q": {"id": "minecraft:smooth_quartz"},                      # piso interno
    "y": {"id": "minecraft:gray_concrete"},                      # faixa da calçada
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
    "S": {"id": "minecraft:oak_wall_sign", "properties": {"facing": "south"}},
    # quadro de PRECOS: placa de parede interna sobre a parede do fundo (menu
    # de bodega, atras do caixa) — facing=south, suporte = parede z0
    "P": {"id": "minecraft:oak_wall_sign", "properties": {"facing": "south"}},
    # painel de luz embutido no teto (luz de noite, sem precisar de suporte
    # pendurado — o teto E' o proprio bloco)
    "K": {"id": "minecraft:shroomlight"},
    # (o TEXTO do letreiro vai no NBT da ENTRADA DE BLOCO, não da paleta — ver
    # build_nbt: o vanilla tem 5597 casos de nbt em blocks[*] e ZERO na paleta;
    # StructureTemplate so repassa o nbt da entrada ao placeInWorld)
    # moldura da porta (cinza escuro): coluna solida ao lado dos vidros pra as
    # panes de vidro conectarem entre si SEM "vazar" pro vão da porta
    "m": {"id": "minecraft:gray_concrete"},
    ".": {"id": "minecraft:air"},
}
# a antiga definição de B (sem conexões) foi substituída acima

# ============================================================ TEMPLATE ROWS[y][z][x]
# z=10 é a FRENTE (alpendre/colunas); z=0 é o fundo. x=0..14 (oeste->leste).
SIZE_X, SIZE_Y, SIZE_Z = 15, 5, 11
ROWS = [
    # y = 0 — piso: quartz interno, borda branca, calçada com faixa cinza
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
    ],
    # y = 1 — paredes verdes, vitrines, porta, colunas, balcão e barril
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
    ],
    # y = 2 — vitrines continuam, porta (parte de cima), prateleira do balcão
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
        ".L...........L.",
    ],
    # y = 3 — faixa branca com a placa (x7), lanterna, quadro de preços no fundo
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
        "WWWWWWWWWWWWWWW",  # z9: parede cheia (SUPORTE da placa que fica em z10)
        "WWWWWWWSWWWWWWW",  # z10: LETREIRO na frente do alpendre (x7, acima da porta)
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
    ],
]

# O Gago, atrás do balcão (x4, y1, z3), de frente pra porta (+z/sul).
GAGO_BLOCK_POS = [4, 1, 3]
GAGO_POS = [4.5, 1.0, 3.5]


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


def build_nbt():
    estados = [PALETTE[ch] for ch in PALETTE]  # ordem determinística do dict
    estado_para_indice = {ch: i for i, ch in enumerate(PALETTE)}

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

    # ---- blocks (lista de compounds {pos, state, nbt?}; air fica de fora).
    # nbt de tile entity (placa!) vai NA ENTRADA DE BLOCO — padrao do vanilla
    # (5597 ocorrencias, ex. baús de shipwreck).
    blocks_payload = []
    for y in range(SIZE_Y):
        for z in range(SIZE_Z):
            linha = ROWS[y][z]
            assert len(linha) == SIZE_X, f"linha y={y} z={z} tem {len(linha)} chars"
            for x in range(SIZE_X):
                ch = linha[x]
                if ch not in PALETTE:
                    raise SystemExit(f"caractere '{ch}' (y={y} z={z} x={x}) nao esta na PALETTE")
                if ch == ".":
                    continue
                body = _int_list("pos", (x, y, z))
                body += _tag_int("state", estado_para_indice[ch])
                if ch == "S":
                    # o LETREIRO: texto verde escuro brilhante, 4 linhas
                    body += _tag_compound("nbt", _sign_nbt())
                elif ch == "P":
                    # o QUADRO DE PREÇOS interno (a “parede” é a fonte visível;
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


# ---- textos das placas (letreiro da fachada + quadro de preços interno)
SINAL_LETREIRO = {
    "front_text": {"mensagens": ["SUL", "DISTRIBUIDORA", "& MERCADO", "ESQUINÃO"],
                   "cor": "dark_green", "glow": 1},
    "back_text": {"mensagens": ["", "", "", ""], "cor": "black", "glow": 0},
    "is_waxed": 1,
}
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
        sign = SINAL_LETREIRO

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
    # ---- template pool: unico piece, rotação aleatória do jigsaw
    # processors INLINE (como os pools vanilla): a referencia "minecraft:none" NAO existe
    # no 26.3 e quebra o registry inteiro ("Unbound values in processor_list")
    write_json("src/main/resources/data/intoxicantes/worldgen/template_pool/mercado_gago/start.json", {
        "fallback": "minecraft:empty",
        "elements": [{
            "weight": 1,
            "element": {
                "projection": "rigid",
                "element_type": "minecraft:legacy_single_pool_element",
                "location": "intoxicantes:mercado_gago",
                "processors": {"processors": []},
            },
        }],
    })

    # ---- tag de biomas do mercado: esquina de bairro pode nascer em QUALQUER
    #      lugar urbano/natural — NAO copiar a tag do igloo (so neve! bug do
    #      "mercado so spawna na neve"). Planta+praia+rio ficam de fora.
    write_json("src/main/resources/data/intoxicantes/tags/worldgen/biome/has_structure/mercado_esquinao.json", {
        "replace": False,
        "values": [
            "minecraft:plains", "minecraft:sunflower_plains", "minecraft:savanna",
            "minecraft:savanna_plateau", "minecraft:desert", "minecraft:taiga",
            "minecraft:snowy_plains", "minecraft:snowy_taiga", "minecraft:meadow",
            "minecraft:cherry_grove", "minecraft:forest", "minecraft:birch_forest",
            "minecraft:old_growth_birch_forest", "minecraft:dark_forest",
            "minecraft:flower_forest", "minecraft:windswept_hills",
            "minecraft:windswept_forest", "minecraft:windswept_gravelly_hills",
            "minecraft:windswept_savanna", "minecraft:jungle", "minecraft:sparse_jungle",
            "minecraft:bamboo_jungle", "minecraft:swamp", "minecraft:mangrove_swamp",
            "minecraft:badlands", "minecraft:wooded_badlands", "minecraft:eroded_badlands",
            "minecraft:mushroom_fields", "minecraft:stony_shore", "minecraft:snowy_beach",
            "minecraft:ice_spikes", "minecraft:grove", "minecraft:snowy_slopes",
            "minecraft:jagged_peaks", "minecraft:frozen_peaks", "minecraft:stony_peaks",
            "minecraft:pale_garden",
        ],
    })

    # ---- structure: jigsaw em superfice, adapta terreno (beard_thin)
    write_json("src/main/resources/data/intoxicantes/worldgen/structure/mercado_gago.json", {
        "type": "minecraft:jigsaw",
        "biomes": "#intoxicantes:has_structure/mercado_esquinao",
        "spawn_overrides": {},
        "step": "surface_structures",
        "terrain_adaptation": "beard_thin",
        "start_pool": "intoxicantes:mercado_gago/start",
        "size": 1,
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80,
        "use_expansion_hack": True,
    })

    # ---- structure set: raro, estilo igloo (spacing 40 / separation 22)
    write_json("src/main/resources/data/intoxicantes/worldgen/structure_set/mercado_gago.json", {
        "structures": [{"structure": "intoxicantes:mercado_gago", "weight": 1}],
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

    # ---- NBT da estrutura
    nbt_path = ROOT + "src/main/resources/data/intoxicantes/structure/mercado_gago.nbt"
    os.makedirs(os.path.dirname(nbt_path), exist_ok=True)
    with open(nbt_path, "wb") as f:
        f.write(build_nbt())
    print("  src/main/resources/data/intoxicantes/structure/mercado_gago.nbt")

    # ---- sanity: le de volta e confere
    with open(nbt_path, "rb") as f:
        blob = f.read()
    assert blob[:2] == b"\x1f\x8b", "NBT nao esta gzip!"
    n_blocos = sum(1 for row_rows in ROWS for linha in row_rows for c in linha if c != ".")
    print(f"OK — template {SIZE_X}x{SIZE_Y}x{SIZE_Z} ({n_blocos} blocos + Gago, "
          f"{len(PALETTE)} estados na paleta)")


if __name__ == "__main__":
    main()
