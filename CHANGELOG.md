# CHANGELOG - Intoxicantes Mod

---

## v1.2.17 — O DISJUNTOR DA LÂMPADA UV (20/09/2026)

Plantação indoor agora tem RISCO: alguém pode cortar a energia da sua lâmpada.

### NOVO: estado ligada/desligada controlado por redstone
- A Lâmpada UV ganhou um estado `lit` real (blockstate com textura própria:
  vidro violeta brilhante x apagado escuro)
- A regra é previsível e salva os mundos antigos:
  - **Energia chegando** (redstone ativo adjacente) = LIGADA
  - **Sem energia, com controle adjacente** (alavanca/repetidor/etc.) = DESLIGADA
    — alguém abriu o disjuntor
  - **Sem circuito nenhum** = LIGADA (plugada na tomada; lâmpadas antigas
    continuam funcionando)
- O zumbido toca só quando está ligada; quebrar a lâmpada desligada dá o som de
  desligar
- `isUvLit` e `temLampadaUvPerto` respeitam o estado: lâmpada desligada NÃO
  matura plantação (o boost ×2 de crescimento também para)

### BALANCE: a lâmpada vira investimento
- **Raio no config** (`uvRaio`, padrão 2 — era hardcode; dono de servidor
  calibra farm industrial subindo pra 3+)
- **Custo na loja: R$ 24 → 36** e **estoque 4 → 2/dia** — com a maturação
  confiável ano-redondo, o preço precisava acompanhar o poder
- O custo operacional é o próprio design do mod: **vigilância** — com circuito
  de redstone, qualquer vizinho pode abrir o disjuntor; sem circuito, a lâmpada
  está plugada direto na tomada

### VERIFICAÇÃO
- Game test novo (25 total): disjuntor nos 3 casos (tomada/energia/corte),
  maturação pausada com lâmpada apagada e religada, alavanca ligada energizando

---

## v1.2.16 — AS FAZES DA PLANTA CONTAM A HISTÓRIA CERTA (20/09/2026)

Segunda rodada do playtest (Jumbalaio + Poll, com o mod Jade na mão): as fases
de crescimento estavam difíceis de ler.
### VISUAL: "a 75% é idêntica à madura" / "só tem 2 estágios?"
- Medição por pixels confirmou os reports: stage2 vs stage3 tinham diferença média
  **55/765** (praticamente a mesma imagem) e a fase "crescida esperando UV"
  compartilhava textura com a quase-madura
- Nova leitura visual em **6 fases inequívocas**:
  1. broto → 2. meia verde → 3. alta verde → 4. **cor surgindo** (esmaecida, age 3)
  → 5. **cor cheia** sem brilho (madura, esperando UV) → 6. **dourada** (colhe!)
- A cor do fruto agora é sinal de MATURAÇÃO, não de crescimento; o brilho xadrez
  só aparece na fase madura; o corte de altura dos estágios 2/3 foi aberto
- Diferença média entre fases vizinhas: 55 → **182-390**; novo modelo/textura
  `*_plant_dormant` pra fase esperando-UV (blockstates e modelos regenerados)
### CORRIGIDO: "às vezes é quase instantâneo" (variação de crescimento)
- Era o boost ×2 no SOL forte (corrigido na v1.2.15 — o tester ainda não tinha
  pegado o jar); planta a céu aberto agora anda no ritmo vanilla
### CORRIGIDO: deriva de loot (regeneração expôs)
- O gerador escrevia `set_count 3` (+1 da pool base = **4x**), mas as tables em
  disco estavam afinadas à mão pra **3x total** — o game test guardava a spec
- Gerador = disk = teste: esperar a maturação vale **3x uniforme** pra todas as
  culturas (a semente sempre volta)
### NOTA
- "Acho que a papoula cresce mais rápido": ilusão das fases idênticas — o código
  das 5 culturas é idêntico (mesma `velocidadeCrescimento`); com as fases agora
  legíveis, a percepção de salto some
### TESTADO (24 game tests)
- Teste de colheita atualizado pra spec uniforme de 3x (gerador alinhado)
---

## v1.2.15 — BALANCE DA FAZENDA: O SOL NÃO É LÂMPADA UV (20/09/2026)

Segunda rodada de reports do playtest — um balance real e um esclarecimento de design.
### BALANCEADO: "as plantas crescem bem rápido"
- O `isUvLit` do mod considerava SOL FORTE (luz ≥ 12 + céu aberto) como "UV" — e o
  boost ×2 de crescimento valia pra ele: **toda fazenda ao ar livre andava 2× o ritmo
  vanilla, o dia todo**, lâmpada ou não
- Agora o **boost ×2 é EXCLUSIVO da Lâmpada UV** (a tecnologia do mod tem que valer o
  investimento); a fazenda a céu aberto anda no ritmo vanilla
- A **maturação** (UV_AGE 0→3) continua valendo sob sol forte — o early game sem
  lâmpada segue funcionando; o sol agora é caminho lento, a lâmpada é o caminho rápido
- Lógica extraída pra `UvCropBlock.velocidadeCrescimento` — conta exata, coberta por
  game test (0.15 solo a céu aberto / 0.30 com lâmpada)
### ESCLARECIMENTO: "maximizou 1 Gago, o resto fica com preço bom"
- É DESIGN, não bug: o Cartão Fidelidade é **do FREGUÊS** (por UUID do jogador), não
  da loja — você é conhecido em **qualquer esquina** onde abrir um Gago. A loja é dele,
  o cartão é seu kkkk
### TESTADO: +1 game test (24 no total)
- `uvLampBoostsGrowthButSunlightDoesNot`: conta exata das velocidades (solo a céu
  aberto = base; lâmpada = ×2) e a maturação sob sol preservada
---

## v1.2.14 — CORREÇÕES DO BETA TESTER: MEDIDOR SÓ DE ÁLCOOL, SEM PT/EN MISTURADO, MENSAGENS LEGÍVEIS (20/09/2026)

Três reports da rodada de testes — todos reais, todos corrigidos.
### CORRIGIDO: ficar "bêbado com maconha" ("não faz sentido")
- O medidor de embriaguez era ÚNICO: maconha, baseado, ópio, cocaína, LSD etc.
  alimentavam o mesmo medidor da cachaça
- Agora **SÓ ÁLCOOL emborracha** (cerveja, vinho, hidromel, cachaça, rum); erva, pó e
  comprimido têm os efeitos PRÓPRIOS dos itens — o medidor é de bebedeira, ponto
- O Traficante continua vendendo tudo; o Gago continua debochando de quem cruza o
  limiar (que agora só é cruzado por bebida)
### CORRIGIDO: português e inglês misturados ("you are caído")
- Raiz do problema: ~25 mensagens estavam ESCRITAS no código (Component.literal pt)
  e nunca passaram pelo lang — cliente em inglês lia português cravado
- Tudo migrado pra chaves i18n com tradução real nos dois idiomas: saldo/pagar/darreal/
  depositar/sacar, gagomarket (set/rebuild/pos), clock do mercado, anúncio de
  descoberta, expulsão do Gago, encerramento de atendimento…
- `money.intoxicantes.*`, `market.intoxicantes.*` e `entity.gago.expulso` novos nos
  dois langs
### MELHORADO: mensagens rápidas demais pra ler ("aparece no chat")
- Action bar dura ~2s — status de embriaguez, ressaca (entrada/fim), BLOP, cura e
  cabelo do cachorro AGORA VÃO PRO CHAT (dá pra ler, rolar e reler)
- Exceção proposital: o `*HIC!*` continua na action bar — é sabor que repete a cada
  ~10s; no chat viraria spam (o som e a nota já comunicam)
### TESTADO (23 game tests)
- Teste do gole atualizado: cerveja completa = dose 2, **baseado completo = 0 no
  medidor** (prova direta do report)
