package br.com.translog.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Formatação de moeda, datas e números no padrão brasileiro. */
public final class Formatos {

    public static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

    private Formatos() {
    }

    public static String moeda(BigDecimal valor) {
        if (valor == null) {
            return "-";
        }
        NumberFormat formato = NumberFormat.getCurrencyInstance(BRASIL);
        // o Java usa espaço não separável depois do "R$"; troco por espaço comum para não quebrar o PDF
        return formato.format(valor).replace(' ', ' ');
    }

    public static String percentual(BigDecimal fracao) {
        if (fracao == null) {
            return "-";
        }
        return fracao.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
    }

    public static String numero(double valor) {
        NumberFormat formato = NumberFormat.getNumberInstance(BRASIL);
        formato.setMaximumFractionDigits(1);
        return formato.format(valor);
    }

    public static String data(LocalDate data) {
        return data == null ? "-" : data.format(DATA);
    }

    public static String dataHora(LocalDateTime dataHora) {
        return dataHora == null ? "-" : dataHora.format(DATA_HORA);
    }

    public static String telefone(String digitos) {
        if (digitos == null) {
            return "-";
        }
        if (digitos.length() == 11) {
            return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2, 7) + "-" + digitos.substring(7);
        }
        if (digitos.length() == 10) {
            return "(" + digitos.substring(0, 2) + ") " + digitos.substring(2, 6) + "-" + digitos.substring(6);
        }
        return digitos;
    }
}
