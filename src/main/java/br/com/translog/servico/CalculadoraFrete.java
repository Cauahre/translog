package br.com.translog.servico;

import br.com.translog.modelo.Carga;
import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Contrato;
import br.com.translog.modelo.DetalheFrete;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Regras de preço da Translog.
 *
 * <pre>
 * base        = km x valor do tipo de carga (leve 1,50 / média 2,50 / pesada 4,00)   RN08
 * especial    = 40% da base se a carga for frágil ou perigosa                       RN09
 * urgência    = 30% da base se a entrega for urgente                                RN10
 * subtotal    = base + especial + urgência
 * desconto    = subtotal x percentual do cliente (polimorfismo)                     RN01, RN02
 * frete       = subtotal - desconto, respeitando o frete mínimo do contrato         RN11
 * penalidade  = 5% do frete por dia de atraso, no máximo 25%                        RN25
 * </pre>
 *
 * Os adicionais são calculados sobre a base (e não um em cima do outro) para que o
 * cliente consiga conferir o valor com uma conta simples.
 */
public class CalculadoraFrete {

    public static final BigDecimal ADICIONAL_ESPECIAL = new BigDecimal("0.40");
    public static final BigDecimal ADICIONAL_URGENCIA = new BigDecimal("0.30");
    public static final BigDecimal PENALIDADE_POR_DIA = new BigDecimal("0.05");
    public static final BigDecimal PENALIDADE_MAXIMA = new BigDecimal("0.25");

    public DetalheFrete calcular(Cliente cliente, Contrato contrato, Carga carga, double distanciaKm, boolean urgente) {
        BigDecimal base = dinheiro(BigDecimal.valueOf(distanciaKm).multiply(carga.getTipo().getValorPorKm()));

        BigDecimal especial = carga.exigeTransporteEspecial()
                ? dinheiro(base.multiply(ADICIONAL_ESPECIAL))
                : zero();
        BigDecimal urgencia = urgente
                ? dinheiro(base.multiply(ADICIONAL_URGENCIA))
                : zero();

        BigDecimal subtotal = base.add(especial).add(urgencia);
        BigDecimal percentualDesconto = cliente.percentualDesconto(urgente);
        BigDecimal desconto = dinheiro(subtotal.multiply(percentualDesconto));
        BigDecimal frete = subtotal.subtract(desconto);

        BigDecimal ajusteMinimo = zero();
        BigDecimal minimo = contrato == null ? BigDecimal.ZERO : contrato.getFreteMinimo();
        if (frete.compareTo(minimo) < 0) {
            ajusteMinimo = dinheiro(minimo.subtract(frete));
            frete = dinheiro(minimo);
        }

        return new DetalheFrete(base, especial, urgencia, percentualDesconto, desconto, ajusteMinimo, frete);
    }

    /** Aplica (ou zera) o abatimento por atraso no detalhe do frete. */
    public void aplicarPenalidade(DetalheFrete frete, long diasAtraso) {
        if (diasAtraso <= 0) {
            frete.registrarAtraso(0, BigDecimal.ZERO, zero());
            return;
        }
        BigDecimal percentual = PENALIDADE_POR_DIA.multiply(BigDecimal.valueOf(diasAtraso)).min(PENALIDADE_MAXIMA);
        BigDecimal valor = dinheiro(frete.getValorFrete().multiply(percentual));
        frete.registrarAtraso(diasAtraso, percentual, valor);
    }

    private static BigDecimal dinheiro(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2);
    }
}
