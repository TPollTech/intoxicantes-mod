"""Validador de worldgen do mod: impede que um JSON quebrado trave a criacao de mundo.

Motivacao: o template_pool chegou a referenciar "minecraft:none" (processor que nao
existe), derrubando o registry worldgen/processor_list inteiro e travando o jogo em
"Preparing for world creation". Este script varre os JSONs de worldgen + receitas do
mod e confere TODAS as referencias contra o jar vanilla mapeado (fonte da verdade),
alem de validar o NBT da estrutura byte a byte.

O que e validado:
  - Referencias de registro (start_pool -> template_pool, placed -> feature,
    structure_set -> structure, processors) existem no vanilla ou no proprio mod
  - feature type / placement type / element_type existem (coletados dos JSONs vanilla)
  - Blocos e blockstates (id[prop=valor]) existem; propriedades/valores conferem
    com o blockstate do jar (variants E multipart); tags de bloco existem
  - Estrutura NBT: parse byte a byte, DataVersion bate com version.json, paleta
    valida, total de blocks = x*y*z, indices dentro da paleta, template citado
    pelo pool existe

Uso:  python tools/validate_worldgen.py
Saida: exit 0 se tudo ok; exit 1 listando os erros (bloqueia o build).
"""
import gzip
import json
import os
import re
import struct
import sys
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src", "main", "resources", "data")
GRADLE_CACHE = os.path.join(os.environ.get("USERPROFILE", ""), ".gradle", "caches", "fabric-loom")

erros = []


def erro(msg):
    erros.append(msg)


# ============================================================ jar vanilla
def achar_jar_vanilla():
    """Acha o minecraft-merged-deobf mais recente no cache do Loom."""
    candidatos = []
    maven = os.path.join(GRADLE_CACHE, "minecraftMaven", "net", "minecraft")
    if not os.path.isdir(maven):
        return None
    for raiz, _, arquivos in os.walk(maven):
        for a in arquivos:
            if a.endswith(".jar") and "deobf" in a and "sources" not in a:
                cheio = os.path.join(raiz, a)
                candidatos.append((os.path.getmtime(cheio), cheio))
    if not candidatos:
        return None
    return max(candidatos)[1]


JAR = achar_jar_vanilla()
if JAR is None:
    print("ERRO: jar vanilla (minecraft-merged-deobf) nao encontrado no cache do Loom.")
    print("      Rode o build do mod uma vez antes (gradlew build) pra popular o cache.")
    sys.exit(2)

z = zipfile.ZipFile(JAR)


# ============================================================ referencias do vanilla
DATA_VERSION = None
VANILLA_WORLDGEN_IDS = {}       # "worldgen/structure" -> {"minecraft:igloo", ...}
VANILLA_BLOCK_TAGS = set()      # {"minecraft:dirt", ...}
VANILLA_BIOME_TAGS = set()      # {"minecraft:has_structure/igloo", ...}
VANILLA_BLOCKSTATES = {}        # "minecraft:poppy" -> {"age": {"0","1",...}}
VANILLA_FEATURE_TYPES = set()   # coletados dos JSONs vanilla
VANILLA_PLACEMENT_TYPES = set()
VANILLA_POOL_ELEMENT_TYPES = set()
VANILLA_RECIPE_TYPES = set()


