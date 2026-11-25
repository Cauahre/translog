package br.com.translog.excecao;

/** Operação que os dados permitem, mas as regras da transportadora não. */
public class RegraNegocioException extends TranslogException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
