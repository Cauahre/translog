package br.com.translog.modelo;

import br.com.translog.excecao.RegraNegocioException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegrasDoModeloTest {

    @Test
    void classificacaoDaCargaPeloPeso() {
        assertEquals(TipoCarga.LEVE, TipoCarga.classificar(500));
        assertEquals(TipoCarga.MEDIA, TipoCarga.classificar(500.5));
        assertEquals(TipoCarga.MEDIA, TipoCarga.classificar(3_000));
        assertEquals(TipoCarga.PESADA, TipoCarga.classificar(3_001));
    }

    @Test
    void hierarquiaDaCnh() {
        assertTrue(CategoriaCnh.E.habilita(CategoriaCnh.C));
        assertTrue(CategoriaCnh.C.habilita(CategoriaCnh.C));
        assertTrue(CategoriaCnh.D.habilita(CategoriaCnh.B));
        assertFalse(CategoriaCnh.B.habilita(CategoriaCnh.C));
        assertFalse(CategoriaCnh.A.habilita(CategoriaCnh.B));
    }

    @Test
    void polimorfismoDoDescontoPorTipoDeCliente() {
        Cliente empresarial = new ClienteEmpresarial("A Ltda", "99351819000160", null, null);
        Cliente prioritario = new ClientePrioritario("B Ltda", "09378657000161", null, null);

        assertEquals(new BigDecimal("0.10"), empresarial.percentualDesconto(false));
        assertEquals(BigDecimal.ZERO, empresarial.percentualDesconto(true));
        assertEquals(new BigDecimal("0.20"), prioritario.percentualDesconto(true));
        assertFalse(empresarial.podeSolicitarUrgencia());
        assertTrue(prioritario.podeSolicitarUrgencia());
    }

    @Test
    void transicoesDeStatusPermitidas() {
        assertTrue(StatusEntrega.AGENDADA.podeMudarPara(StatusEntrega.EM_TRANSITO));
        assertTrue(StatusEntrega.EM_TRANSITO.podeMudarPara(StatusEntrega.ENTREGUE));
        assertTrue(StatusEntrega.AGENDADA.podeMudarPara(StatusEntrega.CANCELADA));
        assertFalse(StatusEntrega.AGENDADA.podeMudarPara(StatusEntrega.ENTREGUE));
        assertFalse(StatusEntrega.ENTREGUE.podeMudarPara(StatusEntrega.CANCELADA));
        assertFalse(StatusEntrega.CANCELADA.podeMudarPara(StatusEntrega.AGENDADA));
    }

    @Test
    void entregaNaoPulaEtapas() {
        Entrega entrega = novaEntrega(LocalDate.of(2026, 12, 1), 3);
        assertThrows(RegraNegocioException.class,
                () -> entrega.mudarStatus(StatusEntrega.ENTREGUE, LocalDateTime.now(), "teste"));
    }

    @Test
    void ocupacaoDaAgendaConsideraDuracaoDaViagem() {
        // 1.200 km = 3 dias de viagem: 01, 02 e 03/12
        Entrega entrega = novaEntrega(LocalDate.of(2026, 12, 1), 1_200);
        assertEquals(LocalDate.of(2026, 12, 3), entrega.getFimOcupacao());
        assertTrue(entrega.ocupaPeriodo(LocalDate.of(2026, 12, 3), LocalDate.of(2026, 12, 3)));
        assertFalse(entrega.ocupaPeriodo(LocalDate.of(2026, 12, 4), LocalDate.of(2026, 12, 5)));
    }

    @Test
    void contagemDeDiasDeAtraso() {
        Entrega entrega = novaEntrega(LocalDate.of(2026, 12, 1), 100); // prazo de 5 dias -> 06/12
        assertEquals(0, entrega.diasDeAtraso(LocalDate.of(2026, 12, 6)));
        assertEquals(2, entrega.diasDeAtraso(LocalDate.of(2026, 12, 8)));
    }

    private static Entrega novaEntrega(LocalDate saida, double km) {
        Rota rota = new Rota(List.of("Teresina", "Parnaíba"), km);
        Carga carga = new Carga("Teste", 100, NaturezaCarga.COMUM);
        return new Entrega(1, 1, 1, 1, carga, rota, false, saida, saida, 5, null);
    }
}