def carregar_vanilla():
    global DATA_VERSION, VANILLA_BLOCK_TAGS, VANILLA_BIOME_TAGS
    global VANILLA_BLOCKSTATES, VANILLA_FEATURE_TYPES, VANILLA_PLACEMENT_TYPES
    global VANILLA_POOL_ELEMENT_TYPES, VANILLA_RECIPE_TYPES

    ver = None
    try:
        ver = json.loads(z.read("version.json").decode("utf-8"))
    except KeyError:
        pass
    if ver and "world_version" in ver:
        DATA_VERSION = ver["world_version"]

    nomes = z.namelist()

    # ---- ids de worldgen por prefixo
    prefixos = set()
    for n in nomes:
        m = re.match(r"^data/minecraft/(worldgen/[a-z_]+)/[a-z0-9_/]+\.json$", n)
        if m:
            prefixos.add(m.group(1))
    for prefixo in sorted(prefixos):
        ids = set()
        for n in nomes:
            m = re.match(rf"^data/minecraft/{re.escape(prefixo)}/([a-z0-9_/]+)\.json$", n)
            if m:
                ids.add("minecraft:" + m.group(1))
        VANILLA_WORLDGEN_IDS[prefixo] = ids

    # ---- tags de bloco e de bioma
    for n in nomes:
        m = re.match(r"^data/minecraft/tags/block/([a-z0-9_/]+)\.json$", n)
        if m:
            VANILLA_BLOCK_TAGS.add("minecraft:" + m.group(1))
        m = re.match(r"^data/minecraft/tags/worldgen/biome/([a-z0-9_/]+)\.json$", n)
        if m:
            VANILLA_BIOME_TAGS.add("minecraft:" + m.group(1))

    # ---- blockstates: id -> propriedade -> valores validos
    def coletar_when(when, props):
        if not isinstance(when, dict):
            return
        for k, v in when.items():
            if k in ("OR", "AND", "NOR") and isinstance(v, list):
                for sub in v:
                    coletar_when(sub, props)
            elif isinstance(v, str):
                props.setdefault(k, set()).update(v.split("|"))

    for n in nomes:
        m = re.match(r"^assets/minecraft/blockstates/([a-z0-9_]+)\.json$", n)
        if not m:
            continue
        bid = "minecraft:" + m.group(1)
        try:
            bs = json.loads(z.read(n).decode("utf-8"))
        except Exception:
            continue
        props = {}
        # variants: chaves tipo "face=floor,facing=east" (ou "" pra sem props)
        for chave in bs.get("variants", {}).keys():
            if not chave:
                continue
            for par in chave.split(","):
                if "=" in par:
                    k, v = par.split("=", 1)
                    props.setdefault(k, set()).add(v)
        # multipart: "when" dentro de cada entrada
        for entrada in bs.get("multipart", []):
            coletar_when(entrada.get("when", {}), props)
        VANILLA_BLOCKSTATES[bid] = props

    # ---- tipos: coleta dos proprios JSONs vanilla (fonte da verdade)
    tipos_feature, tipos_placement = set(), set()
    tipos_pool, tipos_receita = set(), set()
    for n in nomes:
        if n.startswith("data/minecraft/worldgen/") and n.endswith(".json"):
            try:
                dado = json.loads(z.read(n).decode("utf-8"))
            except Exception:
                continue
            if not isinstance(dado, dict):
                continue
            if n.startswith("data/minecraft/worldgen/feature/"):
                t = dado.get("type")
                if isinstance(t, str):
                    tipos_feature.add(t)
            elif n.startswith("data/minecraft/worldgen/placed_feature/"):
                for p in dado.get("placement", []):
                    if isinstance(p, dict) and isinstance(p.get("type"), str):
                        tipos_placement.add(p["type"])
            elif n.startswith("data/minecraft/worldgen/template_pool/"):
                for el in dado.get("elements", []):
                    et = el.get("element", {}).get("element_type")
                    if isinstance(et, str):
                        tipos_pool.add(et)
        elif n.startswith("data/minecraft/recipe/") and n.endswith(".json"):
            try:
                r = json.loads(z.read(n).decode("utf-8"))
            except Exception:
                continue
            if isinstance(r.get("type"), str):
                tipos_receita.add(r["type"])

    VANILLA_FEATURE_TYPES = tipos_feature
    VANILLA_PLACEMENT_TYPES = tipos_placement
    VANILLA_POOL_ELEMENT_TYPES = tipos_pool
    VANILLA_RECIPE_TYPES = tipos_receita


carregar_vanilla()


# ============================================================ arquivos do mod
MOD_JSONS = {}   # "intoxicantes/worldgen/structure/x.json" -> dict
MOD_IDS = {}     # prefixo ("worldgen/structure") -> {"intoxicantes:x"}
MOD_NBTS = set() # {"intoxicantes/structure/mercado_gago.nbt", ...}

