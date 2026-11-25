package br.com.translog.ui;

import br.com.translog.excecao.TranslogException;

import java.util.List;

/**
 * Esqueleto comum dos menus. A classe cuida do laço, da leitura da opção e do tratamento
 * de erros; cada menu concreto só informa o título, as opções e o que fazer em cada uma.
 * <p>
 * Erros de regra/validação ({@link TranslogException}) são mostrados ao usuário e o menu
 * continua. Qualquer outro erro também é capturado para o sistema não fechar no meio do uso.
 */
public abstract class Menu {

    protected final Console console;

    protected Menu(Console console) {
        this.console = console;
    }

    protected abstract String titulo();

    protected abstract List<String> opcoes();

    protected abstract void executar(int opcao);

    protected String textoSaida() {
        return "Voltar";
    }

    /**
     * Se deve esperar o Enter depois da opção. Opções que abrem um submenu devolvem false,
     * porque o usuário já viu o resultado lá dentro.
     */
    protected boolean pausarDepoisDe(int opcao) {
        return true;
    }

    /** Executado sempre antes de mostrar as opções (ex.: alertas). */
    protected void antesDeExibir() {
    }

    public void abrir() {
        while (true) {
            console.titulo(titulo());
            antesDeExibir();
            List<String> opcoes = opcoes();
            for (int i = 0; i < opcoes.size(); i++) {
                console.info(String.format("  %2d - %s", i + 1, opcoes.get(i)));
            }
            console.info(String.format("  %2d - %s", 0, textoSaida()));
            int escolha = console.lerInteiro("\nOpção", 0, opcoes.size());
            if (escolha == 0) {
                return;
            }
            boolean pausar = pausarDepoisDe(escolha);
            try {
                executar(escolha);
            } catch (TranslogException e) {
                console.erro(e.getMessage());
                pausar = true;
            } catch (EntradaEncerradaException e) {
                throw e;
            } catch (RuntimeException e) {
                console.erro("Ocorreu um erro inesperado: " + e.getClass().getSimpleName()
                        + (e.getMessage() == null ? "" : " - " + e.getMessage())
                        + ". A operação foi interrompida, mas os dados salvos não foram afetados.");
                pausar = true;
            }
            if (pausar) {
                console.pausar();
            }
        }
    }
}
