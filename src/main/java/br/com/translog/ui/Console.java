package br.com.translog.ui;

import br.com.translog.util.Formatos;

import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Toda a leitura e escrita no terminal passa por aqui. Os métodos de leitura repetem a
 * pergunta até receber um valor válido, então o resto do sistema nunca recebe lixo
 * (e uma letra digitada no lugar de um número não derruba o programa).
 */
public class Console {

    private static final String LINHA = "-".repeat(70);

    private final Scanner entrada;
    private final PrintStream saida;

    public Console(InputStream in, PrintStream out) {
        this.entrada = new Scanner(in);
        this.saida = out;
    }

    // ---------- leitura ----------

    public String lerLinha(String rotulo) {
        saida.print(rotulo + ": ");
        saida.flush();
        if (!entrada.hasNextLine()) {
            throw new EntradaEncerradaException();
        }
        return entrada.nextLine().trim();
    }

    public String lerTexto(String rotulo) {
        while (true) {
            String valor = lerLinha(rotulo);
            if (!valor.isEmpty()) {
                return valor;
            }
            erro("Campo obrigatório.");
        }
    }

    /** Campo que pode ficar em branco. */
    public String lerTextoOpcional(String rotulo) {
        String valor = lerLinha(rotulo + " (opcional)");
        return valor.isEmpty() ? null : valor;
    }

    /** Usado na edição: Enter mantém o valor atual. */
    public String lerTextoMantendo(String rotulo, String atual) {
        String valor = lerLinha(rotulo + " [" + (atual == null ? "" : atual) + "]");
        return valor.isEmpty() ? atual : valor;
    }

    public int lerInteiro(String rotulo, int minimo, int maximo) {
        while (true) {
            String valor = lerLinha(rotulo);
            try {
                int numero = Integer.parseInt(valor);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
                erro("Digite um número entre " + minimo + " e " + maximo + ".");
            } catch (NumberFormatException e) {
                erro("Digite apenas números inteiros.");
            }
        }
    }

    public int lerInteiroMantendo(String rotulo, int atual, int minimo, int maximo) {
        while (true) {
            String valor = lerLinha(rotulo + " [" + atual + "]");
            if (valor.isEmpty()) {
                return atual;
            }
            try {
                int numero = Integer.parseInt(valor);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
                erro("Digite um número entre " + minimo + " e " + maximo + ".");
            } catch (NumberFormatException e) {
                erro("Digite apenas números inteiros.");
            }
        }
    }

    public int lerCodigo(String rotulo) {
        return lerInteiro(rotulo, 1, Integer.MAX_VALUE);
    }

    /** Aceita vírgula ou ponto como separador decimal. */
    public double lerDecimal(String rotulo, double minimo, double maximo) {
        while (true) {
            String valor = lerLinha(rotulo);
            try {
                double numero = converterNumero(valor);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
                erro("Digite um valor entre " + Formatos.numero(minimo) + " e " + Formatos.numero(maximo) + ".");
            } catch (NumberFormatException e) {
                erro("Valor numérico inválido. Exemplo: 1250,5");
            }
        }
    }

    public double lerDecimalMantendo(String rotulo, double atual, double minimo, double maximo) {
        while (true) {
            String valor = lerLinha(rotulo + " [" + Formatos.numero(atual) + "]");
            if (valor.isEmpty()) {
                return atual;
            }
            try {
                double numero = converterNumero(valor);
                if (numero >= minimo && numero <= maximo) {
                    return numero;
                }
                erro("Digite um valor entre " + Formatos.numero(minimo) + " e " + Formatos.numero(maximo) + ".");
            } catch (NumberFormatException e) {
                erro("Valor numérico inválido.");
            }
        }
    }

