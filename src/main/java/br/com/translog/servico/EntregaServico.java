package br.com.translog.servico;

import br.com.translog.excecao.RegistroNaoEncontradoException;
import br.com.translog.excecao.RegraNegocioException;
import br.com.translog.excecao.ValidacaoException;
import br.com.translog.modelo.Carga;
import br.com.translog.modelo.CategoriaCnh;
import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Contrato;
import br.com.translog.modelo.DetalheFrete;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Motorista;
import br.com.translog.modelo.NotaFiscal;
import br.com.translog.modelo.StatusEntrega;
import br.com.translog.modelo.TipoCarga;
import br.com.translog.modelo.Veiculo;
import br.com.translog.persistencia.Repositorio;
import br.com.translog.util.Formatos;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Agendamento e acompanhamento das entregas. É aqui que as regras se juntam:
 * contrato vigente, urgência, CNH, capacidade do veículo, agenda, frete e penalidade.
 * <p>
 * O {@link Clock} é recebido no construtor para que os testes possam fixar o "hoje".
 */
public class EntregaServico {

    private final Repositorio<Entrega> entregas;
    private final ClienteServico clienteServico;
    private final FrotaServico frotaServico;
    private final AgendaServico agendaServico;
    private final FaturamentoServico faturamentoServico;
    private final CalculadoraFrete calculadora;
    private final Clock relogio;

    public EntregaServico(Repositorio<Entrega> entregas, ClienteServico clienteServico, FrotaServico frotaServico,
                          AgendaServico agendaServico, FaturamentoServico faturamentoServico,
                          CalculadoraFrete calculadora, Clock relogio) {
        this.entregas = entregas;
        this.clienteServico = clienteServico;
        this.frotaServico = frotaServico;
        this.agendaServico = agendaServico;
        this.faturamentoServico = faturamentoServico;
        this.calculadora = calculadora;
        this.relogio = relogio;
    }

    /**
     * Calcula o frete sem agendar nada (orçamento). Só verifica cliente, contrato e urgência,
     * já que motorista e veículo ainda podem não ter sido escolhidos.
     */
    public DetalheFrete simular(int clienteId, Carga carga, double distanciaKm,
                                boolean urgente, LocalDate data) {
        Cliente cliente = clienteServico.buscar(clienteId);
        verificarUrgencia(cliente, urgente);
        Contrato contrato = contratoVigente(cliente, data);
        return calculadora.calcular(cliente, contrato, carga, distanciaKm, urgente);
    }

    /** RF07. Verifica RN03, RN04, RN06, RN13 a RN16, RN19 e RN22 antes de salvar. */
    public Entrega agendar(SolicitacaoEntrega pedido) {
        LocalDate data = pedido.dataAgendada();
        if (data == null) {
            throw new ValidacaoException("Informe a data de saída da entrega.");
        }
        if (data.isBefore(hoje())) {
            throw new ValidacaoException("A data de saída não pode estar no passado.");
        }

        Cliente cliente = clienteServico.buscar(pedido.clienteId());
        if (!cliente.isAtivo()) {
            throw new RegraNegocioException("O cliente " + cliente.getNome() + " está inativo.");
        }
        verificarUrgencia(cliente, pedido.urgente());
        Contrato contrato = contratoVigente(cliente, data);

        Motorista motorista = frotaServico.buscarMotorista(pedido.motoristaId());
        Veiculo veiculo = frotaServico.buscarVeiculo(pedido.veiculoId());
        verificarMotoristaEVeiculo(motorista, veiculo, pedido, data);
        verificarAgenda(motorista, veiculo, data, pedido.rota().getDiasEstimados(), 0);

        DetalheFrete frete = calculadora.calcular(cliente, contrato, pedido.carga(),
                pedido.rota().getDistanciaKm(), pedido.urgente());

        Entrega entrega = new Entrega(cliente.getId(), contrato.getId(), motorista.getId(), veiculo.getId(),
                pedido.carga(), pedido.rota(), pedido.urgente(), hoje(), data,
                contrato.getPrazoEntregaDias(), frete);
        entrega.registrarCriacao(agora());
        return entregas.salvar(entrega);
    }

    public Entrega iniciarTransporte(int entregaId, String observacao) {
        Entrega entrega = buscar(entregaId);
        if (hoje().isBefore(entrega.getDataAgendada())) {
            throw new RegraNegocioException("A saída está agendada para " + Formatos.data(entrega.getDataAgendada())
                    + ". Para sair antes, reagende a entrega.");
        }
        entrega.mudarStatus(StatusEntrega.EM_TRANSITO, agora(), textoOuPadrao(observacao, "Saiu para entrega"));
        return entregas.salvar(entrega);
    }