# prefixos de registro que o mod usa (ordem mais-longa-primeiro: structure_set
# antes de structure, senao o id sai errado)
PREFIXOS_MOD = ["worldgen/template_pool", "worldgen/placed_feature", "worldgen/structure_set",
                "worldgen/structure", "worldgen/feature", "recipe", "loot_table"]


def carregar_mod():
    for raiz, _, arquivos in os.walk(DATA):
        for a in arquivos:
            cheio = os.path.join(raiz, a)
            rel = os.path.relpath(cheio, DATA).replace("\\", "/")
            if a.endswith(".nbt"):
                MOD_NBTS.add(rel)
                continue
            if not a.endswith(".json"):
                continue
            try:
                with open(cheio, encoding="utf-8") as f:
                    dado = json.load(f)
            except Exception as e:
                erro(f"JSON invalido: {rel} ({e})")
                continue
            MOD_JSONS[rel] = dado
            for prefixo in sorted(PREFIXOS_MOD, key=len, reverse=True):
                cabeca = f"intoxicantes/{prefixo}/"
                if rel.startswith(cabeca) and rel.endswith(".json"):
                    MOD_IDS.setdefault(prefixo, set()).add(
                        "intoxicantes:" + rel[len(cabeca):-len(".json")])
                    break


carregar_mod()


# ============================================================ helpers
def ref_valida(ref, prefixo):
    """'ns:caminho' existe como data/<ns>/<prefixo>/<caminho>.json (vanilla ou mod)?"""
    if ":" not in ref:
        return False
    ns, caminho = ref.split(":", 1)
    if ns == "intoxicantes":
        return ref in MOD_IDS.get(prefixo, set())
    if ns == "minecraft":
        return ref in VANILLA_WORLDGEN_IDS.get(prefixo, set())
    return True  # outro mod: fora do escopo deste validador


def checar_bloco_estado(ref, onde):
    """'minecraft:poppy', 'minecraft:slab[type=bottom]', '#minecraft:dirt'."""
    if ref.startswith("#"):
        if ref[1:] not in VANILLA_BLOCK_TAGS:
            erro(f"{onde}: tag de bloco inexistente no vanilla: {ref}")
        return
    if ":" not in ref:
        erro(f"{onde}: id de bloco sem namespace: {ref}")
        return
    ns, caminho = ref.split(":", 1)
    base, _, props_txt = caminho.partition("[")
    bid = f"{ns}:{base}"
    if ns == "minecraft" and bid not in VANILLA_BLOCKSTATES:
        erro(f"{onde}: bloco inexistente no vanilla: {bid}")
        return
    if ns != "minecraft":
        return
    props = {}
    if props_txt:
        if not props_txt.endswith("]"):
            erro(f"{onde}: blockstate malformado: {ref}")
            return
        for par in props_txt[:-1].split(","):
            if "=" not in par:
                erro(f"{onde}: propriedade malformada em {ref}")
                return
            k, v = par.split("=", 1)
            props[k] = v
    validas = VANILLA_BLOCKSTATES.get(bid, {})
    for k, v in props.items():
        if k not in validas:
            erro(f"{onde}: propriedade '{k}' nao existe em {bid} (validas: {sorted(validas)})")
        elif v not in validas[k]:
            erro(f"{onde}: valor '{v}' invalido pra '{k}' de {bid} (validos: {sorted(validas[k])})")


# ============================================================ validacoes por tipo
def validar_template_pool(rel, pool):
    for i, el in enumerate(pool.get("elements", [])):
        e = el.get("element", {})
        et = e.get("element_type", "")
        if et and et not in VANILLA_POOL_ELEMENT_TYPES:
            erro(f"{rel}[{i}]: element_type inexistente no vanilla: {et} "
                 f"(existentes: {sorted(VANILLA_POOL_ELEMENT_TYPES)})")
        proc = e.get("processors")
        if isinstance(proc, str):
            if not ref_valida(proc, "worldgen/processor_list"):
                erro(f"{rel}[{i}]: processors referencia inexistente: {proc} "
                     f"(isso TRAVA a criacao de mundo! use {{\"processors\": []}} inline)")
        loc = e.get("location")
        if isinstance(loc, str) and ":" in loc:
            ns, caminho = loc.split(":", 1)
            chave = f"{ns}/structure/{caminho}.nbt"
            if ns == "intoxicantes" and chave not in MOD_NBTS:
                erro(f"{rel}[{i}]: template nao existe no mod: {loc} "
                     f"(esperado data/{chave})")