---

## v1.2.13 — O REFINAMENTO SUPIMPA: RESSACA, CAMBALEIO E A VANTAGEM DO GAGO (20/09/2026)

Polimento da embriaguez em quatro frentes — o ciclo completo da bebedeira agora tem
consequência, física, preço e cara própria.
### NOVO: RESSACA de verdade (o dia seguinte da bebedeira)
- Voltar a **nível zero vindo de estado bêbado** (≥ limiar da fala) deixa o freguês
  **DE RESSACA** por `embriaguezRessacaSegundos` (novo config, padrão 90s): náusea +
  lentidão + fraqueza constantes, com aviso de entrada e saída
- Zerar vindo de "alegre" não dá ressaca — só quem cruzou o limiar paga a conta
- **CABELO DO CACHORRO**: qualquer dose durante a ressaca cura ela na hora (e conta
  como dose normal, claro) — o clássico, agora mecânica kkkk
- Ressaca **persiste** no `intoxicantes_embriaguez.json` (relog não cura)
- Config em `0` desliga a feature
### NOVO: o Gago aproveita do bêbado (markup)
- Freguês **visivelmente bêbado** paga **+10% por nível** acima do limiar da fala
  (`embriaguezMarkup`, novo config), com **teto absoluto de 30%**
- Implementado em `FidelidadeData.precoComDesconto` — o **ponto único de preço**: a
  tela e a cobrança usam o mesmo método, o markup aparece pro freguês ANTES de pagar
- Limpo e "alegre" pagam normal; o desconto do fidelidade é aplicado primeiro
### NOVO: cambaleio mecânico
- No nível do soluço, **empurrão horizontal aleatório** (25%/s, config
  `embriaguezCambaleio`) — sem componente vertical: bêbado cambaleia, não voa
### MELHORADO: action bar gráfica + tradução de verdade
- A mensagem de estado virou **barra**: `▮▮▮▮▯▯▯ 4/7 — bêbado`, com cor por faixa
  (verde alegre, âmbar bêbado, vermelho caído) e rótulo "de ressaca" quando na conta
- Os rótulos eram texto pt_br HARDCODED (o en_us exibia português!) — agora são chaves
  `effect.intoxicantes.rotulo.*`: sober/tipsy/drunk/wasted/hungover
### TESTADO: +2 game tests (23 no total)
- `hangoverHitsAfterTheLastLevelAndDogHairCuresIt`: ressaca só depois do limiar,
  efeitos do combo, cabelo do cachorro, config desligada
- `drunkCustomerPaysMarkupAndStumbles`: matemática exata do markup (limpo, alegre,
  limiar, teto absoluto de 30%) e o empurrão do cambaleio no delta
---

## v1.2.12 — RELATÓRIO DO BETA TESTER: CURA, DOSE HONESTA E O GAGO NÃO É SERRILHA (20/09/2026)

Três achados do playtest (valeu, Jumbalaio!): um bug real, um buraco de design e um
desequilíbrio — todos corrigidos e cobertos por game tests.

### CORRIGIDO: dose contava com CLIQUE sem terminar o gole ("fica bêbado sem beber")
- O detector antigo contava a dose quando o uso ACABAVA — mas soltar o botão, trocar de
  item ou ter o baseado apagado pelo Gago também "acabam" com o uso
- Agora a dose (e a cura) só conta se o gole **COMPLETOU**: os ticks restantes de uso
  (`getUseItemRemainingTicks`) têm que chegar ao fim; interrupção no meio não conta nada

### NOVO: a cura da bebedeira ("tem a cura da cachaçada?")
- O **Extrato de Cafeína** agora SOBRIAt: derruba **2 níveis de embriaguez** na hora
  (com piso zero e aviso na tela: "sentiu a cabeça clarear...")
- Café não é mágico: sem reset total de uma vez — o fregues continua bêbado o
  suficiente pra lembrar da vergonha; pt_br e en_us

### BALANCEADO: o Gago não é mais serrilha de boss ("o wither não durou 5 segundos")
- Com o fix dos i-frames da v1.2.11 + tiro por segundo + munição infinita, o volley do
  Gago derretia um wither (300 de vida) em ~15s de automação
- Contra alvo de **vida máxima > 100** (withers, dragões, wardens...), o volley dele é
  **capado** em `escopetaCapDanoBoss` (novo config, padrão 6.0 — ~50s pra derrubar um
  wither: é comédia, não boss-killer); freguês comum continua levando o volley inteiro
- A 12 do **player** não sofre cap nenhum (quem paga cartucho tem dano pleno)

### NOTA
- Falar "gago" no chat enfurecer os NPCs da redondeza é a feature do mod desde a v1.1.0
  — o tester achou o gatilho, não um bug kkkk

### TESTADO: +2 game tests (21 no total)
- `interruptedSipDoesNotDoseAndCaffeineCures`: gole interrompido não conta nada, gole
  completo conta, cafeína cura 2 níveis, cura com piso zero
- `gagoVolleyIsCappedAgainstBossesButNotCustomers`: volley no wither capado, volley no
  freguês pleno

---

## v1.2.11 — O CASO DO WITHER: I-FRAMES DO 26.3 (20/09/2026)

### CORRIGIDO: a escopeta (do Gago E do jogador) não feria o Wither direito
- **Causa raiz decifrada no bytecode + provada empiricamente com game tests**: o 26.3
  MIGROU os i-frames de dano — o cooldown vivo agora é o campo público
  `damageCooldownTime`, e com ele ativo só entra dano **MAIOR que o último hit**
  (`lastHurt`). O `setInvulnerableTime(0)` que o mod chamava é OUTRO campo (legado)
  e nunca limpou o cooldown real
- Efeito: os 8 balins do mesmo tamanho viravam **1 hit** (o 2º em diante quicava), e o
  volley seguinte do Gago (dano igual a cada 1s) quicava inteiro — contra um chefe de
  300 de vida parecia "o gago não consegue dar hit no wither"
- **Fix**: `Chumbo.aplicar` (novo helper compartilhado pelos DOIS atiradores) zera o
  `damageCooldownTime` real a cada aplicação — o tiro inteiro entra e todo volley é
  limpo; de quebra, unifica a blindagem anti-Invulnerable (save antigo com escudo)
  num só lugar