    /**
     * Conclui a entrega, aplica o abatimento por atraso (RN24, RN25) e emite a nota fiscal (RN26).
     *
     * @return a nota fiscal emitida
     */
    public NotaFiscal concluir(int entregaId, LocalDate dataEntrega, String observacao) {
        Entrega entrega = buscar(entregaId);
        if (entrega.getStatus() != StatusEntrega.EM_TRANSITO) {
            throw new RegraNegocioException("Só entregas em trânsito podem ser concluídas (status atual: "
                    + entrega.getStatus().getDescricao() + ").");
        }
        if (dataEntrega == null || dataEntrega.isAfter(hoje())) {
            throw new ValidacaoException("A data de entrega não pode ser futura.");
        }
        if (dataEntrega.isBefore(entrega.getDataAgendada())) {
            throw new ValidacaoException("A data de entrega não pode ser anterior à data de saída ("
                    + Formatos.data(entrega.getDataAgendada()) + ").");
        }

        long atraso = entrega.diasDeAtraso(dataEntrega);
        calculadora.aplicarPenalidade(entrega.getFrete(), atraso);
        entrega.setDataEntrega(dataEntrega);
        String obs = textoOuPadrao(observacao, "Entrega realizada");
        if (atraso > 0) {
            obs += " (" + atraso + " dia(s) de atraso)";
        }
        entrega.mudarStatus(StatusEntrega.ENTREGUE, agora(), obs);
        entregas.salvar(entrega);

        Cliente cliente = clienteServico.buscar(entrega.getClienteId());
        return faturamentoServico.emitirNota(entrega, cliente);
    }

    public Entrega cancelar(int entregaId, String motivo) {
        Entrega entrega = buscar(entregaId);
        String motivoInformado = textoOuPadrao(motivo, "Cancelada sem motivo informado");
        entrega.mudarStatus(StatusEntrega.CANCELADA, agora(), motivoInformado);
        entrega.setMotivoCancelamento(motivoInformado);
        return entregas.salvar(entrega);
    }

    /** Muda a data de saída de uma entrega ainda não iniciada, checando a agenda de novo. */
    public Entrega reagendar(int entregaId, LocalDate novaData) {
        Entrega entrega = buscar(entregaId);
        if (novaData == null || novaData.isBefore(hoje())) {
            throw new ValidacaoException("A nova data não pode estar no passado.");
        }
        Cliente cliente = clienteServico.buscar(entrega.getClienteId());
        Contrato contrato = contratoVigente(cliente, novaData);
        Motorista motorista = frotaServico.buscarMotorista(entrega.getMotoristaId());
        Veiculo veiculo = frotaServico.buscarVeiculo(entrega.getVeiculoId());
        if (!motorista.isCnhValidaEm(novaData)) {
            throw new RegraNegocioException("A CNH de " + motorista.getNome() + " estará vencida em "
                    + Formatos.data(novaData) + ".");
        }
        verificarAgenda(motorista, veiculo, novaData, entrega.getRota().getDiasEstimados(), entrega.getId());
        entrega.reagendar(novaData, contrato.getPrazoEntregaDias(), agora());
        return entregas.salvar(entrega);
    }

    public Entrega buscar(int id) {
        return entregas.buscarPorId(id).orElseThrow(() -> new RegistroNaoEncontradoException("Entrega", id));
    }

    /**
     * Lista na ordem de despacho (RN23): primeiro as pendentes (urgentes na frente, depois por data),
     * em seguida as finalizadas, das mais recentes para as mais antigas.
     */
    public List<Entrega> listarPorPrioridade() {
        Comparator<Entrega> pendentesPrimeiro = Comparator.comparing(e -> !e.getStatus().isPendente());
        Comparator<Entrega> urgentesPrimeiro = Comparator.comparing(e -> !(e.isUrgente() && e.getStatus().isPendente()));
        Comparator<Entrega> porData = (a, b) -> a.getStatus().isPendente()
                ? a.getDataAgendada().compareTo(b.getDataAgendada())
                : b.getDataAgendada().compareTo(a.getDataAgendada());
        return entregas.listarTodos().stream()
                .sorted(pendentesPrimeiro.thenComparing(urgentesPrimeiro).thenComparing(porData))
                .toList();
    }

    public List<Entrega> listarPorStatus(StatusEntrega status) {
        return listarPorPrioridade().stream().filter(e -> e.getStatus() == status).toList();
    }

    public List<Entrega> listarDoCliente(int clienteId) {
        return listarPorPrioridade().stream().filter(e -> e.getClienteId() == clienteId).toList();
    }

    public List<Entrega> listarAtrasadas() {
        LocalDate hoje = hoje();
        return listarPorPrioridade().stream().filter(e -> e.isAtrasadaEm(hoje)).toList();
    }

    public PainelResumo painel() {
        Map<StatusEntrega, Long> porStatus = new EnumMap<>(StatusEntrega.class);
        for (StatusEntrega s : StatusEntrega.values()) {
            porStatus.put(s, 0L);
        }
        entregas.listarTodos().forEach(e -> porStatus.merge(e.getStatus(), 1L, Long::sum));

        LocalDate hoje = hoje();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        List<NotaFiscal> notasDoMes = faturamentoServico.listarNotas().stream()
                .filter(n -> !n.getDataEmissao().isBefore(inicioMes) && !n.getDataEmissao().isAfter(hoje))
                .toList();
        BigDecimal faturado = notasDoMes.stream().map(NotaFiscal::getValorTotal)
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);

