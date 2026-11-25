package br.com.translog.ui;

import br.com.translog.modelo.Carga;
import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Contrato;
import br.com.translog.modelo.DetalheFrete;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Motorista;
import br.com.translog.modelo.NaturezaCarga;
import br.com.translog.modelo.NotaFiscal;
import br.com.translog.modelo.RegistroStatus;
import br.com.translog.modelo.Rota;
import br.com.translog.modelo.StatusEntrega;
import br.com.translog.modelo.TipoCarga;
import br.com.translog.modelo.Veiculo;
import br.com.translog.relatorio.GeradorRelatorios;
import br.com.translog.servico.AgendaServico;
import br.com.translog.servico.ClienteServico;
import br.com.translog.servico.EntregaServico;
import br.com.translog.servico.FrotaServico;
import br.com.translog.servico.SolicitacaoEntrega;
import br.com.translog.util.Formatos;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuEntregas extends Menu {

    private final EntregaServico entregas;
    private final ClienteServico clientes;
    private final FrotaServico frota;
    private final AgendaServico agenda;
    private final GeradorRelatorios relatorios;

    public MenuEntregas(Console console, EntregaServico entregas, ClienteServico clientes, FrotaServico frota,
                        AgendaServico agenda, GeradorRelatorios relatorios) {
        super(console);
        this.entregas = entregas;
        this.clientes = clientes;
        this.frota = frota;
        this.agenda = agenda;
        this.relatorios = relatorios;
    }

    @Override
    protected String titulo() {
        return "Entregas";
    }

    @Override
    protected void antesDeExibir() {
        int atrasadas = entregas.listarAtrasadas().size();
        if (atrasadas > 0) {
            console.aviso(atrasadas + " entrega(s) pendente(s) com prazo vencido. Veja a opção 10.");
        }
    }

    @Override
    protected List<String> opcoes() {
        return List.of(
                "Simular frete (orçamento)",
                "Agendar entrega",
                "Listar entregas (ordem de prioridade)",
                "Filtrar por status",
                "Detalhes e histórico de uma entrega",
                "Iniciar transporte (saída)",
                "Concluir entrega",
                "Cancelar entrega",
                "Reagendar entrega",
                "Entregas atrasadas",
                "Gerar relatório de entregas em PDF");
    }

    @Override
    protected void executar(int opcao) {
        switch (opcao) {
            case 1 -> simular();
            case 2 -> agendar();
            case 3 -> listar(entregas.listarPorPrioridade());
            case 4 -> listar(entregas.listarPorStatus(console.escolher("Status", StatusEntrega.values(),
                    StatusEntrega::getDescricao)));
            case 5 -> detalhar();
            case 6 -> iniciar();
            case 7 -> concluir();
            case 8 -> cancelar();
            case 9 -> reagendar();
            case 10 -> listar(entregas.listarAtrasadas());
            case 11 -> gerarPdf();
            default -> console.erro("Opção inválida.");
        }
    }

    // ---------- operações ----------

    private void simular() {
        console.subtitulo("Simulação de frete");
        Cliente cliente = escolherCliente();
        Carga carga = lerCarga();
        double km = console.lerDecimal("Distância total (km)", 1, 10_000);
        boolean urgente = cliente.podeSolicitarUrgencia() && console.confirmar("Entrega urgente?");
        DetalheFrete frete = entregas.simular(cliente.getId(), carga, km, urgente, entregas.hoje());
        imprimirFrete(frete, carga, km);
        console.info("\nCNH mínima para esta carga: " + carga.getTipo().getCnhMinima()
                + " | Veículos com capacidade: " + frota.veiculosQueSuportam(carga.getPesoKg()).size());
    }

    private void agendar() {
        console.subtitulo("Agendamento de entrega");
        Cliente cliente = escolherCliente();
        Contrato contrato = clientes.contratoAtual(cliente.getId()).orElse(null);
        if (contrato == null) {
            console.erro("O cliente não tem contrato ativo. Registre um contrato no menu de clientes.");
            return;
        }
        console.info("Contrato #" + contrato.getId() + ": prazo de " + contrato.getPrazoEntregaDias() + " dias"
                + (contrato.getFreteMinimo().signum() > 0 ? ", frete mínimo " + Formatos.moeda(contrato.getFreteMinimo()) : ""));

        Carga carga = lerCarga();
        Rota rota = lerRota();
        boolean urgente = false;
        if (cliente.podeSolicitarUrgencia()) {
            urgente = console.confirmar("Entrega urgente? (+30%, prioridade no despacho)");
        } else {
            console.info("Cliente empresarial: entrega regular (urgência só para clientes prioritários).");
        }
        LocalDate data = console.lerData("Data de saída", entregas.hoje().plusDays(1));
        LocalDate fim = data.plusDays(rota.getDiasEstimados() - 1L);

        mostrarMotoristasAptos(carga, data, fim);
        int motoristaId = console.lerCodigo("Código do motorista");
        mostrarVeiculosAptos(carga, data, fim);
        int veiculoId = console.lerCodigo("Código do veículo");

        DetalheFrete previa = entregas.simular(cliente.getId(), carga, rota.getDistanciaKm(), urgente, data);
        imprimirFrete(previa, carga, rota.getDistanciaKm());
        console.info("Viagem estimada em " + rota.getDiasEstimados() + " dia(s). Entrega até "
                + Formatos.data(data.plusDays(contrato.getPrazoEntregaDias())) + " sem multa.");
        if (rota.getDiasEstimados() > contrato.getPrazoEntregaDias()) {
            console.aviso("A duração estimada da viagem é maior que o prazo do contrato; há risco de multa por atraso.");
        }
        if (!console.confirmar("Confirmar agendamento?")) {
            console.info("Agendamento descartado.");
            return;
        }
        Entrega entrega = entregas.agendar(new SolicitacaoEntrega(cliente.getId(), motoristaId, veiculoId,
                carga, rota, data, urgente));
        console.sucesso("Entrega #" + entrega.getId() + " agendada para " + Formatos.data(entrega.getDataAgendada())
                + " - valor " + Formatos.moeda(entrega.getFrete().getValorFrete()) + ".");
    }

    private void detalhar() {
        Entrega e = entregas.buscar(console.lerCodigo("Número da entrega"));
        Motorista m = frota.buscarMotorista(e.getMotoristaId());
        Veiculo v = frota.buscarVeiculo(e.getVeiculoId());
        console.subtitulo("Entrega #" + e.getId() + (e.isUrgente() ? " [URGENTE]" : ""));
        console.campo("Cliente", nomeCliente(e.getClienteId()));
        console.campo("Status", e.getStatus().getDescricao() + (e.isAtrasadaEm(entregas.hoje()) ? " - PRAZO VENCIDO" : ""));
        console.campo("Rota", e.getRota() + " (" + Formatos.numero(e.getRota().getDistanciaKm()) + " km)");
        console.campo("Carga", e.getCarga().getDescricao() + ", " + Formatos.numero(e.getCarga().getPesoKg()) + " kg, "
                + e.getCarga().getTipo().getDescricao() + ", " + e.getCarga().getNatureza().getDescricao());
        console.campo("Motorista", m.getNome() + " (CNH " + m.getCategoriaCnh() + ")");
        console.campo("Veículo", v.getPlaca() + " - " + v.getModelo());
        console.campo("Solicitada em", Formatos.data(e.getDataSolicitacao()));
        console.campo("Saída", Formatos.data(e.getDataAgendada()));
        console.campo("Reserva até", Formatos.data(e.getFimOcupacao()));
        console.campo("Prazo do contrato", Formatos.data(e.getDataLimite()) + " (" + e.getPrazoDias() + " dias)");
        console.campo("Entregue em", Formatos.data(e.getDataEntrega()));
        if (e.getMotivoCancelamento() != null) {
            console.campo("Motivo cancelamento", e.getMotivoCancelamento());
        }
        imprimirFrete(e.getFrete(), e.getCarga(), e.getRota().getDistanciaKm());

        console.subtitulo("Histórico");
        List<String[]> linhas = new ArrayList<>();
        for (RegistroStatus r : e.getHistorico()) {
            linhas.add(new String[]{Formatos.dataHora(r.getDataHora()), r.getStatus().getDescricao(), r.getObservacao()});
        }
        console.tabela(new String[]{"Data/hora", "Status", "Observação"}, linhas);
    }

    private void iniciar() {
        int id = console.lerCodigo("Número da entrega");
        String obs = console.lerTextoOpcional("Observação");
        Entrega e = entregas.iniciarTransporte(id, obs);
        console.sucesso("Entrega #" + e.getId() + " em trânsito.");
    }

    private void concluir() {
        int id = console.lerCodigo("Número da entrega");
        Entrega e = entregas.buscar(id);
        console.info("Prazo sem multa: " + Formatos.data(e.getDataLimite()));
        LocalDate data = console.lerData("Data da entrega", entregas.hoje());
        String obs = console.lerTextoOpcional("Observação (ex.: recebido por)");
        NotaFiscal nota = entregas.concluir(id, data, obs);
        Entrega concluida = entregas.buscar(id);
        if (concluida.getFrete().getDiasAtraso() > 0) {
            console.aviso("Entrega com " + concluida.getFrete().getDiasAtraso() + " dia(s) de atraso: abatimento de "
                    + Formatos.percentual(concluida.getFrete().getPercentualPenalidade()) + " ("
                    + Formatos.moeda(concluida.getFrete().getPenalidade()) + ").");
        }
        console.sucesso("Entrega concluída. Nota fiscal nº " + nota.getNumeroFormatado() + " emitida no valor de "
                + Formatos.moeda(nota.getValorTotal()) + ".");
        if (console.confirmar("Gerar o PDF da nota agora?")) {
            Path arquivo = relatorios.notaFiscal(nota.getId());
            console.sucesso("PDF salvo em " + arquivo.toAbsolutePath());
        }
    }

    private void cancelar() {
        int id = console.lerCodigo("Número da entrega");
        Entrega e = entregas.buscar(id);
        if (!console.confirmar("Cancelar a entrega #" + e.getId() + " (" + e.getRota() + ")?")) {
            return;
        }
        String motivo = console.lerTextoOpcional("Motivo");
        entregas.cancelar(id, motivo);
        console.sucesso("Entrega cancelada. Motorista e veículo foram liberados na agenda.");
    }

    private void reagendar() {
        int id = console.lerCodigo("Número da entrega");
        Entrega e = entregas.buscar(id);
        console.info("Saída atual: " + Formatos.data(e.getDataAgendada()));
        LocalDate nova = console.lerData("Nova data de saída", null);
        entregas.reagendar(id, nova);
        console.sucesso("Entrega reagendada para " + Formatos.data(nova) + ".");
    }

    private void gerarPdf() {
        Path arquivo = relatorios.entregas();
        console.sucesso("Relatório salvo em " + arquivo.toAbsolutePath());
    }

    // ---------- leitura de dados ----------

    private Cliente escolherCliente() {
        List<String[]> linhas = new ArrayList<>();
        for (Cliente c : clientes.listar()) {
            if (c.isAtivo()) {
                linhas.add(new String[]{String.valueOf(c.getId()), c.getNome(), c.getTipo().getDescricao()});
            }
        }
        console.tabela(new String[]{"Cód", "Cliente", "Tipo"}, linhas);
        return clientes.buscar(console.lerCodigo("Código do cliente"));
    }

    private Carga lerCarga() {
        String descricao = console.lerTexto("Descrição da carga");
        double peso = console.lerDecimal("Peso (kg)", 0.1, 60_000);
        NaturezaCarga natureza = console.escolher("Natureza da carga", NaturezaCarga.values(),
                n -> n.getDescricao() + (n.exigeTransporteEspecial() ? " (transporte especial, +40%)" : ""));
        Carga carga = new Carga(descricao, peso, natureza);
        TipoCarga tipo = carga.getTipo();
        console.info("Classificação: carga " + tipo.getDescricao().toLowerCase() + " ("
                + Formatos.moeda(tipo.getValorPorKm()) + "/km, exige CNH " + tipo.getCnhMinima() + " ou superior).");
        return carga;
    }

    private Rota lerRota() {
        console.info("Informe as cidades na ordem da viagem, separadas por ponto e vírgula.");
        console.info("Ex.: Teresina; Campo Maior; Piripiri; Parnaíba");
        while (true) {
            String texto = console.lerTexto("Cidades");
            List<String> cidades = Arrays.stream(texto.split("[;>]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            if (cidades.size() >= 2) {
                double km = console.lerDecimal("Distância total da rota (km)", 1, 10_000);
                return new Rota(cidades, km);
            }
            console.erro("Informe pelo menos a origem e o destino.");
        }
    }

    private void mostrarMotoristasAptos(Carga carga, LocalDate inicio, LocalDate fim) {
        console.subtitulo("Motoristas aptos e livres de " + Formatos.data(inicio) + " a " + Formatos.data(fim));
        List<String[]> linhas = new ArrayList<>();
        for (Motorista m : frota.listarMotoristas()) {
            boolean apto = m.isAtivo() && m.podeConduzir(carga.getTipo().getCnhMinima(), inicio);
            boolean livre = agenda.conflitos(m.getId(), -1, inicio, fim, 0).isEmpty();
            if (apto && livre) {
                linhas.add(new String[]{String.valueOf(m.getId()), m.getNome(), "CNH " + m.getCategoriaCnh()});
            }
        }
        console.tabela(new String[]{"Cód", "Motorista", "Categoria"}, linhas);
    }

    private void mostrarVeiculosAptos(Carga carga, LocalDate inicio, LocalDate fim) {
        console.subtitulo("Veículos com capacidade e livres no período");
        List<String[]> linhas = new ArrayList<>();
        for (Veiculo v : frota.veiculosQueSuportam(carga.getPesoKg())) {
            if (agenda.conflitos(-1, v.getId(), inicio, fim, 0).isEmpty()) {
                linhas.add(new String[]{String.valueOf(v.getId()), v.getPlaca(), v.getModelo(),
                        Formatos.numero(v.getCapacidadeKg()) + " kg", "CNH " + v.getTipo().getCnhMinima()});
            }
        }
        console.tabela(new String[]{"Cód", "Placa", "Modelo", "Capacidade", "Exige"}, linhas);
    }

    // ---------- exibição ----------

    private void listar(List<Entrega> lista) {
        LocalDate hoje = entregas.hoje();
        List<String[]> linhas = new ArrayList<>();
        for (Entrega e : lista) {
            linhas.add(new String[]{
                    String.valueOf(e.getId()),
                    e.isUrgente() ? "URG" : "",
                    nomeCliente(e.getClienteId()),
                    e.getRota().getOrigem() + " > " + e.getRota().getDestino(),
                    Formatos.data(e.getDataAgendada()),
                    Formatos.data(e.getDataLimite()),
                    e.getStatus().getDescricao() + (e.isAtrasadaEm(hoje) ? " (atrasada)" : ""),
                    Formatos.moeda(e.getFrete().getValorFinal())});
        }
        console.tabela(new String[]{"#", "", "Cliente", "Rota", "Saída", "Prazo", "Status", "Valor"}, linhas);
    }

    private void imprimirFrete(DetalheFrete f, Carga carga, double km) {
        console.subtitulo("Memória de cálculo");
        console.campo("Base", Formatos.numero(km) + " km x " + Formatos.moeda(carga.getTipo().getValorPorKm())
                + " = " + Formatos.moeda(f.getValorBase()));
        if (f.getAdicionalEspecial().signum() > 0) {
            console.campo("Transporte especial", "+ " + Formatos.moeda(f.getAdicionalEspecial()) + " (40%)");
        }
        if (f.getAdicionalUrgencia().signum() > 0) {
            console.campo("Urgência", "+ " + Formatos.moeda(f.getAdicionalUrgencia()) + " (30%)");
        }
        if (f.getDesconto().signum() > 0) {
            console.campo("Desconto do cliente", "- " + Formatos.moeda(f.getDesconto())
                    + " (" + Formatos.percentual(f.getPercentualDesconto()) + ")");
        }
        if (f.getAjusteFreteMinimo().signum() > 0) {
            console.campo("Ajuste frete mínimo", "+ " + Formatos.moeda(f.getAjusteFreteMinimo()));
        }
        console.campo("Frete", Formatos.moeda(f.getValorFrete()));
        if (f.getPenalidade().signum() > 0) {
            console.campo("Abatimento atraso", "- " + Formatos.moeda(f.getPenalidade()) + " (" + f.getDiasAtraso()
                    + " dia(s), " + Formatos.percentual(f.getPercentualPenalidade()) + ")");
            console.campo("Valor final", Formatos.moeda(f.getValorFinal()));
        }
    }

    private String nomeCliente(int id) {
        try {
            return clientes.buscar(id).getNome();
        } catch (RuntimeException e) {
            return "Cliente #" + id;
        }
    }
}
