# Regras de trabalho — Intoxicantes Mod

Estas instruções se aplicam a todo o projeto. Instruções explícitas do usuário para a tarefa atual têm prioridade. Converse em português brasileiro, com explicações claras e sem excesso de termos técnicos.

## Regras fundamentais do usuário

- Mantenha-se informado sobre a documentação atual das linguagens, ferramentas e APIs utilizadas. Confirme a compatibilidade com as versões efetivamente usadas pelo projeto; não atualize dependências automaticamente só porque existe uma versão mais recente.
- Quando achar que terminou, volte ao início do pedido e revise todo o fluxo, inclusive os erros surgidos durante o trabalho. Não declare conclusão com falhas conhecidas escondidas ou testes relevantes pendentes.
- Termine a melhoria priorizada antes de iniciar outras. Preserve o estilo e os comportamentos que já funcionam.

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
