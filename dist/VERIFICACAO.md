# Verificação da entrega 1.2.5 — 20/09/2026

- Compilação final: aprovada.
- Servidor: todos os 10 testes obrigatórios passaram.
- Cliente Minecraft 26.3 real: abertura por clique direito, compra de cerveja, venda de 12 lotes, consumo exato, bloqueio da cota esgotada e fechamento com Escape aprovados.
- Interface conferida em 1280×800 e 640×480.
- Recursos: 51 JSONs e estruturas NBT validados.
- JAR final contém o mod; os testes ficam somente no projeto-fonte.
- Arquivos e melhorias do outro app preservados.

## Erros revisados

Corrigidos os códigos SDL dos botões, a compatibilidade dos pacotes Fabric, as tabelas de colheita do Minecraft 26.3 e a preparação de carregamento do jogador simulado no teste da escopeta. A revisão visual também corrigiu a sobreposição da barra de fidelidade sobre o contador de compras.

Os avisos de contadores de desempenho do Windows e da autenticação Realms no cliente de teste não impediram os testes. Não foram alteradas configurações do Windows.

Os testes usaram mundos isolados com Fabric e o mod. O mundo do usuário e a compatibilidade com todos os demais mods da instância não foram testados.

Log completo: ../build/commerce-final.log
