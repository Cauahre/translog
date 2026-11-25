package br.com.translog.persistencia;

import br.com.translog.modelo.Identificavel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Repositório que mantém os registros em um mapa. Serve sozinho para os testes e é a
 * base do {@link RepositorioJson}, que só acrescenta a gravação em arquivo.
 */
public class RepositorioMemoria<T extends Identificavel> implements Repositorio<T> {

    protected final Map<Integer, T> registros = new LinkedHashMap<>();

    @Override
    public T salvar(T entidade) {
        if (entidade.getId() <= 0) {
            entidade.setId(proximoId());
        }
        registros.put(entidade.getId(), entidade);
        aposAlteracao();
        return entidade;
    }

    @Override
    public void remover(int id) {
        if (registros.remove(id) != null) {
            aposAlteracao();
        }
    }

    @Override
    public Optional<T> buscarPorId(int id) {
        return Optional.ofNullable(registros.get(id));
    }

    @Override
    public List<T> listarTodos() {
        return new ArrayList<>(registros.values());
    }

    @Override
    public List<T> filtrar(Predicate<T> condicao) {
        return registros.values().stream().filter(condicao).toList();
    }

    @Override
    public boolean existe(Predicate<T> condicao) {
        return registros.values().stream().anyMatch(condicao);
    }

    @Override
    public boolean estaVazio() {
        return registros.isEmpty();
    }

    /** Chamado depois de cada inclusão, alteração ou remoção. */
    protected void aposAlteracao() {
        // em memória não há nada a fazer
    }

    private int proximoId() {
        return registros.keySet().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
    }
}
