package br.com.translog.servico;

import br.com.translog.modelo.Carga;
import br.com.translog.modelo.Rota;

import java.time.LocalDate;

/** Dados que o atendente informa para pedir uma entrega (ou simular o frete). */
public record SolicitacaoEntrega(
        int clienteId,
        int motoristaId,
        int veiculoId,
        Carga carga,
        Rota rota,
        LocalDate dataAgendada,
        boolean urgente) {
}
