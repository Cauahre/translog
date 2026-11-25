package br.com.translog.relatorio;

import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.StatusEntrega;
import br.com.translog.util.Formatos;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/** Relatório operacional com todas as entregas, na ordem de prioridade. */
public class EntregasPdf extends DocumentoPdf {

    private final List<Entrega> entregas;
    private final IntFunction<String> nomeCliente;
    private final LocalDate hoje;

    public EntregasPdf(List<Entrega> entregas, IntFunction<String> nomeCliente, LocalDate hoje) {
        this.entregas = entregas;
        this.nomeCliente = nomeCliente;
        this.hoje = hoje;
    }

    @Override
    protected String getTitulo() {
        return "Relatório de Entregas";
    }

    @Override
    protected String getSubtitulo() {
        return "Situação em " + Formatos.data(hoje) + " - " + entregas.size() + " entrega(s)";
    }

    @Override
    protected void escreverCorpo() throws IOException {
        secao("Resumo por status");
        Map<StatusEntrega, Integer> contagem = new EnumMap<>(StatusEntrega.class);
        long atrasadas = 0;
        for (Entrega e : entregas) {
            contagem.merge(e.getStatus(), 1, Integer::sum);
            if (e.isAtrasadaEm(hoje)) {
                atrasadas++;
            }
        }
        for (StatusEntrega s : StatusEntrega.values()) {
            campo(s.getDescricao(), String.valueOf(contagem.getOrDefault(s, 0)));
        }
        campo("Pendentes com prazo vencido", String.valueOf(atrasadas));

        secao("Entregas");
        List<String[]> linhas = new ArrayList<>();
        for (Entrega e : entregas) {
            String status = e.isAtrasadaEm(hoje) ? "ATRASADA" : e.getStatus().getDescricao();
            linhas.add(new String[]{
                    String.valueOf(e.getId()),
                    nomeCliente.apply(e.getClienteId()),
                    e.getRota().getOrigem() + " > " + e.getRota().getDestino(),
                    Formatos.data(e.getDataAgendada()),
                    Formatos.data(e.getDataLimite()),
                    e.isUrgente() ? "Sim" : "Não",
                    status,
                    Formatos.moeda(e.getFrete().getValorFinal())});
        }
        tabela(new String[]{"#", "Cliente", "Rota", "Saída", "Prazo", "Urg.", "Status", "Valor"},
                new float[]{0.05f, 0.20f, 0.19f, 0.115f, 0.115f, 0.06f, 0.13f, 0.14f},
                linhas,
                new boolean[]{true, false, false, false, false, false, false, true});
    }
}
