package br.com.translog.modelo;

public enum NaturezaCarga {

    COMUM("Comum", false),
    FRAGIL("Frágil", true),
    PERIGOSA("Perigosa", true);

    private final String descricao;
    private final boolean exigeTransporteEspecial;

    NaturezaCarga(String descricao, boolean exigeTransporteEspecial) {
        this.descricao = descricao;
        this.exigeTransporteEspecial = exigeTransporteEspecial;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean exigeTransporteEspecial() {
        return exigeTransporteEspecial;
    }
}
