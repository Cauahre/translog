package br.com.translog.ui;

/** Lançada quando a entrada padrão termina (Ctrl+D / Ctrl+Z ou fim do arquivo redirecionado). */
public class EntradaEncerradaException extends RuntimeException {

    public EntradaEncerradaException() {
        super("Entrada encerrada.");
    }
}
