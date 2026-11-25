package br.com.translog.servico;

import br.com.translog.modelo.Entrega;
import br.com.translog.persistencia.Repositorio;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Controle de ocupação de motoristas e veículos (RN18, RN19). Uma entrega pendente bloqueia o
 * motorista e o veículo do dia agendado até o fim estimado da viagem
 * (distância / 500 km por dia).
 */
public class AgendaServico {

    private static final int DIAS_DE_BUSCA = 90;

    private final Repositorio<Entrega> entregas;

    public AgendaServico(Repositorio<Entrega> entregas) {
        this.entregas = entregas;
    }

    /**
     * Entregas pendentes que usam o motorista ou o veículo no período.
     *
     * @param ignorarEntregaId usado no reagendamento, para a entrega não conflitar com ela mesma
     */
    public List<Entrega> conflitos(int motoristaId, int veiculoId, LocalDate inicio, LocalDate fim, int ignorarEntregaId) {
        return entregas.filtrar(e -> e.getId() != ignorarEntregaId
                && (e.getMotoristaId() == motoristaId || e.getVeiculoId() == veiculoId)
                && e.ocupaPeriodo(inicio, fim));
    }

    /** Primeira data a partir de {@code aPartirDe} em que motorista e veículo estão livres pela duração toda. */
    public Optional<LocalDate> proximaDataLivre(int motoristaId, int veiculoId, LocalDate aPartirDe,
                                                int diasDuracao, int ignorarEntregaId) {
        for (int i = 0; i <= DIAS_DE_BUSCA; i++) {
            LocalDate inicio = aPartirDe.plusDays(i);
            LocalDate fim = inicio.plusDays(diasDuracao - 1L);
            if (conflitos(motoristaId, veiculoId, inicio, fim, ignorarEntregaId).isEmpty()) {
                return Optional.of(inicio);
            }
        }
        return Optional.empty();
    }

    public List<Entrega> agendaDoMotorista(int motoristaId) {
        return pendentes(e -> e.getMotoristaId() == motoristaId);
    }

    public List<Entrega> agendaDoVeiculo(int veiculoId) {
        return pendentes(e -> e.getVeiculoId() == veiculoId);
    }

    public boolean motoristaLivreEm(int motoristaId, LocalDate dia) {
        return !entregas.existe(e -> e.getMotoristaId() == motoristaId && e.ocupaPeriodo(dia, dia));
    }

    public boolean veiculoLivreEm(int veiculoId, LocalDate dia) {
        return !entregas.existe(e -> e.getVeiculoId() == veiculoId && e.ocupaPeriodo(dia, dia));
    }

    private List<Entrega> pendentes(Predicate<Entrega> filtro) {
        return entregas.filtrar(e -> filtro.test(e) && e.getStatus().isPendente()).stream()
                .sorted(Comparator.comparing(Entrega::getDataAgendada))
                .toList();
    }
}
