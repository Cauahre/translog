package br.com.translog.excecao;

/** Falha ao ler ou gravar arquivos (dados ou relatórios). */
public class PersistenciaException extends TranslogException {

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
