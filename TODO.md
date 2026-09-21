# TODO — Intoxicantes Mod (atualizado em v1.2.10)

O mercado, a economia e a fazenda estão completos e funcionais. O que sobra é lapidação:

## Próximos passos (ordem sugerida)

1. **(FEITO na v1.2.4 + v1.2.7 + v1.2.10)** Sons próprios: `soco_d12` (estouro da 12),
   `voz_gago` (blip de fala), `zumbido_uv` (hum da lâmpada), `caixa_registradora`,
   `balim_acerto`, `hic` (soluço do bêbado) e `glup` (o gole em si, com refluxo grave).

2. **(FEITO na v1.2.7)** Partículas custom — nota de R$ na venda, folha da planta madura,
   fumaça verde-erva do baseado contínua enquanto puxa.

3. **Mercado por bioma (fase 2)** — a roupa do Gago já varia (7 fantasias); o PRÉDIO não.
   Variações no gen_mercado.py (arenito no deserto, spruce na serra, telhado de cobre no
   brejo) fariam a loja nascer "do lugar".

4. **(FEITO na v1.2.8)** Texturas das bebidas — as 16 refeitas no pipeline procedural
   (rampa de volume, brilho de vidro, silhueta própria por produto).

5. **(FEITO na v1.2.7)** Game tests das conquistas — fidelidade completa (raiz, tiers,
   descontos) e colheita perfeita validados por award programático.

6. **(FEITO na v1.2.9 + v1.2.10 + v1.2.13)** Embriaguez completa — dose por item, fala
   "fonar" embaralhada no chat (todo mundo lê), *hic!* audível e NA fala, persistência no
   mundo (sobrevive a relog/restart), limiares/decay/teto no config, refluxo no teto,
   cambaleio mecânico (náusea + fraqueza gradual + EMPURRÃO), glup sonoro, action bar
   GRÁFICA com rótulos traduzidos, o Gago zoeiro debochando do freguês bêbado no balcão,
   RESSACA com cabelo do cachorro e MARKUP pro freguês bêbado no preço do balcão.

## O que NÃO precisa mais fazer (já está pronto)
- v1.2.17: Lâmpada UV com estado ligada/desligada por redstone (disjuntor: vizinho
  pode cortar a energia da fazenda indoor), raio no config (`uvRaio`), lâmpada como
  investimento (R$ 36, estoque 2/dia) e 25 game tests
- Estrutura do mercado: gerada por template NBT próprio (nada de Structure Block manual)
- Loot dos NPCs: Gago (cachaça, cervejas, R$, cartuchos) e Traficante (estoque)
- Reparo da escopeta: componente REPAIRABLE com cartucho (bigorna)
- Validador de worldgen no build: JSON/NBT quebrado NÃO sobe pro jar
- Config: config/intoxicantes.json (escopeta, cotação da rua, UV, embriaguez)
- v1.2.14: medidor de embriaguez SÓ de álcool (drogas têm efeitos próprios), todas
  as mensagens i18n (nada de pt cravado no código) e informativos no CHAT (action bar
  só pra sabor repetitivo como o *HIC!*)
- Conquistas: aba "O Esquinão" com 6 progressões
- 24 game tests de economia/estoque/escopeta/progressões/fumaça/embriaguez/withers (teto
  de saldo e config da 12 na v1.2.4; fidelidade e colheita perfeita na v1.2.7;
  escalonamento da fumaça no balcão na v1.2.8; dose/fonar/decay na v1.2.9;
  cap/refluxo/persistência/zoeira na v1.2.10; i-frames do 26.3 no chumbo na v1.2.11;
  gole interrompido/cafeína/cap de boss na v1.2.12; ressaca/cabelo do cachorro e
  markup/cambaleio na v1.2.13; medidor de álcool na v1.2.14; balance UV na v1.2.15)
