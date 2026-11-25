# 1. Especificação de requisitos

Sistema **Translog**: gestão de entregas de uma transportadora (enunciado S2a, Logística de Transporte).

Cada requisito tem um código. Esses códigos aparecem nos comentários do código-fonte (ex.: `RN07`), nos casos de uso e no plano de testes. Assim dá para seguir uma regra do enunciado até a classe que a implementa e o teste que a confere.

- **RF** = requisito funcional (o que o sistema faz)
- **RN** = regra de negócio (a condição ou o cálculo que precisa ser respeitado)
- **RNF** = requisito não funcional (como o sistema deve ser)

---

## 1.1 Requisitos funcionais

| Código | Requisito | Menu | Classe principal |
|---|---|---|---|
| RF01 | Cadastrar, listar, pesquisar, editar, inativar e remover clientes empresariais e prioritários | Clientes | `ClienteServico`, `MenuClientes` |
| RF02 | Alterar o tipo do cliente (empresarial ↔ prioritário) | Clientes | `ClienteServico.alterarTipo` |
| RF03 | Registrar contrato de frete com prazo padrão, frete mínimo, condições e vigência | Clientes | `ClienteServico.registrarContrato` |
| RF04 | Cadastrar, listar, editar, inativar e remover motoristas (CPF, CNH, categoria, validade) | Motoristas | `FrotaServico`, `MenuMotoristas` |
| RF05 | Cadastrar, listar, editar, inativar e remover veículos (placa, tipo, capacidade, ano) | Veículos | `FrotaServico`, `MenuVeiculos` |
| RF06 | Simular o frete (orçamento) sem agendar | Entregas | `EntregaServico.simular` |
| RF07 | Agendar entrega informando carga, rota, data de saída, motorista, veículo e urgência | Entregas | `EntregaServico.agendar` |
| RF08 | Registrar a rota como sequência de cidades com a distância total | Entregas | `Rota` |
| RF09 | Iniciar o transporte (status "Em trânsito") | Entregas | `EntregaServico.iniciarTransporte` |
| RF10 | Concluir a entrega, calcular o atraso e emitir a nota fiscal | Entregas | `EntregaServico.concluir` |
| RF11 | Cancelar entrega com motivo | Entregas | `EntregaServico.cancelar` |
| RF12 | Reagendar entrega ainda não iniciada | Entregas | `EntregaServico.reagendar` |
| RF13 | Listar entregas por prioridade, por status e as atrasadas, e mostrar detalhes com histórico | Entregas | `EntregaServico`, `MenuEntregas` |
| RF14 | Consultar a agenda de um motorista e de um veículo | Motoristas / Veículos | `AgendaServico` |
| RF15 | Listar e consultar notas fiscais | Faturamento | `FaturamentoServico` |
| RF16 | Relatório de faturamento por cliente, na tela | Faturamento | `FaturamentoServico.resumoDoCliente` |
| RF17 | Gerar PDFs: nota fiscal, faturamento geral, faturamento de um cliente, relatório de entregas | Faturamento / Entregas | pacote `relatorio` |
| RF18 | Painel geral: entregas por status, faturamento do mês, atrasadas, próximas saídas, CNHs vencendo | Principal | `EntregaServico.painel` |
| RF19 | Carregar um cenário de demonstração | Principal | `DadosDemonstracao` |
| RF20 | Salvar e carregar todos os dados em arquivos | (automático) | `RepositorioJson` |

---

## 1.2 Regras de negócio

### Clientes e contratos

| Código | Regra | Onde está |
|---|---|---|
| RN01 | Cliente **empresarial** tem 10% de desconto em entregas **regulares** | `ClienteEmpresarial.percentualDesconto` |
| RN02 | Cliente **prioritário** tem 20% de desconto em **qualquer** entrega | `ClientePrioritario.percentualDesconto` |
| RN03 | Só cliente prioritário pode pedir entrega **urgente** | `Cliente.podeSolicitarUrgencia`, `EntregaServico.verificarUrgencia` |
| RN04 | Para agendar, o cliente precisa ter **contrato vigente** na data de saída | `Contrato.isVigenteEm`, `EntregaServico.contratoVigente` |
| RN05 | Cada cliente tem **um contrato ativo** por vez; registrar outro encerra o anterior | `ClienteServico.registrarContrato` |
| RN06 | Cliente **inativo** não pode agendar entregas | `EntregaServico.agendar` |

### Carga e preço

| Código | Regra | Onde está |
|---|---|---|
| RN07 | A carga é classificada pelo peso: **leve** até 500 kg, **média** até 3.000 kg, **pesada** acima de 3.000 kg | `TipoCarga.classificar` |
| RN08 | Valor por km: leve R$ 1,50, média R$ 2,50, pesada R$ 4,00 | `TipoCarga` |
| RN09 | Carga **frágil ou perigosa** tem adicional de **40%** sobre a base | `CalculadoraFrete` |
| RN10 | Entrega **urgente** tem adicional de **30%** sobre a base | `CalculadoraFrete` |
| RN11 | Se o frete ficar abaixo do **frete mínimo** do contrato, ele sobe para o mínimo | `CalculadoraFrete` |
| RN12 | Valores em dinheiro têm 2 casas decimais, com arredondamento HALF_UP | `CalculadoraFrete.dinheiro` |

