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