def validar_structure(rel, s):
    biomas = s.get("biomes", "")
    if isinstance(biomas, str) and biomas.startswith("#"):
        ref = biomas[1:]
        if ref in VANILLA_BIOME_TAGS:
            pass
        elif ref.startswith("intoxicantes:"):
            # tag de bioma DO PROPRIO MOD: o arquivo precisa existir
            # (data/intoxicantes/tags/worldgen/biome/<caminho>.json)
            caminho_tag = os.path.join(DATA, "intoxicantes", "tags", "worldgen",
                                       "biome", ref.split(":", 1)[1] + ".json")
            if not os.path.exists(caminho_tag):
                erro(f"{rel}: tag de bioma do mod sem arquivo: {biomas} "
                     f"(esperado {caminho_tag})")
        else:
            erro(f"{rel}: tag de bioma inexistente no vanilla: {biomas}")
    pool = s.get("start_pool", "")
    if isinstance(pool, str) and ":" in pool and not ref_valida(pool, "worldgen/template_pool"):
        erro(f"{rel}: start_pool nao existe (vanilla nem mod): {pool}")
    hm = s.get("project_start_to_heightmap")
    validas_hm = {"WORLD_SURFACE_WG", "WORLD_SURFACE", "OCEAN_FLOOR_WG", "OCEAN_FLOOR",
                  "MOTION_BLOCKING", "MOTION_BLOCKING_NO_LEAVES"}
    if hm is not None and hm not in validas_hm:
        erro(f"{rel}: heightmap desconhecida: {hm}")


def validar_placed(rel, pf):
    f = pf.get("feature", "")
    if isinstance(f, str) and ":" in f and not ref_valida(f, "worldgen/feature"):
        erro(f"{rel}: feature referenciada nao existe (vanilla nem mod): {f}")
    for i, p in enumerate(pf.get("placement", [])):
        t = p.get("type", "")
        if t and t not in VANILLA_PLACEMENT_TYPES:
            erro(f"{rel}[{i}]: placement type inexistente: {t} "
                 f"(existentes: {sorted(VANILLA_PLACEMENT_TYPES)})")
        if t == "minecraft:block_predicate_filter":
            _checar_predicado(rel, i, p.get("predicate", {}))
        if t == "minecraft:heightmap":
            hm = p.get("heightmap", "")
            validas = {"WORLD_SURFACE_WG", "WORLD_SURFACE", "OCEAN_FLOOR_WG", "OCEAN_FLOOR",
                       "MOTION_BLOCKING", "MOTION_BLOCKING_NO_LEAVES"}
            if hm not in validas:
                erro(f"{rel}[{i}]: heightmap desconhecida: {hm}")


def _checar_predicado(rel, i, pred):
    if not isinstance(pred, dict):
        return
    t = pred.get("type", "")
    if "blocks" in pred:
        b = pred["blocks"]
        alvo = b if isinstance(b, list) else [b]
        for ref in alvo:
            checar_bloco_estado(ref, f"{rel}[{i}] predicate({t})")
    if isinstance(pred.get("tag"), str):
        tag = pred["tag"]
        checar_bloco_estado(tag if tag.startswith("#") else "#" + tag, f"{rel}[{i}] predicate({t})")
    for k in ("all_of", "any_of", "none_of"):
        for sub in pred.get(k, []):
            _checar_predicado(rel, i, sub)


def validar_feature(rel, feat):
    t = feat.get("type", "")
    if t and t not in VANILLA_FEATURE_TYPES:
        erro(f"{rel}: feature type inexistente: {t} "
             f"(existentes: {len(VANILLA_FEATURE_TYPES)} tipos, ex: {sorted(VANILLA_FEATURE_TYPES)[:4]})")
    # simple_block / random_patch: estados de bloco
    for chave in ("to_place", "state", "config"):
        cfg = feat.get(chave)
        if isinstance(cfg, dict):
            state = cfg.get("state")
            if isinstance(state, dict) and isinstance(state.get("id"), str):
                props = state.get("properties", {})
                sufixo = "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]" if props else ""
                checar_bloco_estado(state["id"] + sufixo, f"{rel} {chave}")


