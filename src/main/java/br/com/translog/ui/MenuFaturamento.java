package br.com.translog.ui;

import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.NotaFiscal;
import br.com.translog.relatorio.GeradorRelatorios;
import br.com.translog.servico.ClienteServico;
import br.com.translog.servico.FaturamentoServico;
import br.com.translog.servico.ResumoFaturamento;
import br.com.translog.util.Formatos;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MenuFaturamento extends Menu {

    private final FaturamentoServico faturamento;
    private final ClienteServico clientes;
    private final GeradorRelatorios relatorios;

    public MenuFaturamento(Console console, FaturamentoServico faturamento, ClienteServico clientes,
                           GeradorRelatorios relatorios) {
        super(console);
        this.faturamento = faturamento;
        this.clientes = clientes;
        this.relatorios = relatorios;
    }

    @Override
    protected String titulo() {
        return "Faturamento e notas fiscais";
    }

    @Override
    protected List<String> opcoes() {
        return List.of(
                "Listar notas fiscais",
                "Ver nota fiscal",
                "Gerar PDF de uma nota fiscal",
                "Faturamento por cliente (na tela)",
                "Relatório de faturamento de todos os clientes (PDF)",
                "Relatório de faturamento de um cliente (PDF)");
    }

    @Override
    protected void executar(int opcao) {
        switch (opcao) {
            case 1 -> listarNotas();
            case 2 -> verNota();
            case 3 -> salvar(relatorios.notaFiscal(console.lerCodigo("Número da nota")));
            case 4 -> resumoNaTela();
            case 5 -> {
                LocalDate[] periodo = lerPeriodo();
                salvar(relatorios.faturamento(periodo[0], periodo[1]));
            }
            case 6 -> {
                int clienteId = console.lerCodigo("Código do cliente");
                LocalDate[] periodo = lerPeriodo();
                salvar(relatorios.faturamentoDoCliente(clienteId, periodo[0], periodo[1]));
            }
            default -> console.erro("Opção inválida.");
        }
    }

    private void listarNotas() {
        List<String[]> linhas = new ArrayList<>();
        for (NotaFiscal n : faturamento.listarNotas()) {
            linhas.add(new String[]{
                    n.getNumeroFormatado(),
                    Formatos.data(n.getDataEmissao()),
                    n.getClienteNome(),
                    String.valueOf(n.getEntregaId()),
                    Formatos.moeda(n.getValorTotal())});
        }
        console.tabela(new String[]{"NF", "Emissão", "Cliente", "Entrega", "Valor"}, linhas);
    }

    private void verNota() {
        NotaFiscal n = faturamento.buscarNota(console.lerCodigo("Número da nota"));
        console.subtitulo("Nota fiscal nº " + n.getNumeroFormatado());
        console.campo("Emissão", Formatos.data(n.getDataEmissao()));
        console.campo("Cliente", n.getClienteNome());
        console.campo("CNPJ", n.getClienteCnpj());
        console.campo("Entrega", "#" + n.getEntregaId());
        console.campo("Serviço", n.getDescricaoServico());
        console.campo("Valor do serviço", Formatos.moeda(n.getValorServico()));
        console.campo("Desconto", "- " + Formatos.moeda(n.getDesconto()));
        if (n.getAjusteFreteMinimo() != null && n.getAjusteFreteMinimo().signum() > 0) {
            console.campo("Ajuste frete mínimo", "+ " + Formatos.moeda(n.getAjusteFreteMinimo()));
        }
        console.campo("Abatimento atraso", "- " + Formatos.moeda(n.getAbatimentoAtraso()));
        console.campo("TOTAL", Formatos.moeda(n.getValorTotal()));
    }

    private void resumoNaTela() {
        Cliente cliente = clientes.buscar(console.lerCodigo("Código do cliente"));
        LocalDate[] periodo = lerPeriodo();
        ResumoFaturamento r = faturamento.resumoDoCliente(cliente, periodo[0], periodo[1]);
        console.subtitulo("Faturamento de " + cliente.getNome() + " - " + Formatos.data(periodo[0])
                + " a " + Formatos.data(periodo[1]));
        List<String[]> linhas = new ArrayList<>();
        for (NotaFiscal n : r.notas()) {
            linhas.add(new String[]{n.getNumeroFormatado(), Formatos.data(n.getDataEmissao()),
                    Formatos.moeda(n.getValorServico()), Formatos.moeda(n.getDesconto()),
                    Formatos.moeda(n.getAbatimentoAtraso()), Formatos.moeda(n.getValorTotal())});
        }
        console.tabela(new String[]{"NF", "Emissão", "Serviço", "Desconto", "Atraso", "Total"}, linhas);
        console.info("");
        console.campo("Entregas faturadas", r.quantidadeEntregas());
        console.campo("Valor dos serviços", Formatos.moeda(r.totalServicos()));
        console.campo("Descontos", Formatos.moeda(r.totalDescontos()));
        console.campo("Abatimentos atraso", Formatos.moeda(r.totalAbatimentos()));
        console.campo("Total faturado", Formatos.moeda(r.totalLiquido()));
        console.campo("Ticket médio", Formatos.moeda(r.ticketMedio()));
    }

    private LocalDate[] lerPeriodo() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = console.lerData("Data inicial", hoje.withDayOfMonth(1).minusMonths(2));
        LocalDate fim = console.lerData("Data final", hoje);
        if (fim.isBefore(inicio)) {
            console.aviso("Data final antes da inicial; as datas foram invertidas.");
            return new LocalDate[]{fim, inicio};
        }
        return new LocalDate[]{inicio, fim};
    }

    private void salvar(Path arquivo) {
        console.sucesso("PDF salvo em " + arquivo.toAbsolutePath());
    }
}
