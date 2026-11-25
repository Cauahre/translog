package br.com.translog.servico;

import br.com.translog.excecao.RegraNegocioException;
import br.com.translog.excecao.ValidacaoException;
import br.com.translog.modelo.Carga;
import br.com.translog.modelo.CategoriaCnh;
import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Contrato;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Motorista;
import br.com.translog.modelo.NaturezaCarga;
import br.com.translog.modelo.NotaFiscal;
import br.com.translog.modelo.Rota;
import br.com.translog.modelo.StatusEntrega;
import br.com.translog.modelo.TipoCliente;
import br.com.translog.modelo.TipoVeiculo;
import br.com.translog.modelo.Veiculo;
import br.com.translog.persistencia.RepositorioMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Testa as regras de agendamento usando repositórios em memória e uma data fixa. */
class EntregaServicoTest {

    private static final ZoneId ZONA = ZoneId.of("America/Fortaleza");
    private static final LocalDate HOJE = LocalDate.of(2026, 12, 1);

    private RepositorioMemoria<Entrega> entregasRepo;
    private ClienteServico clientes;
    private FrotaServico frota;
    private FaturamentoServico faturamento;
    private AgendaServico agenda;

    private Cliente empresarial;
    private Cliente prioritario;
    private Motorista motoristaB;
    private Motorista motoristaE;
    private Veiculo van;
    private Veiculo carreta;

    @BeforeEach
    void preparar() {
        entregasRepo = new RepositorioMemoria<>();
        clientes = new ClienteServico(new RepositorioMemoria<>(), new RepositorioMemoria<>(), entregasRepo);
        frota = new FrotaServico(new RepositorioMemoria<>(), new RepositorioMemoria<>(), entregasRepo);
        faturamento = new FaturamentoServico(new RepositorioMemoria<>());
        agenda = new AgendaServico(entregasRepo);

        empresarial = clientes.cadastrar(TipoCliente.EMPRESARIAL, "Empresa Ltda", "99351819000160", null, null);
        prioritario = clientes.cadastrar(TipoCliente.PRIORITARIO, "Prioritária Ltda", "09378657000161", null, null);
        clientes.registrarContrato(empresarial.getId(), 5, BigDecimal.ZERO, null, HOJE.minusMonths(1), null);
        clientes.registrarContrato(prioritario.getId(), 3, BigDecimal.ZERO, null, HOJE.minusMonths(1), null);

        motoristaB = frota.cadastrarMotorista("Motorista B", "52601815906", null, null, "11111111111",
                CategoriaCnh.B, HOJE.plusYears(1));
        motoristaE = frota.cadastrarMotorista("Motorista E", "08301661305", null, null, "22222222222",
                CategoriaCnh.E, HOJE.plusYears(1));
        van = frota.cadastrarVeiculo("PIA4B56", "Sprinter", TipoVeiculo.VAN, 1_500, 2022);
        carreta = frota.cadastrarVeiculo("OEP7D89", "Scania", TipoVeiculo.CARRETA, 30_000, 2020);
    }

