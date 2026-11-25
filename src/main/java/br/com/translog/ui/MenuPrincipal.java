package br.com.translog.ui;

import br.com.translog.DadosDemonstracao;
import br.com.translog.Sistema;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Motorista;
import br.com.translog.modelo.StatusEntrega;
import br.com.translog.servico.PainelResumo;
import br.com.translog.util.Formatos;

import java.util.ArrayList;
import java.util.List;

public class MenuPrincipal extends Menu {

    private final Sistema sistema;
    private final MenuClientes menuClientes;
    private final MenuMotoristas menuMotoristas;
    private final MenuVeiculos menuVeiculos;
    private final MenuEntregas menuEntregas;
    private final MenuFaturamento menuFaturamento;

    public MenuPrincipal(Console console, Sistema sistema) {
        super(console);
        this.sistema = sistema;
        this.menuClientes = new MenuClientes(console, sistema.clientes(), sistema.entregas());
        this.menuMotoristas = new MenuMotoristas(console, sistema.frota(), sistema.agenda());
        this.menuVeiculos = new MenuVeiculos(console, sistema.frota(), sistema.agenda());
        this.menuEntregas = new MenuEntregas(console, sistema.entregas(), sistema.clientes(), sistema.frota(),
                sistema.agenda(), sistema.relatorios());
        this.menuFaturamento = new MenuFaturamento(console, sistema.faturamento(), sistema.clientes(),
                sistema.relatorios());
    }

    @Override
    protected String titulo() {
        return "Translog - Sistema de Logística de Transporte";
    }

    @Override
    protected String textoSaida() {
        return "Sair";
    }

    @Override
    protected List<String> opcoes() {
        return List.of(
                "Painel geral",
                "Clientes e contratos",
                "Motoristas",
                "Veículos",
                "Entregas",
                "Faturamento e notas fiscais",
                "Carregar dados de demonstração");
    }

    @Override
    protected boolean pausarDepoisDe(int opcao) {
        // as opções 2 a 6 abrem submenus
        return opcao == 1 || opcao == 7;
    }

    @Override
    protected void executar(int opcao) {
        switch (opcao) {
            case 1 -> painel();
            case 2 -> menuClientes.abrir();
            case 3 -> menuMotoristas.abrir();
            case 4 -> menuVeiculos.abrir();
            case 5 -> menuEntregas.abrir();
            case 6 -> menuFaturamento.abrir();
            case 7 -> carregarDemonstracao();
            default -> console.erro("Opção inválida.");
        }
    }

    private void painel() {
        PainelResumo p = sistema.entregas().painel();
        console.subtitulo("Painel - " + Formatos.data(sistema.entregas().hoje()));
        for (StatusEntrega s : StatusEntrega.values()) {
            console.campo(s.getDescricao(), p.entregasPorStatus().get(s));
        }
        console.campo("Faturado no mês", Formatos.moeda(p.faturamentoDoMes()) + " (" + p.notasDoMes() + " nota(s))");

        if (!p.atrasadas().isEmpty()) {
            console.subtitulo("Pendentes com prazo vencido");
            p.atrasadas().forEach(e -> console.info("  #" + e.getId() + " " + e.getRota()
                    + " - prazo era " + Formatos.data(e.getDataLimite())));
        }

        console.subtitulo("Saídas dos próximos 7 dias");
        List<String[]> linhas = new ArrayList<>();
        for (Entrega e : p.proximasSaidas()) {
            linhas.add(new String[]{String.valueOf(e.getId()), e.isUrgente() ? "URG" : "",
                    Formatos.data(e.getDataAgendada()), e.getRota().toString()});
        }
        console.tabela(new String[]{"#", "", "Saída", "Rota"}, linhas);

        List<Motorista> cnh = sistema.frota().motoristasComCnhVencendo(sistema.entregas().hoje(), 30);
        if (!cnh.isEmpty()) {
            console.subtitulo("CNH vencida ou vencendo em 30 dias");
            cnh.forEach(m -> console.info("  " + m.getNome() + " - " + Formatos.data(m.getValidadeCnh())));
        }
    }

    private void carregarDemonstracao() {
        if (!sistema.semDados()) {
            console.erro("Já existem dados cadastrados. A demonstração só pode ser carregada com o sistema vazio "
                    + "(apague a pasta " + sistema.getPastaDados().toAbsolutePath() + " ou use outra pasta de dados).");
            return;
        }
        new DadosDemonstracao(sistema).carregar();
        console.sucesso("Dados de demonstração carregados: 4 clientes, 4 motoristas, 5 veículos e 9 entregas.");
    }
}
