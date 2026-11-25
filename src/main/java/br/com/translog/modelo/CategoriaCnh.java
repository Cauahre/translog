package br.com.translog.modelo;

/**
 * Categorias da CNH. A categoria A (moto) não habilita nenhum veículo de carga da frota.
 * De B em diante, uma categoria maior habilita tudo o que as menores habilitam.
 */
public enum CategoriaCnh {
    A, B, C, D, E;

    public boolean habilita(CategoriaCnh exigida) {
        if (exigida == A) {
            return this == A;
        }
        if (this == A) {
            return false;
        }
        return this.ordinal() >= exigida.ordinal();
    }

    public static CategoriaCnh deTexto(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            return valueOf(texto.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
