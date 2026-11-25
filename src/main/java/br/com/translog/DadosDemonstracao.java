package br.com.translog;

import br.com.translog.modelo.Carga;
import br.com.translog.modelo.CategoriaCnh;
import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Motorista;
import br.com.translog.modelo.NaturezaCarga;
import br.com.translog.modelo.Rota;
import br.com.translog.modelo.TipoCliente;
import br.com.translog.modelo.TipoVeiculo;
import br.com.translog.modelo.Veiculo;
import br.com.translog.servico.ClienteServico;
import br.com.translog.servico.EntregaServico;
import br.com.translog.servico.FrotaServico;
import br.com.translog.servico.SolicitacaoEntrega;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * Popula o sistema com um cenário para a apresentação: entregas concluídas no prazo e com
 * atraso, uma cancelada, uma em trânsito já atrasada e outras agendadas para os próximos dias.
 * <p>
 * Tudo passa pelos serviços normais (com as mesmas validações). Para as entregas antigas,
 * usamos um relógio fixo na data do evento, assim o histórico fica com as datas corretas.
 */
public class DadosDemonstracao {

    private final Sistema sistema;
    private final ClienteServico clientes;
    private final FrotaServico frota;
    private final LocalDate hoje = LocalDate.now();
    private final ZoneId zona = ZoneId.systemDefault();

    public DadosDemonstracao(Sistema sistema) {
        this.sistema = sistema;
        this.clientes = sistema.clientes();
        this.frota = sistema.frota();
    }

