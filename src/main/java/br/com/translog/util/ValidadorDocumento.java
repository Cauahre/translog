package br.com.translog.util;

import br.com.translog.excecao.ValidacaoException;

import java.util.regex.Pattern;

/**
 * Validação de CPF, CNPJ (com dígitos verificadores) e placa de veículo (RN28)
 * (padrão antigo ABC1234 e padrão Mercosul ABC1D23).
 */
public final class ValidadorDocumento {

    private static final Pattern PLACA = Pattern.compile("^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$");

    private ValidadorDocumento() {
    }

    public static String validarCpf(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || todosIguais(d)) {
            throw new ValidacaoException("CPF inválido: " + cpf);
        }
        int[] pesos1 = {10, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {11, 10, 9, 8, 7, 6, 5, 4, 3, 2};
        if (digito(d, pesos1) != d.charAt(9) - '0' || digito(d, pesos2) != d.charAt(10) - '0') {
            throw new ValidacaoException("CPF inválido: " + cpf);
        }
        return d;
    }

    public static String validarCnpj(String cnpj) {
        String d = somenteDigitos(cnpj);
        if (d.length() != 14 || todosIguais(d)) {
            throw new ValidacaoException("CNPJ inválido: " + cnpj);
        }
        int[] pesos1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] pesos2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        if (digito(d, pesos1) != d.charAt(12) - '0' || digito(d, pesos2) != d.charAt(13) - '0') {
            throw new ValidacaoException("CNPJ inválido: " + cnpj);
        }
        return d;
    }

    public static String validarPlaca(String placa) {
        String p = placa == null ? "" : placa.replaceAll("[\\s-]", "").toUpperCase();
        if (!PLACA.matcher(p).matches()) {
            throw new ValidacaoException("Placa inválida: " + placa + " (use ABC1234 ou ABC1D23).");
        }
        return p;
    }

    public static String formatarCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }

    public static String formatarCnpj(String cnpj) {
        if (cnpj == null || cnpj.length() != 14) {
            return cnpj;
        }
        return cnpj.substring(0, 2) + "." + cnpj.substring(2, 5) + "." + cnpj.substring(5, 8) + "/"
                + cnpj.substring(8, 12) + "-" + cnpj.substring(12);
    }

    private static int digito(String numeros, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (numeros.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    private static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    private static boolean todosIguais(String d) {
        return d.chars().distinct().count() == 1;
    }
}
