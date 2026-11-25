# 5. Plano de testes

## 5.1 Testes automatizados (JUnit 5)

Rodar com:

```bash
mvn test
```

São **32 testes** em 5 classes. Eles usam `RepositorioMemoria` (sem arquivos) e um `Clock` fixo (a data de "hoje" não muda), então o resultado é sempre o mesmo.

### CalculadoraFreteTest (7)

| Teste | Regra | Resultado esperado |
|---|---|---|
| cargaLeveComumParaEmpresarialTemDescontoDe10Porcento | RN01, RN08 | 100 km × 1,50 = 150,00 − 15,00 = 135,00 |
| cargaMediaUsaValorDe2e50PorKm | RN02, RN08 | 250,00 − 20% = 200,00 |
| cargaPesadaPerigosaUrgenteSomaAdicionaisSobreABase | RN09, RN10 | 1.200 + 480 + 360 − 408 = 1.632,00 |
| freteMinimoDoContratoEhRespeitado | RN11 | 13,50 sobe para 300,00 |
| atrasoDe3DiasAbate15Porcento | RN25 | 1.350,00 − 202,50 = 1.147,50 |
| penalidadeEhLimitadaA25Porcento | RN25 | 12 dias → 25% = 337,50 |
| entregaNoPrazoNaoTemPenalidade | RN25 | valor final = frete |

### EntregaServicoTest (10)

| Teste | Regra | Resultado esperado |
|---|---|---|
| clienteEmpresarialNaoPodePedirUrgencia | RN03 | `RegraNegocioException` |
| cargaPesadaExigeCnhC | RN13 | `RegraNegocioException` |
| pesoNaoPodeUltrapassarCapacidadeDoVeiculo | RN16 | `RegraNegocioException` |
| naoAgendaNoPassado | RN22 | `ValidacaoException` |
| bloqueiaMotoristaJaOcupadoESugereProximaData | RN18, RN19 | erro com "Próxima data livre"; no dia seguinte ao fim da viagem, agenda |
| cancelamentoLiberaAAgenda | RN19 | depois de cancelar, a mesma data aceita outra entrega |
| entregaAtrasadaGeraNotaComAbatimento | RN24, RN25, RN26 | 2 dias de atraso → nota de 121,50 |
| naoConcluiEntregaQueNaoSaiu | RN20 | `RegraNegocioException` |
| semContratoVigenteNaoAgenda | RN04, RN05 | contrato antigo encerrado; agendamento recusado |
| urgentesVemPrimeiroNaListagem | RN23 | a urgente é a primeira da lista |

### RegrasDoModeloTest (7)

| Teste | Regra |
|---|---|
| classificacaoDaCargaPeloPeso | RN07 (limites 500 e 3.000 kg) |
| hierarquiaDaCnh | RN13, RN14 |
| polimorfismoDoDescontoPorTipoDeCliente | RN01, RN02, RN03 |
| transicoesDeStatusPermitidas | RN20 |
| entregaNaoPulaEtapas | RN20 |
| ocupacaoDaAgendaConsideraDuracaoDaViagem | RN18 (1.200 km = 3 dias) |
| contagemDeDiasDeAtraso | RN24 |

### ValidadorDocumentoTest (4)

| Teste | Regra |
|---|---|
| aceitaCpfValidoComOuSemMascara | RN28 |
| rejeitaCpfComDigitoErradoOuRepetido | RN28 |
| validaCnpj | RN28 |
| aceitaPlacaAntigaEMercosul | RN28 |

### RepositorioJsonTest (4)

| Teste | Requisito |
|---|---|
| arquivoAusenteComecaVazio | RNF04 |
| salvaERecarregaMantendoOTipoDoCliente | RF20, herança no JSON |
| arquivoCorrompidoEhIsoladoESistemaContinua | RNF04 |
| idsContinuamDepoisDoMaiorExistente | RF20 |

---

## 5.2 Testes manuais de robustez

Feitos pelo console com os dados de demonstração carregados.

| # | Ação | Resultado esperado | Resultado |
|---|---|---|---|
| M01 | Digitar `abc` na escolha de uma opção | "Digite apenas números inteiros." e a pergunta se repete | OK |
| M02 | Digitar `99` num menu de 7 opções | "Digite um número entre 0 e 7." | OK |
| M03 | Digitar `32/13/2026` numa data | "Data inválida. Use o formato dd/mm/aaaa" | OK |
| M04 | Peso `abc` no agendamento | "Valor numérico inválido" e pede de novo | OK |
| M05 | Rota com uma cidade só | "Informe pelo menos a origem e o destino." | OK |
| M06 | Buscar o cliente de código 999 | "Cliente com código 999 não encontrado(a)." e volta ao menu | OK |
| M07 | Cadastrar o CNPJ `11.111.111/1111-11` | "CNPJ inválido" | OK |
| M08 | Agendar carga de 8.000 kg com o motorista Antônio (CNH B) | erro informando a CNH exigida | OK |
| M09 | Agendar com motorista já reservado | erro com a entrega em conflito e a próxima data livre | OK |
| M10 | Iniciar o transporte de uma entrega de amanhã | "A saída está agendada para ... reagende" | OK |
| M11 | Apagar a pasta `dados` e abrir o sistema | começa vazio, sem erro | OK |
| M12 | Escrever lixo em `clientes.json` e abrir o sistema | aviso de arquivo corrompido; o arquivo é guardado como `.corrompido-DATA` | OK |
| M13 | Encerrar a entrada com Ctrl+D (Linux/Mac) ou Ctrl+Z + Enter (Windows) | "Entrada encerrada. Os dados já estão salvos." | OK |
| M14 | Remover um cliente que tem entregas | erro orientando a inativar | OK |
