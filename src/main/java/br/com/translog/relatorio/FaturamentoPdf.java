package br.com.translog.relatorio;

import br.com.translog.modelo.NotaFiscal;
import br.com.translog.servico.ResumoFaturamento;
import br.com.translog.util.Formatos;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Relatório de faturamento por cliente: detalhe das notas de cada um e um quadro consolidado. */
public class FaturamentoPdf extends DocumentoPdf {

    private final List<ResumoFaturamento> resumos;
    private final LocalDate inicio;
    private final LocalDate fim;

    public FaturamentoPdf(List<ResumoFaturamento> resumos, LocalDate inicio, LocalDate fim) {
        this.resumos = resumos;
        this.inicio = inicio;
        this.fim = fim;
    }

    @Override
    protected String getTitulo() {
        return "Relatório de Faturamento por Cliente";
    }

    @Override
    protected String getSubtitulo() {
        return "Período: " + Formatos.data(inicio) + " a " + Formatos.data(fim);
    }

    @Override
    protected void escreverCorpo() throws IOException {
        if (resumos.isEmpty()) {
            espaco(10);
            paragrafo("Nenhuma nota fiscal emitida no período.", 11);
            return;
        }

        for (ResumoFaturamento r : resumos) {
            secao(r.cliente().getNome());
            campo("CNPJ", r.cliente().getDocumento() + "   |   Tipo: " + r.cliente().getTipo().getDescricao());
            List<String[]> linhas = new ArrayList<>();
            for (NotaFiscal n : r.notas()) {
                linhas.add(new String[]{
                        n.getNumeroFormatado(),
                        Formatos.data(n.getDataEmissao()),
                        n.getDescricaoServico(),
                        Formatos.moeda(n.getValorServico()),
                        Formatos.moeda(n.getDesconto()),
                        Formatos.moeda(n.getAbatimentoAtraso()),
                        Formatos.moeda(n.getValorTotal())});
            }
            linhas.add(new String[]{"", "", "Total do cliente", Formatos.moeda(r.totalServicos()),
                    Formatos.moeda(r.totalDescontos()), Formatos.moeda(r.totalAbatimentos()),
                    Formatos.moeda(r.totalLiquido())});
            tabela(new String[]{"NF", "Emissão", "Serviço", "Bruto", "Desconto", "Atraso", "Líquido"},
                    new float[]{0.08f, 0.12f, 0.32f, 0.12f, 0.12f, 0.12f, 0.12f},
                    linhas,
                    new boolean[]{false, false, false, true, true, true, true});
        }

        secao("Consolidado do período");
        List<String[]> consolidado = new ArrayList<>();
        BigDecimal totalGeral = BigDecimal.ZERO.setScale(2);
        int entregas = 0;
        for (ResumoFaturamento r : resumos) {
            consolidado.add(new String[]{
                    r.cliente().getNome(),
                    String.valueOf(r.quantidadeEntregas()),
                    Formatos.moeda(r.ticketMedio()),
                    Formatos.moeda(r.totalAbatimentos()),
                    Formatos.moeda(r.totalLiquido())});
            totalGeral = totalGeral.add(r.totalLiquido());
            entregas += r.quantidadeEntregas();
        }
        tabela(new String[]{"Cliente", "Entregas", "Ticket médio", "Abatimentos", "Faturado"},
                new float[]{0.40f, 0.12f, 0.16f, 0.16f, 0.16f},
                consolidado,
                new boolean[]{false, true, true, true, true});
        espaco(4);
        destaque("TOTAL FATURADO (" + entregas + " entregas)", Formatos.moeda(totalGeral));
    }
}
