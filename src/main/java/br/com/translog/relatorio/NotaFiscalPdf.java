package br.com.translog.relatorio;

import br.com.translog.modelo.Cliente;
import br.com.translog.modelo.DetalheFrete;
import br.com.translog.modelo.Entrega;
import br.com.translog.modelo.Motorista;
import br.com.translog.modelo.NotaFiscal;
import br.com.translog.modelo.Veiculo;
import br.com.translog.util.Formatos;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class NotaFiscalPdf extends DocumentoPdf {

    private final NotaFiscal nota;
    private final Entrega entrega;
    private final Cliente cliente;
    private final Motorista motorista;
    private final Veiculo veiculo;

    public NotaFiscalPdf(NotaFiscal nota, Entrega entrega, Cliente cliente, Motorista motorista, Veiculo veiculo) {
        this.nota = nota;
        this.entrega = entrega;
        this.cliente = cliente;
        this.motorista = motorista;
        this.veiculo = veiculo;
    }

    @Override
    protected String getTitulo() {
        return "Nota Fiscal de Serviço de Transporte nº " + nota.getNumeroFormatado();
    }

    @Override
    protected String getSubtitulo() {
        return "Emitida em " + Formatos.data(nota.getDataEmissao()) + " - referente à entrega #" + entrega.getId();
    }

    @Override
    protected void escreverCorpo() throws IOException {
        secao("Tomador do serviço");
        campo("Razão social", nota.getClienteNome());
        campo("CNPJ", nota.getClienteCnpj());
        campo("Tipo de cliente", cliente.getTipo().getDescricao());

        secao("Dados do transporte");
        campo("Rota", entrega.getRota().toString());
        campo("Distância", Formatos.numero(entrega.getRota().getDistanciaKm()) + " km");
        campo("Carga", entrega.getCarga().getDescricao() + " - " + Formatos.numero(entrega.getCarga().getPesoKg())
                + " kg (" + entrega.getCarga().getTipo().getDescricao() + ", "
                + entrega.getCarga().getNatureza().getDescricao().toLowerCase() + ")");
        campo("Motorista", motorista.getNome() + " - CNH " + motorista.getCategoriaCnh());
        campo("Veículo", veiculo.getPlaca() + " - " + veiculo.getModelo());
        campo("Modalidade", entrega.isUrgente() ? "Urgente" : "Regular");
        campo("Saída", Formatos.data(entrega.getDataAgendada()));
        campo("Prazo contratual", Formatos.data(entrega.getDataLimite()) + " (" + entrega.getPrazoDias() + " dias)");
        campo("Entregue em", Formatos.data(entrega.getDataEntrega()));

        secao("Composição do valor");
        DetalheFrete f = entrega.getFrete();
        List<String[]> linhas = new ArrayList<>();
        linhas.add(new String[]{"Frete base (" + Formatos.numero(entrega.getRota().getDistanciaKm()) + " km x "
                + Formatos.moeda(entrega.getCarga().getTipo().getValorPorKm()) + "/km)", Formatos.moeda(f.getValorBase())});
        adicionarSeHouver(linhas, "Adicional transporte especial (40%)", f.getAdicionalEspecial(), false);
        adicionarSeHouver(linhas, "Adicional entrega urgente (30%)", f.getAdicionalUrgencia(), false);
        adicionarSeHouver(linhas, "Desconto cliente " + cliente.getTipo().getDescricao().toLowerCase()
                + " (" + Formatos.percentual(f.getPercentualDesconto()) + ")", f.getDesconto(), true);
        adicionarSeHouver(linhas, "Ajuste ao frete mínimo do contrato", f.getAjusteFreteMinimo(), false);
        adicionarSeHouver(linhas, "Abatimento por atraso (" + f.getDiasAtraso() + " dia(s), "
                + Formatos.percentual(f.getPercentualPenalidade()) + ")", f.getPenalidade(), true);

        tabela(new String[]{"Descrição", "Valor"}, new float[]{0.75f, 0.25f}, linhas, new boolean[]{false, true});
        espaco(4);
        destaque("VALOR TOTAL DA NOTA", Formatos.moeda(nota.getValorTotal()));

        espaco(10);
        paragrafo("Documento de controle interno gerado pelo sistema Translog. Não substitui a NFS-e/CT-e "
                + "emitida junto à Secretaria da Fazenda.", 8);
    }

    private static void adicionarSeHouver(List<String[]> linhas, String descricao, BigDecimal valor, boolean negativo) {
        if (valor != null && valor.signum() != 0) {
            String texto = Formatos.moeda(valor);
            linhas.add(new String[]{descricao, negativo ? "- " + texto : texto});
        }
    }
}
