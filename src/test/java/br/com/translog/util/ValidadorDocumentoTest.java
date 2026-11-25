package br.com.translog.util;

import br.com.translog.excecao.ValidacaoException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidadorDocumentoTest {

    @Test
    void aceitaCpfValidoComOuSemMascara() {
        assertEquals("52601815906", ValidadorDocumento.validarCpf("526.018.159-06"));
        assertEquals("52601815906", ValidadorDocumento.validarCpf("52601815906"));
    }

    @Test
    void rejeitaCpfComDigitoErradoOuRepetido() {
        assertThrows(ValidacaoException.class, () -> ValidadorDocumento.validarCpf("526.018.159-07"));
        assertThrows(ValidacaoException.class, () -> ValidadorDocumento.validarCpf("111.111.111-11"));
        assertThrows(ValidacaoException.class, () -> ValidadorDocumento.validarCpf("123"));
    }

    @Test
    void validaCnpj() {
        assertEquals("99351819000160", ValidadorDocumento.validarCnpj("99.351.819/0001-60"));
        assertThrows(ValidacaoException.class, () -> ValidadorDocumento.validarCnpj("99.351.819/0001-61"));
    }

    @Test
    void aceitaPlacaAntigaEMercosul() {
        assertEquals("ABC1234", ValidadorDocumento.validarPlaca("abc-1234"));
        assertEquals("ABC1D23", ValidadorDocumento.validarPlaca("ABC1D23"));
        assertThrows(ValidacaoException.class, () -> ValidadorDocumento.validarPlaca("AB12345"));
    }
}
