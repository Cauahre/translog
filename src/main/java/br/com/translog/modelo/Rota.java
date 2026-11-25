package br.com.translog.modelo;

import br.com.translog.excecao.ValidacaoException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Sequência de cidades de parada, da origem ao destino, com a distância total. */
public class Rota {

    /** Quantos km um motorista roda por dia, usado para estimar a duração da viagem. */
    public static final double KM_POR_DIA = 500;

    private List<String> paradas = new ArrayList<>();
    private double distanciaKm;

    public Rota() {
    }

    public Rota(List<String> paradas, double distanciaKm) {
        if (paradas == null || paradas.size() < 2) {
            throw new ValidacaoException("A rota precisa de pelo menos origem e destino.");
        }
        for (String cidade : paradas) {
            if (cidade == null || cidade.isBlank()) {
                throw new ValidacaoException("Há uma cidade sem nome na rota.");
            }
            this.paradas.add(cidade.trim());
        }
        if (distanciaKm <= 0 || distanciaKm > 10_000) {
            throw new ValidacaoException("A distância deve ser maior que zero e no máximo 10.000 km.");
        }
        this.distanciaKm = distanciaKm;
    }

    public String getOrigem() {
        return paradas.get(0);
    }

    public String getDestino() {
        return paradas.get(paradas.size() - 1);
    }

    public List<String> getParadas() {
        return Collections.unmodifiableList(paradas);
    }

    public double getDistanciaKm() {
        return distanciaKm;
    }

    /** Dias que o veículo e o motorista ficam ocupados, mínimo 1 (RN18). */
    public int getDiasEstimados() {
        return Math.max(1, (int) Math.ceil(distanciaKm / KM_POR_DIA));
    }

    @Override
    public String toString() {
        return String.join(" > ", paradas);
    }
}
