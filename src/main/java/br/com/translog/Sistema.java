package br.com.translog;

import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Contrato;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Identificavel;
import br.com.translog.modelo.Motorista;
import br.com.translog.modelo.NotaFiscal;
import br.com.translog.modelo.Veiculo;
import br.com.translog.persistencia.GsonFactory;
import br.com.translog.persistencia.RepositorioJson;
import br.com.translog.relatorio.GeradorRelatorios;
import br.com.translog.servico.AgendaServico;
import br.com.translog.servico.CalculadoraFrete;
import br.com.translog.servico.ClienteServico;
import br.com.translog.servico.EntregaServico;
import br.com.translog.servico.FaturamentoServico;
import br.com.translog.servico.FrotaServico;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

/**
 * Monta as dependências da aplicação: repositórios JSON, serviços e gerador de relatórios.
 * Fica tudo num lugar só para que as outras classes recebam o que precisam pelo construtor.
 */
public class Sistema {

    private final Path pastaDados;
    private final List<String> avisos = new ArrayList<>();
    private final Gson gson = GsonFactory.criar();

    private final RepositorioJson<Cliente> clientes;
    private final RepositorioJson<Contrato> contratos;
    private final RepositorioJson<Motorista> motoristas;
    private final RepositorioJson<Veiculo> veiculos;
    private final RepositorioJson<Entrega> entregas;
    private final RepositorioJson<NotaFiscal> notas;

    private final ClienteServico clienteServico;
    private final FrotaServico frotaServico;
    private final AgendaServico agendaServico;
    private final FaturamentoServico faturamentoServico;
    private final CalculadoraFrete calculadora = new CalculadoraFrete();
    private final EntregaServico entregaServico;
    private final GeradorRelatorios relatorios;

    public Sistema(Path pastaDados, Clock relogio) {
        this.pastaDados = pastaDados;
        clientes = repositorio("clientes.json", new TypeToken<List<Cliente>>() { }.getType());
        contratos = repositorio("contratos.json", new TypeToken<List<Contrato>>() { }.getType());
        motoristas = repositorio("motoristas.json", new TypeToken<List<Motorista>>() { }.getType());
        veiculos = repositorio("veiculos.json", new TypeToken<List<Veiculo>>() { }.getType());
        entregas = repositorio("entregas.json", new TypeToken<List<Entrega>>() { }.getType());
        notas = repositorio("notas-fiscais.json", new TypeToken<List<NotaFiscal>>() { }.getType());

        clienteServico = new ClienteServico(clientes, contratos, entregas);
        frotaServico = new FrotaServico(motoristas, veiculos, entregas);
        agendaServico = new AgendaServico(entregas);
        faturamentoServico = new FaturamentoServico(notas);
        entregaServico = entregaServicoCom(relogio);
        relatorios = new GeradorRelatorios(pastaDados.resolve("relatorios"), clienteServico, frotaServico,
                entregaServico, faturamentoServico);
    }

    /** Cria um serviço de entregas que enxerga outra data como "hoje" (usado nos dados de demonstração). */
    public EntregaServico entregaServicoCom(Clock relogio) {
        return new EntregaServico(entregas, clienteServico, frotaServico, agendaServico, faturamentoServico,
                calculadora, relogio);
    }

    public boolean semDados() {
        return clientes.estaVazio() && motoristas.estaVazio() && veiculos.estaVazio() && entregas.estaVazio();
    }

    private <T extends Identificavel> RepositorioJson<T> repositorio(String arquivo, Type tipo) {
        RepositorioJson<T> repositorio = new RepositorioJson<>(pastaDados.resolve(arquivo), tipo, gson);
        repositorio.getAviso().ifPresent(avisos::add);
        return repositorio;
    }

    public Path getPastaDados() {
        return pastaDados;
    }

    public List<String> getAvisos() {
        return avisos;
    }

    public ClienteServico clientes() {
        return clienteServico;
    }

    public FrotaServico frota() {
        return frotaServico;
    }

    public AgendaServico agenda() {
        return agendaServico;
    }

    public EntregaServico entregas() {
        return entregaServico;
    }

    public FaturamentoServico faturamento() {
        return faturamentoServico;
    }

    public GeradorRelatorios relatorios() {
        return relatorios;
    }
}
