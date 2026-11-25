# 2. Arquitetura e diagramas

Os diagramas estão em [Mermaid](https://mermaid.js.org/). O GitHub mostra os diagramas direto na página; no VS Code, use a extensão "Markdown Preview Mermaid Support".

---

## 2.1 Camadas

As dependências seguem um sentido só: a interface chama os serviços, e os serviços usam o modelo e os repositórios. O modelo não conhece ninguém de fora dele.

```mermaid
flowchart TD
    APP["App + Sistema<br/>(monta tudo)"] --> UI
    UI["ui<br/>Console, Menu e 6 menus"] --> SERV
    UI --> REL
    REL["relatorio<br/>DocumentoPdf e subclasses"] --> SERV
    SERV["servico<br/>CalculadoraFrete, AgendaServico,<br/>EntregaServico, ClienteServico,<br/>FrotaServico, FaturamentoServico"] --> MOD
    SERV --> REPO
    REPO["persistencia<br/>Repositorio, RepositorioMemoria,<br/>RepositorioJson"] --> MOD
    MOD["modelo<br/>entidades e enums"]
    UTIL["util + excecao<br/>validações, formatação, exceções"]
    MOD -.-> UTIL
    SERV -.-> UTIL
```

| Pacote | Responsabilidade | Não pode |
|---|---|---|
| `modelo` | Guardar os dados e as regras que pertencem a um único objeto (ex.: transições de status) | Ler o teclado, gravar arquivo |
| `servico` | Regras que envolvem vários objetos (agendar, faturar) | Ler o teclado, imprimir na tela |
| `persistencia` | Ler e gravar os dados | Ter regra de negócio |
| `relatorio` | Montar os PDFs | Alterar dados |
| `ui` | Conversar com o usuário | Ter regra de negócio |
| `util` / `excecao` | Validações e exceções compartilhadas | — |

Por causa dessa separação, os testes usam os serviços sem console e sem arquivos (`RepositorioMemoria` + `Clock` fixo).

---

## 2.2 Diagrama de classes do modelo

```mermaid
classDiagram
    direction TB

    class Identificavel {
        <<interface>>
        +getId() int
        +setId(int)
    }

    class Pessoa {
        <<abstract>>
        -nome: String
        -telefone: String
        -email: String
        +getDocumento()* String
        +getDescricaoTipo()* String
    }

    class Cliente {
        <<abstract>>
        -cnpj: String
        -ativo: boolean
        +getTipo()* TipoCliente
        +percentualDesconto(urgente)* BigDecimal
        +podeSolicitarUrgencia()* boolean
    }

    class ClienteEmpresarial {
        +percentualDesconto(urgente) BigDecimal
        +podeSolicitarUrgencia() boolean
    }

    class ClientePrioritario {
        +percentualDesconto(urgente) BigDecimal
        +podeSolicitarUrgencia() boolean
    }

    class Motorista {
        -cpf: String
        -numeroCnh: String
        -categoriaCnh: CategoriaCnh
        -validadeCnh: LocalDate
        +podeConduzir(exigida, data) boolean
    }

    class Veiculo {
        -placa: String
        -modelo: String
        -tipo: TipoVeiculo
        -capacidadeKg: double
        +suporta(pesoKg) boolean
    }

    class Contrato {
        -clienteId: int
        -prazoEntregaDias: int
        -freteMinimo: BigDecimal
        -dataInicio: LocalDate
        -dataFim: LocalDate
        +isVigenteEm(data) boolean
        +encerrar()
    }

    class Entrega {
        -clienteId: int
        -motoristaId: int
        -veiculoId: int
        -urgente: boolean
        -dataAgendada: LocalDate
        -dataEntrega: LocalDate
        -status: StatusEntrega
        +mudarStatus(novo, quando, obs)
        +getDataLimite() LocalDate
        +getFimOcupacao() LocalDate
        +ocupaPeriodo(inicio, fim) boolean
        +diasDeAtraso(data) long
    }

    class Carga {
        -descricao: String
        -pesoKg: double
        -natureza: NaturezaCarga
        +getTipo() TipoCarga
    }

    class Rota {
        -paradas: List~String~
        -distanciaKm: double
        +getDiasEstimados() int
    }

    class DetalheFrete {
        -valorBase: BigDecimal
        -adicionalEspecial: BigDecimal
        -adicionalUrgencia: BigDecimal
        -desconto: BigDecimal
        -valorFrete: BigDecimal
        -penalidade: BigDecimal
        +getValorFinal() BigDecimal
    }

    class NotaFiscal {
        -entregaId: int
        -clienteNome: String
        -clienteCnpj: String
        -valorTotal: BigDecimal
    }

    class RegistroStatus {
        -status: StatusEntrega
        -dataHora: LocalDateTime
        -observacao: String
    }

    Identificavel <|.. Pessoa
    Identificavel <|.. Veiculo
    Identificavel <|.. Contrato
    Identificavel <|.. Entrega
    Identificavel <|.. NotaFiscal
    Pessoa <|-- Cliente
    Pessoa <|-- Motorista
    Cliente <|-- ClienteEmpresarial
    Cliente <|-- ClientePrioritario

    Entrega *-- Carga
    Entrega *-- Rota
    Entrega *-- DetalheFrete
    Entrega *-- "1..*" RegistroStatus
    Entrega ..> Cliente : clienteId
    Entrega ..> Motorista : motoristaId
    Entrega ..> Veiculo : veiculoId
    Entrega ..> Contrato : contratoId
    Contrato ..> Cliente : clienteId
    NotaFiscal ..> Entrega : entregaId
```

**Por que a `Entrega` guarda ids e não os objetos?** Se ela guardasse o objeto `Cliente`, o JSON das entregas repetiria os dados do cliente em cada entrega. Uma mudança de telefone deixaria cópias diferentes espalhadas. Guardando o id, o cliente existe num lugar só (`clientes.json`).

### Enums

| Enum | Valores | Comportamento |
|---|---|---|
| `TipoCliente` | EMPRESARIAL, PRIORITARIO | cria a subclasse certa de `Cliente` |
| `TipoCarga` | LEVE, MEDIA, PESADA | valor por km, peso máximo e CNH mínima; `classificar(peso)` |
| `NaturezaCarga` | COMUM, FRAGIL, PERIGOSA | `exigeTransporteEspecial()` |
| `CategoriaCnh` | A, B, C, D, E | `habilita(exigida)` |
| `TipoVeiculo` | UTILITARIO, VAN, CAMINHAO_TOCO, CAMINHAO_TRUCK, CARRETA | CNH mínima de cada tipo |
| `StatusEntrega` | AGENDADA, EM_TRANSITO, ENTREGUE, CANCELADA | `podeMudarPara(novo)`, `isPendente()` |

---

## 2.3 Hierarquias de apoio

```mermaid
classDiagram
    class Repositorio~T~ {
        <<interface>>
        +salvar(entidade) T
        +remover(id)
        +buscarPorId(id) Optional
        +listarTodos() List
        +filtrar(condicao) List
    }
    class RepositorioMemoria~T~ {
        #registros: Map
        #aposAlteracao()
    }
    class RepositorioJson~T~ {
        -arquivo: Path
        #aposAlteracao() grava o arquivo
    }
    Repositorio <|.. RepositorioMemoria
    RepositorioMemoria <|-- RepositorioJson

    class DocumentoPdf {
        <<abstract>>
        +gerar(destino) Path
        #getTitulo()* String
        #escreverCorpo()*
    }
    DocumentoPdf <|-- NotaFiscalPdf
    DocumentoPdf <|-- FaturamentoPdf
    DocumentoPdf <|-- EntregasPdf

    class Menu {
        <<abstract>>
        +abrir()
        #titulo()* String
        #opcoes()* List
        #executar(opcao)*
    }
    Menu <|-- MenuPrincipal
    Menu <|-- MenuClientes
    Menu <|-- MenuMotoristas
    Menu <|-- MenuVeiculos
    Menu <|-- MenuEntregas
    Menu <|-- MenuFaturamento

    class TranslogException {
        <<abstract>>
    }
    TranslogException <|-- ValidacaoException
    TranslogException <|-- RegraNegocioException
    TranslogException <|-- RegistroNaoEncontradoException
    TranslogException <|-- PersistenciaException
```

---

## 2.4 Ciclo de vida da entrega (RN20)

```mermaid
stateDiagram-v2
    [*] --> Agendada : agendar()
    Agendada --> Agendada : reagendar()
    Agendada --> EmTransito : iniciarTransporte()
    Agendada --> Cancelada : cancelar()
    EmTransito --> Entregue : concluir() / calcula atraso e emite NF
    EmTransito --> Cancelada : cancelar()
    Entregue --> [*]
    Cancelada --> [*]
```

Qualquer outra transição (por exemplo, de Agendada direto para Entregue) lança `RegraNegocioException`.

---

## 2.5 Sequência do agendamento (RF07)

```mermaid
sequenceDiagram
    actor A as Atendente
    participant M as MenuEntregas
    participant E as EntregaServico
    participant C as ClienteServico
    participant F as FrotaServico
    participant G as AgendaServico
    participant K as CalculadoraFrete
    participant R as Repositório de entregas

    A->>M: cliente, carga, rota, data, motorista, veículo
    M->>E: simular(...) para mostrar a prévia
    A->>M: confirma
    M->>E: agendar(SolicitacaoEntrega)
    E->>E: data não está no passado (RN22)
    E->>C: buscar cliente / ativo? (RN06)
    E->>E: urgência só para prioritário (RN03)
    E->>C: contratoVigente(data) (RN04)
    E->>F: buscar motorista e veículo
    E->>E: CNH x veículo, CNH x carga, validade, capacidade (RN13-RN16)
    E->>G: conflitos(motorista, veículo, período) (RN18-RN19)
    alt há conflito
        G-->>E: lista de conflitos
        E->>G: proximaDataLivre(...)
        E-->>M: RegraNegocioException com a data sugerida
    else livre
        E->>K: calcular(cliente, contrato, carga, km, urgente)
        K->>K: cliente.percentualDesconto(urgente) (polimorfismo)
        K-->>E: DetalheFrete
        E->>R: salvar(entrega)
        E-->>M: Entrega agendada
    end
```

---

## 2.6 Arquivos gerados

```
dados/
├── clientes.json        lista de clientes, cada um com o campo "tipo"
├── contratos.json
├── motoristas.json
├── veiculos.json
├── entregas.json        cada entrega com carga, rota, frete e histórico
├── notas-fiscais.json
└── relatorios/
    ├── nota-fiscal-000001.pdf
    ├── faturamento-AAAAMMDD-AAAAMMDD.pdf
    ├── faturamento-cliente-N-AAAAMMDD.pdf
    └── entregas-AAAAMMDD.pdf
```

Exemplo de um cliente no JSON (o campo `tipo` permite recriar a subclasse certa):

```json
{
  "cnpj": "09378657000161",
  "ativo": true,
  "id": 2,
  "nome": "Farmácia Vida Nordeste S.A.",
  "telefone": "86988123030",
  "email": "compras@vidanordeste.com.br",
  "tipo": "PRIORITARIO"
}
```