    public BigDecimal lerValorMonetario(String rotulo) {
        return BigDecimal.valueOf(lerDecimal(rotulo + " (R$)", 0, 1_000_000)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Lê uma data no formato dd/mm/aaaa. Se {@code padrao} não for nulo, Enter usa o padrão.
     * Também aceita "hoje".
     */
    public LocalDate lerData(String rotulo, LocalDate padrao) {
        String sufixo = padrao == null ? " (dd/mm/aaaa)" : " (dd/mm/aaaa) [" + Formatos.data(padrao) + "]";
        while (true) {
            String valor = lerLinha(rotulo + sufixo);
            if (valor.isEmpty() && padrao != null) {
                return padrao;
            }
            if (valor.equalsIgnoreCase("hoje")) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(valor, Formatos.DATA);
            } catch (DateTimeParseException e) {
                erro("Data inválida. Use o formato dd/mm/aaaa, ex.: 05/12/2026.");
            }
        }
    }

    /** Data opcional: Enter devolve null. */
    public LocalDate lerDataOpcional(String rotulo) {
        while (true) {
            String valor = lerLinha(rotulo + " (dd/mm/aaaa, Enter para nenhuma)");
            if (valor.isEmpty()) {
                return null;
            }
            try {
                return LocalDate.parse(valor, Formatos.DATA);
            } catch (DateTimeParseException e) {
                erro("Data inválida. Use o formato dd/mm/aaaa.");
            }
        }
    }

    public boolean confirmar(String pergunta) {
        while (true) {
            String valor = lerLinha(pergunta + " (s/n)").toLowerCase();
            if (valor.equals("s") || valor.equals("sim")) {
                return true;
            }
            if (valor.equals("n") || valor.equals("nao") || valor.equals("não")) {
                return false;
            }
            erro("Responda com s ou n.");
        }
    }

    /** Mostra as opções de um enum numeradas e devolve a escolhida. */
    public <E extends Enum<E>> E escolher(String rotulo, E[] valores, Function<E, String> descricao) {
        saida.println(rotulo + ":");
        for (int i = 0; i < valores.length; i++) {
            saida.println("  " + (i + 1) + " - " + descricao.apply(valores[i]));
        }
        int opcao = lerInteiro("Escolha", 1, valores.length);
        return valores[opcao - 1];
    }

    public void pausar() {
        saida.print("\nPressione Enter para continuar...");
        saida.flush();
        if (!entrada.hasNextLine()) {
            throw new EntradaEncerradaException();
        }
        entrada.nextLine();
    }

    // ---------- escrita ----------

    public void titulo(String texto) {
        saida.println();
        saida.println(LINHA);
        saida.println("  " + texto.toUpperCase());
        saida.println(LINHA);
    }

    public void subtitulo(String texto) {
        saida.println();
        saida.println("> " + texto);
    }

    public void info(String texto) {
        saida.println(texto);
    }

    public void campo(String rotulo, Object valor) {
        saida.printf("  %-22s %s%n", rotulo + ":", valor == null ? "-" : valor);
    }

    public void sucesso(String texto) {
        saida.println("[OK] " + texto);
    }

    public void aviso(String texto) {
        saida.println("[ATENÇÃO] " + texto);
    }

    public void erro(String texto) {
        saida.println("[ERRO] " + texto);
    }

    /** Imprime uma tabela com colunas ajustadas ao maior conteúdo (limitadas a 40 caracteres). */
    public void tabela(String[] cabecalho, List<String[]> linhas) {
        if (linhas.isEmpty()) {
            saida.println("  (nenhum registro)");
            return;
        }
        int[] larguras = new int[cabecalho.length];
        for (int i = 0; i < cabecalho.length; i++) {
            larguras[i] = cabecalho[i].length();
        }
        for (String[] linha : linhas) {
            for (int i = 0; i < cabecalho.length && i < linha.length; i++) {
                larguras[i] = Math.min(40, Math.max(larguras[i], texto(linha[i]).length()));
            }
        }
        imprimirLinha(cabecalho, larguras);
        StringBuilder separador = new StringBuilder("  ");
        for (int largura : larguras) {
            separador.append("-".repeat(largura)).append("  ");
        }
        saida.println(separador.toString().stripTrailing());
        for (String[] linha : linhas) {
            imprimirLinha(linha, larguras);
        }
    }

    private void imprimirLinha(String[] celulas, int[] larguras) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < larguras.length; i++) {
            String valor = i < celulas.length ? texto(celulas[i]) : "";
            if (valor.length() > larguras[i]) {
                valor = valor.substring(0, larguras[i] - 1) + "~";
            }
            sb.append(String.format("%-" + larguras[i] + "s", valor)).append("  ");
        }
        saida.println(sb.toString().stripTrailing());
    }

    private static String texto(String valor) {
        return valor == null ? "" : valor;
    }

    /** "1.250,50" e "1250.5" viram 1250.5. Com vírgula, o ponto é tratado como separador de milhar. */
    private static double converterNumero(String valor) {
        String normalizado = valor.trim();
        if (normalizado.contains(",")) {
            normalizado = normalizado.replace(".", "").replace(',', '.');
        }
        return Double.parseDouble(normalizado);
    }
}
