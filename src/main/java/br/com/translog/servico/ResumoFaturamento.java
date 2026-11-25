package br.com.translog.servico;

import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.NotaFiscal;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

/** Totais de faturamento de um cliente em um período. */
public record ResumoFaturamento(
        Cliente cliente,
        List<NotaFiscal> notas,
        BigDecimal totalServicos,
        BigDecimal totalDescontos,
        BigDecimal totalAbatimentos,
        BigDecimal totalLiquido) {

    public static ResumoFaturamento de(Cliente cliente, List<NotaFiscal> notas) {
        return new ResumoFaturamento(cliente, notas,
                somar(notas, NotaFiscal::getValorServico),
                somar(notas, NotaFiscal::getDesconto),
                somar(notas, NotaFiscal::getAbatimentoAtraso),
                somar(notas, NotaFiscal::getValorTotal));
    }

    public int quantidadeEntregas() {
        return notas.size();
    }

    public BigDecimal ticketMedio() {
        if (notas.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        return totalLiquido.divide(BigDecimal.valueOf(notas.size()), 2, RoundingMode.HALF_UP);
    }

    private static BigDecimal somar(List<NotaFiscal> notas, Function<NotaFiscal, BigDecimal> campo) {
        return notas.stream()
                .map(campo)
                .map(v -> v == null ? BigDecimal.ZERO : v)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
}
