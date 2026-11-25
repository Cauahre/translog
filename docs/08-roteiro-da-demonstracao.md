# 8. Roteiro da demonstração

Roteiro para a parte 6.3 da apresentação (cerca de 6 minutos). O **Ítalo opera** o sistema e o **Cauã explica** a regra de cada passo. Este roteiro foi executado do começo ao fim, e os resultados abaixo são os que o sistema mostra.

## Antes de começar

1. **Apague a pasta `dados`** (ou rode com `--dados=demo`). As datas da demonstração são calculadas a partir do dia em que ela é carregada. Por isso, ela deve ser carregada **no próprio dia da apresentação**.
2. Abra o terminal em tela cheia, com a fonte grande.
3. No Windows, rode `chcp 65001` antes, para os acentos aparecerem certos.
4. Rode `java -jar target/translog.jar`.

Nas tabelas abaixo, `Enter` significa apertar Enter sem digitar nada (o sistema usa o valor padrão mostrado entre colchetes).

---

## Passo 1 – Carregar o cenário e mostrar o painel (1 min)

| Digitar | O que aparece | O que explicar |
|---|---|---|
| `7`, `Enter` | "Dados de demonstração carregados: 4 clientes, 4 motoristas, 5 veículos e 9 entregas." | Os dados passaram pelos mesmos serviços e validações do uso normal |
| `1`, `Enter` | Painel: 3 agendadas, 1 em trânsito, 4 entregues, 1 cancelada; entrega #5 com prazo vencido; saídas da semana com a #7 marcada URG; CNH do Antônio vencendo | RF18: o painel junta as informações que o gerente olha primeiro |

## Passo 2 – Mostrar que entrada errada não quebra (15 s)

| Digitar | O que aparece | O que explicar |
|---|---|---|
| `abc` | "[ERRO] Digite apenas números inteiros." e a pergunta se repete | RNF04: toda leitura passa pela classe `Console` |
| `5` | Abre o menu Entregas, com o aviso "1 entrega(s) pendente(s) com prazo vencido" | |

## Passo 3 – Listagem por prioridade e entrega com multa (1 min)

| Digitar | O que aparece | O que explicar |
|---|---|---|
| `3`, `Enter` | Lista com a #7 (URG) no topo e a #5 marcada "(atrasada)" | RN23: urgentes pendentes primeiro |
| `5`, `3`, `Enter` | Memória de cálculo: 560 km × R$ 4,00 = R$ 2.240,00; desconto 10%; frete R$ 2.016,00; abatimento de 3 dias (15%) = R$ 302,40; **valor final R$ 1.713,60**. E o histórico com data e hora de cada status | RN08, RN01, RN25 e RN21 |

## Passo 4 – Agendamento recusado pela CNH (1 min)

| Digitar | Pergunta | O que explicar |
|---|---|---|
| `2` | Agendar entrega | |
| `3` | Cliente: Construmax (empresarial) | Mostra o contrato: prazo 7 dias, frete mínimo R$ 500,00 |
| `Vigas de aço` | Descrição | |
| `8000` | Peso | Classificação automática: **carga pesada, exige CNH C** (RN07, RN13) |
| `1` | Natureza: comum | |
| `Teresina; Timon` | Cidades | |
| `15` | Distância | Não pergunta urgência: cliente empresarial (RN03) |
| `Enter` | Data de saída: amanhã | |
| `3` | Motorista: Antônio | Ele **não aparece** na lista de aptos; digitamos de propósito |
| `5` | Veículo: Volvo (truck) | |
| `s` | Confirmar | Antes, a prévia mostra o frete subindo para o **mínimo de R$ 500,00** (RN11) |

**Resultado:** "[ERRO] O veículo Caminhão truck exige CNH C e o motorista tem CNH B." (RN14). `Enter` para continuar.

## Passo 5 – Conflito de agenda com sugestão de data (1 min)

