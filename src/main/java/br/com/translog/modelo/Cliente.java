package br.com.translog.modelo;

import br.com.translog.util.ValidadorDocumento;

import java.math.BigDecimal;

/**
 * Cliente da transportadora. Cada tipo de cliente tem uma política própria de desconto
 * e de permissão para entregas urgentes, por isso esses métodos são abstratos.
 */
public abstract class Cliente extends Pessoa {

    private String cnpj;
    private boolean ativo = true;

    protected Cliente() {
    }

    protected Cliente(String razaoSocial, String cnpj, String telefone, String email) {
        super(razaoSocial, telefone, email);
        setCnpj(cnpj);
    }

    public abstract TipoCliente getTipo();

    /**
     * Percentual de desconto (0.10 = 10%) aplicado sobre o frete.
     *
     * @param urgente se a entrega foi solicitada como urgente
     */
    public abstract BigDecimal percentualDesconto(boolean urgente);

    public abstract boolean podeSolicitarUrgencia();

    @Override
    public String getDocumento() {
        return ValidadorDocumento.formatarCnpj(cnpj);
    }

    @Override
    public String getDescricaoTipo() {
        return getTipo().getDescricao();
    }

    /** Copia os dados cadastrais para outro cliente (usado ao trocar o tipo do cliente). */
    public void copiarDadosPara(Cliente destino) {
        destino.setId(getId());
        destino.setNome(getNome());
        destino.setTelefone(getTelefone());
        destino.setEmail(getEmail());
        destino.cnpj = cnpj;
        destino.ativo = ativo;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = ValidadorDocumento.validarCnpj(cnpj);
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
