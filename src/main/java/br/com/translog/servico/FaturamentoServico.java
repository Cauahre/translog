package br.com.translog.servico;

import br.com.translog.excecao.RegistroNaoEncontradoException;
import br.com.translog.excecao.RegraNegocioException;
import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.NotaFiscal;
import br.com.translog.modelo.StatusEntrega;
import br.com.translog.persistencia.Repositorio;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Emissão de notas fiscais e relatórios de faturamento. */
public class FaturamentoServico {

    private final Repositorio<NotaFiscal> notas;

    public FaturamentoServico(Repositorio<NotaFiscal> notas) {
        this.notas = notas;
    }

    /** Emite a nota de uma entrega concluída. Cada entrega gera uma única nota (RN26). */
    public NotaFiscal emitirNota(Entrega entrega, Cliente cliente) {
        if (entrega.getStatus() != StatusEntrega.ENTREGUE) {
            throw new RegraNegocioException("Só é possível emitir nota fiscal de entregas concluídas.");
        }
        if (notas.existe(n -> n.getEntregaId() == entrega.getId())) {
            throw new RegraNegocioException("A entrega #" + entrega.getId() + " já possui nota fiscal.");
        }
        return notas.salvar(new NotaFiscal(entrega, cliente, entrega.getDataEntrega()));
    }

    public NotaFiscal buscarNota(int numero) {
        return notas.buscarPorId(numero).orElseThrow(() -> new RegistroNaoEncontradoException("Nota fiscal", numero));
    }

    public Optional<NotaFiscal> notaDaEntrega(int entregaId) {
        return notas.filtrar(n -> n.getEntregaId() == entregaId).stream().findFirst();
    }

    public List<NotaFiscal> listarNotas() {
        return notas.listarTodos().stream()
                .sorted(Comparator.comparing(NotaFiscal::getId).reversed())
                .toList();
    }

    public ResumoFaturamento resumoDoCliente(Cliente cliente, LocalDate inicio, LocalDate fim) {
        List<NotaFiscal> doCliente = notas.filtrar(n -> n.getClienteId() == cliente.getId()
                        && !n.getDataEmissao().isBefore(inicio)
                        && !n.getDataEmissao().isAfter(fim)).stream()
                .sorted(Comparator.comparing(NotaFiscal::getDataEmissao))
                .toList();
        return ResumoFaturamento.de(cliente, doCliente);
    }

    /** Resumo de todos os clientes que faturaram no período, do maior para o menor. */
    public List<ResumoFaturamento> resumoGeral(List<Cliente> clientes, LocalDate inicio, LocalDate fim) {
        return clientes.stream()
                .map(c -> resumoDoCliente(c, inicio, fim))
                .filter(r -> r.quantidadeEntregas() > 0)
                .sorted(Comparator.comparing(ResumoFaturamento::totalLiquido).reversed())
                .toList();
    }
}
