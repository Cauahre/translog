package br.com.translog.modelo;

import java.math.BigDecimal;

/**
 * Cliente prioritário: 20% de desconto em qualquer entrega (RN02) e
 * é o único tipo que pode solicitar entregas urgentes (RN03).
 */
public class ClientePrioritario extends Cliente {

    private static final BigDecimal DESCONTO = new BigDecimal("0.20");

    public ClientePrioritario() {
    }

    public ClientePrioritario(String razaoSocial, String cnpj, String telefone, String email) {
        super(razaoSocial, cnpj, telefone, email);
    }

    @Override
    public TipoCliente getTipo() {
        return TipoCliente.PRIORITARIO;
    }

    @Override
    public BigDecimal percentualDesconto(boolean urgente) {
        return DESCONTO;
    }

    @Override
    public boolean podeSolicitarUrgencia() {
        return true;
    }
}
