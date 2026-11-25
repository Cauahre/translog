/**
 * Regras de negócio que envolvem mais de um objeto: cálculo do frete, agenda,
 * ciclo das entregas, cadastros e faturamento.
 *
 * <p>Os serviços recebem os repositórios e o relógio pelo construtor, por isso podem ser
 * testados sem arquivos e com data fixa.</p>
 *
 * <p>Responsáveis: Cauã (CalculadoraFrete, AgendaServico, EntregaServico, SolicitacaoEntrega,
 * PainelResumo) e Ítalo (ClienteServico, FrotaServico, FaturamentoServico, ResumoFaturamento).</p>
 */
package br.com.translog.servico;
