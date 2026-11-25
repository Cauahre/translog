package br.com.translog.ui;

import br.com.translog.modelo.CategoriaCnh;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Motorista;
import br.com.translog.servico.AgendaServico;
import br.com.translog.servico.FrotaServico;
import br.com.translog.util.Formatos;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MenuMotoristas extends Menu {

    private static final int DIAS_ALERTA_CNH = 60;

    private final FrotaServico frota;
    private final AgendaServico agenda;

    public MenuMotoristas(Console console, FrotaServico frota, AgendaServico agenda) {
        super(console);
        this.frota = frota;
        this.agenda = agenda;
    }

    @Override
    protected String titulo() {
        return "Motoristas";
    }

    @Override
    protected List<String> opcoes() {
        return List.of(
                "Cadastrar motorista",
                "Listar motoristas",
                "Editar motorista",
                "Agenda do motorista",
                "Ativar ou inativar motorista",
                "Remover motorista",
                "CNHs vencidas ou vencendo (" + DIAS_ALERTA_CNH + " dias)");
    }

    @Override
    protected void executar(int opcao) {
        switch (opcao) {
            case 1 -> cadastrar();
            case 2 -> listar(frota.listarMotoristas());
            case 3 -> editar();
            case 4 -> mostrarAgenda();
            case 5 -> alterarSituacao();
            case 6 -> remover();
            case 7 -> listar(frota.motoristasComCnhVencendo(LocalDate.now(), DIAS_ALERTA_CNH));
            default -> console.erro("Opção inválida.");
        }
    }

    private void cadastrar() {
        console.subtitulo("Novo motorista");
        String nome = console.lerTexto("Nome completo");
        String cpf = console.lerTexto("CPF");
        String telefone = console.lerTextoOpcional("Telefone");
        String email = console.lerTextoOpcional("E-mail");
        String cnh = console.lerTexto("Número da CNH (11 dígitos)");
        CategoriaCnh categoria = lerCategoria(null);
        LocalDate validade = console.lerData("Validade da CNH", null);
        Motorista m = frota.cadastrarMotorista(nome, cpf, telefone, email, cnh, categoria, validade);
        console.sucesso("Motorista cadastrado com o código " + m.getId() + ".");
        if (!m.isCnhValidaEm(LocalDate.now())) {
            console.aviso("A CNH informada já está vencida. O motorista não poderá ser escalado até a renovação.");
        }
    }

    private void listar(List<Motorista> lista) {
        LocalDate hoje = LocalDate.now();
        List<String[]> linhas = new ArrayList<>();
        for (Motorista m : lista) {
            String validade = Formatos.data(m.getValidadeCnh());
            if (!m.isCnhValidaEm(hoje)) {
                validade += " (VENCIDA)";
            }
            linhas.add(new String[]{
                    String.valueOf(m.getId()),
                    m.getNome(),
                    m.getDocumento(),
                    m.getCategoriaCnh().name(),
                    validade,
                    agenda.motoristaLivreEm(m.getId(), hoje) ? "Livre" : "Em serviço",
                    m.isAtivo() ? "Ativo" : "Inativo"});
        }
        console.tabela(new String[]{"Cód", "Nome", "CPF", "CNH", "Validade", "Hoje", "Situação"}, linhas);
    }

    private void editar() {
        Motorista m = frota.buscarMotorista(console.lerCodigo("Código do motorista"));
        console.info("Enter mantém o valor atual.");
        String nome = console.lerTextoMantendo("Nome", m.getNome());
        String telefone = console.lerTextoMantendo("Telefone", m.getTelefone());
        String email = console.lerTextoMantendo("E-mail", m.getEmail());
        CategoriaCnh categoria = lerCategoria(m.getCategoriaCnh());
        LocalDate validade = console.lerData("Validade da CNH", m.getValidadeCnh());
        frota.atualizarMotorista(m.getId(), nome, telefone, email, categoria, validade);
        console.sucesso("Motorista atualizado.");
    }

    private void mostrarAgenda() {
        Motorista m = frota.buscarMotorista(console.lerCodigo("Código do motorista"));
        console.subtitulo("Agenda de " + m.getNome());
        imprimirAgenda(console, agenda.agendaDoMotorista(m.getId()));
    }

    private void alterarSituacao() {
        Motorista m = frota.buscarMotorista(console.lerCodigo("Código do motorista"));
        boolean ativar = !m.isAtivo();
        if (console.confirmar((ativar ? "Ativar " : "Inativar ") + m.getNome() + "?")) {
            frota.alterarSituacaoMotorista(m.getId(), ativar);
            console.sucesso("Situação alterada.");
        }
    }

    private void remover() {
        Motorista m = frota.buscarMotorista(console.lerCodigo("Código do motorista"));
        if (console.confirmar("Remover definitivamente " + m.getNome() + "?")) {
            frota.removerMotorista(m.getId());
            console.sucesso("Motorista removido.");
        }
    }

    private CategoriaCnh lerCategoria(CategoriaCnh atual) {
        while (true) {
            String texto = atual == null
                    ? console.lerTexto("Categoria da CNH (A, B, C, D ou E)")
                    : console.lerTextoMantendo("Categoria da CNH (A, B, C, D ou E)", atual.name());
            CategoriaCnh categoria = CategoriaCnh.deTexto(texto);
            if (categoria != null) {
                return categoria;
            }
            console.erro("Categoria inválida.");
        }
    }

    /** Também usado pelo menu de veículos. */
    static void imprimirAgenda(Console console, List<Entrega> agenda) {
        List<String[]> linhas = new ArrayList<>();
        for (Entrega e : agenda) {
            linhas.add(new String[]{
                    String.valueOf(e.getId()),
                    Formatos.data(e.getDataAgendada()),
                    Formatos.data(e.getFimOcupacao()),
                    e.getRota().toString(),
                    e.getStatus().getDescricao()});
        }
        console.tabela(new String[]{"#", "De", "Até", "Rota", "Status"}, linhas);
    }
}
