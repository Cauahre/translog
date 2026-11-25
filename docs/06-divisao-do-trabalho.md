# 6. Divisão do trabalho

| Aluno | RA |
|---|---|
| Cauã Henrique da Silva Oliveira | 0031305 |
| Ítalo de Sousa Amorim | 0031320 |

O trabalho foi dividido **por camadas**, com a mesma carga para os dois: **29 classes de produção para cada um**, do total de 58.

## 6.1 Responsabilidades por arquivo

### Cauã – núcleo do domínio e regras de negócio

| Pacote / arquivo | Classes | Conteúdo |
|---|---|---|
| `modelo/` | 20 | `Identificavel`, `Pessoa`, `Cliente`, `ClienteEmpresarial`, `ClientePrioritario`, `TipoCliente`, `Motorista`, `CategoriaCnh`, `Veiculo`, `TipoVeiculo`, `Contrato`, `Carga`, `TipoCarga`, `NaturezaCarga`, `Rota`, `Entrega`, `StatusEntrega`, `RegistroStatus`, `DetalheFrete`, `NotaFiscal` |
| `servico/` | 5 | `CalculadoraFrete`, `AgendaServico`, `EntregaServico`, `SolicitacaoEntrega`, `PainelResumo` |
| `persistencia/` | 4 | `Repositorio`, `RepositorioMemoria`, `RepositorioJson`, `GsonFactory` |
| testes | 3 classes, 21 testes | `CalculadoraFreteTest`, `EntregaServicoTest`, `RepositorioJsonTest` |
| docs | 3 | `01-especificacao-de-requisitos.md`, `02-arquitetura-e-diagramas.md`, `03-regras-de-calculo.md` |

### Ítalo – cadastros, faturamento, interface e integração

| Pacote / arquivo | Classes | Conteúdo |
|---|---|---|
| `servico/` | 4 | `ClienteServico`, `FrotaServico`, `FaturamentoServico`, `ResumoFaturamento` |
| `relatorio/` | 5 | `DocumentoPdf`, `NotaFiscalPdf`, `FaturamentoPdf`, `EntregasPdf`, `GeradorRelatorios` |
| `ui/` | 9 | `Console`, `Menu`, `MenuPrincipal`, `MenuClientes`, `MenuMotoristas`, `MenuVeiculos`, `MenuEntregas`, `MenuFaturamento`, `EntradaEncerradaException` |
| `util/` | 3 | `Validacao`, `ValidadorDocumento`, `Formatos` |
| `excecao/` | 5 | `TranslogException`, `ValidacaoException`, `RegraNegocioException`, `RegistroNaoEncontradoException`, `PersistenciaException` |
| raiz | 3 | `App`, `Sistema`, `DadosDemonstracao` |
| build | — | `pom.xml`, `.gitignore`, `README.md` |
| testes | 2 classes, 11 testes | `RegrasDoModeloTest`, `ValidadorDocumentoTest` |
| docs | 5 | `04-casos-de-uso.md` a `08-roteiro-da-demonstracao.md` |

### Decidido em conjunto
- O desenho das hierarquias de classes.
- A classificação da carga pelo peso.
- A reserva de agenda pela duração da viagem (500 km por dia).
- As premissas do item 1.4 da especificação.
- A troca do iText pelo PDFBox, por causa da licença.

## 6.2 Divisão da apresentação

| Parte | Quem | Tempo sugerido | Conteúdo |
|---|---|---|---|
| 6.1 Introdução | Cauã | 2 min | Problema da Translog, público-alvo, fluxo geral |
| 6.2 Estrutura e POO | Cauã e Ítalo | 4 min | Cauã: hierarquia `Pessoa` → `Cliente` e o polimorfismo no frete. Ítalo: `DocumentoPdf` e `Menu` (Template Method), interface `Repositorio` |
| 6.3 Demonstração | Ítalo opera, Cauã explica | 6 min | Roteiro em [08-roteiro-da-demonstracao.md](08-roteiro-da-demonstracao.md) |
| 6.4 Desafios e soluções | cada um, da sua área | 2 min | Cauã: Gson e herança, `BigDecimal`, agenda. Ítalo: entradas inválidas, PDFs |
| 6.5 Conclusão e melhorias | Ítalo | 1 min | Resultado e próximos passos |

## 6.3 Histórico no Git

A avaliação considera a contribuição individual. Por isso, cada um deve subir **os próprios arquivos com a própria conta do GitHub**. A ordem abaixo faz o projeto compilar a cada etapa.

**Preparação (uma vez)**
1. O Ítalo cria o repositório `translog` no GitHub e adiciona o Cauã como colaborador (Settings → Collaborators).
2. Os dois clonam o repositório e copiam o conteúdo do zip para dentro da pasta clonada.
3. Cada um configura o próprio nome e e-mail no Git:
   ```bash
   git config user.name "Seu Nome"
   git config user.email "seu-email-do-github@exemplo.com"
   ```

**Commits, em ordem** (antes de cada um: `git pull`; depois: `git push`)

| # | Quem | Comandos |
|---|---|---|
| 1 | Ítalo | `git add pom.xml .gitignore src/main/java/br/com/translog/util src/main/java/br/com/translog/excecao` <br> `git commit -m "Estrutura do projeto Maven, validações e exceções"` |
| 2 | Cauã | `git add src/main/java/br/com/translog/modelo` <br> `git commit -m "Modelo de domínio: clientes, frota, contratos e entregas"` |
| 3 | Cauã | `git add src/main/java/br/com/translog/persistencia` <br> `git commit -m "Persistência em JSON com Gson e tratamento de arquivo corrompido"` |
| 4 | Ítalo | `git add src/main/java/br/com/translog/servico/ClienteServico.java src/main/java/br/com/translog/servico/FrotaServico.java src/main/java/br/com/translog/servico/FaturamentoServico.java src/main/java/br/com/translog/servico/ResumoFaturamento.java` <br> `git commit -m "Serviços de cadastro e faturamento"` |
| 5 | Cauã | `git add src/main/java/br/com/translog/servico` <br> `git commit -m "Cálculo de frete, agenda e ciclo das entregas"` |
| 6 | Ítalo | `git add src/main/java/br/com/translog/relatorio` <br> `git commit -m "Geração de nota fiscal e relatórios em PDF"` |
| 7 | Ítalo | `git add src/main/java/br/com/translog/ui src/main/java/br/com/translog/App.java src/main/java/br/com/translog/Sistema.java src/main/java/br/com/translog/DadosDemonstracao.java src/main/java/br/com/translog/package-info.java` <br> `git commit -m "Menus de console, integração e dados de demonstração"` |
| 8 | Cauã | `git add src/test/java/br/com/translog/servico src/test/java/br/com/translog/persistencia` <br> `git commit -m "Testes do cálculo de frete, das entregas e do repositório"` |
| 9 | Ítalo | `git add src/test/java/br/com/translog/modelo src/test/java/br/com/translog/util` <br> `git commit -m "Testes das regras do modelo e dos validadores"` |
| 10 | Cauã | `git add docs/01-especificacao-de-requisitos.md docs/02-arquitetura-e-diagramas.md docs/03-regras-de-calculo.md` <br> `git commit -m "Especificação, arquitetura e regras de cálculo"` |
| 11 | Ítalo | `git add README.md docs` <br> `git commit -m "README, casos de uso, plano de testes e roteiro"` |

No commit 5, `git add` da pasta inteira só pega os arquivos que ainda não foram enviados, ou seja, os do Cauã.

Depois do último commit, os dois rodam `mvn package` para confirmar que tudo compila.
