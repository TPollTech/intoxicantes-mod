# Regras de trabalho — Intoxicantes Mod

Estas instruções se aplicam a todo o projeto. Instruções explícitas do usuário para a tarefa atual têm prioridade. Converse em português brasileiro, com explicações claras e sem excesso de termos técnicos.

## Regras fundamentais do usuário

- **Onde ficam as regras:** quando o usuário falar em "regra", "nova regra" ou pedir para salvar/guardar uma regra ou instrução permanente, registre-a SEMPRE neste `AGENTS.md` — é este arquivo que guia todo o trabalho de criação no mod. Nunca salve regras em outro arquivo, em comentário de código ou apenas na conversa.
- **Correção na raiz, sem bolas de neve (regra do usuário):** NUNCA crie arquivos novos de correção/versão — nada de `v2`, `v21`, `fix`, `hotfix`, `corrige_*`, `refaz_*`, `completa_*`, `adiciona_*`, `finaliza_*` ou qualquer variante disso. Quando algo estiver errado, corrija DIRETO no arquivo fonte: a classe Java responsável ou o gerador `tools/gen_*.py` que é a fonte principal daquele asset. Scripts-patch paralelos acumulam lixo, envelhecem, são esquecidos e fazem a próxima regeneração da fonte desfazer o conserto. Scripts de patch que já cumpriram o papel devem ser absorvidos pela fonte e apagados (conferindo antes se não têm conteúdo único), não deixados acumulando em `tools/`.
- **Textura sempre em alta resolução (regra do usuário):** toda textura nova (item, bloco, entidade) deve nascer em ALTA RESOLUÇÃO e com acabamento caprichado — por padrão **128×128**, pintada pixel a pixel pela fonte (gerador `tools/gen_*.py`), com gradiente/shading, variação de tom, brilho, contorno legível e coerência de paleta com o resto do mod. Exceção: modelos 3D usam seu próprio atlas na resolução que o modelo pedir (a geometria é o detalhe). Nada de 16×16 preguiçoso: se a textura parece "simples demais" em zoom, ela não está pronta.
- **O nome oficial do mod é `SNC Adventures`** (já é o `name` do `fabric.mod.json`). O mod id/namespace `intoxicantes` é técnico/legado e NÃO deve aparecer ao jogador como nome do mod; não renomear IDs existentes.
- **Documentação no Guia (regra do usuário):** toda nova funcionalidade relevante do SNC Adventures deve ser documentada no Guia do SNC Adventures no MESMO trabalho em que for implementada — implementação, receitas, assets, lang, página do guia, changelog. Uma feature não é considerada completa se o guia ficar desatualizado. (Guia em planejamento — ver `PLANO-GUIA.md`; a regra passa a valer a partir da implementação dele.)
- **Arma só está pronta depois de revisar lateral, isométrica e primeira pessoa.** Não considerar uma arma finalizada apenas porque o JSON é válido — o modelo pode estar tecnicamente correto e ainda ficar estranho na mão (foi o que fez a diferença na escopeta 12).
- Mantenha-se informado sobre a documentação atual das linguagens, ferramentas e APIs utilizadas. Confirme a compatibilidade com as versões efetivamente usadas pelo projeto; não atualize dependências automaticamente só porque existe uma versão mais recente.
- Quando achar que terminou, volte ao início do pedido e revise todo o fluxo, inclusive os erros surgidos durante o trabalho. Não declare conclusão com falhas conhecidas escondidas ou testes relevantes pendentes.
- Termine a melhoria priorizada antes de iniciar outras. Preserve o estilo e os comportamentos que já funcionam.

## Fluxo de aprovação para mudanças visuais (regra do usuário, obrigatória)

- Para mudanças com resultado visível no jogo (estruturas, blocos, placas, texturas, telas, HUD), NÃO rode build, geradores ou testes logo após implementar. O erro a evitar é acumular builds errados.
- Fluxo obrigatório: 1) implementar a mudança no código/recursos; 2) montar uma PRÉVIA do resultado (mock HTML mostrado na aba Preview do Freebuff, ou imagem equivalente); 3) PARAR a turn e esperar a aprovação explícita do usuário; 4) somente depois do "ok" rodar geradores de recursos, `gradlew.bat build` e testes.
- Se a prévia for reprovada, ajuste o código e apresente nova prévia. Não siga para build sem aprovação.
- Mudanças puramente lógicas (economia, persistência, correções sem efeito visual) seguem o fluxo normal de testes.

## Identidade visual própria por tela e feature (regra do usuário)

Nesse mod, CADA coisa tem a sua identidade visual própria: tela, feature, sistema. Nenhuma tela nasce copiando a cara de outra; nenhuma feature reaproveita a paleta de outra por conveniência. Aprender a usar o mod também é reconhecer "onde eu estou" pela cara da interface.

### O que a identidade cobre

- Paleta de cores (fundo, moldura, destaque, texto, hover), textura/fundo, estilo de moldura e botões, forma dos títulos e a METÁFORA visual central — o objeto do mundo real que a tela representa (balcão, livro, porta, rádio).
- Vale também para som e partículas: cada sistema com o próprio vocabulário. Não reciclar o zumbido de LED fora dos letreiros nem a fumaça do baseado em outro produto.

### Identidades já consagradas (não contaminar umas às outras)

- **Comércio do Esquinão** (cardápio do Gago, balcão, fidelidade): fachada de loja — verde de letreiro, bege de papel-moeda, dourado de fidelidade, moldura de vitrine.
- **Ponto do Traficante**: rua — asfalto escuro, madeira crua, neon de porta de bar; nada de "loja de vilarejo".
- **Guia do SNC Adventures**: livro de couro verde com costura e rebite, papel envelhecido com manchas, tinta vermelho-ferrugem e oliva, sumário com pontilhado, carimbos inclinados, fichas pautadas — nada de faixa verde de fachada.
- **Central de Comando, painéis e letreiros de LED**: eletrônico — fonte de LED 5×7, vidro escuro, moldura de painel.
- **Armas 3D**: realismo de armamento (aço, madeira, couro) — segue a seção específica de armas.
- **Itens do catálogo** (bebidas, drogas, sementes): sprite 16×16 com silhueta e detalhe próprio por produto — trocar só a cor não diferencia (regra do padrão de bebidas).

### Regras práticas para telas e features novas

1. Antes de desenhar, responda: "o que esta tela É no mundo real?" A metáfora define paleta, moldura e textura. Se a resposta for "parecida com outra tela que já existe", pare e reinterprete.
2. Pode reutilizar MECANISMOS (desenho vetorial pelo GuiGraphicsExtractor, hit-testing manual, payload de rede) — NUNCA a identidade (paleta, moldura, estilo de título) de outra tela.
3. Elementos compartilhados que valem em qualquer tela: fonte do jogo, desenho 100% vetorial, hit-testing nas mesmas caixas desenhadas, legibilidade em fundo claro e escuro.
4. Dentro de uma MESMA família (os 4 barris, as 5 bebidas, as 7 skins do Gago), a identidade é da FAMÍLIA; o item individual se diferencia por detalhe próprio, não por paleta divergente.
5. Quando uma tela nova criar uma identidade inédita, registre a paleta e a metáfora AQUI nesta seção (uma linha, como as acima) para as próximas não roubarem por acidente.
6. Mudança visual segue o fluxo de aprovação: prévia na aba Preview antes de buildar.

## Antes de alterar arquivos

