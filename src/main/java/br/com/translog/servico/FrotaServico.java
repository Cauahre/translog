package br.com.translog.servico;

import br.com.translog.excecao.RegistroNaoEncontradoException;
import br.com.translog.excecao.RegraNegocioException;
import br.com.translog.modelo.CategoriaCnh;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Motorista;
import br.com.translog.modelo.TipoVeiculo;
import br.com.translog.modelo.Veiculo;
import br.com.translog.persistencia.Repositorio;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/** Cadastro de motoristas e veículos. */
public class FrotaServico {

    private final Repositorio<Motorista> motoristas;
    private final Repositorio<Veiculo> veiculos;
    private final Repositorio<Entrega> entregas;

    public FrotaServico(Repositorio<Motorista> motoristas, Repositorio<Veiculo> veiculos, Repositorio<Entrega> entregas) {
        this.motoristas = motoristas;
        this.veiculos = veiculos;
        this.entregas = entregas;
    }

    // ---------- motoristas ----------

    public Motorista cadastrarMotorista(String nome, String cpf, String telefone, String email,
                                        String numeroCnh, CategoriaCnh categoria, LocalDate validadeCnh) {
        Motorista motorista = new Motorista(nome, cpf, telefone, email, numeroCnh, categoria, validadeCnh);
        verificarMotoristaUnico(motorista, 0);
        return motoristas.salvar(motorista);
    }

    public Motorista atualizarMotorista(int id, String nome, String telefone, String email,
                                        CategoriaCnh categoria, LocalDate validadeCnh) {
        Motorista motorista = buscarMotorista(id);
        motorista.setNome(nome);
        motorista.setTelefone(telefone);
        motorista.setEmail(email);
        motorista.setCategoriaCnh(categoria);
        motorista.setValidadeCnh(validadeCnh);
        return motoristas.salvar(motorista);
    }

    public void alterarSituacaoMotorista(int id, boolean ativo) {
        Motorista motorista = buscarMotorista(id);
        if (!ativo && possuiPendencias(e -> e.getMotoristaId() == id)) {
            throw new RegraNegocioException("O motorista tem entregas agendadas ou em trânsito. "
                    + "Reatribua ou cancele essas entregas antes de inativá-lo.");
        }
        motorista.setAtivo(ativo);
        motoristas.salvar(motorista);
    }

    public void removerMotorista(int id) {
        buscarMotorista(id);
        if (entregas.existe(e -> e.getMotoristaId() == id)) {
            throw new RegraNegocioException("O motorista está vinculado a entregas e não pode ser removido. Inative-o.");
        }
        motoristas.remover(id);
    }

    public Motorista buscarMotorista(int id) {
        return motoristas.buscarPorId(id).orElseThrow(() -> new RegistroNaoEncontradoException("Motorista", id));
    }

    public List<Motorista> listarMotoristas() {
        return motoristas.listarTodos().stream()
                .sorted(Comparator.comparing(Motorista::getNome, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Motoristas cuja CNH vence nos próximos dias (ou já venceu). */
    public List<Motorista> motoristasComCnhVencendo(LocalDate hoje, int dias) {
        LocalDate limite = hoje.plusDays(dias);
        return motoristas.filtrar(m -> m.isAtivo() && !m.getValidadeCnh().isAfter(limite));
    }

    // ---------- veículos ----------

    public Veiculo cadastrarVeiculo(String placa, String modelo, TipoVeiculo tipo, double capacidadeKg, int ano) {
        Veiculo veiculo = new Veiculo(placa, modelo, tipo, capacidadeKg, ano);
        verificarPlacaUnica(veiculo.getPlaca(), 0);
        return veiculos.salvar(veiculo);
    }

    public Veiculo atualizarVeiculo(int id, String modelo, TipoVeiculo tipo, double capacidadeKg, int ano) {
        Veiculo veiculo = buscarVeiculo(id);
        veiculo.setModelo(modelo);
        veiculo.setTipo(tipo);
        veiculo.setCapacidadeKg(capacidadeKg);
        veiculo.setAno(ano);
        return veiculos.salvar(veiculo);
    }

    public void alterarSituacaoVeiculo(int id, boolean ativo) {
        Veiculo veiculo = buscarVeiculo(id);
        if (!ativo && possuiPendencias(e -> e.getVeiculoId() == id)) {
            throw new RegraNegocioException("O veículo tem entregas agendadas ou em trânsito.");
        }
        veiculo.setAtivo(ativo);
        veiculos.salvar(veiculo);
    }

    public void removerVeiculo(int id) {
        buscarVeiculo(id);
        if (entregas.existe(e -> e.getVeiculoId() == id)) {
            throw new RegraNegocioException("O veículo está vinculado a entregas e não pode ser removido. Inative-o.");
        }
        veiculos.remover(id);
    }

    public Veiculo buscarVeiculo(int id) {
        return veiculos.buscarPorId(id).orElseThrow(() -> new RegistroNaoEncontradoException("Veículo", id));
    }

    public List<Veiculo> listarVeiculos() {
        return veiculos.listarTodos().stream()
                .sorted(Comparator.comparing(Veiculo::getPlaca))
                .toList();
    }

    /** Veículos ativos que aguentam o peso informado. */
    public List<Veiculo> veiculosQueSuportam(double pesoKg) {
        return listarVeiculos().stream().filter(v -> v.isAtivo() && v.suporta(pesoKg)).toList();
    }

    // ---------- auxiliares ----------

    private boolean possuiPendencias(Predicate<Entrega> vinculo) {
        return entregas.existe(e -> vinculo.test(e) && e.getStatus().isPendente());
    }

    private void verificarMotoristaUnico(Motorista novo, int idIgnorado) {
        if (motoristas.existe(m -> m.getCpf().equals(novo.getCpf()) && m.getId() != idIgnorado)) {
            throw new RegraNegocioException("Já existe um motorista com esse CPF.");
        }
        if (motoristas.existe(m -> m.getNumeroCnh().equals(novo.getNumeroCnh()) && m.getId() != idIgnorado)) {
            throw new RegraNegocioException("Já existe um motorista com esse número de CNH.");
        }
    }

    private void verificarPlacaUnica(String placa, int idIgnorado) {
        if (veiculos.existe(v -> v.getPlaca().equals(placa) && v.getId() != idIgnorado)) {
            throw new RegraNegocioException("Já existe um veículo com a placa " + placa + ".");
        }
    }
}