    public void carregar() {
        LocalDate inicioContratos = hoje.minusMonths(4);

        Cliente distribuidora = clientes.cadastrar(TipoCliente.EMPRESARIAL, "Distribuidora Piauí Alimentos Ltda",
                "99.351.819/0001-60", "(86) 3221-4500", "logistica@piauialimentos.com.br");
        clientes.registrarContrato(distribuidora.getId(), 5, new BigDecimal("300.00"),
                "Descarga por conta do cliente", inicioContratos, null);

        Cliente farmacia = clientes.cadastrar(TipoCliente.PRIORITARIO, "Farmácia Vida Nordeste S.A.",
                "09.378.657/0001-61", "(86) 98812-3030", "compras@vidanordeste.com.br");
        clientes.registrarContrato(farmacia.getId(), 3, BigDecimal.ZERO,
                "Medicamentos termolábeis exigem caixa térmica", inicioContratos, null);

        Cliente construmax = clientes.cadastrar(TipoCliente.EMPRESARIAL, "Construmax Materiais de Construção Ltda",
                "97.543.231/0001-83", "(86) 3194-7788", "expedicao@construmax.com.br");
        clientes.registrarContrato(construmax.getId(), 7, new BigDecimal("500.00"),
                "Entrega em obra com acesso para carreta", inicioContratos, null);

        Cliente techparts = clientes.cadastrar(TipoCliente.PRIORITARIO, "TechParts Eletrônicos Ltda",
                "94.875.749/0001-26", "(85) 99100-2020", "suprimentos@techparts.com.br");
        clientes.registrarContrato(techparts.getId(), 4, new BigDecimal("150.00"),
                null, inicioContratos, hoje.plusYears(1));

        Motorista jose = frota.cadastrarMotorista("José Ribamar Sousa", "526.018.159-06", "(86) 99901-1111",
                null, "04512398761", CategoriaCnh.E, hoje.plusYears(3));
        Motorista francisca = frota.cadastrarMotorista("Francisca Alves Lima", "083.016.613-05", "(86) 99902-2222",
                "francisca.lima@email.com", "07745123980", CategoriaCnh.C, hoje.plusYears(1));
        Motorista antonio = frota.cadastrarMotorista("Antônio Carlos Mendes", "186.091.390-34", "(86) 99903-3333",
                null, "01239874560", CategoriaCnh.B, hoje.plusDays(20));
        Motorista raimundo = frota.cadastrarMotorista("Raimundo Nonato Ferreira", "996.030.824-30", "(86) 99904-4444",
                null, "09988776655", CategoriaCnh.D, hoje.plusYears(2));

        Veiculo fiorino = frota.cadastrarVeiculo("QRS1A23", "Fiat Fiorino", TipoVeiculo.UTILITARIO, 650, 2021);
        Veiculo sprinter = frota.cadastrarVeiculo("PIA4B56", "Mercedes-Benz Sprinter", TipoVeiculo.VAN, 1_500, 2022);
        Veiculo delivery = frota.cadastrarVeiculo("NHX2C34", "VW Delivery 11.180", TipoVeiculo.CAMINHAO_TOCO, 7_000, 2020);
        Veiculo scania = frota.cadastrarVeiculo("OEP7D89", "Scania R450", TipoVeiculo.CARRETA, 30_000, 2019);
        Veiculo volvo = frota.cadastrarVeiculo("LVJ5E67", "Volvo VM 270", TipoVeiculo.CAMINHAO_TRUCK, 14_000, 2021);

        // 1) concluída no prazo
        Entrega e1 = agendar(dias(-30), distribuidora, francisca, delivery,
                new Carga("Cestas básicas", 4_200, NaturezaCarga.COMUM),
                rota(165, "Teresina", "Campo Maior", "Piripiri"), dias(-28), false);
        concluir(e1, dias(-28), dias(-26), "Recebido pelo almoxarifado");

        // 2) urgente, concluída no prazo
        Entrega e2 = agendar(dias(-21), farmacia, antonio, sprinter,
                new Carga("Medicamentos termolábeis", 350, NaturezaCarga.FRAGIL),
                rota(245, "Teresina", "Floriano"), dias(-20), true);
        concluir(e2, dias(-20), dias(-19), "Recebido pela farmacêutica responsável");

        // 3) concluída com 3 dias de atraso (15% de abatimento)
        Entrega e3 = agendar(dias(-18), construmax, jose, scania,
                new Carga("Vergalhões e cimento", 22_000, NaturezaCarga.COMUM),
                rota(560, "Teresina", "Picos", "Juazeiro do Norte"), dias(-15), false);
        concluir(e3, dias(-15), dias(-5), "Atraso por interdição na BR-316");

        // 4) cancelada
        Entrega e4 = agendar(dias(-14), techparts, raimundo, volvo,
                new Carga("Baterias de lítio", 2_500, NaturezaCarga.PERIGOSA),
                rota(600, "Teresina", "Sobral", "Fortaleza"), dias(-12), false);
        sistema.entregaServicoCom(relogio(dias(-13))).cancelar(e4.getId(), "Cliente adiou o pedido");

        // 5) em trânsito e já com prazo vencido
        Entrega e5 = agendar(dias(-8), distribuidora, francisca, delivery,
                new Carga("Bebidas", 3_800, NaturezaCarga.COMUM),
                rota(340, "Teresina", "Piripiri", "Parnaíba"), dias(-6), false);
        sistema.entregaServicoCom(relogio(dias(-6))).iniciarTransporte(e5.getId(), "Saída do CD Teresina");

        // 6) urgente curta, concluída
        Entrega e6 = agendar(dias(-4), farmacia, antonio, fiorino,
                new Carga("Vacinas", 120, NaturezaCarga.FRAGIL),
                rota(40, "Teresina", "Altos"), dias(-3), true);
        concluir(e6, dias(-3), dias(-3), "Entregue no mesmo dia");

        // 7 a 9) agendadas para os próximos dias
        agendar(hoje, techparts, jose, sprinter,
                new Carga("Servidores e notebooks", 900, NaturezaCarga.FRAGIL),
                rota(450, "Teresina", "Caxias", "São Luís"), dias(1), true);
        agendar(hoje, construmax, raimundo, volvo,
                new Carga("Telhas e tijolos", 9_000, NaturezaCarga.COMUM),
                rota(310, "Teresina", "Picos"), dias(3), false);
        agendar(hoje, distribuidora, francisca, delivery,
                new Carga("Cestas básicas", 5_000, NaturezaCarga.COMUM),
                rota(310, "Teresina", "Oeiras"), dias(2), false);
    }

    private Entrega agendar(LocalDate pedidoEm, Cliente cliente, Motorista motorista, Veiculo veiculo, Carga carga,
                            Rota rota, LocalDate saida, boolean urgente) {
        return sistema.entregaServicoCom(relogio(pedidoEm)).agendar(new SolicitacaoEntrega(cliente.getId(),
                motorista.getId(), veiculo.getId(), carga, rota, saida, urgente));
    }

    private void concluir(Entrega entrega, LocalDate saida, LocalDate chegada, String observacao) {
        sistema.entregaServicoCom(relogio(saida)).iniciarTransporte(entrega.getId(), "Saída do CD Teresina");
        EntregaServico naChegada = sistema.entregaServicoCom(relogio(chegada));
        naChegada.concluir(entrega.getId(), chegada, observacao);
    }

    private LocalDate dias(int deslocamento) {
        return hoje.plusDays(deslocamento);
    }

    private Clock relogio(LocalDate dia) {
        return Clock.fixed(dia.atTime(8, 30).atZone(zona).toInstant(), zona);
    }

    private static Rota rota(double km, String... cidades) {
        return new Rota(List.of(cidades), km);
    }
}
