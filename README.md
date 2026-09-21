# Intoxicantes Mod

Mod de Fabric para Minecraft que adiciona plantações, bebidas e um mercadinho
com NPCs pra vender tudo — incluindo cultivo indoor com a **Lâmpada UV**.

## Visão geral

- **Culturas**: maconha, lúpulo, uva, café e papoula — crescem em 5 estágios
  (`age 0..4`) e depois **amadurecem** em 4 níveis de UV (`uv_age 0..3`).
- **Lâmpada UV** (`intoxicantes:lampada_uv`): bloco de luz 15 que acelera o
  crescimento (×2) e amadurece as plantações indoor. Colher no ponto máximo
  (`age=4` + `uv_age=3`) rende o produto de melhor qualidade.
- **Mercadinho**: NPC vendedor com caixa registradora, estantes e comércio
  de produtos.
- **Config**: `config/intoxicantes.json` (escopeta, cotação da rua, UV,
  embriaguez).

## Estrutura

| Pasta | Conteúdo |
|---|---|
| `src/` | Código Java (Fabric), assets e data do mod |
| `tools/` | Scripts Python que geram texturas, modelos e data-driven JSON |
| `backups/` | Cópias de segurança antigas do código |
| `dist/` | `.jar` prontos pra instalar no `.minecraft/mods` |
| `run/` | Ambiente de execução de desenvolvimento |

## Build

```bash
./gradlew build
```

O `.jar` sai em `build/libs/`.

## Documentação

- `AGENTS.md` — guia de arquitetura e convenções do projeto
- `CHANGELOG.md` — histórico de versões
- `LEIA-ME-COMERCIO.md` — como funciona o comércio de NPCs
