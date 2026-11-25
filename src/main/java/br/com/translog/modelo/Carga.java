package br.com.translog.modelo;

import br.com.translog.excecao.ValidacaoException;
import br.com.translog.util.Validacao;

public class Carga {

    private String descricao;
    private double pesoKg;
    private NaturezaCarga natureza;

    public Carga() {
    }

    public Carga(String descricao, double pesoKg, NaturezaCarga natureza) {
        this.descricao = Validacao.exigirTexto(descricao, "Descrição da carga");
        if (pesoKg <= 0) {
            throw new ValidacaoException("O peso da carga deve ser maior que zero.");
        }
        if (natureza == null) {
            throw new ValidacaoException("Informe a natureza da carga.");
        }
        this.pesoKg = pesoKg;
        this.natureza = natureza;
    }

    /** O tipo não é guardado: sempre sai do peso, assim não tem como ficar inconsistente. */
    public TipoCarga getTipo() {
        return TipoCarga.classificar(pesoKg);
    }

    public boolean exigeTransporteEspecial() {
        return natureza.exigeTransporteEspecial();
    }

    public String getDescricao() {
        return descricao;
    }

    public double getPesoKg() {
        return pesoKg;
    }

    public NaturezaCarga getNatureza() {
        return natureza;
    }
}
