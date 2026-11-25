package br.com.translog.persistencia;

import br.com.translog.excecao.PersistenciaException;
import br.com.translog.modelo.Identificavel;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Repositório que grava todos os registros de um tipo em um arquivo JSON.
 *
 * <ul>
 *   <li>Arquivo ausente: começa vazio e o arquivo é criado no primeiro salvamento.</li>
 *   <li>Arquivo corrompido: é renomeado para *.corrompido-DATA (para não perder nada)
 *       e o sistema segue com o repositório vazio, avisando o usuário.</li>
 *   <li>A gravação é feita num arquivo temporário e depois movida, para que uma queda
 *       no meio da escrita não deixe o JSON pela metade.</li>
 * </ul>
 */
public class RepositorioJson<T extends Identificavel> extends RepositorioMemoria<T> {

    private static final DateTimeFormatter SUFIXO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path arquivo;
    private final Type tipoLista;
    private final Gson gson;
    private String aviso;

    public RepositorioJson(Path arquivo, Type tipoLista, Gson gson) {
        this.arquivo = arquivo;
        this.tipoLista = tipoLista;
        this.gson = gson;
        carregar();
    }

    /** Mensagem sobre algum problema encontrado ao carregar o arquivo, se houver. */
    public Optional<String> getAviso() {
        return Optional.ofNullable(aviso);
    }

    @Override
    protected void aposAlteracao() {
        gravar();
    }

    private void carregar() {
        if (!Files.exists(arquivo)) {
            return;
        }
        String conteudo;
        try {
            conteudo = Files.readString(arquivo, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível ler o arquivo " + arquivo + ": " + e.getMessage(), e);
        }
        if (conteudo.isBlank()) {
            return;
        }
        try {
            List<T> lista = gson.fromJson(conteudo, tipoLista);
            if (lista != null) {
                for (T item : lista) {
                    if (item != null && item.getId() > 0) {
                        registros.put(item.getId(), item);
                    }
                }
            }
        } catch (JsonParseException | IllegalStateException | IllegalArgumentException e) {
            isolarArquivoCorrompido();
        }
    }

    private void isolarArquivoCorrompido() {
        registros.clear();
        Path copia = arquivo.resolveSibling(arquivo.getFileName() + ".corrompido-" + LocalDateTime.now().format(SUFIXO));
        try {
            Files.move(arquivo, copia, StandardCopyOption.REPLACE_EXISTING);
            aviso = "O arquivo " + arquivo.getFileName() + " estava corrompido e foi guardado como "
                    + copia.getFileName() + ". Os dados desse tipo começaram vazios.";
        } catch (IOException e) {
            throw new PersistenciaException("O arquivo " + arquivo + " está corrompido e não pôde ser isolado.", e);
        }
    }

    private void gravar() {
        Path temporario = arquivo.resolveSibling(arquivo.getFileName() + ".tmp");
        try {
            Path pasta = arquivo.toAbsolutePath().getParent();
            if (pasta != null) {
                Files.createDirectories(pasta);
            }
            Files.writeString(temporario, gson.toJson(listarTodos(), tipoLista), StandardCharsets.UTF_8);
            try {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporario, arquivo, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new PersistenciaException("Não foi possível salvar " + arquivo.getFileName() + ": " + e.getMessage(), e);
        }
    }
}
