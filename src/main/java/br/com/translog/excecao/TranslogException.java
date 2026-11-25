package br.com.translog.excecao;

/**
 * Exceção base do sistema. Tudo que herda daqui é um erro "esperado", com mensagem
 * pronta para mostrar ao usuário; o menu captura e o programa segue normalmente.
 */
public abstract class TranslogException extends RuntimeException {

    protected TranslogException(String mensagem) {
        super(mensagem);
    }

    protected TranslogException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
