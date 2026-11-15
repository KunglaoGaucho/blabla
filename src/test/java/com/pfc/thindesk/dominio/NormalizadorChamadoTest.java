package com.pfc.thindesk.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class NormalizadorChamadoTest {

    @ParameterizedTest
    @CsvSource({
        "Aberto, ABERTO",
        "em andamento, EM_ANDAMENTO",
        "EM_ANDAMENTO, EM_ANDAMENTO",
        "Em-Andamento, EM_ANDAMENTO",
        "Concluído, CONCLUIDO",
        "resolvido, CONCLUIDO",
        "aguardando, PENDENTE",
        "Cancelado, CANCELADO"
    })
    void padronizaGrafiasDiferentesDeStatus(String entrada, StatusChamado esperado) {
        assertThat(NormalizadorChamado.status(entrada)).contains(esperado);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "xyz", "fechou"})
    void statusDesconhecidoNaoEhAceito(String entrada) {
        assertThat(NormalizadorChamado.status(entrada)).isEmpty();
    }

    @Test
    void statusNuloNaoEhAceito() {
        assertThat(NormalizadorChamado.status(null)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"Média, MEDIA", "alta, ALTA", "Crítica, CRITICA", "urgente, CRITICA", "normal, MEDIA"})
    void padronizaPrioridade(String entrada, Prioridade esperada) {
        assertThat(NormalizadorChamado.prioridade(entrada)).contains(esperada);
    }

    @Test
    void textoVazioRecebeValorPadrao() {
        assertThat(NormalizadorChamado.texto("   ", NormalizadorChamado.NAO_ATRIBUIDO)).isEqualTo("Não atribuído");
        assertThat(NormalizadorChamado.texto(null, "padrão")).isEqualTo("padrão");
        assertThat(NormalizadorChamado.texto("  Rede   local ", null)).isEqualTo("Rede local");
    }
}
