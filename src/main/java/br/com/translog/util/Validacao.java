package br.com.translog.util;

import br.com.translog.excecao.ValidacaoException;

import java.util.regex.Pattern;

/** Validações simples de texto reaproveitadas pelas classes de modelo. */
public final class Validacao {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private Validacao() {
    }

    public static String exigirTexto(String valor, String campo) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacaoException(campo + " é obrigatório.");
        }
        String limpo = valor.trim();
        if (limpo.length() > 120) {
            throw new ValidacaoException(campo + " pode ter no máximo 120 caracteres.");
        }
        return limpo;
    }

    public static String textoOpcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    /** Telefone é opcional, mas se vier precisa ter DDD + número (10 ou 11 dígitos). */
    public static String validarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return null;
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() != 10 && digitos.length() != 11) {
            throw new ValidacaoException("Telefone inválido. Informe DDD + número, ex.: (86) 99999-0000.");
        }
        return digitos;
    }

    public static String validarEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        String limpo = email.trim().toLowerCase();
        if (!EMAIL.matcher(limpo).matches()) {
            throw new ValidacaoException("E-mail inválido: " + email);
        }
        return limpo;
    }
}
