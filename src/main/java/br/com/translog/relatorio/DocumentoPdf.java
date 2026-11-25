package br.com.translog.relatorio;

import br.com.translog.excecao.PersistenciaException;
import br.com.translog.util.Formatos;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Base dos documentos PDF (nota fiscal e relatórios).
 * <p>
 * Usa o padrão Template Method: {@link #gerar(Path)} define a sequência fixa
 * (página, cabeçalho, corpo, rodapé, gravação) e cada subclasse só implementa
 * {@link #getTitulo()} e {@link #escreverCorpo()}. Os métodos protegidos
 * (seções, campos, tabelas) cuidam da quebra de página automaticamente.
 */
public abstract class DocumentoPdf {

    protected static final PDFont FONTE = PDType1Font.HELVETICA;
    protected static final PDFont FONTE_NEGRITO = PDType1Font.HELVETICA_BOLD;

    private static final PDRectangle TAMANHO = PDRectangle.A4;
    private static final float MARGEM = 50;
    private static final float RODAPE = 40;
    private static final Color AZUL = new Color(31, 73, 125);
    private static final Color CINZA_CLARO = new Color(240, 240, 240);
    private static final Color CINZA_LINHA = new Color(190, 190, 190);

    private PDDocument documento;
    private PDPageContentStream conteudo;
    private float y;

    /** Gera o arquivo e devolve o caminho dele. */
    public final Path gerar(Path destino) {
        try (PDDocument doc = new PDDocument()) {
            this.documento = doc;
            novaPagina();
            escreverCabecalho();
            escreverCorpo();
            fecharPagina();
            escreverRodapes();

            Path pasta = destino.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            doc.save(destino.toFile());
            return destino;
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível gerar o PDF " + destino.getFileName() + ": " + e.getMessage(), e);
        } finally {
            fecharSemErro();
            documento = null;
        }
    }

    protected abstract String getTitulo();

    protected abstract void escreverCorpo() throws IOException;

    /** Subtítulo opcional exibido abaixo do título (ex.: período do relatório). */
    protected String getSubtitulo() {
        return null;
    }

    // ---------- blocos de conteúdo ----------

    protected void secao(String titulo) throws IOException {
        garantirEspaco(40);
        y -= 14;
        escrever(titulo.toUpperCase(), FONTE_NEGRITO, 11, MARGEM, y, AZUL);
        y -= 5;
        linhaHorizontal(MARGEM, larguraUtil(), AZUL, 0.8f);
        y -= 14;
    }

    protected void campo(String rotulo, String valor) throws IOException {
        garantirEspaco(16);
        String textoRotulo = rotulo + ": ";
        escrever(textoRotulo, FONTE_NEGRITO, 10, MARGEM, y, Color.BLACK);
        float x = MARGEM + largura(textoRotulo, FONTE_NEGRITO, 10);
        List<String> linhas = quebrarLinhas(valor == null ? "-" : valor, FONTE, 10, larguraUtil() - (x - MARGEM));
        for (int i = 0; i < linhas.size(); i++) {
            if (i > 0) {
                y -= 13;
                garantirEspaco(13);
            }
            escrever(linhas.get(i), FONTE, 10, x, y, Color.BLACK);
        }
        y -= 15;
    }

    protected void paragrafo(String texto, float tamanho) throws IOException {
        for (String linha : quebrarLinhas(texto, FONTE, tamanho, larguraUtil())) {
            garantirEspaco(tamanho + 4);
            escrever(linha, FONTE, tamanho, MARGEM, y, Color.DARK_GRAY);
            y -= tamanho + 4;
        }
    }

    protected void espaco(float altura) {
        y -= altura;
    }

    /**
     * Tabela simples com cabeçalho cinza. Se não couber na página, continua na próxima
     * repetindo o cabeçalho.
     *
     * @param proporcoes largura relativa de cada coluna (somam 1)
     * @param aDireita   colunas numéricas, alinhadas à direita
     */
    protected void tabela(String[] cabecalho, float[] proporcoes, List<String[]> linhas, boolean[] aDireita)
            throws IOException {
        float alturaLinha = 16;
        float[] larguras = new float[proporcoes.length];
        for (int i = 0; i < proporcoes.length; i++) {
            larguras[i] = proporcoes[i] * larguraUtil();
        }
        garantirEspaco(alturaLinha * 2);
        linhaTabela(cabecalho, larguras, aDireita, FONTE_NEGRITO, CINZA_CLARO, alturaLinha);
        for (String[] linha : linhas) {
            if (y - alturaLinha < MARGEM + RODAPE) {
                novaPaginaContinuacao();
                linhaTabela(cabecalho, larguras, aDireita, FONTE_NEGRITO, CINZA_CLARO, alturaLinha);
            }
            linhaTabela(linha, larguras, aDireita, FONTE, null, alturaLinha);
        }
        y -= 6;
    }

    /** Linha de destaque com rótulo à esquerda e valor à direita (totais). */
    protected void destaque(String rotulo, String valor) throws IOException {
        garantirEspaco(26);
        float altura = 20;
        conteudo.setNonStrokingColor(AZUL);
        conteudo.addRect(MARGEM, y - altura + 14, larguraUtil(), altura);
        conteudo.fill();
        escrever(rotulo, FONTE_NEGRITO, 11, MARGEM + 6, y - 1, Color.WHITE);
        float larguraValor = largura(valor, FONTE_NEGRITO, 11);
        escrever(valor, FONTE_NEGRITO, 11, MARGEM + larguraUtil() - larguraValor - 6, y - 1, Color.WHITE);
        y -= altura + 8;
    }

    // ---------- estrutura da página ----------

    private void escreverCabecalho() throws IOException {
        escrever("TRANSLOG", FONTE_NEGRITO, 20, MARGEM, y, AZUL);
        String empresa = "Transportes e Logística";
        escrever(empresa, FONTE, 10, MARGEM + largura("TRANSLOG ", FONTE_NEGRITO, 20), y + 1, Color.DARK_GRAY);
        y -= 22;
        escrever(getTitulo(), FONTE_NEGRITO, 13, MARGEM, y, Color.BLACK);
        y -= 14;
        if (getSubtitulo() != null) {
            escrever(getSubtitulo(), FONTE, 10, MARGEM, y, Color.DARK_GRAY);
            y -= 12;
        }
        linhaHorizontal(MARGEM, larguraUtil(), Color.BLACK, 1.2f);
        y -= 8;
    }

    private void escreverRodapes() throws IOException {
        int total = documento.getNumberOfPages();
        String gerado = "Documento gerado pelo sistema Translog em " + Formatos.dataHora(LocalDateTime.now());
        for (int i = 0; i < total; i++) {
            PDPage pagina = documento.getPage(i);
            try (PDPageContentStream rodape = new PDPageContentStream(documento, pagina,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {
                this.conteudo = rodape;
                linhaHorizontalEm(MARGEM, RODAPE + 4, larguraUtil(), CINZA_LINHA, 0.5f);
                escrever(gerado, FONTE, 8, MARGEM, RODAPE - 8, Color.GRAY);
                String paginacao = "Página " + (i + 1) + " de " + total;
                escrever(paginacao, FONTE, 8, MARGEM + larguraUtil() - largura(paginacao, FONTE, 8), RODAPE - 8, Color.GRAY);
            }
        }
        this.conteudo = null;
    }

    private void novaPagina() throws IOException {
        fecharPagina();
        PDPage pagina = new PDPage(TAMANHO);
        documento.addPage(pagina);
        conteudo = new PDPageContentStream(documento, pagina);
        y = TAMANHO.getHeight() - MARGEM;
    }

    private void novaPaginaContinuacao() throws IOException {
        novaPagina();
        escrever(getTitulo() + " (continuação)", FONTE_NEGRITO, 10, MARGEM, y, Color.GRAY);
        y -= 20;
    }

    private void garantirEspaco(float altura) throws IOException {
        if (y - altura < MARGEM + RODAPE) {
            novaPaginaContinuacao();
        }
    }

    private void fecharPagina() throws IOException {
        if (conteudo != null) {
            conteudo.close();
            conteudo = null;
        }
    }

    private void fecharSemErro() {
        try {
            fecharPagina();
        } catch (IOException ignorada) {
            // o documento já falhou; o erro original é o que importa
        }
    }

    private void linhaTabela(String[] celulas, float[] larguras, boolean[] aDireita, PDFont fonte, Color fundo,
                             float altura) throws IOException {
        if (fundo != null) {
            conteudo.setNonStrokingColor(fundo);
            conteudo.addRect(MARGEM, y - 5, larguraUtil(), altura);
            conteudo.fill();
        }
        float x = MARGEM;
        for (int i = 0; i < larguras.length; i++) {
            String texto = i < celulas.length && celulas[i] != null ? celulas[i] : "";
            texto = cortar(texto, fonte, 9, larguras[i] - 8);
            float xTexto = aDireita != null && aDireita[i]
                    ? x + larguras[i] - largura(texto, fonte, 9) - 4
                    : x + 4;
            escrever(texto, fonte, 9, xTexto, y, Color.BLACK);
            x += larguras[i];
        }
        linhaHorizontalEm(MARGEM, y - 5, larguraUtil(), CINZA_LINHA, 0.4f);
        y -= altura;
    }

    // ---------- desenho de baixo nível ----------

    private void escrever(String texto, PDFont fonte, float tamanho, float x, float yTexto, Color cor) throws IOException {
        conteudo.beginText();
        conteudo.setNonStrokingColor(cor);
        conteudo.setFont(fonte, tamanho);
        conteudo.newLineAtOffset(x, yTexto);
        conteudo.showText(compativel(texto, fonte));
        conteudo.endText();
    }

    private void linhaHorizontal(float x, float largura, Color cor, float espessura) throws IOException {
        linhaHorizontalEm(x, y, largura, cor, espessura);
    }

    private void linhaHorizontalEm(float x, float yLinha, float largura, Color cor, float espessura) throws IOException {
        conteudo.setStrokingColor(cor);
        conteudo.setLineWidth(espessura);
        conteudo.moveTo(x, yLinha);
        conteudo.lineTo(x + largura, yLinha);
        conteudo.stroke();
    }

    private float larguraUtil() {
        return TAMANHO.getWidth() - 2 * MARGEM;
    }

    private static float largura(String texto, PDFont fonte, float tamanho) throws IOException {
        return fonte.getStringWidth(compativel(texto, fonte)) / 1000 * tamanho;
    }

    private static String cortar(String texto, PDFont fonte, float tamanho, float maximo) throws IOException {
        if (largura(texto, fonte, tamanho) <= maximo) {
            return texto;
        }
        String reticencias = "...";
        String atual = texto;
        while (!atual.isEmpty() && largura(atual + reticencias, fonte, tamanho) > maximo) {
            atual = atual.substring(0, atual.length() - 1);
        }
        return atual + reticencias;
    }

    private static List<String> quebrarLinhas(String texto, PDFont fonte, float tamanho, float maximo) throws IOException {
        List<String> linhas = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        for (String palavra : texto.split("\\s+")) {
            String tentativa = atual.isEmpty() ? palavra : atual + " " + palavra;
            if (largura(tentativa, fonte, tamanho) > maximo && !atual.isEmpty()) {
                linhas.add(atual.toString());
                atual = new StringBuilder(palavra);
            } else {
                atual = new StringBuilder(tentativa);
            }
        }
        if (!atual.isEmpty()) {
            linhas.add(atual.toString());
        }
        return linhas;
    }

    /**
     * As fontes padrão do PDF usam a codificação WinAnsi, que cobre os acentos do português
     * mas não todos os caracteres Unicode. O que não for suportado vira "?" em vez de
     * derrubar a geração do arquivo.
     */
    private static String compativel(String texto, PDFont fonte) {
        if (texto == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(texto.length());
        for (char c : texto.replace(' ', ' ').replace('\t', ' ').toCharArray()) {
            try {
                fonte.encode(String.valueOf(c));
                sb.append(c);
            } catch (IllegalArgumentException | IOException e) {
                sb.append('?');
            }
        }
        return sb.toString();
    }
}
