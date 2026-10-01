# Auditoria de identidade dos recursos — SNC Adventures

Inspeção dos arquivos e das artes atuais em 01/10/2026. Versão fonte observada: 1.2.70. Há trabalho paralelo adicionando recursos elétricos; o estado da pasta não deve ser confundido com tudo já disponível no JAR instalado.

## Critério

O objeto do mundo, o item coletado e o item na mão devem compartilhar forma, materiais e linguagem visual. Imagens 2D continuam adequadas quando representam bem o produto; resolução alta sozinha não transforma uma imagem realista em arte voxel do SNC.

## Pares com diferenças claras

| Recurso | Estado encontrado | Correção sugerida | Fonte principal |
|---|---|---|---|
| Uva | Item é imagem realista plana1254×1254; a parreira renderiza cachos voxel com atlas128. | Compartilhar a geometria e o atlas do cacho entre parreira e item, preservandoID/receitas/colheita. | models/item/uva.json; ParreiraRenderer.java; gen_farm_resources.py |
| Coco | Item tem11cuboides e atlas128; coco_bloco é cube_all, com outra textura. | Mesmo coco voxel pendurado e coletado. | models/item/coco.json; models/block/coco_bloco.json; fonte geradora do coco/bloco |
| LâmpadaUV | Item retrata bulboU realista; bloco é um cubo com imagem de painel duplo. | Um projeto de luminária UV com item/bloco coerentes. | models/item/lampada_uv.json; models/block/lampada_uv.json; gen_farm_resources.py |
| Poste de luz | Definition do item aponta só poste_luz_base; no mundo existem base, corpo e topo. | Modelo completo do poste no inventário/mão, com escala própria. | items/poste_luz.json; blockstates/poste_luz.json; gerador do poste |
| Porta-grade | Ícone põe a grade na metade inferior; porta colocada põe na superior. | Corrigir a silhueta do ícone para representar a mesma porta. | gen_porta_grade.py |
| Porta-grade/pack | minhas-texturas está selecionado; porta_grade.png do pack é atlas antigo de quatro materiais, mas o modelo atual usa uma textura inteira de madeira. | Sincronizar a referência/arte que efetivamente sobrescreve o mod. | resourcepacks/minhas-texturas; gen_porta_grade.py |
| Prateleira | UV da etiqueta recortaY0..2, mas linhas do desenho estão emY4/5 eY10/11. | Mapear a área da etiqueta que contém a arte. | gen_prateleira.py |

Caminhos de modelos relativos a src/main/resources/assets/intoxicantes. Classes em src/main/java/com/intoxicantes; geradores em tools.

## Artes antigas que pedem revisão

- Guia e Central de Comando: sprites16×16; o livro/controle podem representar melhor o couro, costura/rebites e a identidade eletrônica das respectivas interfaces.
- Cigarro Camel, camisa Matanza e ovo do Juça: sprites16×16 antigos. O ovo do Juça também destoa da família dos ovos de Gago/Traficante.
- Malte e cevada: mesma forma de feixe/espiga, diferenciada principalmente por cor; malte deve ter identidade de ingrediente processado.
- Café verde, lúpulo e cana: imagens realistas planas. Revisar a linguagem em conjunto com as plantas, mantendo a forma própria de cada produto.
- Extrato de cafeína e Névoa do Deserto: frasco/garrafa representados como sprites realistas, diferentes da linguagem dos recipientes3D atuais.
- Demais consumíveis realistas na folha de contato: baseado, pó branco, cogumelo, cristal, heroína, LSD, maconha/seda, ópio, pó estelar e raiz de sombra. É um grupo para revisão artística, não prova de bug nem obrigação de converter tudo em3D.
- LEDs recentes: item e bloco são coerentes entre si, mas várias potências compartilham formas muito simples e mudam quase só o tom; revisar silhueta/detalhes/identificação da potência como trabalho próprio. Atlas16×16 em modelo3D não foi tratado automaticamente como erro, pois AGENTS permite resolução adequada à geometria.

## Recursos já coerentes

As oito bebidas prontas colocadas usam o mesmo modelo do item, com atlas128; os seis saquinhos3D estão íntegros e sem override. As cinco máquinas de bebidas usam a geometria completa no inventário e as partes correspondentes no mundo, com atlas128. Notas de dinheiro já têm identidade SNC própria; papel pode continuar2D.

