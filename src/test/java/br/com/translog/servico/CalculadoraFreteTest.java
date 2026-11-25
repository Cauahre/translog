package br.com.translog.servico;

import br.com.translog.modelo.Carga;
import br.com.translog.modelo.ClienteEmpresarial;
import br.com.translog.modelo.ClientePrioritario;
import br.com.translog.modelo.Contrato;
import br.com.translog.modelo.DetalheFrete;
import br.com.translog.modelo.NaturezaCarga;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraFreteTest {

    private final CalculadoraFrete calculadora = new CalculadoraFrete();
    private final ClienteEmpresarial empresarial =
            new ClienteEmpresarial("Empresa Teste Ltda", "99351819000160", null, null);
    private final ClientePrioritario prioritario =
            new ClientePrioritario("Prioritaria Teste Ltda", "09378657000161", null, null);
    private final Contrato semMinimo = contrato(BigDecimal.ZERO);

    @Test
    void cargaLeveComumParaEmpresarialTemDescontoDe10Porcento() {
        Carga carga = new Carga("Caixas", 200, NaturezaCarga.COMUM);
        DetalheFrete f = calculadora.calcular(empresarial, semMinimo, carga, 100, false);

        assertEquals(new BigDecimal("150.00"), f.getValorBase());
        assertEquals(new BigDecimal("15.00"), f.getDesconto());
        assertEquals(new BigDecimal("135.00"), f.getValorFrete());
    }

    @Test
    void cargaMediaUsaValorDe2e50PorKm() {
        Carga carga = new Carga("Móveis", 1_000, NaturezaCarga.COMUM);
        DetalheFrete f = calculadora.calcular(prioritario, semMinimo, carga, 100, false);

        assertEquals(new BigDecimal("250.00"), f.getValorBase());
        assertEquals(new BigDecimal("200.00"), f.getValorFrete());
    }

    @Test
    void cargaPesadaPerigosaUrgenteSomaAdicionaisSobreABase() {
        // 300 km x 4,00 = 1200 | +40% = 480 | +30% = 360 | subtotal 2040 | -20% = 408 | frete 1632
        Carga carga = new Carga("Produtos químicos", 5_000, NaturezaCarga.PERIGOSA);
        DetalheFrete f = calculadora.calcular(prioritario, semMinimo, carga, 300, true);

        assertEquals(new BigDecimal("1200.00"), f.getValorBase());
        assertEquals(new BigDecimal("480.00"), f.getAdicionalEspecial());
        assertEquals(new BigDecimal("360.00"), f.getAdicionalUrgencia());
        assertEquals(new BigDecimal("408.00"), f.getDesconto());
        assertEquals(new BigDecimal("1632.00"), f.getValorFrete());
    }

    @Test
    void freteMinimoDoContratoEhRespeitado() {
        Carga carga = new Carga("Documentos", 5, NaturezaCarga.COMUM);
        DetalheFrete f = calculadora.calcular(empresarial, contrato(new BigDecimal("300.00")), carga, 10, false);

        // 10 km x 1,50 = 15 - 10% = 13,50 -> sobe para o mínimo de 300
        assertEquals(new BigDecimal("300.00"), f.getValorFrete());
        assertEquals(new BigDecimal("286.50"), f.getAjusteFreteMinimo());
    }

    @Test
    void atrasoDe3DiasAbate15Porcento() {
        DetalheFrete f = calculadora.calcular(empresarial, semMinimo, new Carga("Caixas", 200, NaturezaCarga.COMUM), 1000, false);
        // frete = 1500 - 150 = 1350
        calculadora.aplicarPenalidade(f, 3);

        assertEquals(new BigDecimal("202.50"), f.getPenalidade());
        assertEquals(new BigDecimal("1147.50"), f.getValorFinal());
    }

    @Test
    void penalidadeEhLimitadaA25Porcento() {
        DetalheFrete f = calculadora.calcular(empresarial, semMinimo, new Carga("Caixas", 200, NaturezaCarga.COMUM), 1000, false);
        calculadora.aplicarPenalidade(f, 12);

        assertEquals(0, new BigDecimal("0.25").compareTo(f.getPercentualPenalidade()));
        assertEquals(new BigDecimal("337.50"), f.getPenalidade());
    }

    @Test
    void entregaNoPrazoNaoTemPenalidade() {
        DetalheFrete f = calculadora.calcular(empresarial, semMinimo, new Carga("Caixas", 200, NaturezaCarga.COMUM), 100, false);
        calculadora.aplicarPenalidade(f, 0);

        assertEquals(f.getValorFrete(), f.getValorFinal());
    }

    private static Contrato contrato(BigDecimal minimo) {
        return new Contrato(1, 5, minimo, null, LocalDate.of(2026, 1, 1), null);
    }
}