        List<Entrega> proximas = listarPorStatus(StatusEntrega.AGENDADA).stream()
                .filter(e -> !e.getDataAgendada().isAfter(hoje.plusDays(7)))
                .limit(5)
                .toList();

        return new PainelResumo(porStatus, listarAtrasadas(), proximas, faturado, notasDoMes.size());
    }

    public LocalDate hoje() {
        return LocalDate.now(relogio);
    }

    // ---------- regras ----------

    private void verificarUrgencia(Cliente cliente, boolean urgente) {
        if (urgente && !cliente.podeSolicitarUrgencia()) {
            throw new RegraNegocioException("Entregas urgentes só podem ser solicitadas por clientes prioritários. "
                    + cliente.getNome() + " é cliente " + cliente.getTipo().getDescricao().toLowerCase() + ".");
        }
    }

    private Contrato contratoVigente(Cliente cliente, LocalDate data) {
        return clienteServico.contratoVigente(cliente.getId(), data).orElseThrow(() ->
                new RegraNegocioException("O cliente " + cliente.getNome() + " não tem contrato vigente em "
                        + Formatos.data(data) + ". Cadastre ou renove o contrato antes de agendar."));
    }

    private void verificarMotoristaEVeiculo(Motorista motorista, Veiculo veiculo, SolicitacaoEntrega pedido,
                                            LocalDate data) {
        if (!motorista.isAtivo()) {
            throw new RegraNegocioException("O motorista " + motorista.getNome() + " está inativo.");
        }
        if (!veiculo.isAtivo()) {
            throw new RegraNegocioException("O veículo " + veiculo.getPlaca() + " está inativo.");
        }
        if (!motorista.isCnhValidaEm(data)) {
            throw new RegraNegocioException("A CNH de " + motorista.getNome() + " vence/venceu em "
                    + Formatos.data(motorista.getValidadeCnh()) + " e não estará válida na data da entrega.");
        }

        CategoriaCnh exigidaVeiculo = veiculo.getTipo().getCnhMinima();
        if (!motorista.getCategoriaCnh().habilita(exigidaVeiculo)) {
            throw new RegraNegocioException("O veículo " + veiculo.getTipo().getDescricao() + " exige CNH "
                    + exigidaVeiculo + " e o motorista tem CNH " + motorista.getCategoriaCnh() + ".");
        }

        TipoCarga tipoCarga = pedido.carga().getTipo();
        if (!motorista.getCategoriaCnh().habilita(tipoCarga.getCnhMinima())) {
            throw new RegraNegocioException("Carga " + tipoCarga.getDescricao().toLowerCase() + " exige motorista com CNH "
                    + tipoCarga.getCnhMinima() + " ou superior. " + motorista.getNome() + " tem CNH "
                    + motorista.getCategoriaCnh() + ".");
        }

        if (!veiculo.suporta(pedido.carga().getPesoKg())) {
            throw new RegraNegocioException("A carga (" + Formatos.numero(pedido.carga().getPesoKg())
                    + " kg) ultrapassa a capacidade do veículo " + veiculo.getPlaca() + " ("
                    + Formatos.numero(veiculo.getCapacidadeKg()) + " kg).");
        }
    }

    private void verificarAgenda(Motorista motorista, Veiculo veiculo, LocalDate inicio, int dias, int ignorarId) {
        LocalDate fim = inicio.plusDays(dias - 1L);
        List<Entrega> conflitos = agendaServico.conflitos(motorista.getId(), veiculo.getId(), inicio, fim, ignorarId);
        if (conflitos.isEmpty()) {
            return;
        }
        Entrega primeira = conflitos.get(0);
        String quem = primeira.getMotoristaId() == motorista.getId()
                ? "O motorista " + motorista.getNome()
                : "O veículo " + veiculo.getPlaca();
        StringBuilder msg = new StringBuilder(quem + " já está reservado para a entrega #" + primeira.getId()
                + " de " + Formatos.data(primeira.getDataAgendada()) + " a " + Formatos.data(primeira.getFimOcupacao()) + ".");
        agendaServico.proximaDataLivre(motorista.getId(), veiculo.getId(), inicio, dias, ignorarId)
                .ifPresent(livre -> msg.append(" Próxima data livre para os dois: ").append(Formatos.data(livre)).append("."));
        throw new RegraNegocioException(msg.toString());
    }

    private LocalDateTime agora() {
        return LocalDateTime.now(relogio).withSecond(0).withNano(0);
    }

    private static String textoOuPadrao(String texto, String padrao) {
        return texto == null || texto.isBlank() ? padrao : texto.trim();
    }
}
