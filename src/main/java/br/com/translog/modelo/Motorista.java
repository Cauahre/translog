package br.com.translog.modelo;

import br.com.translog.excecao.ValidacaoException;
import br.com.translog.util.ValidadorDocumento;

import java.time.LocalDate;

public class Motorista extends Pessoa {

    private String cpf;
    private String numeroCnh;
    private CategoriaCnh categoriaCnh;
    private LocalDate validadeCnh;
    private boolean ativo = true;

    public Motorista() {
    }

    public Motorista(String nome, String cpf, String telefone, String email,
                     String numeroCnh, CategoriaCnh categoria, LocalDate validadeCnh) {
        super(nome, telefone, email);
        setCpf(cpf);
        setNumeroCnh(numeroCnh);
        setCategoriaCnh(categoria);
        setValidadeCnh(validadeCnh);
    }

    /** O motorista só pode dirigir se a CNH tiver a categoria exigida e estiver válida na data (RN14, RN15). */
    public boolean podeConduzir(CategoriaCnh exigida, LocalDate data) {
        return categoriaCnh.habilita(exigida) && isCnhValidaEm(data);
    }

    public boolean isCnhValidaEm(LocalDate data) {
        return validadeCnh != null && !data.isAfter(validadeCnh);
    }

    @Override
    public String getDocumento() {
        return ValidadorDocumento.formatarCpf(cpf);
    }

    @Override
    public String getDescricaoTipo() {
        return "Motorista CNH " + categoriaCnh;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = ValidadorDocumento.validarCpf(cpf);
    }

    public String getNumeroCnh() {
        return numeroCnh;
    }

    public void setNumeroCnh(String numeroCnh) {
        String digitos = numeroCnh == null ? "" : numeroCnh.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            throw new ValidacaoException("O número da CNH deve ter 11 dígitos.");
        }
        this.numeroCnh = digitos;
    }

    public CategoriaCnh getCategoriaCnh() {
        return categoriaCnh;
    }

    public void setCategoriaCnh(CategoriaCnh categoriaCnh) {
        if (categoriaCnh == null) {
            throw new ValidacaoException("Informe a categoria da CNH (A, B, C, D ou E).");
        }
        this.categoriaCnh = categoriaCnh;
    }

    public LocalDate getValidadeCnh() {
        return validadeCnh;
    }

    public void setValidadeCnh(LocalDate validadeCnh) {
        if (validadeCnh == null) {
            throw new ValidacaoException("A validade da CNH é obrigatória.");
        }
        this.validadeCnh = validadeCnh;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