def validar_receita(rel, r):
    t = r.get("type", "")
    if t and t not in VANILLA_RECIPE_TYPES:
        print(f"  aviso: {rel}: recipe type nao encontrado nos JSONs vanilla: {t}")


def validar_nbt_mercado():
    nbt = os.path.join(DATA, "intoxicantes", "structure", "mercado_gago.nbt")
    if not os.path.exists(nbt):
        erro("structure/mercado_gago.nbt nao existe (o template_pool precisa dele)")
        return
    raw = gzip.decompress(open(nbt, "rb").read())

    # ---- parser NBT completo (13 tipos), byte a byte, formato 26.3
    class Fail(Exception):
        pass

    class R:
        def __init__(self, b):
            self.b, self.i = b, 0

        def need(self, k):
            if self.i + k > len(self.b):
                raise Fail(f"EOF: pedindo {k} em {self.i}/{len(self.b)}")
            v = self.b[self.i:self.i + k]
            self.i += k
            return v

        def u1(self):
            return self.need(1)[0]

        def i2(self):
            return struct.unpack(">H", self.need(2))[0]

        def i4(self):
            return struct.unpack(">i", self.need(4))[0]

        def st(self):
            n = self.i2()
            if n > 1_000_000:
                raise Fail(f"string absurda ({n}) em {self.i}")
            return self.need(n).decode("utf-8", "replace")

        def val(self, t):
            if t == 1:
                return self.need(1)[0]
            if t == 2:
                return self.i2()
            if t == 3:
                return self.i4()
            if t == 4:
                return struct.unpack(">q", self.need(8))[0]
            if t == 5:
                return struct.unpack(">f", self.need(4))[0]
            if t == 6:
                return struct.unpack(">d", self.need(8))[0]
            if t == 7:
                k = self.i4()
                if k < 0:
                    raise Fail("bytearray negativo")
                return self.need(k)
            if t == 8:
                return self.st()
            if t == 9:
                it = self.u1()
                k = self.i4()
                if it == 0:
                    return []  # lista vazia de tipo 0 (fim) e permitida
                if k < 0 or k > 5_000_000:
                    raise Fail(f"lista len {k}")
                return [self.val(it) for _ in range(k)]
            if t == 10:
                d = {}
                while True:
                    tt = self.u1()
                    if tt == 0:
                        return d
                    # IMPORTANTE: nome ANTES do payload — em Python,
                    # d[self.st()] = self.val(tt) avaliaria o RHS (payload)
                    # PRIMEIRO e dessincronizaria o stream inteiro!
                    nm = self.st()
                    d[nm] = self.val(tt)
            if t == 11:
                k = self.i4()
                if k < 0 or k > 10_000_000:
                    raise Fail(f"intarray len {k}")
                return list(struct.unpack(f">{k}i", self.need(4 * k)))
            if t == 12:
                k = self.i4()
                if k < 0 or k > 10_000_000:
                    raise Fail(f"longarray len {k}")
                return list(struct.unpack(f">{k}q", self.need(8 * k)))
            raise Fail(f"tag NBT {t} inesperada em {self.i}")

    try:
        r = R(raw)
        t = r.u1()
        if t != 10:
            erro("mercado_gago.nbt: raiz nao e TAG_Compound")
            return
        r.st()
        d = r.val(10)
        if r.i != len(raw):
            erro(f"mercado_gago.nbt: {len(raw) - r.i} byte(s) sobrando no fim (NBT malformado)")
    except Fail as f:
        erro(f"mercado_gago.nbt: NBT malformado ({f})")
        return

    # ---- AUTO-TESTE do parser: tem que parsear um template vanilla conhecido.
    # Se isso falhar, o parser esta quebrado (nao o arquivo do mod!).
    try:
        igloo_raw = gzip.decompress(z.read("data/minecraft/structure/igloo/top.nbt"))
        ri = R(igloo_raw)
        assert ri.u1() == 10
        ri.st()
        ig = ri.val(10)
        if not (isinstance(ig.get("blocks"), list) and len(ig["blocks"]) > 0):
            erro("AUTO-TESTE: parser NBT nao consegue ler o igloo/top.nbt vanilla "
                 "(parser quebrado — nao confie nos resultados dele)")
    except Exception as e:
        erro(f"AUTO-TESTE: parser NBT falhou no igloo vanilla: {e}")

    if DATA_VERSION and d.get("DataVersion") != DATA_VERSION:
        erro(f"mercado_gago.nbt: DataVersion {d.get('DataVersion')} != vanilla {DATA_VERSION} "
             f"(template de outra versao pode corromper mundos)")

    # ---- formato 26.3: size = TAG_List de 3 ints; blocks/palette = TAG_List de compounds
    size = d.get("size")
    if not (isinstance(size, list) and len(size) == 3):
        erro("mercado_gago.nbt: 'size' deveria ser TAG_List de 3 ints (formato 26.3)")
        return
    sx, sy, sz = size
    total = sx * sy * sz

    paleta = d.get("palette")
    blocks = d.get("blocks")
    if not isinstance(paleta, list) or not isinstance(blocks, list):
        erro("mercado_gago.nbt: 'blocks' e 'palette' deveriam ser TAG_List de compounds (formato 26.3)")
        return
    if len(blocks) == 0:
        erro("mercado_gago.nbt: nenhum bloco no template (estrutura vazia)")
    if len(paleta) < 2:
        erro(f"mercado_gago.nbt: paleta com {len(paleta)} entradas (menos que 2 = estrutura vazia ou placeholder)")

    vistos = set()
    for e in blocks:
        if not isinstance(e, dict) or "pos" not in e or "state" not in e:
            erro("mercado_gago.nbt: entrada de bloco sem 'pos'/'state'")
            break
        pos = e["pos"]
        if not (isinstance(pos, list) and len(pos) == 3):
            erro("mercado_gago.nbt: 'pos' deveria ser lista de 3 ints")
            break
        px, py, pz = pos
        if not (0 <= px < sx and 0 <= py < sy and 0 <= pz < sz):
            erro(f"mercado_gago.nbt: pos {pos} fora do tamanho {size}")
            break
        if not isinstance(e["state"], int) or not (0 <= e["state"] < len(paleta)):
            erro(f"mercado_gago.nbt: state {e.get('state')} fora da paleta (tam {len(paleta)})")
            break
        vistos.add(tuple(pos))
    if len(vistos) != len(blocks):
        erro(f"mercado_gago.nbt: {len(blocks) - len(vistos)} bloco(s) duplicado(s) na mesma pos")

    # ---- NBT de tile entity: tem que ser NA ENTRADA DE BLOCO, nunca na paleta.
    # (o vanilla inteiro: 0 na paleta, ~5600 na entrada; StructureTemplate so
    # repassa o nbt da entrada pro placeInWorld — na paleta e' ignorado e a
    # placa nasce SEM TEXTO, que foi o bug do letreiro do mercado)
    for e in paleta:
        if isinstance(e, dict) and "nbt" in e:
            erro(f"mercado_gago.nbt: paleta[{e.get('id')}] tem 'nbt' — o vanilla so le "
                 "nbt da ENTRADA DE BLOCO (blocks[*].nbt); na paleta e' ignorado")
            break

    # ---- placa de parede precisa de SUPORTE: wall_sign se prende no bloco
    # ATRAS dela (oposto ao facing); sem suporte ela desanexa na geracao e o
    # letreiro vira item caindo (2o bug do letreiro)
    idx_por_pos = {tuple(b.get("pos", [0, 0, 0])): b for b in blocks if isinstance(b, dict)}

    def id_da_entrada(b):
        st = b.get("state")
        return paleta[st].get("id", "") if isinstance(st, int) and st < len(paleta) else ""

    for b in blocks:
        if not isinstance(b, dict) or "sign" not in id_da_entrada(b):
            continue
        px, py, pz = b["pos"]
        props = paleta[b["state"]].get("properties", {}) or {}
        facing = props.get("facing", "north")
        dx, dz = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}.get(
            facing, (0, -1))
        suporte = idx_por_pos.get((px - dx, py, pz - dz))
        sid = id_da_entrada(suporte) if suporte else ""
        if sid.endswith("air") or sid == "":
            erro(f"mercado_gago.nbt: wall_sign em {b['pos']} facing={facing} sem bloco de "
                 f"suporte atras ({px - dx},{py},{pz - dz}) — desanexa na geracao (use o "
                 "caractere de parede na posicao de tras)")

    for e in paleta:
        nome = e.get("id") if isinstance(e, dict) else None
        if not isinstance(nome, str) or ":" not in nome:
            erro(f"mercado_gago.nbt: paleta com id invalido: {nome!r}")
            continue
        props = e.get("properties", {}) if isinstance(e, dict) else {}
        sufixo = "[" + ",".join(f"{k}={v}" for k, v in props.items()) + "]" if props else ""
        checar_bloco_estado(nome + sufixo, "mercado_gago.nbt paleta")

    # entidades: lista de compounds com blockPos/pos/nbt(id)
    ents = d.get("entities", [])
    if not isinstance(ents, list):
        erro("mercado_gago.nbt: 'entities' deveria ser TAG_List")
    for ent in ents:
        if not isinstance(ent, dict):
            erro("mercado_gago.nbt: entrada de entity nao e compound")
            continue
        if not isinstance(ent.get("blockPos"), list) or not isinstance(ent.get("pos"), list):
            erro("mercado_gago.nbt: entity sem blockPos/pos")
            continue
        inner = ent.get("nbt", {})
        eid = inner.get("id") if isinstance(inner, dict) else None
        if not isinstance(eid, str) or ":" not in eid:
            erro(f"mercado_gago.nbt: entity sem id valido: {eid!r}")
        elif eid.startswith("intoxicantes:"):
            ns, caminho = eid.split(":", 1)
            chave = f"{ns}/entities/{caminho}.json"
            if chave in MOD_JSONS or os.path.exists(os.path.join(DATA, chave)):
                pass  # definicao de entidade do mod existe
            # (a entidade tambem pode ser registrada por codigo; nao e erro)