A última leitura encontrou113definitions e221modelos sem referência de modelo ausente. A folha catalogo-texturas.png mostra45modelos do tipo sprite no levantamento inicial; inclui6arquivos antigos *_plant que não são BlockItems registrados, portanto eles não contam como45itens visíveis no inventário. Os quatro novos componentes elétricos apareceram durante a revisão.

## Prioridade sugerida

Começar pela uva, usando uma única fonte para o cacho do mundo e o item. Depois corrigir o coco e o override da porta, que já têm diferenças diretamente demonstráveis. Não houve alteração de recursos de produção, execução de geradores nem build nesta auditoria. A folha de contato e este relatório são artefatos de inspeção; não são propostas aprovadas de novas artes.


## Correções propostas a partir desta auditoria

Estado em01/10/2026: fontes implementadas; aprovação visual pendente. A prévia `identidade.html` reúne47entradas para46IDs (a porta aparece colocada e como ícone). Os dados vêm de `preview_catalogo.py`, importando as funções puras dos geradores; não são assets gerados para o jogo.

| Pontos do relatório | Fonte da correção | Proposta na prévia |
|---|---|---|
| Uva, UV e poste | gen_farm_resources.py; ParreiraRenderer.java; LampadaUvBlock.java | Cacho nativo único; luminária de dois tubos ligada/apagada; miniatura completa do poste |
| Coco pendurado/coletado | gen_garrafas.py; CocoBlock.java | Mesmo fruto e atlas, com apoio e seleção alinhados |
| Porta e override do pack | gen_porta_grade.py | Grade na parte superior do ícone e seisPNG sincronizados com o pack |
| Prateleira e etiqueta | gen_prateleira.py | Materiais128 e UV na arte correta da etiqueta |
| Guia, Central, camisa, cigarro e ovos | gen_catalogo.py | Modelos detalhados; três ovos com casco comum e emblemas próprios |
| Café, lúpulo, cana, malte/cevada e bagaço | gen_catalogo.py | Formas próprias; malte processado distinto de espigas |
| Doze consumíveis e dois frascos citados | gen_catalogo.py | Catálogo voxel, atlas128 e recipientes artesanais |
| LEDs simples/potências | gen_lampada_led.py; gen_lampada_led_potencia.py; LedPotenciaBlock.java | Tubo, bulbos, refletores e campânulas com geometria, proporção e potência próprias |
| Quatro componentes elétricos | gen_eletrica.py | Bobina, soquete, interruptor e quadro com materiais128 e detalhes |
| Próximas regenerações | gen_textures.py; gen_farm_textures.py; gen_texturas_v1258.py; gen_texturas_saude.py; gen_npc_textures.py; gen_bebidas.py | Fontes antigas respeitam os donos novos; gerador de ópio antigo absorvido/removido |

Guia da parreira e changelog registram a proposta. A revisão da implementação corrigiu a extração acidental de um trecho do gerador agrícola, o chanfro de capas finas que impedia montar o estúdio, os recortesUV do poste antes da escala e a largura indevida da prévia em tela pequena. A mudança paralela das partículas do coco foi preservada e incorporada ao manifesto/estúdio.

Pendente após aprovação: gerar somente recursos da correção, conferir o pack ativo, compilar com Java25, validar modelos/referências/poses/colheita/UV/energia no Minecraft e instalar o JAR. As imagens de estúdio não comprovam o resultado no cliente do jogo. Armas, bebidas, máquinas, dinheiro e saquinhos aprovados foram preservados.


## Conferência da prévia

Os47seletores foram visitados no navegador real sem erro de construção. Busca/famílias, vistas de frente/verso/inventário/mão/mundo, maturação da uva e UV apagada foram conferidas; capa do Guia, porta/ícone e etiqueta também foram inspecionadas. A tela pequena passou a caber sem rolagem horizontal do documento após correção do grid. A prévia também usa os recortesUV implícitos do Minecraft quando o JSON não informaUV. Console da última carga: nenhum erro; o fornecedor Three legado do estúdio ainda emite aviso de depreciação não bloqueante.

Todos os25itens do catálogo têm partícula128procedural própria, sem letras de rótulo. A inspeção de duplicação das fontes apontou quatro marcadores históricos (três geradores antigos versionados e uma mensagem de revisão da escopeta); não são novos remendos desta tarefa. As guardas foram aplicadas nas fontes antigas; a arte exclusiva do ópio agora pertence ao catálogo.

Esta conferência é da proposta visual. Não houve build, instalação nem testes do jogo desta correção, conforme o fluxo de aprovação.
