package br.com.translog.relatorio;

import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.NotaFiscal;
import br.com.translog.servico.ClienteServico;
import br.com.translog.servico.EntregaServico;
import br.com.translog.servico.FaturamentoServico;
import br.com.translog.servico.FrotaServico;
import br.com.translog.servico.ResumoFaturamento;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Junta os dados dos serviços e monta os PDFs na pasta de relatórios. */
public class GeradorRelatorios {

    private static final DateTimeFormatter ARQUIVO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path pasta;
    private final ClienteServico clienteServico;
    private final FrotaServico frotaServico;
    private final EntregaServico entregaServico;
    private final FaturamentoServico faturamentoServico;

    public GeradorRelatorios(Path pasta, ClienteServico clienteServico, FrotaServico frotaServico,
                             EntregaServico entregaServico, FaturamentoServico faturamentoServico) {
        this.pasta = pasta;
        this.clienteServico = clienteServico;
        this.frotaServico = frotaServico;
        this.entregaServico = entregaServico;
        this.faturamentoServico = faturamentoServico;
    }

    public Path notaFiscal(int numero) {
        NotaFiscal nota = faturamentoServico.buscarNota(numero);
        Entrega entrega = entregaServico.buscar(nota.getEntregaId());
        Cliente cliente = clienteServico.buscar(nota.getClienteId());
        NotaFiscalPdf pdf = new NotaFiscalPdf(nota, entrega, cliente,
                frotaServico.buscarMotorista(entrega.getMotoristaId()),
                frotaServico.buscarVeiculo(entrega.getVeiculoId()));
        return pdf.gerar(pasta.resolve("nota-fiscal-" + nota.getNumeroFormatado() + ".pdf"));
    }

    public Path faturamento(LocalDate inicio, LocalDate fim) {
        List<ResumoFaturamento> resumos = faturamentoServico.resumoGeral(clienteServico.listar(), inicio, fim);
        return new FaturamentoPdf(resumos, inicio, fim)
                .gerar(pasta.resolve("faturamento-" + inicio.format(ARQUIVO) + "-" + fim.format(ARQUIVO) + ".pdf"));
    }

    public Path faturamentoDoCliente(int clienteId, LocalDate inicio, LocalDate fim) {
        Cliente cliente = clienteServico.buscar(clienteId);
        ResumoFaturamento resumo = faturamentoServico.resumoDoCliente(cliente, inicio, fim);
        List<ResumoFaturamento> lista = resumo.quantidadeEntregas() == 0 ? List.of() : List.of(resumo);
        return new FaturamentoPdf(lista, inicio, fim)
                .gerar(pasta.resolve("faturamento-cliente-" + clienteId + "-" + inicio.format(ARQUIVO) + ".pdf"));
    }

    public Path entregas() {
        LocalDate hoje = entregaServico.hoje();
        EntregasPdf pdf = new EntregasPdf(entregaServico.listarPorPrioridade(), this::nomeCliente, hoje);
        return pdf.gerar(pasta.resolve("entregas-" + hoje.format(ARQUIVO) + ".pdf"));
    }

    private String nomeCliente(int id) {
        try {
            return clienteServico.buscar(id).getNome();
        } catch (RuntimeException e) {
            return "Cliente #" + id;
        }
    }
}
