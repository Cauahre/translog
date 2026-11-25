package br.com.translog.ui;

import br.com.translog.modelo.TipoVeiculo;
import br.com.translog.modelo.Veiculo;
import br.com.translog.servico.AgendaServico;
import br.com.translog.servico.FrotaServico;
import br.com.translog.util.Formatos;

import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

public class MenuVeiculos extends Menu {

    private final FrotaServico frota;
    private final AgendaServico agenda;

    public MenuVeiculos(Console console, FrotaServico frota, AgendaServico agenda) {
        super(console);
        this.frota = frota;
        this.agenda = agenda;
    }

    @Override
    protected String titulo() {
        return "Veículos";
    }

    @Override
    protected List<String> opcoes() {
        return List.of(
                "Cadastrar veículo",
                "Listar veículos",
                "Editar veículo",
                "Agenda do veículo",
                "Ativar ou inativar veículo",
                "Remover veículo");
    }

    @Override
    protected void executar(int opcao) {
        switch (opcao) {
            case 1 -> cadastrar();
            case 2 -> listar();
            case 3 -> editar();
            case 4 -> mostrarAgenda();
            case 5 -> alterarSituacao();
            case 6 -> remover();
            default -> console.erro("Opção inválida.");
        }
    }

    private void cadastrar() {
        console.subtitulo("Novo veículo");
        String placa = console.lerTexto("Placa (ABC1234 ou ABC1D23)");
        String modelo = console.lerTexto("Marca/modelo");
        TipoVeiculo tipo = escolherTipo();
        double capacidade = console.lerDecimal("Capacidade de carga (kg)", 1, 60_000);
        int ano = console.lerInteiro("Ano", 1980, Year.now().getValue() + 1);
        Veiculo v = frota.cadastrarVeiculo(placa, modelo, tipo, capacidade, ano);
        console.sucesso("Veículo cadastrado com o código " + v.getId() + ".");
    }

    private void listar() {
        LocalDate hoje = LocalDate.now();
        List<String[]> linhas = new ArrayList<>();
        for (Veiculo v : frota.listarVeiculos()) {
            linhas.add(new String[]{
                    String.valueOf(v.getId()),
                    v.getPlaca(),
                    v.getModelo(),
                    v.getTipo().getDescricao(),
                    Formatos.numero(v.getCapacidadeKg()) + " kg",
                    "CNH " + v.getTipo().getCnhMinima(),
                    agenda.veiculoLivreEm(v.getId(), hoje) ? "Livre" : "Em serviço",
                    v.isAtivo() ? "Ativo" : "Inativo"});
        }
        console.tabela(new String[]{"Cód", "Placa", "Modelo", "Tipo", "Capacidade", "Exige", "Hoje", "Situação"}, linhas);
    }

    private void editar() {
        Veiculo v = frota.buscarVeiculo(console.lerCodigo("Código do veículo"));
        console.info("Enter mantém o valor atual. A placa não pode ser alterada.");
        String modelo = console.lerTextoMantendo("Marca/modelo", v.getModelo());
        TipoVeiculo tipo = console.confirmar("Alterar o tipo (" + v.getTipo().getDescricao() + ")?")
                ? escolherTipo()
                : v.getTipo();
        double capacidade = console.lerDecimalMantendo("Capacidade (kg)", v.getCapacidadeKg(), 1, 60_000);
        int ano = console.lerInteiroMantendo("Ano", v.getAno(), 1980, Year.now().getValue() + 1);
        frota.atualizarVeiculo(v.getId(), modelo, tipo, capacidade, ano);
        console.sucesso("Veículo atualizado.");
    }

    private void mostrarAgenda() {
        Veiculo v = frota.buscarVeiculo(console.lerCodigo("Código do veículo"));
        console.subtitulo("Agenda do veículo " + v.getPlaca());
        MenuMotoristas.imprimirAgenda(console, agenda.agendaDoVeiculo(v.getId()));
    }

    private void alterarSituacao() {
        Veiculo v = frota.buscarVeiculo(console.lerCodigo("Código do veículo"));
        boolean ativar = !v.isAtivo();
        if (console.confirmar((ativar ? "Ativar " : "Inativar ") + "o veículo " + v.getPlaca() + "?")) {
            frota.alterarSituacaoVeiculo(v.getId(), ativar);
            console.sucesso("Situação alterada.");
        }
    }

    private void remover() {
        Veiculo v = frota.buscarVeiculo(console.lerCodigo("Código do veículo"));
        if (console.confirmar("Remover definitivamente o veículo " + v.getPlaca() + "?")) {
            frota.removerVeiculo(v.getId());
            console.sucesso("Veículo removido.");
        }
    }

    private TipoVeiculo escolherTipo() {
        return console.escolher("Tipo de veículo", TipoVeiculo.values(),
                t -> t.getDescricao() + " (exige CNH " + t.getCnhMinima() + ")");
    }
}
