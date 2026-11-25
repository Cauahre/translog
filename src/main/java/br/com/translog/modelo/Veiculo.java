package br.com.translog.modelo;

import br.com.translog.excecao.ValidacaoException;
import br.com.translog.util.Validacao;
import br.com.translog.util.ValidadorDocumento;

import java.time.Year;

public class Veiculo implements Identificavel {

    private static final double CAPACIDADE_MAXIMA_KG = 60_000;

    private int id;
    private String placa;
    private String modelo;
    private TipoVeiculo tipo;
    private double capacidadeKg;
    private int ano;
    private boolean ativo = true;

    public Veiculo() {
    }

    public Veiculo(String placa, String modelo, TipoVeiculo tipo, double capacidadeKg, int ano) {
        setPlaca(placa);
        setModelo(modelo);
        setTipo(tipo);
        setCapacidadeKg(capacidadeKg);
        setAno(ano);
    }

    /** O peso da carga não pode passar da capacidade (RN16). */
    public boolean suporta(double pesoKg) {
        return pesoKg <= capacidadeKg;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = ValidadorDocumento.validarPlaca(placa);
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = Validacao.exigirTexto(modelo, "Modelo");
    }

    public TipoVeiculo getTipo() {
        return tipo;
    }

    public void setTipo(TipoVeiculo tipo) {
        if (tipo == null) {
            throw new ValidacaoException("Informe o tipo do veículo.");
        }
        this.tipo = tipo;
    }

    public double getCapacidadeKg() {
        return capacidadeKg;
    }

    public void setCapacidadeKg(double capacidadeKg) {
        if (capacidadeKg <= 0 || capacidadeKg > CAPACIDADE_MAXIMA_KG) {
            throw new ValidacaoException("A capacidade deve ser maior que zero e no máximo 60.000 kg.");
        }
        this.capacidadeKg = capacidadeKg;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        int proximoAno = Year.now().getValue() + 1;
        if (ano < 1980 || ano > proximoAno) {
            throw new ValidacaoException("Ano do veículo inválido (1980 a " + proximoAno + ").");
        }
        this.ano = ano;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    @Override
    public String toString() {
        return "#" + id + " - " + placa + " " + modelo + " (" + tipo.getDescricao() + ")";
    }
}