# ============================================================ varredura
for rel, dado in sorted(MOD_JSONS.items()):
    caminho = rel[len("intoxicantes/"):] if rel.startswith("intoxicantes/") else rel
    if caminho.startswith("worldgen/template_pool/"):
        validar_template_pool(rel, dado)
    elif caminho.startswith("worldgen/structure_set/"):
        for s in dado.get("structures", []):
            ref = s.get("structure", "")
            if isinstance(ref, str) and ":" in ref and not ref_valida(ref, "worldgen/structure"):
                erro(f"{rel}: structure nao existe (vanilla nem mod): {ref}")
        pl = dado.get("placement", {})
        if pl.get("type") != "minecraft:random_spread":
            erro(f"{rel}: placement type estranho: {pl.get('type')}")
        elif pl.get("separation", 0) >= pl.get("spacing", 1):
            erro(f"{rel}: separation >= spacing (vanilla exige separation < spacing)")
    elif caminho.startswith("worldgen/structure/"):
        validar_structure(rel, dado)
    elif caminho.startswith("worldgen/placed_feature/"):
        validar_placed(rel, dado)
    elif caminho.startswith("worldgen/feature/"):
        validar_feature(rel, dado)
    elif caminho.startswith("recipe/"):
        validar_receita(rel, dado)

validar_nbt_mercado()

# ============================================================ resultado
if erros:
    print(f"\nvalidate_worldgen: {len(erros)} ERRO(S):")
    for e in erros:
        print("  X " + e)
    print("\nUm worldgen quebrado TRAVA a criacao de mundo (registro 'Unbound values').")
    print("Conserte antes de buildar.")
    sys.exit(1)
print(f"validate_worldgen: OK — {len(MOD_JSONS)} JSONs + NBT validados contra "
      f"{os.path.basename(JAR)} (DataVersion {DATA_VERSION})")