### Motoristas e veículos

| Código | Regra | Onde está |
|---|---|---|
| RN13 | Carga **pesada** exige CNH **C ou superior** | `TipoCarga.getCnhMinima`, `EntregaServico.verificarMotoristaEVeiculo` |
| RN14 | A CNH precisa habilitar o **tipo de veículo** (utilitário e van: B; caminhões: C; carreta: E) | `TipoVeiculo`, `CategoriaCnh.habilita` |
| RN15 | A CNH precisa estar **válida** na data da entrega | `Motorista.isCnhValidaEm` |
| RN16 | O peso da carga **não pode passar** da capacidade do veículo | `Veiculo.suporta` |
| RN17 | Motorista ou veículo com entregas pendentes **não pode ser inativado** | `FrotaServico.alterarSituacao*` |

### Agenda e status

| Código | Regra | Onde está |
|---|---|---|
| RN18 | Uma entrega reserva motorista e veículo do dia de saída até o fim estimado da viagem: `dias = arredondar para cima(km ÷ 500)`, mínimo 1 | `Rota.getDiasEstimados`, `Entrega.getFimOcupacao` |
| RN19 | Não pode haver **sobreposição** de reservas do mesmo motorista ou do mesmo veículo. Em caso de conflito, o sistema sugere a próxima data livre | `AgendaServico` |
| RN20 | Transições permitidas: Agendada → Em trânsito → Entregue; Agendada ou Em trânsito → Cancelada | `StatusEntrega.podeMudarPara` |
| RN21 | Toda mudança de status fica no **histórico**, com data, hora e observação | `Entrega.mudarStatus` |
| RN22 | A data de saída não pode estar no passado. O transporte não pode começar antes da data agendada. A data de entrega não pode ser futura nem anterior à saída | `EntregaServico` |
| RN23 | Na listagem, as **urgentes pendentes** aparecem primeiro, depois as demais por data | `EntregaServico.listarPorPrioridade` |

### Prazo, multa e faturamento

| Código | Regra | Onde está |
|---|---|---|
| RN24 | Prazo limite = data de saída + prazo do contrato (em dias) | `Entrega.getDataLimite` |
| RN25 | Atraso: abatimento de **5% do frete por dia**, limitado a **25%** | `CalculadoraFrete.aplicarPenalidade` |
| RN26 | A nota fiscal é emitida **automaticamente** ao concluir a entrega, e cada entrega tem **uma única** nota | `FaturamentoServico.emitirNota` |
| RN27 | A nota guarda uma cópia dos dados do cliente no momento da emissão | `NotaFiscal` |

### Cadastros

| Código | Regra | Onde está |
|---|---|---|
| RN28 | CPF e CNPJ são validados pelos dígitos verificadores. A placa segue o padrão antigo (ABC1234) ou Mercosul (ABC1D23) | `ValidadorDocumento` |
| RN29 | Não pode haver dois cadastros com o mesmo CNPJ, CPF, número de CNH ou placa | `ClienteServico`, `FrotaServico` |
| RN30 | Cadastro com entregas registradas **não pode ser removido**, só inativado (para preservar notas e histórico) | `ClienteServico.remover`, `FrotaServico.remover*` |

---

## 1.3 Requisitos não funcionais

| Código | Requisito | Como foi atendido |
|---|---|---|
| RNF01 | Java 17 e Maven | `pom.xml` com `maven.compiler.release` 17 |
| RNF02 | Interface de console clara, com menus numerados | pacote `ui` |
| RNF03 | Persistência em arquivo (CSV, JSON ou TXT) | JSON, um arquivo por entidade, salvo a cada operação |
| RNF04 | O sistema não pode quebrar com entradas inválidas nem com arquivos ausentes | `Console` repete a pergunta; `Menu` captura as exceções; `RepositorioJson` trata arquivo ausente ou corrompido |
| RNF05 | A gravação não pode deixar o arquivo pela metade | escrita em arquivo `.tmp` seguida de `move` |
| RNF06 | Código organizado em camadas, com baixo acoplamento | serviços dependem da interface `Repositorio` e recebem tudo pelo construtor |
| RNF07 | Testes automatizados das regras de negócio | 32 testes JUnit 5 |
| RNF08 | Pasta de dados configurável (desafio do Google Drive) | `--dados=` ou variável `TRANSLOG_DADOS` |
| RNF09 | Relatórios em PDF | Apache PDFBox |
| RNF10 | README com instruções de execução | `README.md` |

---

## 1.4 Premissas (o que o enunciado deixou em aberto)

| Ponto em aberto | Decisão | Motivo |
|---|---|---|
| Como definir se a carga é leve, média ou pesada | Pelo peso (RN07) | Evita que alguém cadastre 5 toneladas como "leve" para pagar menos |
| Como somar os adicionais | Os dois sobre o valor base | O cliente confere a conta com uma soma simples |
| De onde contar o prazo do contrato | Da data de saída agendada | É a data que a transportadora controla |
| Sobre qual valor aplicar a multa | Sobre o frete já com desconto | É o valor que o cliente efetivamente paga |
| O que são "condições de frete" | Frete mínimo (calculado) e texto livre de observações | Dá uma regra concreta que entra no cálculo |
| Duração da viagem | 500 km por dia | Média razoável para caminhão em rodovia, com descanso |
