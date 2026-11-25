package br.com.translog.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Nota fiscal de serviço de transporte. Os dados do cliente são copiados no momento
 * da emissão, como numa nota de verdade: se o cadastro mudar depois, a nota não muda.
 */
public class NotaFiscal implements Identificavel {

    private int id;
    private int entregaId;
    private int clienteId;
    private String clienteNome;
    private String clienteCnpj;
    private LocalDate dataEmissao;
    private String descricaoServico;
    private BigDecimal valorServico;
    private BigDecimal desconto;
    private BigDecimal ajusteFreteMinimo;
    private BigDecimal abatimentoAtraso;
    private BigDecimal valorTotal;

    public NotaFiscal() {
    }

    public NotaFiscal(Entrega entrega, Cliente cliente, LocalDate dataEmissao) {
        DetalheFrete frete = entrega.getFrete();
        this.entregaId = entrega.getId();
        this.clienteId = cliente.getId();
        this.clienteNome = cliente.getNome();
        this.clienteCnpj = cliente.getDocumento();
        this.dataEmissao = dataEmissao;
        this.descricaoServico = "Frete " + entrega.getRota() + " - " + entrega.getCarga().getDescricao();
        this.valorServico = frete.getValorServico();
        this.desconto = frete.getDesconto();
        this.ajusteFreteMinimo = frete.getAjusteFreteMinimo();
        this.abatimentoAtraso = frete.getPenalidade();
        this.valorTotal = frete.getValorFinal();
    }

    public String getNumeroFormatado() {
        return String.format("%06d", id);
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public int getEntregaId() {
        return entregaId;
    }

    public int getClienteId() {
        return clienteId;
    }

    public String getClienteNome() {
        return clienteNome;
    }

    public String getClienteCnpj() {
        return clienteCnpj;
    }

    public LocalDate getDataEmissao() {
        return dataEmissao;
    }

    public String getDescricaoServico() {
        return descricaoServico;
    }

    public BigDecimal getValorServico() {
        return valorServico;
    }

    public BigDecimal getDesconto() {
        return desconto;
    }

    public BigDecimal getAjusteFreteMinimo() {
        return ajusteFreteMinimo;
    }

    public BigDecimal getAbatimentoAtraso() {
        return abatimentoAtraso;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }
}
