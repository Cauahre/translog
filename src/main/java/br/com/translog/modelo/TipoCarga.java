package br.com.translog.modelo;

import java.math.BigDecimal;

/**
 * Classificação da carga pelo peso (RN07). Cada faixa tem seu preço por km (RN08) e a
 * categoria mínima de CNH exigida do motorista (RN13).
 */
public enum TipoCarga {

    LEVE("Leve", "1.50", 500, CategoriaCnh.B),
    MEDIA("Média", "2.50", 3_000, CategoriaCnh.B),
    PESADA("Pesada", "4.00", Double.MAX_VALUE, CategoriaCnh.C);

    private final String descricao;
    private final BigDecimal valorPorKm;
    private final double pesoMaximoKg;
    private final CategoriaCnh cnhMinima;

    TipoCarga(String descricao, String valorPorKm, double pesoMaximoKg, CategoriaCnh cnhMinima) {
        this.descricao = descricao;
        this.valorPorKm = new BigDecimal(valorPorKm);
        this.pesoMaximoKg = pesoMaximoKg;
        this.cnhMinima = cnhMinima;
    }

    /** Até 500 kg é leve, até 3.000 kg é média e acima disso é pesada. */
    public static TipoCarga classificar(double pesoKg) {
        for (TipoCarga tipo : values()) {
            if (pesoKg <= tipo.pesoMaximoKg) {
                return tipo;
            }
        }
        return PESADA;
    }

    public String getDescricao() {
        return descricao;
    }

    public BigDecimal getValorPorKm() {
        return valorPorKm;
    }

    public CategoriaCnh getCnhMinima() {
        return cnhMinima;
    }
}
