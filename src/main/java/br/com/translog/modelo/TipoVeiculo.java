package br.com.translog.modelo;

/** Tipos de veículo da frota e a categoria mínima de CNH para dirigir cada um (RN14). */
public enum TipoVeiculo {

    UTILITARIO("Utilitário", CategoriaCnh.B),
    VAN("Van de carga", CategoriaCnh.B),
    CAMINHAO_TOCO("Caminhão toco", CategoriaCnh.C),
    CAMINHAO_TRUCK("Caminhão truck", CategoriaCnh.C),
    CARRETA("Carreta", CategoriaCnh.E);

    private final String descricao;
    private final CategoriaCnh cnhMinima;

    TipoVeiculo(String descricao, CategoriaCnh cnhMinima) {
        this.descricao = descricao;
        this.cnhMinima = cnhMinima;
    }

    public String getDescricao() {
        return descricao;
    }

    public CategoriaCnh getCnhMinima() {
        return cnhMinima;
    }
}
