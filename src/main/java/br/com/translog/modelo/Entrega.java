package br.com.translog.modelo;

import br.com.translog.excecao.RegraNegocioException;
import br.com.translog.util.Formatos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Entrega agendada para um cliente. Guarda os ids de cliente, motorista e veículo
 * (e não os objetos) para que o arquivo JSON não duplique dados de cadastro.
 */
public class Entrega implements Identificavel {

    private int id;
    private int clienteId;
    private int contratoId;
    private int motoristaId;
    private int veiculoId;
    private Carga carga;
    private Rota rota;
    private boolean urgente;
    private LocalDate dataSolicitacao;
    private LocalDate dataAgendada;
    private int prazoDias;
    private LocalDate dataEntrega;
    private StatusEntrega status;
    private DetalheFrete frete;
    private String motivoCancelamento;
    private List<RegistroStatus> historico = new ArrayList<>();

    public Entrega() {
    }

    public Entrega(int clienteId, int contratoId, int motoristaId, int veiculoId, Carga carga, Rota rota,
                   boolean urgente, LocalDate dataSolicitacao, LocalDate dataAgendada, int prazoDias,
                   DetalheFrete frete) {
        this.clienteId = clienteId;
        this.contratoId = contratoId;
        this.motoristaId = motoristaId;
        this.veiculoId = veiculoId;
        this.carga = carga;
        this.rota = rota;
        this.urgente = urgente;
        this.dataSolicitacao = dataSolicitacao;
        this.dataAgendada = dataAgendada;
        this.prazoDias = prazoDias;
        this.frete = frete;
        this.status = StatusEntrega.AGENDADA;
    }

    public void registrarCriacao(LocalDateTime quando) {
        historico.add(new RegistroStatus(StatusEntrega.AGENDADA, quando,
                "Entrega agendada para " + Formatos.data(dataAgendada)));
    }

    /** Única forma de mudar o status: valida a transição (RN20) e registra no histórico (RN21). */
    public void mudarStatus(StatusEntrega novo, LocalDateTime quando, String observacao) {
        if (!status.podeMudarPara(novo)) {
            throw new RegraNegocioException("Não é possível mudar a entrega de "
                    + status.getDescricao() + " para " + novo.getDescricao() + ".");
        }
        this.status = novo;
        historico.add(new RegistroStatus(novo, quando, observacao));
    }

    public void reagendar(LocalDate novaData, int novoPrazo, LocalDateTime quando) {
        if (status != StatusEntrega.AGENDADA) {
            throw new RegraNegocioException("Só é possível reagendar entregas com status Agendada.");
        }
        LocalDate anterior = this.dataAgendada;
        this.dataAgendada = novaData;
        this.prazoDias = novoPrazo;
        historico.add(new RegistroStatus(status, quando, "Reagendada de " + Formatos.data(anterior) + " para " + Formatos.data(novaData)));
    }

    /** Último dia para a entrega ser feita sem multa, conforme o prazo do contrato (RN24). */
    public LocalDate getDataLimite() {
        return dataAgendada.plusDays(prazoDias);
    }

    /** Último dia em que motorista e veículo ficam reservados para esta entrega (RN18). */
    public LocalDate getFimOcupacao() {
        return dataAgendada.plusDays(rota.getDiasEstimados() - 1L);
    }

    public boolean ocupaPeriodo(LocalDate inicio, LocalDate fim) {
        if (!status.isPendente()) {
            return false;
        }
        return !dataAgendada.isAfter(fim) && !getFimOcupacao().isBefore(inicio);
    }

    public long diasDeAtraso(LocalDate dataReferencia) {
        long dias = ChronoUnit.DAYS.between(getDataLimite(), dataReferencia);
        return Math.max(0, dias);
    }

    /** Pendente e com o prazo já vencido na data informada. */
    public boolean isAtrasadaEm(LocalDate hoje) {
        return status.isPendente() && hoje.isAfter(getDataLimite());
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public int getClienteId() {
        return clienteId;
    }

    public int getContratoId() {
        return contratoId;
    }

    public int getMotoristaId() {
        return motoristaId;
    }

    public int getVeiculoId() {
        return veiculoId;
    }

    public Carga getCarga() {
        return carga;
    }

    public Rota getRota() {
        return rota;
    }

    public boolean isUrgente() {
        return urgente;
    }

    public LocalDate getDataSolicitacao() {
        return dataSolicitacao;
    }

    public LocalDate getDataAgendada() {
        return dataAgendada;
    }

    public int getPrazoDias() {
        return prazoDias;
    }

    public LocalDate getDataEntrega() {
        return dataEntrega;
    }

    public void setDataEntrega(LocalDate dataEntrega) {
        this.dataEntrega = dataEntrega;
    }

    public StatusEntrega getStatus() {
        return status;
    }

    public DetalheFrete getFrete() {
        return frete;
    }

    public String getMotivoCancelamento() {
        return motivoCancelamento;
    }

    public void setMotivoCancelamento(String motivoCancelamento) {
        this.motivoCancelamento = motivoCancelamento;
    }

    public List<RegistroStatus> getHistorico() {
        return Collections.unmodifiableList(historico);
    }
}
