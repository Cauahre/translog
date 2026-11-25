package br.com.translog.modelo;

import java.math.BigDecimal;

/**
 * Memória de cálculo do frete. Fica guardada junto com a entrega para que a nota fiscal
 * e os relatórios mostrem exatamente como o valor foi formado.
 */
public class DetalheFrete {

    private BigDecimal valorBase;
    private BigDecimal adicionalEspecial;
    private BigDecimal adicionalUrgencia;
    private BigDecimal percentualDesconto;
    private BigDecimal desconto;
    private BigDecimal ajusteFreteMinimo;
    private BigDecimal valorFrete;

    private long diasAtraso;
    private BigDecimal percentualPenalidade = BigDecimal.ZERO;
    private BigDecimal penalidade = BigDecimal.ZERO;

    public DetalheFrete() {
    }

    public DetalheFrete(BigDecimal valorBase, BigDecimal adicionalEspecial, BigDecimal adicionalUrgencia,
                        BigDecimal percentualDesconto, BigDecimal desconto, BigDecimal ajusteFreteMinimo,
                        BigDecimal valorFrete) {
        this.valorBase = valorBase;
        this.adicionalEspecial = adicionalEspecial;
        this.adicionalUrgencia = adicionalUrgencia;
        this.percentualDesconto = percentualDesconto;
        this.desconto = desconto;
        this.ajusteFreteMinimo = ajusteFreteMinimo;
        this.valorFrete = valorFrete;
    }

    public void registrarAtraso(long dias, BigDecimal percentual, BigDecimal valorPenalidade) {
        this.diasAtraso = dias;
        this.percentualPenalidade = percentual;
        this.penalidade = valorPenalidade;
    }

    /** Valor do serviço antes dos descontos (base + adicionais). */
    public BigDecimal getValorServico() {
        return valorBase.add(adicionalEspecial).add(adicionalUrgencia);
    }

    /** Valor efetivamente cobrado: frete menos o abatimento por atraso. */
    public BigDecimal getValorFinal() {
        return valorFrete.subtract(penalidade);
    }

    public BigDecimal getValorBase() {
        return valorBase;
    }

    public BigDecimal getAdicionalEspecial() {
        return adicionalEspecial;
    }

    public BigDecimal getAdicionalUrgencia() {
        return adicionalUrgencia;
    }

    public BigDecimal getPercentualDesconto() {
        return percentualDesconto;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getAjusteFreteMinimo() {
        return ajusteFreteMinimo;
    }

    public BigDecimal getValorFrete() {
        return valorFrete;
    }

    public long getDiasAtraso() {
        return diasAtraso;
    }

    public BigDecimal getPercentualPenalidade() {
        return percentualPenalidade;
    }

    public BigDecimal getPenalidade() {
        return penalidade;
    }
}
