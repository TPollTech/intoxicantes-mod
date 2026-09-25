"""Gera os JSONs de recursos das plantacoes: blockstates, modelos,
definicoes de item, loot tables de colheita e receitas novas.

Uso: python tools/gen_farm_resources.py  (a partir da raiz do projeto do mod)
"""
import json
import os

RES = os.path.join("src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "intoxicantes")
DATA = os.path.join(RES, "data", "intoxicantes")

CROPS = ["maconha", "lupulo", "uva", "cafe", "papoula"]

def wjson(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)

def item_def(model_id):
    return {"model": {"type": "minecraft:model", "model": model_id}}

# ---------------------------------------------------------------- blockstates
def gen_blockstates():
    for crop in CROPS:
        variants = {}
        for age in range(5):
            for uv in range(4):
                if age < 4:
                    stage = age + 1  # age0 -> stage1 ... age3 -> stage4 (verde)
                    model = "intoxicantes:block/%s_plant_stage%d" % (crop, stage)
                elif uv >= 3:
                    model = "intoxicantes:block/%s_plant_ripe" % crop
                else:
                    # v1.2.16: crescida esperando UV = DORMANT (fruto esmaecido,
                    # sem brilho) — nada de compartilhar visual com quase-madura
                    model = "intoxicantes:block/%s_plant_dormant" % crop
                variants["age=%d,uv_age=%d" % (age, uv)] = {"model": model}
        wjson(os.path.join(ASSETS, "blockstates", crop + "_plant.json"), {"variants": variants})

    # lampada: bloco cheio com estado LIT (v1.2.17 — disjuntor de redstone)
    wjson(os.path.join(ASSETS, "blockstates", "lampada_uv.json"), {
        "variants": {
            "lit=true": {"model": "intoxicantes:block/lampada_uv"},
            "lit=false": {"model": "intoxicantes:block/lampada_uv_off"},
        }
    })

    # POSTE DE LUZ (v1.2.24): PARTE (base/corpo/topo — poste de 3 blocos) × LIT
    # (a luminária do topo apaga; base e corpo não mudam de cara)
    wjson(os.path.join(ASSETS, "blockstates", "poste_luz.json"), {
        "variants": {
            "parte=base,lit=true": {"model": "intoxicantes:block/poste_luz_base"},
            "parte=base,lit=false": {"model": "intoxicantes:block/poste_luz_base"},
            "parte=corpo,lit=true": {"model": "intoxicantes:block/poste_luz_corpo"},
            "parte=corpo,lit=false": {"model": "intoxicantes:block/poste_luz_corpo"},
            "parte=topo,lit=true": {"model": "intoxicantes:block/poste_luz_topo_lit"},
            "parte=topo,lit=false": {"model": "intoxicantes:block/poste_luz_topo_off"},
        }
    })

    # v1.2.23: ASFALTO — o BLOCKSTATE nunca foi gerado (só o modelo existia):
    # o xadrez rosa do estacionamento era isto. Uma variante, modelo cube_all.
    wjson(os.path.join(ASSETS, "blockstates", "asfalto.json"), {
        "variants": {"": {"model": "intoxicantes:block/asfalto"}}
    })

    # LETREIRO DO ESQUINÃO (v1.2.23; CONVENÇÃO CORRIGIDA na v1.2.28): o modelo
    # tem a face FRONT na NORTH local (convenção da fornalha), então o facing
    # gira FORNALHA (north=0, east=90, south=180, west=270) — o mapeamento
    # wall-sign antigo punha a textura da matriz de LED no lado OPOSTO do
    # texto (a "caixa metálica com texto flutuando" do playtest).
    # × PARTE (torres/painel) × NIVEL — 72 estados cobertos.
    placas = {}
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    # v1.2.31: o DISPLAY DE FACHADA é o painel/extensão — a faixa larga
    # MONTADA NA FACHADA (bloco cheio, 1 modelo só; o texto atravessa via
    # BER no painel central). As anatomias VELHAS (torres de 1.2.24–1.2.30)
    # continuam cobertas enquanto os saves antigos migram pro zelador.
    modelos_por_parte_nivel = {
        ("painel", "rodape"): "placa_esquinao",
        ("painel", "coluna"): "placa_esquinao",
        ("painel", "topo"): "placa_esquinao",
        ("extensao", "rodape"): "placa_esquinao",
        ("extensao", "coluna"): "placa_esquinao",
        ("extensao", "topo"): "placa_esquinao",
        ("esquerda", "rodape"): "placa_esquinao_rodape",
        ("esquerda", "coluna"): "placa_esquinao_coluna",
        ("esquerda", "topo"): "placa_esquinao_topo",
        ("direita", "rodape"): "placa_esquinao_rodape",
        ("direita", "coluna"): "placa_esquinao_coluna",
        ("direita", "topo"): "placa_esquinao_topo",
    }
    for face, ang in rot.items():
        for (parte, nivel), modelo in modelos_por_parte_nivel.items():
            for lit in ("true", "false"):
                placas["facing=%s,parte=%s,nivel=%s,lit=%s" % (
                    face, parte, nivel, lit)] = {
                    "model": "intoxicantes:block/" + modelo,
                    "y": ang, "uvlock": False}
    wjson(os.path.join(ASSETS, "blockstates", "placa_esquinao.json"),
          {"variants": placas})

    # PAINEL DE LED CRAFTÁVEL (v1.2.36): a TV de tela plana — PAINEL FINO de
    # 3px colado na face da parede. Mesma tabela FORNALHA do letreiro
    # (facing=north → y=0 → a tela/face north do elemento encara o leitor).
    painel_led = {}
    for face, ang in rot.items():
        for telas in ("1", "2", "3"):
            for lit in ("true", "false"):
                painel_led["facing=%s,telas=%s,lit=%s" % (face, telas, lit)] = {
                    "model": "intoxicantes:block/painel_led",
                    "y": ang, "uvlock": False}
    wjson(os.path.join(ASSETS, "blockstates", "painel_led.json"),
          {"variants": painel_led})

# ---------------------------------------------------------------- modelos de bloco
def gen_block_models():
    for crop in CROPS:
        for st in range(1, 5):
            wjson(os.path.join(ASSETS, "models", "block", "%s_plant_stage%d.json" % (crop, st)),
                  {"parent": "minecraft:block/cross",
                   "textures": {"cross": "intoxicantes:block/%s_stage%d" % (crop, st)}})
        wjson(os.path.join(ASSETS, "models", "block", "%s_plant_ripe.json" % crop),
              {"parent": "minecraft:block/cross",
               "textures": {"cross": "intoxicantes:block/%s_stage4_ripe" % crop}})
        wjson(os.path.join(ASSETS, "models", "block", "%s_plant_dormant.json" % crop),
              {"parent": "minecraft:block/cross",
               "textures": {"cross": "intoxicantes:block/%s_stage4_dormant" % crop}})
    wjson(os.path.join(ASSETS, "models", "block", "lampada_uv.json"),
          {"parent": "minecraft:block/cube_all",
           "textures": {"all": "intoxicantes:block/lampada_uv"}})
    # lampada APAGADA (lit=false): textura escura (v1.2.17)
    wjson(os.path.join(ASSETS, "models", "block", "lampada_uv_off.json"),
          {"parent": "minecraft:block/cube_all",
           "textures": {"all": "intoxicantes:block/lampada_uv_off"}})

    # POSTE DE LUZ (v1.2.24): poste de VERDADE, 3 blocos — BASE (pedestal de
    # concreto no chão + arranque da coluna), CORPO (coluna comprida) e TOPO
    # (braço + luminária de sódio pendurada). LIT só troca a luminária.
    wjson(os.path.join(ASSETS, "models", "block", "poste_luz_base.json"), {
        "textures": {
            "ped": "intoxicantes:block/poste_ped",
            "post": "intoxicantes:block/poste_luz",
            "particle": "intoxicantes:block/poste_ped"},
        "elements": [
            # pedestal de concreto (largão, no chão)
            {"from": [5, 0, 5], "to": [11, 6, 11], "faces": {
                "north": {"texture": "#ped"}, "south": {"texture": "#ped"},
                "west": {"texture": "#ped"}, "east": {"texture": "#ped"},
                "up": {"texture": "#ped"}, "down": {"texture": "#ped", "cullface": "down"}}},
            # arranque da coluna (deixa o metal sair DO pedestal)
            {"from": [6, 6, 6], "to": [10, 16, 10], "faces": {
                "north": {"texture": "#post"}, "south": {"texture": "#post"},
                "west": {"texture": "#post"}, "east": {"texture": "#post"},
                "up": {"texture": "#post"}, "down": {"texture": "#post"}}}]})
    # CORPO: coluna comprida, bloco inteiro de metal (o "comprido" do pedido)
    wjson(os.path.join(ASSETS, "models", "block", "poste_luz_corpo.json"), {
        "textures": {
            "post": "intoxicantes:block/poste_luz",
            "particle": "intoxicantes:block/poste_luz"},
        "elements": [
            {"from": [6, 0, 6], "to": [10, 16, 10], "faces": {
                "north": {"texture": "#post"}, "south": {"texture": "#post"},
                "west": {"texture": "#post"}, "east": {"texture": "#post"},
                "up": {"texture": "#post"}, "down": {"texture": "#post"}}}]})
    # TOPO (v1.2.27): coluna completa + LUMINÁRIA EM CIMA (a "lâmpada do
    # poste" que o jogador pediu — simétrica, visível de todos os lados):
    # coluna (casa com o corpo), capitel largo, lente de sódio e tampa.
    wjson(os.path.join(ASSETS, "models", "block", "poste_luz_topo_lit.json"), {
        "textures": {
            "post": "intoxicantes:block/poste_luz",
            "cap": "intoxicantes:block/poste_ped",
            "light": "intoxicantes:block/poste_luz_on",
            "particle": "intoxicantes:block/poste_luz"},
        "elements": [
            # continuacao da coluna (casa com o corpo de baixo)
            {"from": [6, 0, 6], "to": [10, 10, 10], "faces": {
                "north": {"texture": "#post"}, "south": {"texture": "#post"},
                "west": {"texture": "#post"}, "east": {"texture": "#post"},
                "up": {"texture": "#post"}, "down": {"texture": "#post"}}},
            # capitel (flange larga onde a coluna encontra a luminária)
            {"from": [5, 10, 5], "to": [11, 11, 11], "faces": {
                "north": {"texture": "#cap"}, "south": {"texture": "#cap"},
                "west": {"texture": "#cap"}, "east": {"texture": "#cap"},
                "up": {"texture": "#cap"}, "down": {"texture": "#cap"}}},
            # a LENTE ACESA (o bulbo em cima, visível de longe)
            {"from": [5, 11, 5], "to": [11, 13, 11], "faces": {
                "north": {"texture": "#light"}, "south": {"texture": "#light"},
                "west": {"texture": "#light"}, "east": {"texture": "#light"},
                "up": {"texture": "#light"}, "down": {"texture": "#light"}}},
            # tampa da luminária (chapéu de metal)
            {"from": [4, 13, 4], "to": [12, 14, 12], "faces": {
                "north": {"texture": "#post"}, "south": {"texture": "#post"},
                "west": {"texture": "#post"}, "east": {"texture": "#post"},
                "up": {"texture": "#post"}, "down": {"texture": "#post"}}}]})
    # TOPO apagado: mesma anatomia, lente morta
    wjson(os.path.join(ASSETS, "models", "block", "poste_luz_topo_off.json"), {
        "textures": {
            "post": "intoxicantes:block/poste_luz",
            "cap": "intoxicantes:block/poste_ped",
            "light": "intoxicantes:block/poste_luz_off",
            "particle": "intoxicantes:block/poste_luz"},
        "elements": [
            {"from": [6, 0, 6], "to": [10, 10, 10], "faces": {
                "north": {"texture": "#post"}, "south": {"texture": "#post"},
                "west": {"texture": "#post"}, "east": {"texture": "#post"},
                "up": {"texture": "#post"}, "down": {"texture": "#post"}}},
            {"from": [5, 10, 5], "to": [11, 11, 11], "faces": {
                "north": {"texture": "#cap"}, "south": {"texture": "#cap"},
                "west": {"texture": "#cap"}, "east": {"texture": "#cap"},
                "up": {"texture": "#cap"}, "down": {"texture": "#cap"}}},
            {"from": [5, 11, 5], "to": [11, 13, 11], "faces": {
                "north": {"texture": "#light"}, "south": {"texture": "#light"},
                "west": {"texture": "#light"}, "east": {"texture": "#light"},
                "up": {"texture": "#light"}, "down": {"texture": "#light"}}},
            {"from": [4, 13, 4], "to": [12, 14, 12], "faces": {
                "north": {"texture": "#post"}, "south": {"texture": "#post"},
                "west": {"texture": "#post"}, "east": {"texture": "#post"},
                "up": {"texture": "#post"}, "down": {"texture": "#post"}}}]})

    # ASFALTO do estacionamento (v1.2.19)
    wjson(os.path.join(ASSETS, "models", "block", "asfalto.json"),
          {"parent": "minecraft:block/cube_all",
           "textures": {"all": "intoxicantes:block/asfalto"}})

    # ---- LETREIRO DO ESQUINÃO (v1.2.31): DISPLAY DE FACHADA — caixa custom
    # (o TEXTO é o BER)
    # PAINEL/EXTENSÃO: bloco CHEIO (0..16 nos 3 eixos) — a faixa É a parede
    # da fachada, montada acima da porta (sem torres, sem flutuar na frente
    # da entrada). CONVENÇÃO DA FORNALHA: facing=north → blockstate y=0 → a
    # face LOCAL north (−Z) encara o leitor; "front" na NORTH e "back" na
    # SOUTH. O renderer desenha o LED nos dois lados (−Z e +Z locais).
    wjson(os.path.join(ASSETS, "models", "block", "placa_esquinao.json"), {
        "parent": "minecraft:block/block",
        "textures": {
            # v1.2.31: frente = TELA contínua (sem moldura por bloco — a faixa
            # da fachada é UM display só); a moldura verde fica na placa avulsa
            "front": "intoxicantes:block/placa_esquinao_tela",
            "back": "intoxicantes:block/placa_esquinao_back",
            "metal": "intoxicantes:block/placa_esquinao_back",
            "particle": "intoxicantes:block/placa_esquinao_back"},
        "elements": [{
            "from": [0, 0, 0], "to": [16, 16, 16],
            "faces": {
                "north": {"texture": "#front"},
                "south": {"texture": "#back"},
                "up": {"texture": "#metal"},
                "down": {"texture": "#metal"},
                "west": {"texture": "#metal"},
                "east": {"texture": "#metal"}}}]})
    # PAINEL DE LED (v1.2.36): a TV FINA — 3px de espessura (z13..16), tela
    # na face NORTH local (convenção da fornalha do letreiro) e moldura
    # metálica no resto. Reusa as texturas do letreiro (tela contínua sem
    # moldura por bloco). O texto de LED é desenhado por código
    # (PainelLedRenderer) no plano da tela.
    wjson(os.path.join(ASSETS, "models", "block", "painel_led.json"), {
        "parent": "minecraft:block/block",
        "textures": {
            "tela": "intoxicantes:block/placa_esquinao_tela",
            "moldura": "intoxicantes:block/placa_esquinao_back",
            "particle": "intoxicantes:block/placa_esquinao_back"},
        "elements": [{
            "from": [0, 0, 13], "to": [16, 16, 16],
            "faces": {
                "north": {"texture": "#tela"},
                "south": {"texture": "#moldura"},
                "up": {"texture": "#moldura"},
                "down": {"texture": "#moldura"},
                "west": {"texture": "#moldura"},
                "east": {"texture": "#moldura"}}}]})
    # ---- ANATOMIA VELHA (1.2.24–1.2.30, mantida só pros saves migrarem):
    # TORRE — RODAPÉ: pedestal de concreto com a base da coluna em cima
    wjson(os.path.join(ASSETS, "models", "block", "placa_esquinao_rodape.json"), {
        "parent": "minecraft:block/block",
        "textures": {
            "ped": "intoxicantes:block/placa_esquinao_rodape",
            "post": "intoxicantes:block/placa_esquinao_coluna",
            "particle": "intoxicantes:block/placa_esquinao_rodape"},
        "elements": [
            {"from": [4, 0, 4], "to": [12, 10, 12], "faces": {
                "north": {"texture": "#ped"}, "south": {"texture": "#ped"},
                "west": {"texture": "#ped"}, "east": {"texture": "#ped"},
                "up": {"texture": "#ped"}, "down": {"texture": "#ped"}}},
            {"from": [6, 10, 6], "to": [10, 16, 10], "faces": {
                "north": {"texture": "#post"}, "south": {"texture": "#post"},
                "west": {"texture": "#post"}, "east": {"texture": "#post"},
                "up": {"texture": "#post"}, "down": {"texture": "#post"}}}]})
    # TORRE — COLUNA: metal fino no centro (a antiga, agora com cinta)
    wjson(os.path.join(ASSETS, "models", "block", "placa_esquinao_coluna.json"), {
        "parent": "minecraft:block/block",
        "textures": {
            "post": "intoxicantes:block/placa_esquinao_coluna",
            "particle": "intoxicantes:block/placa_esquinao_coluna"},
        "elements": [{
            "from": [6, 0, 6], "to": [10, 16, 10],
            "faces": {
                "north": {"texture": "#post"},
                "south": {"texture": "#post"},
                "up": {"texture": "#post"},
                "down": {"texture": "#post"},
                "west": {"texture": "#post"},
                "east": {"texture": "#post"}}}]})
    # TORRE — TOPO: capitel (coroa larga) segurando o painel
    wjson(os.path.join(ASSETS, "models", "block", "placa_esquinao_topo.json"), {
        "parent": "minecraft:block/block",
        "textures": {
            "cap": "intoxicantes:block/placa_esquinao_rodape",
            "post": "intoxicantes:block/placa_esquinao_coluna",
            "particle": "intoxicantes:block/placa_esquinao_rodape"},
        "elements": [
            {"from": [6, 0, 6], "to": [10, 12, 10], "faces": {
                "north": {"texture": "#post"}, "south": {"texture": "#post"},
                "west": {"texture": "#post"}, "east": {"texture": "#post"},
                "up": {"texture": "#post"}, "down": {"texture": "#post"}}},
            {"from": [4, 12, 4], "to": [12, 16, 12], "faces": {
                "north": {"texture": "#cap"}, "south": {"texture": "#cap"},
                "west": {"texture": "#cap"}, "east": {"texture": "#cap"},
                "up": {"texture": "#cap"}, "down": {"texture": "#cap"}}}]})

# ---------------------------------------------------------------- modelos/definicoes de item
def gen_item_models():
    # sementes
    for crop in CROPS:
        wjson(os.path.join(ASSETS, "models", "item", "semente_" + crop + ".json"),
              {"parent": "minecraft:item/generated",
               "textures": {"layer0": "intoxicantes:item/semente_" + crop}})
        wjson(os.path.join(ASSETS, "items", "semente_" + crop + ".json"),
              item_def("intoxicantes:item/semente_" + crop))

    # produtos
    for prod in ["lupulo", "uva", "cafe_verde", "cana_de_acucar"]:
        wjson(os.path.join(ASSETS, "models", "item", prod + ".json"),
              {"parent": "minecraft:item/generated",
               "textures": {"layer0": "intoxicantes:item/" + prod}})
        wjson(os.path.join(ASSETS, "items", prod + ".json"),
              item_def("intoxicantes:item/" + prod))

    # lampada (item)
    wjson(os.path.join(ASSETS, "models", "item", "lampada_uv.json"),
          {"parent": "minecraft:item/generated",
           "textures": {"layer0": "intoxicantes:item/lampada_uv"}})
    wjson(os.path.join(ASSETS, "items", "lampada_uv.json"),
          item_def("intoxicantes:item/lampada_uv"))

    # LETREIRO DO ESQUINÃO (item)
    # v1.2.19: poste de luz (item) — o modelo 3D de bloco no inventário
    # v1.2.24: herda a BASE (pedestal + coluna), não um cube_all órfão
    wjson(os.path.join(ASSETS, "models", "item", "poste_luz.json"),
          {"parent": "intoxicantes:block/poste_luz_base"})
    wjson(os.path.join(ASSETS, "items", "poste_luz.json"),
          item_def("intoxicantes:block/poste_luz_base"))
    # v1.2.19: asfalto (item; é obtido via criativo/comando)
    wjson(os.path.join(ASSETS, "models", "item", "asfalto.json"),
          {"parent": "intoxicantes:block/asfalto"})
    wjson(os.path.join(ASSETS, "items", "asfalto.json"),
          item_def("intoxicantes:block/asfalto"))

    # v1.2.23: a placa no inventário ganhou corpo 3D (o painel suspenso do
    # modelo do bloco) — o item model herda a geometria em vez do sprite chato
    wjson(os.path.join(ASSETS, "models", "item", "placa_esquinao.json"),
          {"parent": "intoxicantes:block/placa_esquinao"})
    wjson(os.path.join(ASSETS, "items", "placa_esquinao.json"),
          item_def("intoxicantes:block/placa_esquinao"))

    # v1.2.36: PAINEL DE LED (item) — a TV de tela plana no inventário (o
    # modelo fino de bloco, igual a placa)
    wjson(os.path.join(ASSETS, "models", "item", "painel_led.json"),
          {"parent": "intoxicantes:block/painel_led"})
    wjson(os.path.join(ASSETS, "items", "painel_led.json"),
          item_def("intoxicantes:block/painel_led"))

    # blocos das plantas: item do bloco usa sprite da planta madura
    for crop in CROPS:
        wjson(os.path.join(ASSETS, "models", "item", crop + "_plant.json"),
              {"parent": "minecraft:item/generated",
               "textures": {"layer0": "intoxicantes:block/" + crop + "_stage4_ripe"}})
        wjson(os.path.join(ASSETS, "items", crop + "_plant.json"),
              item_def("intoxicantes:item/" + crop + "_plant"))

# ---------------------------------------------------------------- loot tables de colheita
def gen_loot_tables():
    for crop in CROPS:
        produto = {"maconha": "maconha_seda", "lupulo": "lupulo", "uva": "uva",
                   "cafe": "cafe_verde", "papoula": "opio"}[crop]
        # esperar a maturacao VALE 3x o produto (vs 1x na so-madura) e DEVOLVE a
        # semente garantida — o loop da plantacao nunca morre por esperar demais.
        # v1.2.16: a conta é 1 (pool madura) + EXTRA (pool ripe) = TOTAL; pra 3x
        # o extra é 2 — o set_count 3 antigo dava 4x (a regeneração expôs a
        # deriva entre gerador e tables afinadas à mão; agora gerador = disk)
        bonus = {"maconha": 2, "lupulo": 2, "uva": 2, "cafe": 2, "papoula": 2}[crop]
        mature = {"type": "minecraft:match_block", "blocks": "intoxicantes:" + crop + "_plant", "state": {"age": "4"}}
        ripe = {"type": "minecraft:match_block", "blocks": "intoxicantes:" + crop + "_plant", "state": {"age": "4", "uv_age": "3"}}
        # 26.3 uses singular condition/modifier. Old plural fields are silently ignored.
        # Always return the planted seed; only grown plants yield a sellable product.
        table = {
            "type": "minecraft:block",
            "modifier": {"type": "minecraft:explosion_decay"},
            "pools": [
                {"rolls": 1, "entries": [{"type": "minecraft:item", "name": "intoxicantes:semente_" + crop}]},
                {"rolls": 1, "condition": mature, "entries": [{"type": "minecraft:item", "name": "intoxicantes:" + produto}]},
                {"rolls": 1, "condition": ripe, "entries": [{
                    "type": "minecraft:item", "name": "intoxicantes:" + produto,
                    "modifier": {"type": "minecraft:set_count", "count": bonus}
                }]},
                {"rolls": 1, "condition": ripe, "entries": [{
                    "type": "minecraft:item", "name": "intoxicantes:semente_" + crop
                }]}
            ],
            "random_sequence": "intoxicantes:blocks/" + crop + "_plant"
        }
        wjson(os.path.join(DATA, "loot_table", "blocks", crop + "_plant.json"), table)

    # lampada: dropa a si mesma
    wjson(os.path.join(DATA, "loot_table", "blocks", "lampada_uv.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "intoxicantes:lampada_uv"}]}],
        "random_sequence": "intoxicantes:blocks/lampada_uv"
    })

    # letreiro: qualquer parte dropa o item (o playerWillDestroy derruba o
    # resto do multi-bloco SEM drop — a placa é um objeto só)
    wjson(os.path.join(DATA, "loot_table", "blocks", "placa_esquinao.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "intoxicantes:placa_esquinao"}]}],
        "random_sequence": "intoxicantes:blocks/placa_esquinao"
    })

    # painel de LED (v1.2.36): dropa 1 item POR TELA da linha (TELAS 1..3)
    wjson(os.path.join(DATA, "loot_table", "blocks", "painel_led.json"), {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{
                "type": "minecraft:item", "name": "intoxicantes:painel_led",
                "functions": [{"function": "minecraft:set_count",
                    "count": {"type": "minecraft:state_provider",
                        "state": {"property": "telas"}}}]
            }]
        }],
        "random_sequence": "intoxicantes:blocks/painel_led"
    })

    # poste de luz: dropa a si mesmo (v1.2.19)
    wjson(os.path.join(DATA, "loot_table", "blocks", "poste_luz.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "intoxicantes:poste_luz"}]}],
        "random_sequence": "intoxicantes:blocks/poste_luz"
    })
    # asfalto: dropa a si mesmo (v1.2.19)
    wjson(os.path.join(DATA, "loot_table", "blocks", "asfalto.json"), {
        "type": "minecraft:block",
        "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "intoxicantes:asfalto"}]}],
        "random_sequence": "intoxicantes:blocks/asfalto"
    })

