package br.com.translog;

import br.com.translog.excecao.TranslogException;
import br.com.translog.ui.Console;
import br.com.translog.ui.EntradaEncerradaException;
import br.com.translog.ui.MenuPrincipal;

import java.nio.file.Path;
import java.time.Clock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Ponto de entrada.
 * <p>
 * A pasta dos dados pode ser escolhida (nesta ordem):
 * argumento {@code --dados=CAMINHO}, variável de ambiente {@code TRANSLOG_DADOS}
 * ou, por padrão, a pasta {@code dados} no diretório atual. Apontar para uma pasta
 * sincronizada do Google Drive faz os arquivos ficarem salvos na nuvem.
 */
public class App {

    // referência estática para o logger não ser coletado e perder a configuração
    private static final Logger LOG_PDFBOX = Logger.getLogger("org.apache.pdfbox");

    public static void main(String[] args) {
        // o PDFBox escreve avisos sobre cache de fontes no console; não interessam ao usuário
        LOG_PDFBOX.setLevel(Level.SEVERE);

        Console console = new Console(System.in, System.out);
        Path pastaDados = resolverPastaDados(args);

        Sistema sistema;
        try {
            sistema = new Sistema(pastaDados, Clock.systemDefaultZone());
        } catch (TranslogException e) {
            console.erro(e.getMessage());
            console.erro("Verifique a pasta de dados e tente novamente.");
            return;
        }

        console.info("Translog - dados em " + pastaDados.toAbsolutePath());
        sistema.getAvisos().forEach(console::aviso);
        if (sistema.semDados()) {
            console.info("Nenhum dado encontrado. Use a opção 7 para carregar um cenário de demonstração.");
        }

        try {
            new MenuPrincipal(console, sistema).abrir();
            console.info("Até logo! Os dados ficam salvos automaticamente a cada operação.");
        } catch (EntradaEncerradaException e) {
            console.info("\nEntrada encerrada. Os dados já estão salvos.");
        }
    }

    private static Path resolverPastaDados(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--dados=") && arg.length() > "--dados=".length()) {
                return Path.of(arg.substring("--dados=".length()));
            }
        }
        String variavel = System.getenv("TRANSLOG_DADOS");
        if (variavel != null && !variavel.isBlank()) {
            return Path.of(variavel);
        }
        return Path.of("dados");
    }
}