- Leia este arquivo, `gradle.properties`, `build.gradle` e as instruções específicas existentes na área da mudança. Consulte `CHANGELOG.md`, `TODO.md` e o guia de comércio quando forem relevantes.
- Confira o estado atual dos arquivos e das versões; o histórico da conversa pode estar desatualizado.
- Outro app pode trabalhar simultaneamente neste mesmo diretório. Preserve arquivos novos e alterações alheias, inclusive quando aparecerem durante sua tarefa. Releia o trecho antes de editá-lo e prefira alterações pontuais.
- Não restaure backups inteiros, sobrescreva arquivos com cópias antigas ou reverta trabalho alheio para facilitar a implementação. Se houver conflito real no mesmo trecho, explique o conflito e coordene a solução.
- Verifique se existe Git antes de usar comandos de repositório. Não presuma que esta pasta é um checkout.
- Antes de uma mudança ampla ou migração, guarde uma cópia dos arquivos afetados em `backups/`, com data e finalidade. Nunca inclua mundos, credenciais ou caches sem necessidade.

## Implementação e compatibilidade

- Corrija a causa no módulo responsável. Não acumule versões paralelas, arquivos de override ou remendos que apenas ocultem o problema.
- Use documentação oficial de Java, Fabric e Minecraft, além das assinaturas e recursos da versão instalada. Não copie APIs de outra versão sem conferir.
- A configuração atual usa Java 25 e Minecraft 26.3. As fontes de verdade são `gradle.properties`, `build.gradle` e o wrapper Gradle; confira-as novamente em cada tarefa.
- Use o JDK compatível somente no ambiente da execução. Não altere a configuração global de Java ou do Windows para fazer um teste passar.
- O `java` do PATH é JDK 21 e NÃO serve pro build: o Loom exige JDK 25. Use `JAVA_HOME="C:/Users/enzop/.gradle/jdks/eclipse_adoptium-25-amd64-windows.2"` ao invocar o `gradlew.bat` (JDK 25 já baixado pelo Gradle).
- Minecraft 26.3 usa entrada SDL. Não use números mágicos de GLFW para teclas ou botões: use `InputConstants` e os atalhos configurados pelo jogador.
- Preserve saves existentes, identificadores registrados e dados persistidos. Mudanças de formato precisam de migração explícita e teste de leitura do formato anterior.
- Preserve a separação entre cliente e servidor. Saldo, estoque, inventário, distância e autorização das transações devem ser validados no servidor.
- Não transforme um problema em sucesso aparente removendo uma verificação, enfraquecendo uma asserção ou ignorando uma exceção.

## Padrão para criar bebidas e substâncias

Estas regras valem para novas bebidas, drogas fictícias, consumíveis e seus ingredientes. São o padrão de criação daqui para frente; não autorizam refazer ou rebalancear todo o catálogo existente sem pedido do usuário.

### Identidade e ficha do item

- Antes de implementar, registre uma ficha curta na documentação da funcionalidade usando o modelo abaixo. Preencha valores concretos; não deixe decisões importantes implícitas no código.
- Cada item deve ter uma finalidade reconhecível: combate, mineração, exploração, mobilidade ou outra função definida. Evite adicionar um item que seja apenas outro nome para um existente.
- Compare com pelo menos um item do catálogo de função semelhante. O novo item não deve ser melhor em força, duração, custo e facilidade de obtenção ao mesmo tempo.
- Os nomes podem acompanhar o tom do mod, mas receitas e efeitos representam mecânicas fictícias de Minecraft. Não buscar reprodução química ou farmacológica real.
- Use IDs estáveis em `snake_case`, sem acentos, no namespace `intoxicantes`. O mesmo ID deve ligar registro, receita, modelo, textura e traduções. Não renomeie IDs existentes apenas por estética.

```text
Nome em português / inglês:
ID:
Categoria: bebida / fumável / pó / pílula / ingrediente
Função no jogo e diferença em relação ao item mais parecido:
Obtenção: ingredientes, quantidades, estação e rendimento
Uso: animação, som, tempo de consumo e tamanho da pilha
Benefícios: efeito, nível, duração e chance
Contrapartidas: efeito, nível, duração, chance e momento de aplicação
Reutilização: renova, substitui ou acumula? Existe limite ou intervalo?
Recipiente: exigido na receita, restante do preparo e retorno após uso
Economia: vendedor, preço, estoque e eventual compra dos ingredientes
Visual: silhueta, paleta, detalhe distintivo e partículas, se houver
Descrição curta e informações dos efeitos em português / inglês:
Verificações necessárias:
```

### Receitas, obtenção e recipientes

- Todo novo consumível precisa de uma forma de obtenção funcional no modo sobrevivência. Pode ser receita, comércio, cultivo ou loot, conforme sua identidade; aparecer somente no criativo não basta.
- Priorize ingredientes já existentes e uma cadeia de produção compreensível. Só crie planta, minério ou bancada adicional quando isso acrescentar uma mecânica necessária e estiver dentro do escopo.
- Para bebidas, mantenha coerência entre o ingrediente principal e a identidade do produto. Reutilize a cadeia agrícola e o comércio do Gago quando fizer sentido.
- Defina explicitamente quantas unidades entram e saem da receita. Confira receitas sem forma, padrões de bancada, ingredientes repetidos e rendimento.
- Em novas receitas de bebidas engarrafadas, cada unidade produzida deve ter seu recipiente contabilizado. Não permita gerar garrafas gratuitamente através do ciclo preparar → consumir → recuperar recipiente. Receitas antigas incompatíveis devem ser registradas como achado, sem ajuste amplo silencioso.
- Separe o recipiente que sobra durante o preparo daquele devolvido ao consumir. Ingredientes como frasco de mel precisam de verificação própria para não duplicar ou apagar recipientes.
- Ingredientes brutos usam o comportamento de ingrediente por padrão. Só os torne consumíveis se isso tiver função e efeitos definidos.

### Regra do pipeline de bebidas (onde cada etapa acontece)

- Não obrigue tudo a acontecer dentro do barril só porque queremos barris bonitos. Cada bebida define ONDE cada etapa ocorre, conforme o processo real:
  - **Destiladas (cachaça, rum e futuros como uísque/tequila):** matéria-prima → máquina de preparo → **dorna** (fermentação) → **alambique** (destilação) → **barril** apenas para MATURAÇÃO/envelhecimento. No `ProcessosBebida.Barril` isso é `tempoFermentacao = 0`.
  - **Fermentadas (cerveja, vinho, hidromel e futuros como saquê):** o barril recebe o mosto direto e faz FERMENTAÇÃO + CONDICIONAMENTO/MATURAÇÃO dentro dele mesmo (duas fases no mesmo bloco).
- O barril é o recipiente de maturação/condicionamento, não a etapa obrigatória de tudo. Máquinas intermediárias legais de montar na base (moenda, prensa, caldeirão, dorna, alambique) são o coração do sistema — reutilize-as em vez de duplicar.
- Novo destilado NÃO cria alambique novo: um registro no `ProcessosBebida` basta. Novo fermentado que não passa por destilação entra direto pelo barril.

### Efeitos e balanceamento

- Como ponto de partida, use um benefício principal e, quando necessário, um secundário. Consumíveis potentes devem ter uma contrapartida compreensível, como efeito negativo, custo, escassez, duração curta ou intervalo de uso.
- Não acrescente muitos efeitos apenas para o item parecer forte. Não torne todos os produtos variações de força, velocidade e náusea; preserve funções diferentes.
- Escolha potência e duração comparando com os consumíveis existentes e com a dificuldade de obtenção. Melhorias excepcionais precisam de custo ou limitação proporcional e justificativa na ficha.
- Documente a duração em segundos para o jogador. No código, confirme a conversão: 20 ticks equivalem a 1 segundo na velocidade normal; amplificador 0 corresponde ao nível I, 1 ao nível II.
- Informe se a contrapartida ocorre junto do benefício ou depois dele. O helper atual aplica os efeitos ao consumir; não descreva uma "ressaca depois" se não houver uma etapa posterior implementada e testada.
- Defina o que acontece ao consumir novamente e ao combinar produtos com efeitos iguais ou opostos. Não presuma que durações, níveis ou benefícios se acumulam; confira o comportamento efetivo do Minecraft.
- Efeitos de jogabilidade e consumo devem ser autoritativos no servidor. Partículas e sons não podem causar aplicação duplicada dos efeitos.
- Não introduza dano inevitável, perda permanente de atributos ou efeitos sem duração finita como comportamento padrão. Mecânicas excepcionais precisam ser parte explícita do design solicitado.