    @Test
    void clienteEmpresarialNaoPodePedirUrgencia() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> servicoEm(HOJE).agendar(pedido(empresarial, motoristaB, van, 300, 100, HOJE.plusDays(1), true)));
        assertTrue(erro.getMessage().contains("prioritários"));
    }

    @Test
    void cargaPesadaExigeCnhC() {
        assertThrows(RegraNegocioException.class,
                () -> servicoEm(HOJE).agendar(pedido(empresarial, motoristaB, carreta, 5_000, 100, HOJE.plusDays(1), false)));
    }

    @Test
    void pesoNaoPodeUltrapassarCapacidadeDoVeiculo() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> servicoEm(HOJE).agendar(pedido(empresarial, motoristaE, van, 2_000, 100, HOJE.plusDays(1), false)));
        assertTrue(erro.getMessage().contains("capacidade"));
    }

    @Test
    void naoAgendaNoPassado() {
        assertThrows(ValidacaoException.class,
                () -> servicoEm(HOJE).agendar(pedido(empresarial, motoristaB, van, 300, 100, HOJE.minusDays(1), false)));
    }

    @Test
    void bloqueiaMotoristaJaOcupadoESugereProximaData() {
        EntregaServico servico = servicoEm(HOJE);
        servico.agendar(pedido(empresarial, motoristaE, carreta, 10_000, 900, HOJE.plusDays(1), false)); // 2 dias

        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
                () -> servico.agendar(pedido(prioritario, motoristaE, van, 300, 100, HOJE.plusDays(2), false)));
        assertTrue(erro.getMessage().contains("Próxima data livre"));

        // no dia seguinte ao fim da viagem já pode
        Entrega ok = servico.agendar(pedido(prioritario, motoristaE, van, 300, 100, HOJE.plusDays(3), false));
        assertEquals(StatusEntrega.AGENDADA, ok.getStatus());
    }

    @Test
    void cancelamentoLiberaAAgenda() {
        EntregaServico servico = servicoEm(HOJE);
        Entrega primeira = servico.agendar(pedido(empresarial, motoristaB, van, 300, 100, HOJE.plusDays(1), false));
        servico.cancelar(primeira.getId(), "teste");

        Entrega segunda = servico.agendar(pedido(prioritario, motoristaB, van, 300, 100, HOJE.plusDays(1), true));
        assertEquals(StatusEntrega.AGENDADA, segunda.getStatus());
    }

    @Test
    void entregaAtrasadaGeraNotaComAbatimento() {
        Entrega entrega = servicoEm(HOJE).agendar(pedido(empresarial, motoristaB, van, 300, 100, HOJE.plusDays(1), false));
        LocalDate saida = HOJE.plusDays(1);
        servicoEm(saida).iniciarTransporte(entrega.getId(), null);

        // prazo de 5 dias: limite em 07/12. Entregue em 09/12 = 2 dias de atraso = 10%
        LocalDate chegada = saida.plusDays(7);
        NotaFiscal nota = servicoEm(chegada).concluir(entrega.getId(), chegada, null);

        // 100 km x 1,50 = 150 - 10% = 135 ; abatimento 10% = 13,50
        assertEquals(new BigDecimal("13.50"), nota.getAbatimentoAtraso());
        assertEquals(new BigDecimal("121.50"), nota.getValorTotal());
        assertEquals(1, faturamento.listarNotas().size());
    }

    @Test
    void naoConcluiEntregaQueNaoSaiu() {
        Entrega entrega = servicoEm(HOJE).agendar(pedido(empresarial, motoristaB, van, 300, 100, HOJE, false));
        assertThrows(RegraNegocioException.class, () -> servicoEm(HOJE).concluir(entrega.getId(), HOJE, null));
    }

    @Test
    void semContratoVigenteNaoAgenda() {
        Contrato atual = clientes.contratoAtual(empresarial.getId()).orElseThrow();
        clientes.registrarContrato(empresarial.getId(), 5, BigDecimal.ZERO, null, HOJE.plusDays(10), null);
        assertFalse(atual.isAtivo());

        assertThrows(RegraNegocioException.class,
                () -> servicoEm(HOJE).agendar(pedido(empresarial, motoristaB, van, 300, 100, HOJE.plusDays(1), false)));
    }

    @Test
    void urgentesVemPrimeiroNaListagem() {
        EntregaServico servico = servicoEm(HOJE);
        servico.agendar(pedido(empresarial, motoristaB, van, 300, 100, HOJE.plusDays(1), false));
        Entrega urgente = servico.agendar(pedido(prioritario, motoristaE, carreta, 300, 100, HOJE.plusDays(5), true));

        List<Entrega> lista = servico.listarPorPrioridade();
        assertEquals(urgente.getId(), lista.get(0).getId());
    }

    private EntregaServico servicoEm(LocalDate dia) {
        Clock relogio = Clock.fixed(dia.atTime(9, 0).atZone(ZONA).toInstant(), ZONA);
        return new EntregaServico(entregasRepo, clientes, frota, agenda, faturamento, new CalculadoraFrete(), relogio);
    }

    private static SolicitacaoEntrega pedido(Cliente cliente, Motorista motorista, Veiculo veiculo, double pesoKg,
                                             double km, LocalDate saida, boolean urgente) {
        return new SolicitacaoEntrega(cliente.getId(), motorista.getId(), veiculo.getId(),
                new Carga("Carga de teste", pesoKg, NaturezaCarga.COMUM),
                new Rota(List.of("Teresina", "Parnaíba"), km), saida, urgente);
    }
}
