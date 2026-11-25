package br.com.translog.servico;

import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.StatusEntrega;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Números do painel inicial. */
public record PainelResumo(
        Map<StatusEntrega, Long> entregasPorStatus,
        List<Entrega> atrasadas,
        List<Entrega> proximasSaidas,
        BigDecimal faturamentoDoMes,
        long notasDoMes) {
}
