# Translog - Sistema de Logística de Transporte

Trabalho da P2 de Programação Orientada a Objetos (iCEV - Engenharia de Software).

Sistema de console em Java para a transportadora fictícia **Translog**, que atende clientes empresariais com entregas em várias cidades. O sistema cadastra clientes, contratos, motoristas e veículos. Também agenda entregas sem deixar motorista ou veículo com duas reservas no mesmo dia, calcula o frete, acompanha o status de cada entrega, aplica a multa por atraso e emite notas fiscais e relatórios em PDF.

**Equipe**
- Cauã Henrique da Silva Oliveira – RA 0031305
- Ítalo de Sousa Amorim – RA 0031320

---

## Documentação

A especificação completa fica na pasta [`docs/`](docs):

| Documento | Conteúdo |
|---|---|
| [01 – Especificação de requisitos](docs/01-especificacao-de-requisitos.md) | Requisitos funcionais (RF), regras de negócio (RN), não funcionais (RNF) e premissas |
| [02 – Arquitetura e diagramas](docs/02-arquitetura-e-diagramas.md) | Camadas, diagrama de classes, estados da entrega e sequência do agendamento |
| [03 – Regras de cálculo](docs/03-regras-de-calculo.md) | Fórmula do frete, multa e agenda, com exemplos resolvidos |
| [04 – Casos de uso](docs/04-casos-de-uso.md) | Fluxos principais e alternativos |
| [05 – Plano de testes](docs/05-plano-de-testes.md) | Os 32 testes automatizados e os testes manuais de robustez |
| [06 – Divisão do trabalho](docs/06-divisao-do-trabalho.md) | Responsabilidades, divisão da apresentação e ordem dos commits |
| [07 – Guia de estudo](docs/07-guia-de-estudo-defesa.md) | Perguntas prováveis da defesa oral, com respostas |
| [08 – Roteiro da demonstração](docs/08-roteiro-da-demonstracao.md) | Passo a passo testado para a apresentação |

Os códigos das regras (ex.: `RN25`) aparecem nos comentários do código-fonte. Assim dá para ir da regra do enunciado à classe que a implementa e ao teste que a confere.

---

## Requisitos

| Ferramenta | Versão |
|---|---|
| JDK | 17 ou superior |
| Maven | 3.8 ou superior |

As dependências (Gson, PDFBox e JUnit) são baixadas pelo Maven na primeira compilação, então essa primeira vez precisa de internet.

## Como executar

```bash
# 1. clonar o repositório
git clone <url-do-repositorio>
cd translog

# 2. compilar, rodar os testes e gerar o jar
mvn package

# 3. executar
java -jar target/translog.jar
```

Outra opção, sem gerar o jar:

```bash
mvn compile exec:java
```

Para rodar só os testes: `mvn test`.

No IntelliJ/Eclipse/VS Code, basta importar como projeto Maven e executar a classe `br.com.translog.App`.

> **Acentos no Windows:** se aparecerem caracteres estranhos no lugar dos acentos, rode `chcp 65001` no terminal antes de executar o programa.

## Primeiro uso

Na primeira execução o sistema começa vazio. A opção **7 - Carregar dados de demonstração** do menu principal cria um cenário completo:

- 4 clientes (2 empresariais e 2 prioritários), cada um com seu contrato;
- 4 motoristas com CNH B, C, D e E (um deles com a CNH perto de vencer);
- 5 veículos (utilitário, van, caminhão toco, caminhão truck e carreta);
- 9 entregas: concluídas no prazo, uma concluída com 3 dias de atraso (com multa), uma cancelada, uma em trânsito já fora do prazo e três agendadas para os próximos dias.

As datas do cenário são calculadas a partir do dia em que ele é carregado. Assim, o painel e os alertas sempre mostram uma situação atual.

## Onde ficam os dados

Os dados são gravados em arquivos JSON na pasta `dados/`, criada no diretório de onde o programa é executado:

