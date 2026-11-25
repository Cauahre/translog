package br.com.translog.modelo;

import br.com.translog.util.Validacao;

/**
 * Base para clientes e motoristas. Guarda os dados de contato que os dois têm em comum
 * e deixa para as subclasses dizer qual é o documento principal (CNPJ ou CPF).
 */
public abstract class Pessoa implements Identificavel {

    private int id;
    private String nome;
    private String telefone;
    private String email;

    protected Pessoa() {
    }

    protected Pessoa(String nome, String telefone, String email) {
        setNome(nome);
        setTelefone(telefone);
        setEmail(email);
    }

    /** Documento já formatado para exibição. */
    public abstract String getDocumento();

    /** Descrição curta do tipo de pessoa, usada nas listagens. */
    public abstract String getDescricaoTipo();

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = Validacao.exigirTexto(nome, "Nome");
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = Validacao.validarTelefone(telefone);
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = Validacao.validarEmail(email);
    }

    @Override
    public String toString() {
        return "#" + id + " - " + nome + " (" + getDescricaoTipo() + ")";
    }
}