### Comportamento comum de uso

- Reutilize as fábricas canônicas de `IntoxicantesMod`: `drink`, `smoke`, `powder`, `pill`, `product` e os helpers de efeitos e descrição. Se uma categoria precisar evoluir, altere sua implementação comum, sem copiar a lógica em cada item.
- Padrão atual: consumíveis empilham até 16; ingredientes e sementes até 64. Exceções devem ter motivo de jogabilidade registrado na ficha.
- Bebidas usam animação e som de beber e devolvem o recipiente adequado. Pó e pílula seguem o comportamento comum da categoria; fumáveis devem manter a identidade audiovisual existente.
- Consumíveis devem poder ser usados com a fome cheia, como os atuais. Não adicione nutrição ou saturação sem intenção de design explícita.
- Ao interromper o uso, não consuma o item, não entregue recipiente e não aplique o efeito final. Ao concluir, consuma exatamente uma unidade no modo sobrevivência e respeite o comportamento do criativo.
- Verifique o retorno do recipiente com pilha de uma unidade, pilha maior, inventário cheio e uso pela mão secundária. Não perca nem duplique itens.

### Padrão visual, som e descrição

- Mantenha pixel art de 16×16 para ícones comuns, fundo transparente, contorno legível, sombreamento em poucos tons e contraste suficiente no inventário. Outra resolução só quando o recurso realmente exigir.
- Cada produto deve ser reconhecível pela silhueta e por pelo menos um detalhe próprio, como rótulo, tampa, formato ou símbolo. Trocar apenas a cor de uma textura não basta para diferenciar todo o catálogo.
- Preserve a identidade visual da categoria e a escala dos itens já existentes. Confira a textura no inventário e na mão, com fundo claro e escuro; ampliar a imagem fora do jogo não substitui essa verificação.
- Entregue a cadeia completa: `assets/intoxicantes/items/<id>.json`, modelo, textura e referências válidas. Não deixe textura ausente, modelo provisório ou imagem de outro produto.
- Se o recurso for gerado por um script em `tools/`, atualize também a fonte geradora. Não rode um gerador sobre o catálogo inteiro sem conferir quais arquivos ele substituirá e preservar o trabalho paralelo.
- Sons e partículas devem combinar com a categoria, ter volume e frequência moderados e ocorrer no momento correto. Não prometa fumaça, brilho ou som exclusivo no texto sem implementação correspondente.
- Todo consumível recebe nome e descrição curta em `pt_br.json` e `en_us.json`, usando `item.intoxicantes.<id>` e `item.intoxicantes.<id>.lore`. Reutilize o mecanismo comum de lore.
- A descrição pode ter personalidade, mas deve explicar a função real. Benefícios, níveis, duração e contrapartidas precisam estar consultáveis no jogo, por tooltip, descrição ou mecanismo equivalente, sem depender da leitura do código.

### Integração e definição de pronto

- Inclua o item nas abas criativas apropriadas, sem entradas duplicadas. Integre receita, obtenção e descoberta conforme os mecanismos existentes.
- Defina conscientemente se o produto pertence ao Gago, ao Traficante ou a nenhum vendedor. Não inclua automaticamente todo novo item nas duas lojas.
- Quando uma nova bebida usar um ingrediente comprável pelo Gago, revise o catálogo de ingredientes e sua finalidade exibida. Evite ofertas duplicadas para o mesmo ingrediente e preserve IDs de estoque já salvos.
- Preço, rendimento da receita, retorno de recipientes, fidelidade e cotação da rua devem ser avaliados juntos. Teste ciclos completos de compra, preparo, consumo e revenda para impedir geração ilimitada de dinheiro ou recursos.
- Antes de entregar, confira no jogo: obtenção no sobrevivência, nome, textura, animação, som, conclusão e interrupção do consumo, efeitos reais, duração, reutilização e retorno dos recipientes.
- Para mudanças em efeitos ou consumo, acrescente testes das regras relevantes no servidor e confira o cliente real. Quando houver comportamento adiado, teste também morte, saída e recarga do mundo conforme a persistência prometida.
- Um item só está pronto quando código, recursos, receita/obtenção, traduções, ficha, comércio aplicável e validações estão coerentes. Se algo não foi verificado, diga exatamente o que falta.

## Padrão para plantações (culturas)

Estas regras valem ao criar uma planta nova ou mexer numa existente. Toda alteração em cultura precisa conferir a CADEIA COMPLETA, não só o bloco:

- Cinco estágios de crescimento (`age` 0..4) mais maturação UV (`uv_age` 0..3). O blockstate precisa ter TODOS os pares `age`/`uv_age` mapeados: estágios 1..4, dormente (`age=4`, `uv_age` 0..2) e pronto (`age=4`, `uv_age=3`). Nenhum par sem modelo.
- Estágios usam modelo `cross` e a textura de cada estágio deve estar ANCORADA NO CHÃO: o desenho encosta na borda inferior do PNG, sem margem transparenta embaixo. Margem vazia faz a planta nascer VOANDO dentro do bloco (bug já ocorrido e reportado pelo usuário). Verificar medindo o bounding box do alfa das texturas de estágio.
- A altura visível dos estágios deve crescer de forma consistente (estágio maior nunca é visivelmente mais baixo que o anterior). Conferir a progressão 1→4 de cada planta.
- Loot em `data/intoxicantes/loot_table/blocks/<planta>_plant.json`: semente incondicional; produto somente com `age=4`; com `age=4` E `uv_age=3`, produto em quantidade dobrada + semente extra. Quebrar antes do `age=4` não pode dar produto.
- A semente é o BlockItem da planta (`seedItem` no `IntoxicantesMod`) e precisa dos JSONs `items/<id>.json`, `models/item/<id>.json` e textura, além de nome e descrição em `pt_br.json` e `en_us.json`.
- Referência do padrão completo: maconha, uva, café, lúpulo e papoula. Ao criar planta nova, compare os cinco arquivos equivalentes da maconha (blockstate, modelos, loot, semente, texturas) antes de criar os seus.

## Padrão para NPCs (skins e renderers)

Vale ao criar NPC novo ou mexer na skin de um existente (traficante, Gago, Juça):

- **Corpo de player, não villager**: os NPCs usam `HumanoidModel` + `HumanoidMobRenderer` (padrão do zumbi vanilla) com skin no **layout steve 64×64**. O layer de bake é **`ModelLayers.PLAYER`** (mesh steve: braços 4px, `left_arm` em texOffs(32,48) sem mirror). NUNCA `ModelLayers.SKELETON` — mesh 64×32 de braço fino 2px com membro esquerdo espelhado deixa o braço esquerdo invisível (bug da v1.2.43, corrigido na v1.2.44). Pintar sempre POR CIMA de uma skin de player — nunca usar layout de villager nem o layout 128×128 do antigo `GagoModel` (aposentado na v1.2.41).
- Skins em `assets/intoxicantes/textures/entity/<nome>.png`. As 7 fantasias do Gago por bioma mantêm o padrão de nomes (`gago`, `gago_sertao`, `gago_mata`, `gago_cerrado`, `gago_sul`, `gago_serra`, `gago_brejo`) e o renderer escolhe pelo índice `roupa` sincronizado.
- Ao mudar uma skin, espelhar no MOD e no PACK (`resourcepacks/minhas-texturas`) com o mesmo nome — o pack sobrescreve o jar. EXCEÇÃO: texturas-atlas de armas 3D (revólver/escopeta) NUNCA vão pro pack — ver "Padrão para armas 3D".
- Ferramentas: `tools/converte_skins_player.py` (remapeia arte pra steve 64×64, lendo dos backups), `tools/debug_atlas.py` (valida as regiões do layout: magenta = vazio, vermelho = região que devia ter arte), `tools/gen_preview_skins.py` (prévia antes×depois). Rodar o atlas depois de qualquer pintura nova.
- Item na mão (escopeta/.38) é renderizado pelo `ItemInHandLayer` que o `HumanoidMobRenderer` já adiciona — não precisa de layer manual. Detalhes "3D" (nariz, avental, chinelos) são pixels pintados na textura, não cubos.
- Mudança visual de NPC segue a regra de aprovação: prévia antes×depois na aba Preview antes de buildar.