```
dados/
├── clientes.json
├── contratos.json
├── motoristas.json
├── veiculos.json
├── entregas.json
├── notas-fiscais.json
└── relatorios/          <- PDFs gerados (notas e relatórios)
```

Os arquivos são salvos automaticamente depois de cada operação.

Para usar outra pasta:

```bash
java -jar target/translog.jar --dados=C:\caminho\da\pasta
# ou pela variável de ambiente TRANSLOG_DADOS
```

**Google Drive (desafio):** com o Google Drive para computador instalado, basta apontar a pasta de dados para dentro da pasta sincronizada. Assim os arquivos sobem para o Drive sozinhos:

```bash
java -jar target/translog.jar --dados="G:\Meu Drive\Translog"
```

## Funcionalidades

**Cadastros**
- Clientes empresariais e prioritários com validação de CNPJ (dígitos verificadores), telefone e e-mail. Também é possível trocar o tipo do cliente.
- Contratos de frete com prazo padrão de entrega, frete mínimo, outras condições e vigência. Ao registrar um contrato novo, o anterior é encerrado.
- Motoristas com validação de CPF, número e categoria da CNH e validade da CNH.
- Veículos com tipo, capacidade em kg, ano e placa (padrão antigo ou Mercosul).
- Edição, ativação/inativação e remoção. Registros com histórico de entregas não podem ser removidos, só inativados, para não perder as notas fiscais.

**Entregas**
- Simulação de frete (orçamento) sem agendar nada.
- Agendamento com todas as verificações:
  - o cliente precisa ter contrato vigente na data;
  - só cliente prioritário pode pedir entrega urgente;
  - a CNH precisa ser compatível com o veículo e com a carga, e estar válida na data;
  - o peso não pode passar da capacidade do veículo;
  - a agenda do motorista e do veículo precisa estar livre.
- No agendamento, o sistema lista só os motoristas e veículos aptos e livres no período.
- Quando há conflito de agenda, o sistema informa qual entrega ocupa a data e sugere a próxima data livre.
- A rota é registrada como uma sequência de cidades. A reserva da agenda usa a duração estimada da viagem (500 km por dia).
- Status: agendada → em trânsito → entregue, ou cancelada. O sistema bloqueia transições inválidas, e cada mudança fica num histórico com data e hora.
- Reagendamento com nova verificação da agenda.
- A listagem segue a ordem de prioridade: as urgentes pendentes aparecem primeiro.
- Alerta das entregas pendentes com prazo vencido.

**Faturamento**
- A nota fiscal é emitida automaticamente ao concluir a entrega, com a composição completa do valor.
- O faturamento por cliente pode ser visto na tela, com total, descontos, abatimentos e ticket médio.
- PDFs: nota fiscal, faturamento de todos os clientes, faturamento de um cliente e relatório operacional de entregas.

**Outros**
- Painel geral com as entregas por status, o faturamento do mês, as atrasadas, as saídas da semana e as CNHs vencendo.
- Dados de demonstração para a apresentação.

## Regras de cálculo

| Item | Regra |
|---|---|
| Carga leve | até 500 kg - R$ 1,50/km - CNH B |
| Carga média | até 3.000 kg - R$ 2,50/km - CNH B |
| Carga pesada | acima de 3.000 kg - R$ 4,00/km - CNH C ou superior |
| Carga frágil ou perigosa | + 40% sobre a base |
| Entrega urgente (só prioritário) | + 30% sobre a base |
| Cliente empresarial | 10% de desconto em entrega regular |
| Cliente prioritário | 20% de desconto em qualquer entrega |
| Frete mínimo do contrato | se o frete ficar abaixo, sobe para o mínimo |
| Atraso | 5% do frete por dia, limitado a 25% |

Exemplo: 300 km de carga pesada e perigosa, urgente, para cliente prioritário:
`1.200,00 + 480,00 (40%) + 360,00 (30%) = 2.040,00 - 20% = R$ 1.632,00`

