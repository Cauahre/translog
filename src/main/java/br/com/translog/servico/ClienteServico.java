package br.com.translog.servico;

import br.com.translog.excecao.RegistroNaoEncontradoException;
import br.com.translog.excecao.RegraNegocioException;
import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Contrato;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.TipoCliente;
import br.com.translog.persistencia.Repositorio;
import br.com.translog.util.ValidadorDocumento;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Cadastro de clientes e dos seus contratos de frete. */
public class ClienteServico {

    private final Repositorio<Cliente> clientes;
    private final Repositorio<Contrato> contratos;
    private final Repositorio<Entrega> entregas;

    public ClienteServico(Repositorio<Cliente> clientes, Repositorio<Contrato> contratos, Repositorio<Entrega> entregas) {
        this.clientes = clientes;
        this.contratos = contratos;
        this.entregas = entregas;
    }

    public Cliente cadastrar(TipoCliente tipo, String razaoSocial, String cnpj, String telefone, String email) {
        Cliente cliente = tipo.novaInstancia();
        cliente.setNome(razaoSocial);
        cliente.setCnpj(cnpj);
        cliente.setTelefone(telefone);
        cliente.setEmail(email);
        verificarCnpjUnico(cliente.getCnpj(), 0);
        return clientes.salvar(cliente);
    }

    public Cliente atualizar(int id, String razaoSocial, String telefone, String email) {
        Cliente cliente = buscar(id);
        cliente.setNome(razaoSocial);
        cliente.setTelefone(telefone);
        cliente.setEmail(email);
        return clientes.salvar(cliente);
    }

    /**
     * Troca o tipo do cliente (ex.: empresarial que virou prioritário).
     * Como o tipo é a própria classe do objeto, criamos um novo objeto com o mesmo id.
     * Entregas já agendadas mantêm o valor calculado na época.
     */
    public Cliente alterarTipo(int id, TipoCliente novoTipo) {
        Cliente atual = buscar(id);
        if (atual.getTipo() == novoTipo) {
            return atual;
        }
        Cliente novo = novoTipo.novaInstancia();
        atual.copiarDadosPara(novo);
        return clientes.salvar(novo);
    }

    public void alterarSituacao(int id, boolean ativo) {
        Cliente cliente = buscar(id);
        cliente.setAtivo(ativo);
        clientes.salvar(cliente);
    }

    /** Só remove clientes sem entregas; quem já tem histórico deve ser inativado (RN30). */
    public void remover(int id) {
        buscar(id);
        if (entregas.existe(e -> e.getClienteId() == id)) {
            throw new RegraNegocioException("O cliente possui entregas registradas e não pode ser removido. "
                    + "Use a opção de inativar para preservar o histórico e as notas fiscais.");
        }
        contratos.filtrar(c -> c.getClienteId() == id).forEach(c -> contratos.remover(c.getId()));
        clientes.remover(id);
    }

    public Cliente buscar(int id) {
        return clientes.buscarPorId(id).orElseThrow(() -> new RegistroNaoEncontradoException("Cliente", id));
    }

    public List<Cliente> listar() {
        return clientes.listarTodos().stream()
                .sorted(Comparator.comparing(Cliente::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public List<Cliente> pesquisarPorNome(String trecho) {
        String busca = trecho == null ? "" : trecho.trim().toLowerCase();
        return listar().stream().filter(c -> c.getNome().toLowerCase().contains(busca)).toList();
    }

    /**
     * Registra um novo contrato (RF03). Se o cliente já tinha um contrato ativo, ele é encerrado (RN05),
     * porque cada cliente trabalha com um contrato por vez.
     */
    public Contrato registrarContrato(int clienteId, int prazoDias, BigDecimal freteMinimo, String condicoes,
                                      LocalDate inicio, LocalDate fim) {
        buscar(clienteId);
        Contrato novo = new Contrato(clienteId, prazoDias, freteMinimo, condicoes, inicio, fim);
        contratos.filtrar(c -> c.getClienteId() == clienteId && c.isAtivo()).forEach(antigo -> {
            antigo.encerrar();
            contratos.salvar(antigo);
        });
        return contratos.salvar(novo);
    }

    public Optional<Contrato> contratoVigente(int clienteId, LocalDate data) {
        return contratos.filtrar(c -> c.getClienteId() == clienteId && c.isVigenteEm(data)).stream()
                .max(Comparator.comparing(Contrato::getDataInicio));
    }

    /** Contrato ativo mais recente, mesmo que ainda não tenha começado a valer. */
    public Optional<Contrato> contratoAtual(int clienteId) {
        return contratos.filtrar(c -> c.getClienteId() == clienteId && c.isAtivo()).stream()
                .max(Comparator.comparing(Contrato::getDataInicio));
    }

    public List<Contrato> contratosDoCliente(int clienteId) {
        return contratos.filtrar(c -> c.getClienteId() == clienteId).stream()
                .sorted(Comparator.comparing(Contrato::getDataInicio).reversed())
                .toList();
    }

    public Contrato buscarContrato(int id) {
        return contratos.buscarPorId(id).orElseThrow(() -> new RegistroNaoEncontradoException("Contrato", id));
    }

    private void verificarCnpjUnico(String cnpj, int idIgnorado) {
        if (clientes.existe(c -> c.getCnpj().equals(cnpj) && c.getId() != idIgnorado)) {
            throw new RegraNegocioException("Já existe um cliente com o CNPJ " + ValidadorDocumento.formatarCnpj(cnpj) + ".");
        }
    }
}