- Nota de investigação: a primeira hipótese ("escudo anti-flecha do wither aceita só
  direct entity AbstractArrow/WindCharge" — lido do bytecode do `WitherBoss.hurtServer`)
  estava INVERTIDA; o diagnóstico empírico com matriz de dano desmentiu (o blindado
  recusa flecha e aceita melee fresco) e levou à causa real: o artefato de cooldown
  contaminava TODOS os resultados. Sem game test empírico, o fix teria sido o oposto

### Notas de API 26.3 (registros descobertos nessa rodada)
- Tipos vanilla saíram de `EntityType.WITHER` — o teste busca no registry (`lookupOrThrow`)
- Pacotes reorganizados: `WitherBoss`, `monster/zombie/Zombie`

### TESTADO: +2 game tests (19 no total)
- `gagoBuckshotLandsOnTheWitherThroughItsShield`: reprodução direta do report — dois
  volleys completos de 8 balins iguais entram num wither blindado (vida ≤ 50%,
  `isPowered`), com o segundo espelhando o primeiro
- `shotgunIFrameResetUsesThe263CooldownField`: prova da mecânica — hit inicia o
  cooldown do 26.3, segundo hit igual entra com o reset correto, e o reset legado
  NÃO limpa o campo (a prova do porquê do fix)

---

## v1.2.10 — EMBRIAGUEZ SUPIMPA: PERSISTÊNCIA, CONFIG, REFLUXO E O GAGO ZOEIRO (20/09/2026)

### NOVO: o sistema de embriaguez virou sistema de verdade
- **Persistência**: embriaguez ATRAVESSA relog/restart (`intoxicantes_embriaguez.json` no
  diretório do mundo, padrão PlayerMoney/Fidelidade) — o fregues bêbado continua bêbado
  quando volta; o logout só esquece o consumível em uso
- **Config calibrável**: `embriaguezDecaySegundos`, `embriaguezLimiarFonar`,
  `embriaguezLimiarHic`, `embriaguezCap` e `embriaguezTrancaTeto` no
  config/intoxicantes.json (com saneamento e coerência automática: a fala embaralha
  sempre antes do soluço, limiares nunca passam do teto)
- **Teto com tranca = REFLUXO**: dose no cap com a tranca ligada e o corpo DEVOLVE o gole
  (`*BLOP!*` grave + fumaça + aviso na tela) — nada de emborrachar além do cap
- **Cambaleio de carne e osso**: bêbado de verdade ganha náusea constante renovada e
  **fraqueza gradual** (braços de geleia que escalam com o nível) — efeitos ambient,
  sem chuva de partículas piscando no HUD
- **Gole com som próprio**: `glup.ogg` (0,22s, engolida descendo) ao terminar qualquer
  dose; pitch varia por gole e o refluxo usa o grave
- **Action bar com estado**: "Você está bêbado (nível 4/7)" — reenviada só quando o
  nível muda, sem spam
- ***hic!* NA fala**: no nível do soluço o fonar sai com `*hic!*` intercalado entre as
  palavras (determinístico, igual ao resto)

### NOVO: o Gago zoeiro da borracharia
- O fregues cruzou o limiar da fala bêbada perto do balcão (raio 8)? **Deboche com voz
  + fala gaguejada**: "E-ei! B-balança no pé!", "O b-balde de gelo é GRÁTIS, tá?!"
- Gatilho AUTOMÁTICO: o gole que cruza o limiar puxa a zoeira na hora — sem fila de
  espera pro próximo scan
- 30s de throttle entre deboches; freguês sóbrio não leva deboche nem de graça; raiva
  total resolve na bala, sem deboche
- Falas em pt_br e en_us

### TESTADO: +2 game tests novos + 1 atualizado (17 no total)
- `drunkennessDosesScrambleChatAndDecayOverTime` atualizado: passadas de 1s alinhadas
  com o tick (o bug que o teste antigo mascarava), `*hic!*` na fala do caído
- `drunkennessCapRefluxAndPersistenceRoundTrip`: cap vem do config (não 7 fixo),
  refluxo no teto, round-trip completo save/load da persistência e decay do config
- `gagoRidesADrunkCustomerAtTheCounter`: deboche automático ao cruzar o limiar, nada
  abaixo dele, throttle bloqueia spam, sóbrio não leva deboche de gatilho direto

---

## v1.2.9 — EMBRIAGUEZ: HIC + FALA EMBARALHADA NO CHAT (20/09/2026)

### NOVO: sistema de embriaguez com 4 estados
- **Dose por item**: cada bebida/droga consumida sobe o nível — cerveja/vinho/hidromel 2,
  cachaça/rum/LSD 3, ervas leves 1 (extrato de cafeína não emborracha, óbvio)
- **Decaimento**: 1 nível a cada 60s de vida limpa; dose nova zera o relógio
- **Estados**: 0 limpo · 1-2 alegre · 3-4 bêbado (**fala "fonar" embaralhada**) ·
  5-6 caído (fala pior + **HIC audível** a cada 8-14s com som próprio, pitch subindo
  com o nível, nota musical e `*HIC!*` na action bar)
- **Fala embaralhada via decorator de chat server-side** (`ServerMessageDecoratorEvent`,
  CONTENT_PHASE): o servidor reescreve a mensagem ANTES de distribuir — **todo mundo lê
  o fonar**, e troca de letras vizinhas + alongamento de vogais escala com o nível;
  determinístico (mesma mensagem = mesmo fonar, testável)
- Náusea curta do vanilla nos estados 3+ (a tela gira, como manda o figurino)
- Logout limpa o estado; sintetizado novo `hic.ogg` (pipa dupla agudo+grave)
- +1 game test: dose, limiar, fonar escalonado, decaimento exato em 60 passadas

---

## v1.2.8 — ARTE NOVA DA PRIMEIRA À ÚLTIMA GOTA + O GAGO VS. BASEADO (20/09/2026)

### NOVO: as 16 texturas de bebidas/consumíveis refeitas (item 4 do TODO)
- Mesmo pipeline procedural do resto do mod (`gen_textures.py` v2): pixel maps novos com
  **volume por rampa vertical** (líquido escurecendo pra baixo), **brilho de vidro** na
  parede dos recipientes e **contorno escuro automático**
- Silhueta própria por produto: caneca com espuma em 3 tons e bolhas (cerveja), taça de
  vidro translúcido (vinho), garrafa alta com rótulo amarelo/verde (cachaça), frasco
  bojudo com lacre de cera (hidromel), garrafa quadrada preta com faixa ouro (rum),
  baggie com zip e folhinha estampada (maconha-seda), **cone na diagonal com brasa
  acesa e fumaça** (baseado), espelho negro com linhas (cocaína), seringa com
  graduações (heroína), selo cartão com mandala (LSD), poeira dourada cintilando (pó
  estelar), cogumelo clássico (xamânico), vidrinho com névoa lilás (deserto), raiz
  retorcida em S (sombra), drusa facetada rosa (cristal) e ampola de farmácia (cafeína)
- Ícone do mod (caneca) regenerado da arte nova

### NOVO: o Gago DETESTA baseado no balcão
- Alguém puxando um baseado num raio de 4 blocos do balcão? O dono da esquina reclama:
  **voz própria + fala gaguejada** ("A-AQUI É BEBIDA fria, não erva!")
- Escalonado com memória enquanto a fumaça durar: 1ª vez reclama, 2ª vez reclama de
  novo ("o cheiro i-impregna no b-balcão!"), **na 3ª APAGA NA MÃO** — interrompe o puxão,
  trava o baseado por 10s e solta *"A paciência acabou! \*apaga com a mão\*"*
- 6s entre falas (throttle), contador zera quando a fumaça passa; raiva total (o "gago"
  no chat) resolve na bala, sem conversa
- Falas em pt_br e en_us; +1 game test cobrindo o escalonamento completo

---

## v1.2.7 — Partículas próprias, sons de transação e punchline nos balins (20/09/2026)

### NOVO: partículas do mod (item 2 do TODO)
- **dinheiro**: a nota de R$ voando quando a venda sai — sobe serpenteando como fumaça
  de fogueira, com a cara da própria cédula (sprite 16×16 gerado por código); a esquina
  que vende é a esquina que se vê
- **folha_maconha**: folhinha de sete pontas caindo da planta no ponto (age 4 + uv 3) —
  sinal visual de que vale colher; pousa, para de girar e se desfaz
- **fumaca_erva**: o bafo verde-erva do Baseado — tinta verde no client (setColor), sobe
  devagar expandindo; enquanto o fregues puxa (use duration), o Boca emite contínuo
  (server tick, custo zero quando ninguém fuma)
- Pipeline completo: 3 sprites gerados no mesmo pipeline das texturas do mod
  (`gen_particulas_sons.py`), atlas `particles/*.json`, `FabricParticleTypes.simple`,
  providers no client com `SingleQuadParticle` (26.3)

### NOVO: sons de comércio e combate
- **caixa_registradora**: "ca-ching" de duas campainhas metálicas quando a compra/venda
  sai no cardápio — transação tem festa agora
- **balim_acerto**: estalo seco + corpo grave quando o balim da 12 acerta — dano
  mecânico, comédia auditiva: quem toma 12 fica com o punchline (+ nota musical subindo
  da vítima)

### TESTADO: +2 game tests das progressões (12 no total)
- `loyaltyCardTiersAndAdvancementsProgressCorrectly`: cartão fidelidade completo —
  4 compras não dão nada, a 5ª sobe pra tier 1 (5%) e concede raiz + Freguês da
  Esquina, a 30ª é Dono da Esquina (15%), desconto calculado e piso de R$ 1
- `perfectHarvestAdvancementOnlyAtFullRipeness`: colher imatura não concede; no ponto
  (age 4 + uv 3) concede Colheita Perfeita

---

## v1.2.6 — Ingredientes das bebidas e correções do cardápio

- Gago compra lúpulo, uva, trigo, cana do mod, frascos de mel e garrafas vazias, conforme suas receitas.
- Café, folhas e ópio deixam de ser comprados; cotas antigas de lúpulo e uva são preservadas.
- Lotes e pagamentos explícitos, dicas de uso e motivos de indisponibilidade ao passar o cursor.
- Fidelidade e detalhes dos itens sem sobreposição; arrasto da barra e tecla de inventário corrigidos.
- Posição de rolagem preservada por aba; respostas de compra não reabrem um menu já fechado.

---

## v1.2.5 — Comércio e colheitas integrados (20/09/2026)

- Gago compra cinco colheitas em lotes de oito, com pagamento em saldo virtual.
- Aba Vender colheita integrada ao cardápio e à fidelidade existentes.
- Estoque diário por NPC, persistido no save, com reposição às 07h e adiamento durante atendimento.
- Gago vende sementes e lâmpadas UV; exclusivos de fidelidade também possuem cota.
- Traficante oferece três produtos diferentes por dia, quatro unidades de cada, por preços menores nos itens em comum.
- Catálogo e preços centralizados; servidor valida sessão, distância, saldo, quantidade e estoque.
- Correção das tabelas de colheita para 26.3 e continuação da maturação UV depois do crescimento.
- Testes de servidor e de cliente real em source set separado.
- Cliques do cardápio compatíveis com os códigos SDL do Minecraft 26.3.
- Preservadas as melhorias paralelas até 1.2.4, incluindo cotação diária da rua.

---

## v1.2.4 — SONS PRÓPRIOS + DESCOBERTA COM CURSOR + TESTES DE ECONOMIA/CONFIG (19/09/2026)

### NOVO: sons próprios do mod (item 1 do TODO)
- **soco_d12**: estouro proprietário da 12 (.ogg sintetizado) — substitui os fireworks no
  item do jogador E no tiro do Gago; a arma agora tem identidade sonora própria
- **voz_gago**: blip de fala (sine + vibrato) no lugar do `VILLAGER_AMBIENT` — o Gago
  finalmente tem voz própria, e não a do vilarejo vanilla
- **zumbido_uv**: hum elétrico grave ambiente da Lâmpada UV (~1 a cada 50s, animateTick) —
  quem ouve o zumbido sabe que tem plantação indoor na esquina kkkk
- Pipeline completo: 3 .ogg sintetizados com ffmpeg + `sounds.json` + registro Java;
  nova classe `LampadaUvBlock` (o bloco precisava de `animateTick` próprio)

### OTIMIZADO: descoberta da estrutura não revista mais chunks
- Era: cada tentativa (a cada 30s) revarrava a espiral INTEIRA desde o centro (~2.400
  checagens de estrutura por passada, mesmo já descartadas)
- Agora: cursor progressivo (anel + índice) com orçamento de 600 chunks NOVOS por
  tentativa — nunca re-checa; rodada completa sem achar = backoff de 10 min (log 1x)
- Centro da espiral fixado por sessão (spawn mudando no meio não escorrega o cursor)
- Bônus: cotação da rua cacheada por dia comercial (o refresh de 1s dos NPCs não roda
  mais o `Random` 7x por NPC a cada segundo — pauta fixa do dia, mesmo resultado)

### TESTADO: +2 game tests (10 no total)
- `balanceSaturatesAtCeilingAndNeverGoesNegative`: o teto de R$ 2.147.483.647 (fix do
  v1.2.3) — overflow satura, `/pagar` sem fundo não mexe no pagador, ajuste negativo
  funciona sob saturação
- `gagoShotgunFollowsConfigNotHardcodedValues`: a 12 do Gago obedece o config —
  cooldown exato do config no tiro do NPC, dano calibrado aplica, acalmar guarda a arma

---

## v1.2.3 — AUDITORIA: OVERFLOW DE SALDO + A 12 DO GAGO OBEDECE O CONFIG (19/09/2026)

### CORRIGIDO: saldo podia virar negativo por overflow de int
- `PlayerMoney.add` somava em `int`: `/pagar 2000000000` pro cara com saldo alto estourava
  e virava saldo **negativo**, que o `set` zerava — "meu saldo sumiu sozinho kkkk"
- Agora soma em `long` e satura em `Integer.MAX_VALUE` (2.147.483.647 R$ — fundo suficiente)
- Todos os caminhos (`/pagar`, `/darreal`, venda de colheita) herdam a proteção

### CORRIGIDO: a escopeta do Gago ignorava o config/intoxicantes.json
- Dano por balim, nº de balins, cooldown e alcance eram hardcoded (3.0/8/60/10) no NPC —
  o admin baixava o dano no config e o Gago continuava metralhando com os valores antigos
- Agora lê do mesmo config da 12 do jogador: quem calibra a arma calibra os DOIS atiradores;
  mudança de comportamento: cooldown do NPC 60 → 20 ticks (igual ao do item, que é o padrão do config)
- `[Escopeta] tiro:` no log passa a refletir balins do config também pro Gago

### AUDITADO (sem mudança)
- Rede/sessão do cardápio (handlers já rodam na main thread), validação de compra,
  watchdog, estoque diário, worldgen JSONs, texts de lang, client screen

---

## v1.2.2 — CONQUISTAS + CONFIG + ÓPIO COM NICHO (19/09/2026)

### NOVO: progressões — aba "O Esquinão" (6 conquistas)
- **O Esquinão** (raiz) → **Salve, Freguês!** (primeira negociação com o Gago ou Traficante, trigger `villager_trade`) → **Freguês da Esquina** (5 compras no cartão) → **Dono da Esquina** (fidelidade máxima + a 12 pra casa, challenge)
- **Colheita Perfeita**: colher uma planta no ponto exato da maturação UV (award programático no `playerWillDestroy` do `UvCropBlock`)
- **Dono da Doze** (challenge): despachar o Traficante — trigger `player_killed_entity` com predicate de entity_type
- Conquistas de fidelidade usam o padrão `impossible` + award programático em `FidelidadeData.registrarCompra` (avança junto com o cartão)

### NOVO: config/intoxicantes.json
- Criado com padrões no primeiro boot (raiz da instância, irmã de `mods/`); editável sem recompilar
- **Escopeta**: cooldown, dano por balim, alcance, nº de balins
- **Cotação da rua**: liga/desliga a flutuação e define o intervalo (mín/máx)
- **UV**: chance de maturação por tick e liga/desliga a cue de som+partículas
- Saneamento de faixa: config escrito errado cai pro padrão em vez de derrubar o mod

### BALANCEADO: ópio com nicho real
- Era 100% desvantagem (enjoo + fraqueza) — ninguém consumiria; agora é o **anestésico de rua**: Resistência 30s + Fraqueza 15s (tanque barato que deixa lento, a lore finally bate com o efeito)

### AUDITORIA (nada a fazer, já existia)
- Loot dos NPCs: Gago (cachaça, cervejas, R$ 5–25, 2–5 cartuchos) e Traficante (estoque + partículas) já implementados
- Reparo da escopeta: componente `REPAIRABLE` com cartucho já configurado (tooltip era honesto)

### Nota técnica
- `conditions.entity` em advancement no 26.3 é **objeto único** (a lista quebra o registro — o game test pegou); padrão confirmado contra o `kill_a_mob` vanilla

---

## v1.2.1 — LAPIDAÇÃO: ECONOMIA, MATURAÇÃO, ESCOPETA E ACABAMENTO DO MERCADO (19/09/2026)

Rodada de profundidade no que já existe, na ordem sugerida — nada de item novo, tudo refinado:

### NOVO: cotação da rua flutua por dia (escolha real entre vendedores)
- Os preços do **Traficante oscilam −30%..+40% por dia comercial** (seed = dia + produto; a mesma oferta custa igual o dia inteiro, muda às 07h)
- A rua **nunca fica mais cara que o Esquinão** (teto = preço do Gago −1): a escolha é "rua barata e escassa (4 unid.)" vs "Gago caro e abastecido" — esperar a cotação baixar virou estratégia
- Game test atualizado pra assinatura nova (`traficante(random, dia)`) e roda 100 seeds confirmando rotação completa + invariantes de preço

### NOVO: maturação UV que vale a espera
- O **momento** de amadurecer é audível e visível: som de composto pronto + burst de partículas de nota (cifrão no ar = dinheiro maduro)
- Planta madura solta **partículas discretas** em loop (client-side, custo zero) — dá pra achar a planta pronta de longe
- Textura madura com **brilho dourado** a mais (semiótica de "colhe agora")
- Loot de UV_AGE=3 reescrito: **3× produto + semente garantida** — esperar deixa claro que compensa

### NOVO: personalidade nos consumíveis
- **Lore para todos os itens** (tooltip com a "cara" de cada um: "Turbinha de mineração: cava em velocidade absurda, paga com cansaço") — cada item agora tem nicho e situação pra usar

### NOVO: advertência do Gago antes da bronca
- Clique acidental no Gago não vira mais caçada: ele dá **1 advertência de 30s** ("E-ei! Respeito, na esquina! Próxima, SACO A DOZE!")
- Só a **segunda** ofensa no prazo saca a 12 — a provocação de propósito continua exagerada, do jeito que tem que ser

### MELHORADO: sensação da escopeta
- **Som em 3 camadas** (estouro de cano + bass sub + recarga do ferrolho no cooldown): tiro encorpado
- **Indicador de munição na action bar** a cada tiro (`§7Cartuchos: §fN`) e **click seco + aviso** quando está vazia
- Cooldown de 1s visível no vanilla já existente + recuo (kick) mantidos; falloff de dano por distância da rodada anterior

### MELHORADO: texturas da fazenda (v2, redesenhadas)
- **Sementes**: de 3 pontinhos pra **pacote de sementes** (sachê de papel kraft com costura, broto estampado e etiqueta na cor da cultura) — legível no inventário como o vanilla
- **Plantações**: de rabiscos pra **plantas construídas por código** com identidade por cultura — maconha com buda no topo e folhas caídas, lúpulo com cones pendurados, uva com cacho em losango sob copa, café como arbusto com cerejas em pares, papoula com flor de cápsula; crescimento revela a planta de baixo pra cima (broto → média → alta → madura)
- **Lâmpada UV**: de quadrado roxo chapado pra **luminária industrial** — item com carcaça escura, corrente e dois tubos roxos brilhando; bloco com painel metálico, tubos verticais com highlight de vidro e grade de ventilação
- Aprovada por inspeção visual (preview HTML com zoom pixelado) em 3 rodadas de iteração

### MELHORADO: acabamento do mercado
- **Luz de noite**: fileira de shroomlight embutida no teto interno — a loja fica iluminada depois que escurece
- **Quadro de preços interno** (placa "— CARDÁPIO —" com valores base) na parede do fundo, atrás do balcão — menu de bodega visível de dentro
- Validador worldgen agora **rejeita wall_sign sem suporte** e NBT de placa na paleta (os dois bugs do letreiro nunca mais passam)

---

## v1.2.0 — CARDÁPIO DO ESQUINÃO + CARTÃO FIDELIDADE (19/09/2026)

### NOVO: tela de comércio própria (fim da UI genérica de vilarejo)
- **Clique-direito no Gago abre o CARDÁPIO DO MERCADO ESQUINÃO**: tela customizada com letreiro verde "SUL DISTRIBUIDORA / & MERCADO ESQUINÃO", papel-moeda bege e cartão fidelidade — nada de cara de vilarejo vanilla
- **Abas COMPRAR e VENDER**: compra bebidas/drogas/exclusivos com R$ do saldo; **o Gago COMPRA suas colheitas** (8 unidades por lote) com cota diária — plantar agora tem comprador fixo na esquina
- Desenho 100% vetorial pelo pipeline de GUI do 26.3 (`GuiGraphicsExtractor`), hit-testing manual: o que parece clicável é clicável (botões R$ por linha, scrollbar arrastável, rodapé com saldo e Fechar)
- Tecla **E** fecha o cardápio e abre o inventário sem prender a sessão; Esc e botão Fechar encerram o atendimento limpo

### NOVO: cartão fidelidade do Esquinão
- Cada compra conta no seu cartão (persistido em `intoxicantes_fidelidade.json` no mundo, por UUID)
- **4 níveis**: Freguês · **Freguês da Esquina** (5 compras, −5%) · **Cabaré VIP** (15, −10%) · **Dono da Esquina** (30, −15%) — desconto aplicado direto no preço, com preço antigo rasurado na tela
- **Itens exclusivos por nível**: Cartucho ×8 (R$ 60) no nível 1, Hidromel ×4 (R$ 120) no VIP e **a própria ESCOPETA por R$ 300** pro Dono da Esquina
- Card na tela mostra nível, contador, barra de progresso e quanto falta pro próximo nível; a compra reenvia o cardápio (subiu de nível = exclusivo aparece na hora)

### Detalhes técnicos
- Rede própria por payload (`abrir_cardapio` S2C, `comprar`/`vender_colheita`/`fechar_cardapio` C2S) com **validação 100% server-side**: sessão ativa, Gago vivo a ≤6 blocos, estoque, saldo — cliente desatualizado não compra nada
- **Sessão de atendimento com watchdog** (meio segundo): fregues longe, morto ou em outra dimensão derruba a sessão, fecha a tela no client e solta o Gago pro posto; Gago PUTO encerra atendimento
- Catálogo/estoque/cotas no servidor (`TradeCatalog`, `DailyTradeStock`, `MarketInventory`, `MarketTransactions`); reposição diária às 07h
- Descobertas de API do 26.3 aplicadas: `Gui.setScreen` (o `Minecraft.setScreen` não existe mais), `mouseReleased(MouseButtonEvent)` de 1 argumento, `PayloadTypeRegistry.clientboundPlay()/serverboundPlay()`

---

## v1.1.1 — MERCADO FUNCIONAL DE VERDADE (19/09/2026)

### CORRIGIDO: NPCs ficam parados durante o comércio (rodada 8)
- **Gago e Traficante viram estatuetas enquanto a tela de troca está aberta**: IA congelada (`setNoAi`), navegação parada e velocidade zerada a cada tick de atendimento — o vendedor não passeia mais no meio da compra
- **MarketSystem não teleporta mais o Gago no meio do atendimento**: o gerenciador respeita `isTrading()` — a mudança de plantão (00:00, porta↔balcão) espera a tela fechar; o `stopTrading` devolve ele ao posto sozinho (ou ao passeio, se for Gago de ovo fora do mercado)
- **Gago PUTO não atende mais**: com a 12 na mão, clique-direito falha — faz sentido e evita abrir cardápio durante a perseguição
- Gancho no `stopTrading` (chamado pelo vanilla ao fechar a tela / afastar / morrer): restaura o estado certo — posto do mercado com IA travada, ou IA livre pra passear

### NOVO: Gago com fantasia temática por bioma (rodada 7)
- **7 roupas temáticas** (mesma cara bege do Rei do Bar, muda o tecido + um detalhe desenhado): **Plains** camisa vermelha clássica · **Deserto/Badlands** túnica de couro com cinto (Gago do Sertão) · **Selva** verde-mata com flor no peito · **Savanna** ocre do capim dourado com faixa de palha · **Neve** casaco azul-petróleo com botões (a "sede matriz" do Sul kkkk) · **Taiga/montanha** verde-pinho com faixa de lã · **Swamp** marrom-pantano com salpicos de lama
- A variante é **detectada no 1º tick do servidor** pelo bioma onde ele está (template/ovo/comando/save antigo — todos cobertos) e **gravada no NBT** (`Roupa`): o Gago mantém a roupa dele mesmo te perseguindo pra outro bioma — não troca de fantasia no meio da bronca
- Implementação: dado sincronizado próprio no servidor (`DATA_ROUPA`); no cliente o renderer o traduz pro **VillagerType vanilla** correspondente (os 7 tipos do vanilla = nossas 7 roupas) no campo `villagerData` do render state — o mesmo canal que o vanilla usa pra textura de vilarejo, sem subclassificar o render state (a `CrossedArmsItemLayer` exige o generics exato)
- Gerador `gen_npc_textures.py` refatorado: função `pintar_gago(tecido)` + tabela de 7 variantes com detalhes desenhados por região do torso

### NOVO: vida no mercado — Gago reage ao letreiro e a pedradas (rodada 6)
- **Ler o letreiro** (botão direito em qualquer placa perto do mercado, raio 24): o Gago cumprimenta o fregues — 3 falas novas em pt_br e en_us ("É issso aí! O melhor mercado da esquina, lêlogo lê!"), com partículas felizes e throttle de 8s (não vira spam)
- **Vandalismo** (soco/bomba em bloco num raio de 16 do mercado, jogador NÃO-criativo): o Gago **saca a 12 por 30 segundos** e vai atrás do vândalo — falas novas ("Vandalismo na MINHA esquina?! SACA A DOZE!"). Diferente do "gago" no chat (5 min de raiva total); passou os 30s, guarda a arma e volta ao balcão
- **Fix de oportunidade**: quando a raiva expirava, a 12 ficava na mão do Gago pra sempre (o código só adicionava a arma, nunca removia). Agora ele GUARDA a arma ao acalmar
- Eventos via `UseBlockCallback`/`AttackBlockCallback` (Fabric API), ambos com `PASS` — a edição da placa e o soco funcionam normal por cima

### CORRIGIDO DE VEZ: letreiro + dano da 12 (rodada 5, com prova empírica)
- **Letreiro — causa raiz achada por estatística do jar vanilla**: o NBT do texto da placa estava na **PALETA** do template, mas o `StructureTemplate` do 26.3 **só repassa o nbt da ENTRADA DE BLOCO** (`blocks[*].nbt`) — varredura dos 1511 templates vanilla: **0** casos na paleta, **5597** na entrada (baús de shipwreck etc). A placa sempre nascia SEM TEXTO. NBT movido pra entrada + validador agora REJEITA nbt na paleta
- **Letreiro — 2º bug**: a placa estava no plano da fachada (z9) com **ar atrás** (z8, interior) — wall_sign desanexa sem suporte e virava item caindo. Agora ela fica na FRENTE do alpendre (z10, x7, acima da porta, igual à foto) com a parede branca de suporte atrás; validador checa suporte de toda wall_sign (facing → bloco oposto tem que existir e não ser ar)
- **A 12 não dava dano em NADA**: o `getEntityHitResult` do vanilla varre alvos por **células de chunk no stream** — alvos finos (placas, falanges) escorriam entre células e o raio de 24 blocos não pegava NINGUÉM às vezes. Substituído por detector de **cone amostrado** (12 amostras por balim, teste esfera-AABB, vitima mais próxima leva — o balim "para" no que atravessa). Mesma coisa no tiro do Gago
- **Blindagem anti-invulnerável**: NPCs de saves antigos podiam estar gravados com `Invulnerable=1` — o `hurt` engole o dano SEM LOG nenhum (parece "não dá dano"). Agora: template grava `Invulnerable=0`, boot derruba o escudo de Gago/Traficante no load (`ServerEntityEvents.ENTITY_LOAD`), e o tiro derruba na hora se encontrar escudo (com warn no log)
- **Diagnóstico embutido**: cada tiro loga `[Escopeta] tiro: N balim(s) em M vitima(s)` — se algum dia "não der dano" de novo, o log diz exatamente onde a cadeia quebrou (0 balins = mira/AABB; vítima presente sem dano = escudo/source)
- **Perfuração de vidro generalizada**: trocado `TransparentBlock` (classe) por `isCollisionShapeFullBlock` (propriedade física) — pane/vidro/grade/barras NÃO são full-block e não travam chumbo; concreto cheio trava. Vale pro tiro do player e do Gago
- Mercado agora nasce em **37 biomas** (tag própria), incluindo neve — o bug "só na neve" ficou pra trás (confirmado pelo usuário)

### CORRIGIDO: rodada 4 do playtest (rebuild + biomas)
- **Mercado só nascia na NEVE**: o `structure/mercado_gago.json` apontava pra tag vanilla `#minecraft:has_structure/igloo` (referência copiada do igloo — o igloo só existe em bioma nevado!). Criada tag própria `#intoxicantes:has_structure/mercado_esquinao` com 21 biomas quentes/temperados (plains, savanna, desert, badlands, jungle, forest, beach...)
- **Validador aceita tags de bioma do próprio mod**: falso positivo travou o build anterior — ele só conhecia tags vanilla e rejeitava `#intoxicantes:...`. Agora valida tags do namespace do mod conferindo que o arquivo existe em `tags/worldgen/biome/`
- **`/gagomarket rebuild` não alterava nada**: o jar instalado era anterior às correções (o build quebrado no validador nunca foi instalado) e a versão do comando colocava o template com rotação NONE na âncora errada (7 blocos deslocado). Agora: âncora centro→origem com rotação derivada dos offsets persistidos

### CORRIGIDO: lote do playtest 3
- **Lâmpada UV virava cubo de vidro**: o model do BLOCO usava a textura do ITEM (ícone com cantos transparentes) — dava pra ver o céu através do bloco. Textura full-face dedicada (`textures/block/lampada_uv.png`, painel repetido 3×3) e model apontando pra ela
- **Escopeta deitada na mão**: o modelo era construído com o cano no +X (deitado); o display X=-90 do crossbow precisa de cano no +Y. Modelo remapeado `(x,y,z)→(z,x,y)` no gerador + display verbatim da besta vanilla — agora a arma fica EMPUNHADA (cano apontando pra onde mira, coronha pra baixo)
- **Gago desarmado por padrão**: só SACA a 12 quando ofendem ele; guarda de volta quando a raiva passa (o template NBT também não traz mais arma)
- **Escopeta não feria o Gago**: os balins paravam nos VIDROS da vitrine (clip do vanilla para em TransparentBlock) — agora o chumbo **perfura vidro/pane** (até 4 camadas), no tiro do player E no do Gago (ele atira através da vitrine de dentro)
- **`/gagomarket rebuild`**: estrutura gerada é imutável — chunks antigos NUNCA atualizam. O comando recoloca o template atual (letreiro, moldura, tudo) na posição do mercado do save
- **Porta desalinhada dos vidros**: moldura de concreto cinza (x2/x12) entre as colunas verdes e as vitrines — as panes conectam entre si e não "vazam" pro vão da porta

### CORRIGIDO: escopeta calibre 12 sem dano (rodada 2, playtest)
- **Escopeta do player**: os 8 balins voavam no mesmo tick e o vanilla rejeita dano repetido dentro dos i-frames (`invulnerableTime > 10`) — 7 dos 8 chumbinhos eram engolidos e a rajada virava um tapa de 3.0. Agora o dano é **acumulado por vítima** e aplicado de uma vez (até 24 de perto, decaindo pela dispersão natural)
- **Escopeta do Gago (pior)**: o raycast **excluía o próprio alvo** (`e != alvo`) — o tiro dele nunca podia acertar quem estava na mira, só quem passasse na frente do chumbo. Corrigido; mesmo esquema de dano acumulado

### POLIMENTO DO PLAYTEST (rodada 2)
- **Escopeta aponta pra frente** (1ª e 3ª pessoa): display agora usa a rotação X=-90 da besta/tridente vanilla (cano pro -Z, onde o player mira) em vez da diagonal de espada; ganhou ponto de mira vermelho na boca do cano
- **Gago saca a escopeta de verdade**: os renderers não tinham a `CrossedArmsItemLayer` (a camada que o WanderingTrader usa pra desenhar o item nas mãos cruzadas) — sem ela o heldItem nunca renderizava. Adicionada no Gago e no Traficante
- **Gago e Traficante interagíveis**: botão direito abre o cardápio (padrão `mobInteract` do WanderingTrader decifrado do bytecode). Causa raiz do "não abre": entidades de **template NBT não passam por `finalizeSpawn`** (confirmado no bytecode do StructureTemplate) — o Gago do mercado nascia com ofertas nulas; agora as ofertas são criadas sob demanda no primeiro clique
- **Letreiro do mercado de verdade**: formato de placa corrigido contra o igloo/bottom.nbt — `messages`/`filtered_messages` são TAG_List de 4 compounds `{text, color}` (não strings JSON; o texto JSON apareceria literal na placa). Letreiro "SUL / DISTRIBUIDORA / & MERCADO / ESQUINÃO" em verde escuro, glow ativo, igual à foto
- **Gago de pele bege**: a pele e o robe do villager têm a mesma paleta terrosa — o recolor por matiz pintava a cara junto com a camisa. Agora o recolor é POR REGIÃO da textura (y<16 = pele bege, y≥16 = camisa vermelha)

### CORRIGIDO CRÍTICO: crash ao abrir o inventário criativo
- `IllegalStateException: Accidentally adding the same item stack twice [Opium] to a Creative Mode Tab` — o Ópio entrou **duas vezes** na aba do mod (duplicata de uma correção anterior). Qualquer duplicata crasha a tela de inventário inteira. Removida; lista da aba revisada (sem outras duplicatas)

### NOVO: Mercado idêntico à referência (SUL DISTRIBUIDORA & MERCADO ESQUINÃO)
- Template reconstruído como **prédio de esquina de verdade** (15×5×11, 484 blocos): paredes de concreto verde, faixa superior branca, vitrines pretas com moldura, porta de abeto central, colunas de tronco de abeto, alpendre coberto na frente, piso interno de quartz polido e calçada de smooth stone
- **Placa de parede** na fachada com o nome em 4 linhas: `SUL / DISTRIBUIDORA / & MERCADO / ESQUINÃO` (voltada pra rua, texto NBT embutido na paleta do template)
- **Gago embutido no template** (entidade no NBT, nasce atrás do balcão junto com a estrutura — não depende mais só da descoberta por código)
- **NBT reescrito no formato real do 26.3** (decifrado direto dos templates vanilla): `size` = lista de 3 ints, `blocks` = lista de entradas `{pos, state}` (não o array legacy de índices), paleta com chave `id`. O formato antigo que o gerador escrevia **nunca geraria** no mundo
- Offsets do Gago (balcão **e** porta) giram junto com a rotação do jigsaw e persistem no NBT do save

### NOVO: Validador de worldgen nível 2
- Parser NBT completo (13 tipos) byte a byte, com **auto-teste contra o igloo vanilla** — se o parser estiver quebrado, o validador se auto-rejeita em vez de passar falso-positivo (bug real encontrado: `d[self.st()] = self.val(tt)` avalia o payload ANTES do nome em Python e dessincroniza o stream)
- Valida formato 26.3 do template: size/placa/entidades/paleta/pos, textos da placa (front_text/messages), entidade embutida com id

### NOVO: Validador automático de worldgen (`tools/validate_worldgen.py`)
- Roda em **todo `gradlew build`** (task `validateWorldgen`) e **falha o build** se achar referência órfã — nunca mais mundo travado em "Preparing for world creation"
- Valida contra o jar vanilla do cache do Loom (fonte da verdade, sobe de versão junto):
  - start_pool → template_pool, placed_feature → feature, structure_set → structure, processors
  - feature type / placement type / element_type (coletados dos JSONs vanilla da versão)
  - blocos e blockstates (`id[prop=valor]`) contra os blockstates do jar (variants + multipart)
  - tags de bloco e de bioma
  - NBT da estrutura: parse byte a byte, DataVersion cruzada com `version.json`, paleta, x*y*z, índices
- **Pegou um bug grave no dia 1**: o `blocks.data` do NBT do mercado era placeholder sequencial (o grid real nunca tinha sido encodado!) — corrigido no `gen_mercado.py`, agora codifica o layout ROWS[y][z][x] de verdade
- Teste de regressão: bug `minecraft:none` reinjetado → detectado com exit 1; restaurado → exit 0

### CORRIGIDO CRÍTICO: trava na criação de mundo
- O template_pool do mercado referenciava `"processors": "minecraft:none"` — esse valor **não existe** no 26.3 (a referência órfã derrubava o registry `worldgen/processor_list` inteiro com "Unbound values" e o jogo ficava preso em "Preparing for world creation"). Agora usa processors **inline** (`{"processors": []}`), igual aos pools vanilla, e `element_type: legacy_single_pool_element`

### CORRIGIDO: Assets quebrados (achados nos logs do jogo)
- **Escopeta invisível na mão**: o gerador escrevia UV em pixels da textura (0–32) mas model usa UV NORMALIZADO 0–16 — metade dos elementos fora do range, o jogo recusava o bake (`Cannot compute translucency out of bounds`). Corrigido na fonte (`gen_escopeta.py` divide por 2) e o preview agora lê UV normalizado também
- **R$ era um PNG truncado** (122 bytes, gerador da v1.1.0 morreu no meio): sprite nova de cédula verde (`tools/gen_real.py`)
- **Lâmpada UV rejeitada no block atlas**: o model de bloco usava textura da pasta `item/` (atlas errado no 26.3) — cópia criada em `textures/block/`

### POLIMENTO: o que já existia ficou redondo
- **Rotação do jigsaw respeitada**: a estrutura nasce girada aleatoriamente e a "porta" (onde o Gago atende de madrugada) agora acompanha a rotação real lida do piece — em 3 de 4 rotações ele ficaria de frente pro muro
- **Offset da porta persiste** no NBT (não se perde no reboot)
- **Traficante entrou na economia**: aceitava só esmeralda (ignorava o R$ inteiro) — agora cobra em R$ 10~20 por item, mais barato que o Gago (a rua é a rua), com estoque limitado; e passou a vender o Ópio
- **Gago se apresenta**: a frase de chegada existia mas ninguém chamava — agora ele fala pro freguês mais próximo ao nascer no posto
- **Escopeta fala**: click seco agora avisa na tela ("Click! Sem cartuchos...") e o tooltip ensina o reparo ("Repara com 2 Cartuchos na bigorna")
- **Escopeta reparável**: componente REPAIRABLE com cartuchos (2 cartuchos na bigorna restauram a durabilidade)
- **Versão do jar sincronizada**: `intoxicantes-1.1.1.jar` (era 1.0.0)

### CORRIGIDO: Sistema do Mercado (era o buraco da v1.1.0)
- **marketPos era estático e se perdia ao reiniciar** — agora salva em NBT (`intoxicantes_market.dat` no diretório do mundo) e carrega no boot
- **Auto-descoberta da estrutura**: se o admin nunca setou, o servidor acha a estrutura `mercado_gago` sozinho (espiral de chunks estilo /locate, raio 24 chunks do spawn, 1 tentativa a cada 30s)
- **A estrutura agora EXISTE de verdade**: a v1.1.0 prometia o NBT mas ele nunca foi criado. Template 11×4×9 gerado por código (DataVersion 5023), piso de andesite polido, balcão de slab com prateleira e canto de quartz, vitrine, lanterna, barril e fundação — o jigsaw rotaciona na geração
- Worldgen completo: `template_pool` + `structure` (jigsaw em superfície, terrain_adaptation beard_thin) + `structure_set` (spawn raro estilo igloo: spacing 40 / separation 22)
- **Gago do mercado tinha NoAI eterno** (estátua que nem reagia): agora tem IA de posto fixo — parado no balcão/porta, IA liberada quando fica puto (persegue, atira, volta pro posto quando a raiva passa)
- Gerenciador do Gago agora roda 1× a cada 5s (era 20×/s, spam de teleport)
- **Relógio do HUD errado**: usava `gameTime` (não avança com /time set). Agora usa o clock do overworld (API nova de clocks do 26.3)
- Âncora do Gago no PISO da estrutura (minY), não no meio do telhado

### NOVO: Economia fechada (item ↔ saldo)
- `/depositar` — deposita todo R$ do inventário no saldo virtual
- `/sacar <quantia>` — saca do saldo em R$ de inventário (o que não couber cai no chão)
- `/gagomarket set` agora salva na hora (não perde se o servidor cair)

### CORRIGIDO: Itens
- **ÓPIO era item invisível** (dropava da papoula sem sprite/model): sprite 16×16 de seiva dourada + model + receita (2 papoulas → 1 ópio)
- en_us.json: `itemGroup` ainda em português (regressão da v1.0.1)

### MELHORADO: Escopeta
- Durabilidade pela API vanilla (`hurtAndBreak`): som e lógica de quebra idênticos aos itens do jogo

### ARQUIVOS NOVOS
- `tools/gen_mercado.py` — gera todos os JSONs de worldgen + o template NBT
- `tools/gen_opio.py` — sprite do ópio
- `worldgen/template_pool/mercado_gago/start.json`, `worldgen/structure/mercado_gago.json`, `worldgen/structure_set/mercado_gago.json`
- `structure/mercado_gago.nbt` — **NÃO precisa mais criar em creative!**
- `texture/item/opio.png`, `models/item/opio.json`, `items/opio.json`, `recipe/opio.json`

### ARQUIVOS MODIFICADOS
- `MarketSystem.java` — reescrito: persistência NBT, descoberta de estrutura, throttle, clock correto, API 26.3
- `GagoEntity.java` — `setupPostoMercado()` (IA travada/solta), expulsão via getter
- `MoneyCommands.java` — `/depositar`, `/sacar`, save imediato no `/gagomarket set`
- `IntoxicantesMod.java` — `MarketSystem.load/save` no ciclo de vida, ópio na aba
- `EscopetaItem.java` — `hurtAndBreak`
- `en_us.json` — itemGroup em inglês

### NOTAS
- Mundo NOVO: a estrutura nasce perto do spawn (raio ~24 chunks) e o Gago aparece sozinho no balcão
- Mundo JÁ EXISTENTE: a estrutura só nasce em chunks nunca gerados — nesse caso use `/gagomarket set <x> <y> <z>` (ou ache um chunk inexplorado longe)
- O NBT do template foi validado byte a byte contra a spec de NBT (parser independente)

---

## v1.1.0 — MERCADO ESQUINAO DO GAGO (18/09/2026)

### NOVO: Sistema de Dinheiro R$
- Item `intoxicantes:real` — moeda do mercado, stackavel em 64
- Comandos: `/saldo`, `/pagar <player> <quantia>`, `/darreal <player> <quantia>` (op)
- Recipes: 1 Esmeralda = 1 R$, 9 R$ = 1 Bloco de Esmeralda
- Drops: Zumbis (15%) e Piglins (25%) dropam R$
- Gago dropa 5-20 R$ ao morrer

### NOVO: Sistema 24 Horas
- Gago atende o dia todo, nunca some
- De 07:00~00:00 fica no balcão (área principal)
- De 00:00~07:00 vai pra porta do mercado
- Anuncia quando muda de posição ("Vou fechar o balcão, vou atender na porta!")

### NOVO: Relogio no HUD
- Action bar mostra `14:30 | Mercado Esquinao no balcao` quando perto do mercado
- Atualiza a cada 10 segundos

### NOVO: Gago no Mercado
- Gago fica parado no balcao durante horario
- Cardapio com 12 itens todos em R$ (R$15 a R$100)
- Se chamar de "gago" dentro do mercado, ele te expulsa com teleport + porrada
- Escopeta sempre na mao durante raiva

### NOVO: Worldgen — Mercado Esquinao
- Estrutura gera em planicies e perto de vilas
- Template NBT: `data/intoxicantes/structure/mercado_gago.nbt` (CRIAR EM CREATIVE)
- JSONs de worldgen criados (structure + structure_set)

### ARQUIVOS NOVOS
- `RealItem.java` — classe do item R$
- `PlayerMoney.java` — persistencia JSON (saldo do player)
- `MoneyCommands.java` — registrador de comandos
- `MarketSystem.java` — horario + relogio + spawn do Gago
- `real.png` — textura placeholder do R$
- `items/real.json`, `models/item/real.json` — model do item
- `recipe/real_to_emerald.json` — 9 R$ = 1 bloco de esmeralda
- `recipe/emerald_to_real.json` — 1 bloco = 9 R$
- `recipe/emerald_to_real_single.json` — 1 esmeralda = 1 R$
- `recipe/real_to_emerald_single.json` — 1 R$ = 1 esmeralda
- `worldgen/structure/mercado_gago.json` — definicao da estrutura
- `worldgen/structure_set/mercado_gago.json` — placamento

### ARQUIVOS MODIFICADOS
- `IntoxicantesMod.java` — registrado REAL, comandos, market system, removido spawn aleatorio do Gago
- `GagoEntity.java` — cardapio em R$, falas de mercado, expulsao, drops de R$
- `pt_br.json` — traducoes novas (R$, fechando)
- `en_us.json` — traducoes novas (R$, closing)

### NOTAS
- O Traficante continua spawnando aleatoriamente
- Para criar a estrutura: constroa em creative, exporte com structure block como `mercado_gago.nbt`
- Coloque o arquivo em `data/intoxicantes/structure/mercado_gago.nbt`
- PlayerMoney usa JSON persistente (compativel com todas as versoes)

---

## v1.0.1 — CORRECOES DE BUGS

1. Crash Creative Tab duplicado (semente_maconha)
2. Gago nao despawnava (lag)
3. Traducoes en_us.json em portugues
4. Worldgen plantas inuteis (age=4 -> age=3)
5. Performance isUvLit (raio 3->2, sol primeiro)
6. CANA_DE_ACUCAR sem receita
7. Gago nao equipava escopeta ao ficar puto

---

**Creditos:** OpenCode (analise e implementacao)
**Autor do mod:** enzop
