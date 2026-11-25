package br.com.translog.modelo;

import br.com.translog.excecao.ValidacaoException;
import br.com.translog.util.Validacao;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Contrato de frete de um cliente. Define o prazo padrão de entrega (em dias, contados
 * a partir da data agendada de saída) e as condições de frete. A condição que o sistema
 * aplica no cálculo é o frete mínimo; o restante fica registrado como observação.
 */
public class Contrato implements Identificavel {

    private int id;
    private int clienteId;
    private int prazoEntregaDias;
    private BigDecimal freteMinimo = BigDecimal.ZERO;
    private String condicoes;
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private boolean ativo = true;

    public Contrato() {
    }

    public Contrato(int clienteId, int prazoEntregaDias, BigDecimal freteMinimo, String condicoes,
                    LocalDate dataInicio, LocalDate dataFim) {
        this.clienteId = clienteId;
        setPrazoEntregaDias(prazoEntregaDias);
        setFreteMinimo(freteMinimo);
        setCondicoes(condicoes);
        definirVigencia(dataInicio, dataFim);
    }

    /** Indica se o contrato pode ser usado para uma entrega agendada na data informada (RN04). */
    public boolean isVigenteEm(LocalDate data) {
        if (!ativo || data == null) {
            return false;
        }
        boolean depoisDoInicio = !data.isBefore(dataInicio);
        boolean antesDoFim = dataFim == null || !data.isAfter(dataFim);
        return depoisDoInicio && antesDoFim;
    }

    public void encerrar() {
        this.ativo = false;
    }

    public void definirVigencia(LocalDate inicio, LocalDate fim) {
        if (inicio == null) {
            throw new ValidacaoException("A data de início do contrato é obrigatória.");
        }
        if (fim != null && fim.isBefore(inicio)) {
            throw new ValidacaoException("A data de término não pode ser anterior ao início do contrato.");
        }
        this.dataInicio = inicio;
        this.dataFim = fim;
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

    public int getPrazoEntregaDias() {
        return prazoEntregaDias;
    }

    public void setPrazoEntregaDias(int prazoEntregaDias) {
        if (prazoEntregaDias < 1 || prazoEntregaDias > 90) {
            throw new ValidacaoException("O prazo de entrega deve ficar entre 1 e 90 dias.");
        }
        this.prazoEntregaDias = prazoEntregaDias;
    }

    public BigDecimal getFreteMinimo() {
        return freteMinimo;
    }

    public void setFreteMinimo(BigDecimal freteMinimo) {
        if (freteMinimo == null) {
            freteMinimo = BigDecimal.ZERO;
        }
        if (freteMinimo.signum() < 0) {
            throw new ValidacaoException("O frete mínimo não pode ser negativo.");
        }
        this.freteMinimo = freteMinimo;
    }

    public String getCondicoes() {
        return condicoes;
    }

    public void setCondicoes(String condicoes) {
        this.condicoes = Validacao.textoOpcional(condicoes);
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public LocalDate getDataFim() {
        return dataFim;
    }

    public boolean isAtivo() {
        return ativo;
    }
}
