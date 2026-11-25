package br.com.translog.ui;

import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Contrato;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.TipoCliente;
import br.com.translog.servico.ClienteServico;
import br.com.translog.servico.EntregaServico;
import br.com.translog.util.Formatos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuClientes extends Menu {

    private final ClienteServico clientes;
    private final EntregaServico entregas;

    public MenuClientes(Console console, ClienteServico clientes, EntregaServico entregas) {
        super(console);
        this.clientes = clientes;
        this.entregas = entregas;
    }

    @Override
    protected String titulo() {
        return "Clientes e contratos";
    }

    @Override
    protected List<String> opcoes() {
        return List.of(
                "Cadastrar cliente",
                "Listar clientes",
                "Ver detalhes do cliente",
                "Pesquisar cliente por nome",
                "Editar dados do cliente",
                "Alterar tipo (empresarial/prioritário)",
                "Registrar novo contrato / renovar",
                "Ativar ou inativar cliente",
                "Remover cliente");
    }

    @Override
    protected void executar(int opcao) {
        switch (opcao) {
            case 1 -> cadastrar();
            case 2 -> listar(clientes.listar());
            case 3 -> detalhar();
            case 4 -> listar(clientes.pesquisarPorNome(console.lerTexto("Parte do nome")));
            case 5 -> editar();
            case 6 -> alterarTipo();
            case 7 -> novoContrato(console.lerCodigo("Código do cliente"));
            case 8 -> alterarSituacao();
            case 9 -> remover();
            default -> console.erro("Opção inválida.");
        }
    }

    private void cadastrar() {
        console.subtitulo("Novo cliente");
        TipoCliente tipo = console.escolher("Tipo de cliente", TipoCliente.values(), t -> switch (t) {
            case EMPRESARIAL -> "Empresarial (10% de desconto em entregas regulares)";
            case PRIORITARIO -> "Prioritário (20% de desconto e pode pedir entrega urgente)";
        });
        String nome = console.lerTexto("Razão social");
        String cnpj = console.lerTexto("CNPJ");
        String telefone = console.lerTextoOpcional("Telefone");
        String email = console.lerTextoOpcional("E-mail");
        Cliente cliente = clientes.cadastrar(tipo, nome, cnpj, telefone, email);
        console.sucesso("Cliente cadastrado com o código " + cliente.getId() + ".");

        console.info("\nTodo cliente precisa de um contrato de frete para agendar entregas.");
        novoContrato(cliente.getId());
    }

    private void novoContrato(int clienteId) {
        Cliente cliente = clientes.buscar(clienteId);
        console.subtitulo("Contrato de frete - " + cliente.getNome());
        clientes.contratoAtual(clienteId).ifPresent(c ->
                console.aviso("O contrato atual (#" + c.getId() + ") será encerrado e substituído por este."));
        int prazo = console.lerInteiro("Prazo padrão de entrega (dias)", 1, 90);
        BigDecimal minimo = console.lerValorMonetario("Frete mínimo por entrega, 0 se não houver");
        String condicoes = console.lerTextoOpcional("Outras condições de frete");
        LocalDate inicio = console.lerData("Início da vigência", LocalDate.now());
        LocalDate fim = console.lerDataOpcional("Fim da vigência");
        Contrato contrato = clientes.registrarContrato(clienteId, prazo, minimo, condicoes, inicio, fim);
        console.sucesso("Contrato #" + contrato.getId() + " registrado.");
    }

    private void listar(List<Cliente> lista) {
        List<String[]> linhas = new ArrayList<>();
        for (Cliente c : lista) {
            Optional<Contrato> contrato = clientes.contratoAtual(c.getId());
            linhas.add(new String[]{
                    String.valueOf(c.getId()),
                    c.getNome(),
                    c.getDocumento(),
                    c.getTipo().getDescricao(),
                    contrato.map(ct -> ct.getPrazoEntregaDias() + " dias").orElse("sem contrato"),
                    c.isAtivo() ? "Ativo" : "Inativo"});
        }
        console.tabela(new String[]{"Cód", "Razão social", "CNPJ", "Tipo", "Prazo", "Situação"}, linhas);
    }

    private void detalhar() {
        Cliente c = clientes.buscar(console.lerCodigo("Código do cliente"));
        console.subtitulo(c.getNome());
        console.campo("Código", c.getId());
        console.campo("CNPJ", c.getDocumento());
        console.campo("Tipo", c.getTipo().getDescricao());
        console.campo("Desconto", Formatos.percentual(c.percentualDesconto(false))
                + (c.podeSolicitarUrgencia() ? " (inclusive urgentes)" : " (só entregas regulares)"));
        console.campo("Telefone", Formatos.telefone(c.getTelefone()));
        console.campo("E-mail", c.getEmail());
        console.campo("Situação", c.isAtivo() ? "Ativo" : "Inativo");

        console.subtitulo("Contratos");
        List<String[]> linhas = new ArrayList<>();
        for (Contrato ct : clientes.contratosDoCliente(c.getId())) {
            linhas.add(new String[]{
                    String.valueOf(ct.getId()),
                    ct.getPrazoEntregaDias() + " dias",
                    Formatos.moeda(ct.getFreteMinimo()),
                    Formatos.data(ct.getDataInicio()) + " a " + (ct.getDataFim() == null ? "indeterminado" : Formatos.data(ct.getDataFim())),
                    ct.isAtivo() ? "Ativo" : "Encerrado",
                    ct.getCondicoes() == null ? "" : ct.getCondicoes()});
        }
        console.tabela(new String[]{"Nº", "Prazo", "Frete mín.", "Vigência", "Situação", "Condições"}, linhas);

        console.subtitulo("Entregas");
        List<String[]> entregasCliente = new ArrayList<>();
        for (Entrega e : entregas.listarDoCliente(c.getId())) {
            entregasCliente.add(new String[]{
                    String.valueOf(e.getId()),
                    e.getRota().toString(),
                    Formatos.data(e.getDataAgendada()),
                    e.getStatus().getDescricao(),
                    Formatos.moeda(e.getFrete().getValorFinal())});
        }
        console.tabela(new String[]{"#", "Rota", "Saída", "Status", "Valor"}, entregasCliente);
    }

    private void editar() {
        Cliente c = clientes.buscar(console.lerCodigo("Código do cliente"));
        console.info("Enter mantém o valor atual.");
        String nome = console.lerTextoMantendo("Razão social", c.getNome());
        String telefone = console.lerTextoMantendo("Telefone", c.getTelefone());
        String email = console.lerTextoMantendo("E-mail", c.getEmail());
        clientes.atualizar(c.getId(), nome, telefone, email);
        console.sucesso("Dados atualizados.");
    }

    private void alterarTipo() {
        Cliente c = clientes.buscar(console.lerCodigo("Código do cliente"));
        console.info("Tipo atual: " + c.getTipo().getDescricao());
        TipoCliente novo = console.escolher("Novo tipo", TipoCliente.values(), TipoCliente::getDescricao);
        clientes.alterarTipo(c.getId(), novo);
        console.sucesso("Tipo alterado para " + novo.getDescricao()
                + ". Entregas já agendadas mantêm o valor calculado no agendamento.");
    }

    private void alterarSituacao() {
        Cliente c = clientes.buscar(console.lerCodigo("Código do cliente"));
        boolean ativar = !c.isAtivo();
        if (console.confirmar((ativar ? "Ativar" : "Inativar") + " o cliente " + c.getNome() + "?")) {
            clientes.alterarSituacao(c.getId(), ativar);
            console.sucesso("Cliente " + (ativar ? "ativado." : "inativado."));
        }
    }

    private void remover() {
        Cliente c = clientes.buscar(console.lerCodigo("Código do cliente"));
        if (console.confirmar("Remover definitivamente " + c.getNome() + "?")) {
            clientes.remover(c.getId());
            console.sucesso("Cliente removido.");
        }
    }
}
