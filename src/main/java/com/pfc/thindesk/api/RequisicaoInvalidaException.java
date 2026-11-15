package com.pfc.thindesk.api;

/** Parâmetro ou corpo de requisição inválido (HTTP 400). */
public class RequisicaoInvalidaException extends RuntimeException {

    private final String campo;

    public RequisicaoInvalidaException(String campo, String mensagem) {
        super(mensagem);
        this.campo = campo;
    }

    public String campo() {
        return campo;
    }
}
