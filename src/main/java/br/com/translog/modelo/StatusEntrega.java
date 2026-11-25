package br.com.translog.modelo;

/**
 * Ciclo de vida da entrega (RN20):
 * AGENDADA -> EM_TRANSITO -> ENTREGUE
 * AGENDADA ou EM_TRANSITO -> CANCELADA
 */
public enum StatusEntrega {

    AGENDADA("Agendada"),
    EM_TRANSITO("Em trânsito"),
    ENTREGUE("Entregue"),
    CANCELADA("Cancelada");

    private final String descricao;

    StatusEntrega(String descricao) {
        this.descricao = descricao;
    }

    public boolean podeMudarPara(StatusEntrega novo) {
        return switch (this) {
            case AGENDADA -> novo == EM_TRANSITO || novo == CANCELADA;
            case EM_TRANSITO -> novo == ENTREGUE || novo == CANCELADA;
            case ENTREGUE, CANCELADA -> false;
        };
    }

    /** Entregas pendentes ocupam motorista e veículo na agenda. */
    public boolean isPendente() {
        return this == AGENDADA || this == EM_TRANSITO;
    }

    public String getDescricao() {
        return descricao;
    }
}
