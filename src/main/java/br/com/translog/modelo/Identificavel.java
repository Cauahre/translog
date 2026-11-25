package br.com.translog.modelo;

/**
 * Contrato comum a tudo que é salvo nos repositórios.
 * O id é gerado pelo próprio repositório no momento do primeiro salvamento.
 */
public interface Identificavel {

    int getId();

    void setId(int id);
}
