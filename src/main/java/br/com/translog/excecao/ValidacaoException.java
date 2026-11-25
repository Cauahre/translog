package br.com.translog.excecao;

/** Dado inválido informado pelo usuário (CPF errado, campo vazio, valor negativo...). */
public class ValidacaoException extends TranslogException {

    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}
