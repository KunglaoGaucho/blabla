package com.pfc.thindesk.dominio;

import java.util.List;

/** Situações possíveis de um chamado, com o rótulo exibido no dashboard. */
public enum StatusChamado {

    ABERTO("Aberto"),
    EM_ANDAMENTO("Em andamento"),
    PENDENTE("Pendente"),
    CONCLUIDO("Concluído"),
    CANCELADO("Cancelado");

    private final String rotulo;

    StatusChamado(String rotulo) {
        this.rotulo = rotulo;
    }

    public String rotulo() {
        return rotulo;
    }

    /** Situações que representam trabalho ainda não finalizado. */
    public static List<String> emAberto() {
        return List.of(ABERTO.name(), EM_ANDAMENTO.name(), PENDENTE.name());
    }
}