## Padrão para armas 3D (escopeta, revólver e futuras)

### 1. Formato do modelo

- NÃO usar Blender, OBJ, FBX ou modelos externos, salvo se explicitamente solicitado.
- O padrão do projeto é usar modelos nativos do Minecraft em JSON, com `elements`.
- Cada parte da arma deve ser construída com caixas/paralelepípedos do sistema vanilla.
- O modelo deve continuar compatível com o carregamento normal de resource packs/mod assets.
- Sempre que possível, a arma deve ser gerada por um script Python dentro de `tools/` (ex.: `tools/gen_escopeta.py`). O script deve gerar o JSON 3D do item, a textura e o item definition quando necessário.

### 2. Objetivo visual

Armas devem parecer o máximo possível com armas reais dentro das limitações visuais do Minecraft. Prioridades: proporção correta; silhueta convincente; comprimento realista; espessura adequada; detalhes visuais; boa leitura em primeira e terceira pessoa.
- Não criar armas pequenas, comprimidas ou "naniquinhas" só para caber dentro de 0–16.
- Os `elements` podem ultrapassar os limites tradicionais de 0–16 quando isso for necessário para manter proporções realistas.

### 3. Proporções

Antes de finalizar uma arma, verificar: comprimento geral; comprimento do cano; tamanho da coronha; tamanho do receiver; posição da empunhadura; posição de magazine/tubo; posição do pump/forend; espessura do cano; altura total; largura da arma. A silhueta deve parecer natural vista lateralmente.

Evitar: coronha excessivamente caída; coronha muito grossa; cano curto demais; receiver enorme; pump action recuado demais; peças exageradamente grossas.

### 4. Escopetas pump-action

- O pump/forend deve ficar visivelmente à frente do receiver; não deixar o pump encostado ou excessivamente próximo do receiver.
- O pump deve ter comprimento suficiente para parecer uma empunhadura frontal real.
- As action bars devem conectar visualmente o pump ao conjunto do receiver.
- O tubo do magazine deve acompanhar a região inferior do cano; o cano deve ter comprimento proporcional ao restante da arma; a coronha deve seguir uma linha natural em relação ao receiver e ser relativamente reta.
- Evitar aparência de: coronha apontando muito para baixo; pump action "colado" no receiver; arma compactada artificialmente.

### 5. Construção de curvas

Curvas devem ser simuladas com várias caixas menores, pequenos degraus, mudanças sutis de espessura e volumes sobrepostos (coronha, grip, pump, receiver, boca do cano, guarda-mato). Não usar uma única caixa enorme quando várias caixas menores produzirem uma silhueta melhor.

### 6. Nível de detalhe

Adicionar detalhes somente quando melhorarem a leitura visual: porta de ejeção, loading port, pinos, trava/safety, guarda-mato, gatilho, action bars, banda/abraçadeira entre cano e magazine, muzzle, massa de mira, vent rib, estrias no pump, soleira da coronha. Evitar detalhes decorativos sem sentido.

### 7. Número de elements

Não existe limite rígido baixo. Referência: modelos simples 15–30 elements; modelos detalhados 30–60. Evitar elementos redundantes que não tragam diferença visual perceptível.

### 8. Textura

- Textura criada especificamente para a arma. Preferência atual: 64×64 para modelos médios; 128×128 para armas mais detalhadas. Não limitar automaticamente a 16×16 ou 32×32.
- Materiais com leitura visual distinta (aço, aço oxidado/azulado, metal escuro/claro, madeira clara/escura, borracha, couro, latão, interior escuro do cano).
- Usar pequenos degradês, variações de brilho, grão e desgaste sutil, sem exagerar no ruído.

### 9. UV

Pode ser usado um atlas de materiais único para a arma; cada material ocupa uma região própria da textura. As faces podem usar tons diferentes para simular iluminação (`up` mais claro, `down` mais escuro, laterais médios). O UV deve continuar normalizado em 0–16 no JSON.

### 10. Primeira pessoa (REGRA IMPORTANTE)

Em primeira pessoa, armas longas devem aparecer como em jogos de tiro: apontadas para frente, alinhadas com a direção da visão, não excessivamente inclinadas, não deitadas lateralmente, não parecendo apoiadas no ombro de forma exagerada. O jogador deve enxergar principalmente receiver, pump/fore-end, parte do cano e linha superior da arma; a boca do cano deve seguir visualmente a direção da mira.
- Para mão direita e esquerda, usar transforms espelhados/coerentes.
- Nunca copiar cegamente o `display` da besta vanilla se ele fizer a arma ficar muito lateralizada. Ajustar individualmente `rotation`, `translation` e `scale`.

### 11. Terceira pessoa

Arma longa deve parecer realmente uma arma longa: não reduzir excessivamente a escala, deve ficar proporcional ao personagem e ser segurada de maneira visualmente convincente. Pode usar escala acima de `1.0` se necessário.

### 12. GUI / inventário

O tamanho real do modelo NÃO deve ser reduzido apenas para caber no inventário. O correto é manter o modelo grande e reduzir somente o transform `display.gui`. A mesma lógica vale para `fixed` e `ground`.

### 13. Preview obrigatório

Toda alteração importante de modelo deve gerar preview antes de ser considerada final (além da prévia na aba Preview do fluxo de aprovação). O projeto pode usar `tools/preview_escopeta.py` ou outro preview equivalente, lendo o JSON real gerado. Gerar, quando possível: isométrica, lateral, topo, frente, GUI e primeira pessoa.

### 14. Comparação

Ao fazer uma alteração grande, gerar também uma comparação ANTES × DEPOIS, principalmente quando houver mudança em comprimento, coronha, pump, proporções ou primeira pessoa.

### 15. Script como fonte principal

Quando existir um gerador Python, o script é a fonte principal. Não editar apenas o JSON final e esquecer o gerador. Fluxo correto: 1) editar `tools/gen_arma.py`; 2) executar o gerador; 3) gerar JSON; 4) gerar textura; 5) executar preview; 6) revisar visualmente; 7) entregar arquivos finais. Assim as alterações não somem na próxima regeneração.

### 16. Remapeamento de eixos

Se o modelo for desenhado em um eixo intuitivo e depois remapeado (ex.: `(x, y, z) -> (z, x, y)`), o remapeamento precisa ser aplicado também às rotações: converter `axis` corretamente, não somente `from`, `to` e `origin`.

### 17. Entrega

Para alterações de geometria, NÃO entregar somente o PNG. Uma arma 3D normalmente precisa de `models/item/arma.json`, `textures/item/arma.png`, `items/arma.json`; com gerador, `tools/gen_arma.py`; com preview, `tools/preview_arma.py`. Preferencialmente entregar tudo em ZIP (não `.rar`).

### 18. Compatibilidade com o projeto

Manter namespace, nomes de arquivos, caminhos, referências de textura e estrutura de resources existentes (`assets/intoxicantes/models/item/`, `assets/intoxicantes/textures/item/`, `assets/intoxicantes/items/`). Não inventar outro namespace.

### 19. Regra de preservação

Ao melhorar uma arma existente: preservar o nome do item, o ID, o namespace e as integrações existentes; alterar apenas modelo, textura e transforms necessários. Não quebrar chamadas de código existentes.

### 20. Resultado esperado

"Uma arma visualmente realista dentro da estética e das limitações do Minecraft, com proporções próximas às reais, modelo detalhado porém leve, boa textura, boa visualização no inventário e principalmente uma posição convincente em primeira pessoa."

