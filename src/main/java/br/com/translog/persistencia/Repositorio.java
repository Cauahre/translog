package br.com.translog.persistencia;

import br.com.translog.modelo.Identificavel;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Operações de armazenamento usadas pelos serviços. Os serviços dependem só desta
 * interface, então dá para trocar JSON por banco de dados sem mexer na regra de negócio
 * (e nos testes usamos a versão em memória).
 */
public interface Repositorio<T extends Identificavel> {

    /** Insere (gerando o id) ou atualiza o registro. */
    T salvar(T entidade);

    void remover(int id);

    Optional<T> buscarPorId(int id);

    List<T> listarTodos();

    List<T> filtrar(Predicate<T> condicao);

    boolean existe(Predicate<T> condicao);

    boolean estaVazio();
}
