package br.com.translog.modelo;

import java.math.BigDecimal;

/**
 * Cliente empresarial: 10% de desconto nas entregas regulares (RN01) e
 * não pode pedir entrega urgente (RN03).
 */
public class ClienteEmpresarial extends Cliente {

    private static final BigDecimal DESCONTO_REGULAR = new BigDecimal("0.10");

    public ClienteEmpresarial() {
    }

    public ClienteEmpresarial(String razaoSocial, String cnpj, String telefone, String email) {
        super(razaoSocial, cnpj, telefone, email);
    }

    @Override
    public TipoCliente getTipo() {
        return TipoCliente.EMPRESARIAL;
    }

    @Override
    public BigDecimal percentualDesconto(boolean urgente) {
        // o desconto só vale para entrega regular
        return urgente ? BigDecimal.ZERO : DESCONTO_REGULAR;
    }

    @Override
    public boolean podeSolicitarUrgencia() {
        return false;
    }
}
