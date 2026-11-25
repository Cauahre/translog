package br.com.translog.modelo;

import java.time.LocalDateTime;

/** Uma linha do histórico de acompanhamento da entrega. */
public class RegistroStatus {

    private StatusEntrega status;
    private LocalDateTime dataHora;
    private String observacao;

    public RegistroStatus() {
    }

    public RegistroStatus(StatusEntrega status, LocalDateTime dataHora, String observacao) {
        this.status = status;
        this.dataHora = dataHora;
        this.observacao = observacao;
    }

    public StatusEntrega getStatus() {
        return status;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getObservacao() {
        return observacao;
    }
}
