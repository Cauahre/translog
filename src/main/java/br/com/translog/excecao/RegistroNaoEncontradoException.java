package br.com.translog.excecao;

public class RegistroNaoEncontradoException extends TranslogException {

    public RegistroNaoEncontradoException(String entidade, int id) {
        super(entidade + " com código " + id + " não encontrado(a).");
    }
}