| Digitar | Pergunta |
|---|---|
| `2` | Agendar entrega |
| `1` | Cliente: Distribuidora Piauí |
| `Arroz` | Descrição |
| `4000` | Peso (pesada) |
| `1` | Natureza: comum |
| `Teresina; Picos` | Cidades |
| `310` | Distância |
| data de **depois de amanhã** (dd/mm/aaaa) | Data de saída |
| `2` | Motorista: Francisca |
| `3` | Veículo: VW Delivery |
| `s` | Confirmar |

**Resultado:** "[ERRO] O motorista Francisca Alves Lima já está reservado para a entrega #9 de (data) a (data). Próxima data livre para os dois: (dia seguinte)." `Enter`.

**Explicar:** RN18 e RN19. A entrega #9 reserva a Francisca, e o sistema procura sozinho a próxima data livre para o motorista **e** o veículo.

## Passo 6 – Agendamento urgente com sucesso (1 min)

| Digitar | Pergunta | O que explicar |
|---|---|---|
| `2` | Agendar entrega | |
| `2` | Cliente: Farmácia (prioritário) | |
| `Soro fisiológico` | Descrição | |
| `200` | Peso | Carga leve |
| `2` | Natureza: frágil | +40% |
| `Teresina; Campo Maior` | Cidades | |
| `85` | Distância | |
| `s` | Urgente? | Só aparece para prioritário; +30% |
| `Enter` | Data: amanhã | |
| `3` | Motorista: Antônio (CNH B serve para carga leve) | |
| `1` | Veículo: Fiorino | |
| `s` | Confirmar | Prévia: 85 × 1,50 = 127,50 + 51,00 + 38,25 − 20% = **R$ 173,40** |

**Resultado:** "[OK] Entrega #10 agendada para (amanhã) - valor R$ 173,40." `Enter`.

## Passo 7 – Regra de status (15 s)

| Digitar | Resultado | O que explicar |
|---|---|---|
| `6`, `10`, `Enter` | "[ERRO] A saída está agendada para (amanhã). Para sair antes, reagende a entrega." | RN22 |
| `Enter` | | |

## Passo 8 – Concluir com atraso e emitir a nota (1 min)

| Digitar | Pergunta / resultado |
|---|---|
| `7` | Concluir entrega |
| `5` | Entrega #5 (em trânsito, prazo vencido ontem) |
| `Enter` | Data da entrega: hoje |
| `Recebido pelo gerente` | Observação |
| — | "[ATENÇÃO] Entrega com 1 dia(s) de atraso: abatimento de 5% (R$ 61,20)." e "Nota fiscal nº 000005 emitida no valor de R$ 1.162,80." |
| `s` | Gerar o PDF da nota → caminho do arquivo |
| `Enter` | |

**Abrir o PDF** em `dados/relatorios/nota-fiscal-000005.pdf` e mostrar a composição do valor. **Explicar:** RN24, RN25 e RN26 (nota automática e única por entrega).

## Passo 9 – Faturamento (45 s)

| Digitar | Resultado |
|---|---|
| `0` | Volta ao menu principal |
| `6` | Faturamento e notas fiscais |
| `4`, `1`, `Enter`, `Enter` | Faturamento da Distribuidora: 2 notas, serviços R$ 2.020,00, descontos R$ 202,00, abatimentos R$ 61,20, **total R$ 1.756,80**, ticket médio R$ 878,40 |
| `Enter` | |
| `5`, `Enter`, `Enter` | PDF do faturamento de todos os clientes |

**Abrir o PDF** do faturamento e mostrar o consolidado. Depois, `Enter`, `0`, `0` para sair.

---

## Se o professor pedir algo fora do roteiro

| Pedido | Onde |
|---|---|
| Cadastrar um cliente | Menu 2 → 1. CNPJ válido para teste: `11.222.333/0001-81` |
| Cadastrar um motorista | Menu 3 → 1. CPF válido para teste: `529.982.247-25` |
| Mostrar um CNPJ inválido | `11.111.111/1111-11` |
| Mostrar a agenda de um motorista | Menu 3 → 4 → código |
| Cancelar e mostrar que a agenda libera | Entregas → 8 → 9, depois agende a Francisca na mesma data |
| Mostrar o JSON | Abrir `dados/clientes.json` (campo `"tipo"`) |
| Rodar os testes | `mvn test` |
