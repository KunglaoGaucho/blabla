package com.pfc.thindesk.api;

import java.time.Instant;

/** Formato único de erro devolvido por todos os endpoints da API. */
public record ErroApi(Instant timestamp, int status, String erro, String mensagem, String campo, String caminho) {
}