### 21. Pack do usuário (`resourcepacks/minhas-texturas`) — NUNCA espelhar textura-atlas de arma 3D (bug da v1.2.44)

O pack do usuário SOBRESCREVE os assets do jar (regra geral de resource pack). Isso é desejado pra skins/blocos/itens-sprite, mas é FATAL pra armas 3D: se existir `textures/item/<arma>.png` no pack (ex.: a arte manual antiga 1254×1254 com 62% de fundo transparente), o jogo usa ELA no lugar do atlas 128×128 de materiais, e as UVs do modelo 3D caem em região transparente — a arma fica invisível/minúscula no jogo mesmo estando perfeita no jar (bug do revólver em 24/09/2026: alpha médio 1/255 nas 186 faces).
- NUNCA copiar `textures/item/<arma>.png` de arma 3D pro pack. O atlas de materiais vive SÓ no jar.
- Item novo em 3D: conferir que não existe PNG residual da versão antiga (sprite) no pack — mover pra `backups/` se existir.
- Validação rápida: rodar as UVs do modelo contra a textura efetiva (jar + pack) e exigir alpha médio ~255 nas faces.

## Guia do SNC espelha a progressão real (regra do usuário)

- Sempre que um fluxo de progressão mudar (cadeia de bebidas, tempos de máquina, quantidades, fontes de loot, economia), o livro Guia do SNC deve ser atualizado NO MESMO ciclo, refletindo os valores do código.
- O Guia é fonte derivada: `GuiaConteudo` lê tempos/quantidades de `ProcessosBebida` e `ModConfig` — nunca escreva número fixo no guia quando existir constante no código.
- Cada página de bebida deve mostrar: a cadeia completa (matéria-prima → máquinas → barril → garrafas), tempo real de jogo do ciclo completo, e efeitos da bebida.
- Antes de entregar, conferir cada passo descrito no guia contra o registro real (`ProcessosBebida.registrar()`, receitas, tempos). Passo no guia que não existe no jogo é bug.

## Máquinas com tamanho real — multi-bloco (regra do usuário)

- Máquinas de produção (dorna/barril de fermentação, alambique, esmagadora, prensa, caldeirão e futuras) NÃO são blocos-toy de 1m³: nascem com o tamanho REAL da versão grande — hoje 2 blocos de altura (padrão PARTE: BAIXO/ALTO, como o PosteLuzBlock).
- A metade BAIXO carrega o BlockEntity e a lógica; a metade ALTO é estrutural (sem BE). Cliques na parte ALTO roteiam pra BAIXO; quebrar uma parte quebra a outra; colocar em cima de slot ocupado é recusado.
- Barris de bebida ficam 1 bloco DE PROPÓSITO: barril real tem ~1m de altura — já é tamanho real.
- O modelo é desenhado no gerador (`tools/gen_bebidas.py`) no espaço COMPLETO (0..32 de altura) e fatiado em dois modelos (baixo 0..16, alto 16..32 deslocado -16). O item mostra a máquina inteira (modelo próprio de item).
- Toda máquina NOVA segue o padrão desde a criação: propriedade PARTE, formas por parte, colocação de 2 blocos, quebra acoplada, blockstate com 2 variantes, modelo fatiado no gerador.

## Comércio e menu do Gago

- O Gago compra ingredientes das receitas das bebidas que vende. Confira as receitas reais antes de alterar o catálogo; não presuma que toda plantação deve ser comprada.
- Mantenha coerentes catálogo, receitas, preços, quantidades dos lotes, cotas e textos. Hoje isso inclui lúpulo, uva, trigo, cana do mod, frascos de mel e garrafas vazias.
- Centralize ofertas em `TradeCatalog`; preserve as responsabilidades de `MarketInventory`, `DailyTradeStock` e `MarketTransactions`. Não duplique regras econômicas na tela.
- Preserve cotas compartilhadas por NPC, gravação no save e reposição às 07h do jogo, adiada durante atendimento. Reabrir o menu, recarregar o mundo ou voltar o relógio não deve renovar estoque indevidamente.
- Verifique consumo exato dos ingredientes, pagamento exato, limites de saldo e ausência de lucro automático ao comprar e revender o mesmo item, inclusive com fidelidade.
- Preserve o visual verde, bege e dourado do Esquinão. Melhore legibilidade e interação sem substituir o estilo por uma tela genérica.
- Mostre quantidade por lote, preço, saldo, estoque/cota e motivos de indisponibilidade. Nomes longos devem continuar acessíveis por dicas de contexto.
- Confira cliques, roda do mouse, arrasto da barra, alternância de abas, atualização após transações, Esc e tecla configurada para inventário. Uma resposta atrasada não deve reabrir um menu fechado.
- Atualize pelo menos `pt_br.json` e `en_us.json` juntos. Confira visualmente a versão em português.

## Testes e revisão final

- Para alterações de código ou recursos, execute `gradlew.bat build` com o JDK correto. O projeto inclui validação dos recursos e testes de servidor.
- Para mudanças no menu ou nas interações de comércio, execute também `gradlew.bat runClientGameTest`. Compilar sozinho não comprova que a tela funciona.
- Use mundos isolados dos testes. Não teste destrutivamente no save do usuário nem altere seu dinheiro, inventário ou construções para verificar uma funcionalidade.
- Confira a interface no cliente real em janela grande e pequena; as referências atuais são 1280×800 e 640×480. Inspecione capturas, cortes de texto, sobreposição, rolagem e estados desabilitados.
- Acrescente testes de regressão quando a mudança afetar regras, persistência ou interação. Os testes devem provar comportamentos reais, não repetir a implementação.
- Revise os logs completos e classifique erros e avisos. Avisos ambientais conhecidos não justificam ignorar falhas do mod. Não altere o registro do Windows para eliminar avisos dos contadores de desempenho.
- Ao terminar, releia o pedido original e revise o resultado desde a abertura do fluxo até seu encerramento. Confira que as correções não quebraram os recursos preservados.
- Se novos arquivos ou mudanças surgirem depois da validação, avalie o impacto antes de empacotar. Repita os testes afetados quando necessário.
- Mudanças somente de documentação não exigem iniciar o Minecraft: revise conteúdo, caminhos e formatação.

## Entrega e instalação

- Quando a tarefa pedir uma versão pronta para jogar, entregue o JAR compilado e o código atualizado. Use `dist/` para pacotes, guia e evidências resumidas; exclua caches e mundos do pacote de fontes.
- Confira a versão atual antes de incrementá-la, pois outro app também pode alterar `mod_version`. Atualize o changelog e os guias afetados sem apagar o histórico.
- Garanta que o JAR contém a versão correta e não inclui as classes de testes. Compare o hash do artefato com a cópia instalada.
- A instância atual usa a pasta `../mods`. Verifique o destino antes de instalar e preserve os outros mods.
- Antes de substituir o Intoxicantes instalado, guarde o JAR anterior em `backups/`, fora de `mods`. Deixe apenas uma versão ativa do Intoxicantes.
- Não encerre o Minecraft do usuário. Se o jogo bloquear a substituição, entregue o pacote pronto, informe que a instalação está pendente e peça que ele feche o jogo. Não diga que instalou antes de verificar.
- Não publique, envie mensagens a terceiros, faça push ou distribua o mod externamente sem instrução do usuário.
- Na resposta final, diga o que mudou, quais testes passaram, onde está o pacote e se foi instalado. Diferencie testes em ambiente isolado da compatibilidade com o conjunto completo de mods e com o mundo do usuário.

## Comunicação durante o trabalho

- Dê atualizações curtas com achados concretos e o próximo passo. Não deixe o usuário sem notícia durante trabalhos demorados.
- Pergunte por sintomas de bugs quando isso ajudar, mas continue as verificações independentes enquanto aguarda.
- Não peça aprovação repetida para trabalho já autorizado. Peça esclarecimento quando faltar uma decisão necessária ou houver conflito real de escopo.
- Se o usuário perguntar quanto falta, responda diretamente com o que está concluído e o que resta, sem prometer prazos sem base.