# ---------------------------------------------------------------- receitas
def gen_recipes():
    def shapeless(ingredients, result, count=1):
        return {
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": ingredients,
            "result": {"id": result, "count": count}
        }

    # bebidas agora usam os produtos das plantacoes
    wjson(os.path.join(DATA, "recipe", "cerveja.json"), shapeless(
        ["intoxicantes:lupulo", "intoxicantes:lupulo", "minecraft:glass_bottle", "minecraft:wheat"],
        "intoxicantes:cerveja", 2))
    wjson(os.path.join(DATA, "recipe", "vinho.json"), shapeless(
        ["intoxicantes:uva", "intoxicantes:uva", "intoxicantes:uva", "minecraft:glass_bottle"],
        "intoxicantes:vinho", 1))
    wjson(os.path.join(DATA, "recipe", "cachaca.json"), shapeless(
        ["intoxicantes:cana_de_acucar", "intoxicantes:cana_de_acucar", "minecraft:glass_bottle"],
        "intoxicantes:cachaca", 1))
    wjson(os.path.join(DATA, "recipe", "rum.json"), shapeless(
        ["intoxicantes:cana_de_acucar", "intoxicantes:cana_de_acucar",
         "intoxicantes:cana_de_acucar", "minecraft:glass_bottle"],
        "intoxicantes:rum", 1))

    # heroina agora parte do opio colhido da papoula
    wjson(os.path.join(DATA, "recipe", "heroina.json"), shapeless(
        ["intoxicantes:opio", "intoxicantes:opio", "minecraft:redstone", "minecraft:slime_ball"],
        "intoxicantes:heroina", 1))

    # v1.2.39: CIGARRO CAMEL — papel + folha dourada (tabaco do Juça)
    wjson(os.path.join(DATA, "recipe", "cigarro_camel.json"), shapeless(
        ["minecraft:paper", "minecraft:golden_carrot"],
        "intoxicantes:cigarro_camel", 3))
    # v1.2.39: CAMISA DO MATANZA — lã preta + couro (a farda do rock)
    wjson(os.path.join(DATA, "recipe", "camisa_matanza.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["W W", "WWW", "WWW"],
        "key": {
            "W": "minecraft:black_wool"
        },
        "result": {"id": "intoxicantes:camisa_matanza", "count": 1}
    })

    # v1.2.36: PAINEL DE LED CRAFTÁVEL — vidro + redstone + iron (a TV da
    # fachada, programável pela Central de Comando)
    wjson(os.path.join(DATA, "recipe", "painel_led.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["III", "GRG", "GGG"],
        "key": {
            "I": "minecraft:iron_ingot",
            "G": "minecraft:glass",
            "R": "minecraft:redstone_block"
        },
        "result": {"id": "intoxicantes:painel_led", "count": 1}
    })

    # v1.2.38: o CONTROLE REMOTO — a Central de Comando em item (aponta pro
    # painel/letreiro e edita): vidro de tela, ferro no corpo, redstone de
    # dentro e botão de pedra (o "OK")
    wjson(os.path.join(DATA, "recipe", "central_comando.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["WIW", "GRG", " S "],
        "key": {
            "W": "minecraft:white_stained_glass",
            "I": "minecraft:iron_ingot",
            "G": "minecraft:glass",
            "R": "minecraft:redstone_block",
            "S": "minecraft:stone_button"
        },
        "result": {"id": "intoxicantes:central_comando", "count": 1}
    })

    # lampada UV
    wjson(os.path.join(DATA, "recipe", "lampada_uv.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["IWI", "GGG", "GDG"],
        "key": {
            "I": "minecraft:iron_ingot",
            "W": "minecraft:white_stained_glass",
            "G": "minecraft:glass",
            "D": "minecraft:glowstone_dust"
        },
        "result": {"id": "intoxicantes:lampada_uv", "count": 1}
    })

def main():
    gen_blockstates()
    gen_block_models()
    gen_item_models()
    gen_loot_tables()
    gen_recipes()
    print("OK: blockstates, modelos, items, loot tables e receitas de farm gerados")

if __name__ == "__main__":
    main()
