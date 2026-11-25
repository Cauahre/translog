package br.com.translog.modelo;

import java.util.function.Supplier;

public enum TipoCliente {

    EMPRESARIAL("Empresarial", ClienteEmpresarial.class, ClienteEmpresarial::new),
    PRIORITARIO("Prioritário", ClientePrioritario.class, ClientePrioritario::new);

    private final String descricao;
    private final Class<? extends Cliente> classe;
    private final Supplier<Cliente> fabrica;

    TipoCliente(String descricao, Class<? extends Cliente> classe, Supplier<Cliente> fabrica) {
        this.descricao = descricao;
        this.classe = classe;
        this.fabrica = fabrica;
    }

    public String getDescricao() {
        return descricao;
    }

    public Class<? extends Cliente> getClasse() {
        return classe;
    }

    /** Cria uma instância vazia do tipo de cliente correspondente. */
    public Cliente novaInstancia() {
        return fabrica.get();
    }
}