**Decisões que o enunciado deixou em aberto:**
- O tipo da carga (leve/média/pesada) é definido pelo peso, e não escolhido pelo usuário. Assim ninguém cadastra 5 toneladas como "leve" para pagar menos.
- Os adicionais de 40% e 30% são calculados sobre o valor base, e não um em cima do outro.
- O prazo do contrato é contado a partir da data de saída agendada.
- A multa por atraso é aplicada sobre o frete já com desconto.
- A categoria A (moto) não habilita nenhum veículo da frota. Da B em diante, a categoria maior habilita tudo o que as menores habilitam.

## Estrutura do projeto

```
translog/
├── pom.xml
├── README.md
├── docs/                    especificação, diagramas, testes, divisão e roteiro
└── src/
    ├── main/java/br/com/translog   (58 classes)
    └── test/java/br/com/translog   (5 classes, 32 testes)
```

```
src/main/java/br/com/translog
├── App.java                 ponto de entrada
├── Sistema.java             monta repositórios e serviços
├── DadosDemonstracao.java   cenário para a apresentação
├── modelo/                  entidades e enums (Cliente, Motorista, Entrega, ...)
├── servico/                 regras de negócio (cálculo, agenda, entregas, faturamento)
├── persistencia/            repositórios (memória e JSON) e configuração do Gson
├── relatorio/               geração dos PDFs
├── ui/                      console e menus
├── excecao/                 exceções do sistema
└── util/                    validações e formatação
src/test/java/...            testes JUnit 5
```

### Onde está cada pilar de POO

- **Abstração**
  - `Pessoa` e `Cliente` são classes abstratas.
  - `Repositorio<T>` é uma interface; os serviços dependem dela e não do JSON.
  - `DocumentoPdf` e `Menu` definem o esqueleto que as subclasses completam.
- **Herança**
  - `Pessoa` → `Cliente` → `ClienteEmpresarial` / `ClientePrioritario`.
  - `Pessoa` → `Motorista`.
  - `RepositorioMemoria` → `RepositorioJson`.
  - `DocumentoPdf` → `NotaFiscalPdf` / `FaturamentoPdf` / `EntregasPdf`.
  - `Menu` → os seis menus.
  - `TranslogException` → as exceções específicas.
- **Polimorfismo**
  - A `CalculadoraFrete` chama `cliente.percentualDesconto(urgente)` e `podeSolicitarUrgencia()` sem saber o tipo do cliente.
  - `Menu.abrir()` chama `executar(opcao)` de cada menu.
  - `DocumentoPdf.gerar()` chama `escreverCorpo()` de cada documento.
- **Encapsulamento**
  - Atributos privados com validação nos setters: CPF, CNPJ, placa, capacidade, prazo etc.
  - A mudança de status só acontece por `Entrega.mudarStatus()`, que verifica se a transição é permitida.
  - O histórico é exposto como lista somente leitura.

## Tratamento de erros

- Entradas inválidas (letras no lugar de números, datas erradas, campos vazios) são pedidas de novo. O programa não fecha.
- Erros de regra de negócio mostram uma mensagem clara e o menu continua.
- Se um arquivo de dados não existe, o sistema começa vazio e cria o arquivo no primeiro salvamento.
- Se um arquivo JSON estiver corrompido, ele é renomeado para `*.corrompido-DATA`, o usuário é avisado e o sistema continua funcionando.
- A gravação usa um arquivo temporário e depois o substitui. Se o programa cair no meio, o JSON anterior continua íntegro.
- Se a entrada for encerrada (Ctrl+D / Ctrl+Z), o programa fecha sem erro, e os dados já estão salvos.

## Bibliotecas

| Biblioteca | Uso | Motivo |
|---|---|---|
| Gson 2.10.1 | ler/gravar JSON | simples, sem configuração; permite adaptadores para `LocalDate` e para a herança de `Cliente` |
| Apache PDFBox 2.0.30 | gerar os PDFs | licença Apache (livre), controle total do layout e boa documentação |
| JUnit 5 | testes automatizados | padrão atual para testes em Java |