---

# REGRA OBRIGATÓRIA — BUILD, CHANGELOG, COMMIT E PUSH

Toda alteração concluída no projeto deve seguir este fluxo obrigatório.

## 1. NUNCA commitar código quebrado

Antes de qualquer commit:

1. revisar os arquivos alterados;
2. verificar imports;
3. verificar referências;
4. verificar IDs;
5. verificar assets;
6. verificar JSONs;
7. verificar recipes;
8. verificar models/textures;
9. verificar possíveis duplicações;
10. executar o build completo do projeto.

Se o build falhar:

- NÃO commitar;
- NÃO pushar;
- corrigir o problema;
- executar o build novamente.

Só seguir para commit quando o build estiver concluindo com sucesso.

---

# 2. BUILD É O GATE OBRIGATÓRIO

Sempre executar o comando de build apropriado do projeto.

Exemplos:

`./gradlew build`

ou no Windows:

`gradlew.bat build`

Se o projeto possuir tarefas adicionais relevantes, também executar quando necessário:

- datagen;
- tests;
- check;
- lint;
- runData;
- validateAccessWidener;
- ou equivalentes existentes no projeto.

Nunca assumir que está correto apenas porque o código parece correto.

---

# 3. VERIFICAR ERROS ALÉM DO BUILD

Mesmo com build aprovado, revisar:

- warnings relevantes;
- arquivos duplicados;
- assets faltando;
- referências quebradas;
- IDs incorretos;
- recipes inválidas;
- modelos sem textura;
- texturas apontando para caminho errado;
- BlockEntities sem registro;
- itens/blocos sem tradução;
- creative tab ausente;
- loot table faltando;
- recipe antiga conflitante;
- código morto criado pela alteração.

Se alguma dessas verificações falhar, corrigir antes do commit.

---

# 4. TESTES ESPECÍFICOS DA FEATURE

Depois do build, testar especificamente a alteração feita.

Exemplos:

## Se mexeu em item:
- item registra;
- textura aparece;
- nome aparece;
- recipe funciona;
- comportamento funciona.

## Se mexeu em bloco:
- coloca no mundo;
- quebra corretamente;
- drop funciona;
- modelo funciona;
- estado persiste;
- interação funciona.

## Se mexeu em BlockEntity:
- salva;
- carrega;
- progresso continua;
- não duplica item;
- servidor controla estado.

## Se mexeu em modelo 3D:
- verificar preview;
- verificar GUI;
- verificar primeira pessoa;
- verificar terceira pessoa.

## Se mexeu em bebidas:
- verificar cadeia completa;
- verificar progressão;
- verificar tempo;
- verificar retirada do produto;
- verificar persistência após sair do mundo.

Não considerar a feature pronta sem verificar o comportamento principal.

---

# 5. CHANGELOG OBRIGATÓRIO

Toda alteração que gere commit deve atualizar o changelog.

Usar o arquivo de changelog já existente no projeto.

Se não existir, criar:

`CHANGELOG.md`

Formato recomendado:

## [Unreleased]

### Added
- novas features.

### Changed
- mudanças de comportamento.

### Fixed
- correções de bugs.

### Improved
- melhorias visuais, técnicas ou de performance.

### Removed
- funcionalidades removidas.

Não escrever mensagens vagas como:

- "mudanças";
- "ajustes";
- "coisas novas";
- "fixes".

Descrever exatamente o que mudou.

Exemplos corretos:

### Added
- Adicionado sistema de fermentação para cerveja.
- Adicionado barril 3D específico para vinho.

### Fixed
- Corrigido posicionamento da escopeta em primeira pessoa.
- Corrigida persistência do progresso de fermentação após recarregar o mundo.

### Improved
- Melhoradas proporções do modelo 3D da escopeta.
- Melhorada textura do barril de rum.

---

# 6. CHANGELOG DEVE REFLETIR O COMMIT REAL

Nunca adicionar no changelog algo que ainda não foi implementado.

Nunca prometer feature futura dentro do changelog de uma versão atual.

O changelog deve representar somente alterações presentes no commit.

---

# 7. COMMIT SOMENTE DEPOIS DE TUDO VALIDADO

A ordem correta é:

1. implementar;
2. revisar;
3. buildar;
4. testar;
5. corrigir;
6. buildar novamente;
7. atualizar changelog;
8. revisar `git diff`;
9. verificar `git status`;
10. commitar;
11. pushar.

Nunca inverter essa ordem.

---

# 8. REVISAR GIT DIFF ANTES DO COMMIT

Antes de commitar:

executar:

`git status`

e:

`git diff`

Verificar:

- arquivos alterados esperados;
- arquivos acidentais;
- binários grandes;
- arquivos temporários;
- builds gerados;
- previews;
- caches;
- IDE files;
- logs;
- credenciais;
- tokens;
- secrets.

Nunca adicionar acidentalmente:

- `.env`;
- tokens;
- senhas;
- chaves privadas;
- arquivos de configuração local;
- `.idea/`;
- `.vscode/` quando não pertencem ao projeto;
- `build/` quando estiver ignorado;
- arquivos temporários.

---

# 9. NUNCA USAR `git add .` CEGAMENTE

Preferir revisar e adicionar somente os arquivos relevantes.

Exemplo:

`git add src/... tools/... CHANGELOG.md`

Antes do commit, confirmar novamente:

`git status`

---

# 10. PADRÃO DE COMMIT

Usar commits claros e objetivos.

Formato preferencial:

`tipo: descrição curta`

Tipos:

- `feat:` nova feature;
- `fix:` correção;
- `refactor:` refatoração;
- `perf:` performance;
- `docs:` documentação;
- `chore:` manutenção;
- `style:` alteração visual sem mudança lógica;
- `test:` testes.

Exemplos:

`feat: adiciona sistema de fermentação de bebidas`

`feat: adiciona barris 3D para bebidas`

`fix: corrige posicionamento da escopeta em primeira pessoa`

`fix: corrige persistência do barril de vinho`

`improved` não é um tipo de commit padrão; usar `refactor`, `style`, `perf` ou `feat` conforme o caso.

---

# 11. UM COMMIT DEVE REPRESENTAR UMA ALTERAÇÃO COERENTE

Não misturar no mesmo commit coisas sem relação.

Exemplo ruim:

`feat: bebidas + shotgun + correção de lang + inventário + refactor geral`

Preferir commits separados quando os assuntos forem independentes.

Exemplo:

`feat: adiciona infraestrutura de fermentação`

`feat: adiciona barris de bebidas`

`fix: corrige modelo da escopeta em primeira pessoa`

---

# 12. PUSH OBRIGATÓRIO APÓS COMMIT VÁLIDO

Depois de:

- build aprovado;
- testes aprovados;
- changelog atualizado;
- commit realizado;

executar push para a branch atual.

Exemplo:

`git push origin <branch>`

Nunca fazer push de commit conhecido como quebrado.

---

# 13. SE O PUSH FALHAR

Se o push falhar:

1. identificar o motivo;
2. NÃO criar commits aleatórios tentando resolver;
3. verificar se o remoto avançou;
4. usar fetch;
5. revisar diferenças;
6. fazer merge ou rebase com cuidado;
7. resolver conflitos conscientemente;
8. rodar build novamente;
9. atualizar changelog se necessário;
10. só então pushar.

Nunca usar `git push --force` por padrão.

---

# 14. FORCE PUSH

`git push --force` é proibido por padrão.

Só usar se:

- a branch permitir;
- houver motivo claro;
- o usuário pedir explicitamente;
- não houver risco de apagar trabalho de terceiros.

Se realmente necessário, preferir:

`git push --force-with-lease`

em vez de:

`git push --force`

---

# 15. BUILD APÓS CONFLITO

Toda vez que houver:

- merge;
- rebase;
- resolução de conflito;
- cherry-pick;

executar o build novamente antes do push.

Nunca assumir que conflito resolvido significa projeto funcional.

---

# 16. NÃO COMMITAR CÓDIGO TEMPORÁRIO

Antes do commit remover:

- prints de debug;
- logs temporários;
- TODOs criados apenas para teste;
- código comentado sem necessidade;
- assets de teste;
- screenshots temporárias;
- arquivos de diagnóstico.

Só manter logs úteis e intencionais.

---

# 17. COMMITS AUTOMÁTICOS DEVEM SER SEGUROS

O agente pode commitar e pushar automaticamente SOMENTE quando:

- a tarefa estiver claramente concluída;
- build passar;
- testes relevantes passarem;
- changelog estiver atualizado;
- diff estiver revisado;
- não houver secrets;
- não houver arquivos estranhos.

Se houver qualquer dúvida importante:

NÃO pushar automaticamente.

Parar e relatar o problema.

---

# 18. NUNCA MASCARAR FALHA

É proibido:

- ignorar teste quebrado;
- desativar teste só para build passar;
- remover validação sem motivo;
- comentar código problemático apenas para compilar;
- esconder exceção;
- usar `try/catch` vazio;
- remover feature silenciosamente para fechar build.

Corrigir a causa.

---

# 19. BUILD VERDE NÃO SIGNIFICA FEATURE PRONTA

O build apenas confirma que o projeto compila.

Ainda é obrigatório conferir:

- comportamento;
- integração;
- assets;
- UI;
- recipes;
- persistence;
- efeitos colaterais.

---

# 20. RELATÓRIO APÓS PUSH

Depois de concluir a tarefa e pushar, informar:

- o que foi implementado;
- o que foi corrigido;
- build executado;
- testes executados;
- commit criado;
- hash curto do commit;
- branch;
- push concluído;
- changelog atualizado.

Formato recomendado:

### Concluído

- Feature: sistema de fermentação.
- Build: aprovado.
- Testes: aprovados.
- Changelog: atualizado.
- Commit: `a1b2c3d feat: adiciona sistema de fermentação`
- Branch: `main`
- Push: concluído.

---

# 21. CHECKLIST FINAL OBRIGATÓRIO

Antes de qualquer push, confirmar:

- [ ] tarefa realmente concluída;
- [ ] código revisado;
- [ ] build aprovado;
- [ ] testes relevantes aprovados;
- [ ] assets verificados;
- [ ] sem erro de JSON;
- [ ] sem missing texture/model;
- [ ] sem secrets;
- [ ] `git diff` revisado;
- [ ] `git status` revisado;
- [ ] changelog atualizado;
- [ ] commit claro;
- [ ] branch correta;
- [ ] push realizado.

Se qualquer item estiver pendente, NÃO considerar a tarefa concluída.

---

# REGRA PRINCIPAL

A regra geral do projeto é:

> Implementou → revisou → buildou → testou → corrigiu → buildou novamente → atualizou changelog → revisou o diff → commitou → pushou.

Nunca:

> implementou → commitou → torceu para funcionar.

# 22. PUBLICAÇÃO OBRIGATÓRIA DAS BUILDS `.JAR` NO GITHUB RELEASES

Sempre que uma alteração resultar em uma nova build distribuível do mod, o arquivo `.jar` final deve ser publicado também na área de **Releases do GitHub**.

O fluxo não termina no `git push`.

Fluxo completo:

1. implementar;
2. revisar;
3. executar build;
4. testar;
5. corrigir;
6. executar build novamente;
7. atualizar CHANGELOG;
8. revisar `git diff`;
9. revisar `git status`;
10. criar commit;
11. fazer push;
12. localizar o `.jar` final gerado;
13. validar o `.jar`;
14. criar a versão/tag apropriada;
15. criar GitHub Release;
16. anexar o `.jar` da build;
17. usar o CHANGELOG correspondente como base das Release Notes;
18. verificar se o arquivo realmente aparece no Release.

A tarefa NÃO deve ser considerada completamente publicada enquanto a build distribuível não estiver disponível no GitHub Releases.

---

# 23. QUAL `.JAR` PUBLICAR

Depois do build, localizar o artefato realmente destinado ao jogador.

Normalmente estará em algo semelhante a:

`build/libs/`

Antes de publicar, conferir os arquivos presentes.

NÃO publicar por engano:

- `*-sources.jar`;
- `*-javadoc.jar`;
- jars de desenvolvimento;
- jars intermediários;
- jars temporários;
- jars usados apenas pelo ambiente de desenvolvimento;
- artefatos duplicados;
- jars não remapeados quando o loader exigir remapeamento.

Publicar somente o `.jar` que deve ser colocado pelo jogador na pasta `mods`.

Se houver dúvida sobre qual é o artefato correto, analisar a configuração Gradle e o loader antes de publicar.

---

# 24. O `.JAR` PRECISA VIR DO BUILD VALIDADO

Nunca usar um `.jar` antigo encontrado em `build/libs`.

A sequência obrigatória é:

`clean/build`
↓
build aprovado
↓
identificar JAR recém-gerado
↓
publicar esse JAR

Quando apropriado, executar:

`./gradlew clean build`

ou no Windows:

`gradlew.bat clean build`

Isso evita publicar restos de builds antigas.

---

# 25. VALIDAÇÃO DO `.JAR`

Antes de enviar para GitHub Releases:

- confirmar que o arquivo existe;
- confirmar que foi gerado pela build atual;
- confirmar tamanho plausível;
- confirmar nome e versão;
- confirmar que contém os arquivos esperados;
- confirmar que não é `sources` ou `dev`;
- confirmar que corresponde ao commit que será lançado.

Quando possível, inspecionar o conteúdo do `.jar`.

Exemplo:

- classes compiladas;
- `META-INF`;
- assets;
- models;
- textures;
- lang;
- recipes;
- metadata do mod.

Nunca publicar arquivo vazio, incorreto ou antigo.

---

# 26. VERSIONAMENTO

Antes de criar Release, identificar como o projeto atualmente controla a versão.

Pode estar em:

- `gradle.properties`;
- `build.gradle`;
- `build.gradle.kts`;
- arquivo próprio do mod;
- metadata do loader.

NÃO inventar outro sistema de versionamento se o projeto já possui um.

Sempre manter sincronizados:

- versão do projeto;
- nome do `.jar`;
- tag Git;
- título da Release.

Exemplo:

Versão:

`1.3.0`

Tag:

`v1.3.0`

Release:

`Intoxicantes Mod v1.3.0`

JAR:

`intoxicantes-1.3.0.jar`

Seguir a convenção real existente no repositório.

---

# 27. NÃO SOBRESCREVER VERSÕES PUBLICADAS SILENCIOSAMENTE

Se um Release/tag já existir e a build mudou, NÃO substituir silenciosamente um `.jar` diferente mantendo exatamente a mesma versão.

Se a alteração for uma nova build pública, incrementar a versão adequadamente.

Exemplo:

`1.2.0`
↓
correção
↓
`1.2.1`

Nunca fazer:

`v1.2.0` antigo
↓
alterar código
↓
publicar outro binário diferente também chamado `v1.2.0`

Isso torna builds impossíveis de rastrear.

---

# 28. SEMANTIC VERSIONING

Quando o projeto ainda não possuir outra convenção, preferir:

`MAJOR.MINOR.PATCH`

Exemplo:

`1.4.2`

Uso recomendado:

### PATCH

Correções e pequenos ajustes:

`1.4.2 → 1.4.3`

Exemplos:

- bug fix;
- correção de modelo;
- ajuste de posição da escopeta;
- correção de recipe;
- correção de crash.

### MINOR

Novas funcionalidades compatíveis:

`1.4.3 → 1.5.0`

Exemplos:

- sistema de bebidas;
- novo barril;
- novo item;
- nova máquina;
- nova mecânica.

### MAJOR

Mudanças grandes ou incompatíveis:

`1.x.x → 2.0.0`

Só alterar MAJOR quando realmente fizer sentido para o projeto.

---

# 29. TAG GIT OBRIGATÓRIA PARA RELEASE

Cada Release pública deve possuir tag correspondente.

Exemplo:

`v1.5.0`

A tag deve apontar para o commit exato usado para gerar a build.

Nunca criar Release a partir de um commit diferente daquele que produziu o `.jar`.

Fluxo esperado:

commit final
↓
push
↓
tag desse commit
↓
push da tag
↓
GitHub Release
↓
upload do `.jar`

---

# 30. RELEASE NOTES

As Release Notes devem ser baseadas no `CHANGELOG.md`.

Não escrever apenas:

`Nova versão`

ou:

`Bug fixes`

Criar uma descrição útil.

Exemplo:

## Intoxicantes Mod v1.5.0

### Added

- Sistema completo de fermentação.
- Barris específicos para bebidas.
- Alambique de cobre.
- Processo de fabricação de cerveja.

### Improved

- Modelo 3D da escopeta.
- Posição da arma em primeira pessoa.

### Fixed

- Correção de persistência do barril.
- Correção de recipe duplicada.

O conteúdo deve refletir exclusivamente o que existe nessa versão.

---

# 31. ANEXAR O `.JAR` AO RELEASE

O `.jar` não deve existir apenas no histórico do Git ou em Actions.

Ele deve aparecer explicitamente como arquivo baixável dentro do GitHub Release.

Exemplo de asset:

`intoxicantes-1.5.0.jar`

Depois do upload, verificar se o Release realmente contém o arquivo.

Não assumir que o upload funcionou apenas porque o comando não mostrou erro.

---

# 32. NOME DO ARQUIVO

Preferir nomes identificáveis.

Bom:

`intoxicantes-1.5.0.jar`

Aceitável, caso o projeto use versão de Minecraft:

`intoxicantes-1.5.0-mc1.21.1.jar`

Evitar:

`mod.jar`

`build.jar`

`final.jar`

`teste.jar`

`novo.jar`

O jogador deve saber imediatamente qual arquivo está baixando.

---

# 33. NÃO PUBLICAR BUILD QUEBRADA

É terminantemente proibido criar Release com uma build que:

- não compila;
- falha nos testes relevantes;
- possui crash conhecido;
- possui assets ausentes;
- possui missing textures;
- possui JSON inválido;
- possui funcionalidade principal quebrada;
- foi gerada antes da correção final.

Se for descoberta uma falha antes da publicação:

1. corrigir;
2. buildar novamente;
3. testar novamente;
4. atualizar changelog se necessário;
5. commitar;
6. pushar;
7. só então gerar/publicar o Release.

---

# 34. NÃO PUBLICAR BUILD DE DESENVOLVIMENTO COMO RELEASE ESTÁVEL

Se uma alteração ainda estiver experimental:

- não marcar como Release estável;
- não apresentar como versão final.

Caso o projeto use prereleases, pode ser usado algo como:

`v1.6.0-beta.1`

ou equivalente.

Marcar como pre-release no GitHub.

Não fazer isso automaticamente quando a versão normal estiver pronta.

---

# 35. ARTEFATOS DE RELEASE NÃO DEVEM SER COMMITADOS SEM NECESSIDADE

O `.jar` gerado normalmente NÃO precisa ser adicionado ao repositório Git.

Preferência:

código-fonte → Git
build `.jar` → GitHub Releases

Não adicionar automaticamente:

`build/libs/*.jar`

ao commit.

A menos que o repositório já tenha uma regra explícita diferente.

---

# 36. RELAÇÃO ENTRE COMMIT, CHANGELOG E RELEASE

Uma versão publicada precisa ser completamente rastreável.

Deve ser possível identificar:

Release
↓
Tag
↓
Commit
↓
CHANGELOG
↓
Código-fonte
↓
JAR gerado

Nunca publicar um `.jar` cuja origem não possa ser determinada.

---

# 37. NOVA BUILD = NOVO RELEASE QUANDO FOR DISTRIBUÍVEL

Sempre que o usuário solicitar uma nova build utilizável do mod ou quando uma tarefa concluída representar uma nova versão distribuível:

- atualizar versão;
- buildar;
- validar;
- atualizar changelog;
- commitar;
- pushar;
- criar tag;
- criar Release;
- anexar o novo `.jar`.

Não deixar builds novas somente dentro de `build/libs`.

---

# 38. VERIFICAÇÃO DO GITHUB RELEASE

Depois da publicação, conferir:

- [ ] tag correta;
- [ ] commit correto;
- [ ] título correto;
- [ ] versão correta;
- [ ] Release Notes corretas;
- [ ] `.jar` anexado;
- [ ] nome do `.jar` correto;
- [ ] `.jar` corresponde à build validada;
- [ ] Release não está como draft sem intenção;
- [ ] prerelease somente quando intencional.

A publicação só está concluída depois dessa conferência.

---

# 39. CHECKLIST FINAL COMPLETO

Antes de declarar uma tarefa/versionamento concluído:

- [ ] implementação concluída;
- [ ] código revisado;
- [ ] assets revisados;
- [ ] build limpa aprovada;
- [ ] testes relevantes aprovados;
- [ ] feature testada;
- [ ] sem missing textures;
- [ ] sem models quebrados;
- [ ] sem JSON inválido;
- [ ] sem secrets;
- [ ] CHANGELOG atualizado;
- [ ] versão atualizada;
- [ ] `git diff` revisado;
- [ ] `git status` revisado;
- [ ] commit realizado;
- [ ] push realizado;
- [ ] tag criada;
- [ ] tag enviada ao remoto;
- [ ] `.jar` correto identificado;
- [ ] GitHub Release criado;
- [ ] Release Notes preenchidas;
- [ ] `.jar` anexado ao Release;
- [ ] Release conferido no GitHub.

Se qualquer etapa necessária estiver faltando, não declarar a versão como publicada.

---

# 40. RELATÓRIO FINAL DE RELEASE

Depois de concluir uma versão, informar:

### Release concluído

- Versão: `x.x.x`
- Build: aprovado
- Testes: aprovados
- Changelog: atualizado
- Commit: `<hash> descrição`
- Branch: `<branch>`
- Push: concluído
- Tag: `vx.x.x`
- GitHub Release: criado
- JAR: `<nome-do-arquivo>.jar`
- Upload do JAR: concluído

Se alguma dessas etapas não puder ser feita, informar exatamente qual ficou pendente.

---

# FLUXO MESTRE DO PROJETO

A partir de agora, para toda nova versão distribuível:

IMPLEMENTAR
↓
REVISAR
↓
BUILDAR
↓
TESTAR
↓
CORRIGIR
↓
BUILDAR NOVAMENTE
↓
ATUALIZAR CHANGELOG
↓
ATUALIZAR VERSÃO
↓
REVISAR GIT DIFF
↓
COMMIT
↓
PUSH
↓
TAG
↓
BUILD/JAR FINAL
↓
GITHUB RELEASE
↓
UPLOAD DO JAR
↓
CONFERIR RELEASE

Nunca:

IMPLEMENTAR
↓
COMMITAR
↓
PUSHAR
↓
PUBLICAR QUALQUER JAR ENCONTRADO

O `.jar` disponibilizado ao usuário deve ser sempre uma build validada e rastreável ao commit/tag correspondente.

Sempre que uma tarefa resultar em uma nova versão distribuível do mod, o agente deve obrigatoriamente revisar, buildar, testar, corrigir, executar a build final limpa, atualizar o CHANGELOG e a versão, revisar o diff, commitar, pushar, criar a tag correspondente, criar/atualizar o GitHub Release e anexar o .jar distribuível validado. A tarefa só é considerada publicada após conferir que o .jar correto está disponível para download no Release. Salvo quando o usuário mandar explicitamente não commitar, não pushar ou não publicar uma release.
