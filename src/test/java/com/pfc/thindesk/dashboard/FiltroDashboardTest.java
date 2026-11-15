package com.pfc.thindesk.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.pfc.thindesk.api.RequisicaoInvalidaException;

class FiltroDashboardTest {

    private static final ZoneId SP = ZoneId.of("America/Sao_Paulo");

    @Test
    void parametrosVaziosSignificamSemFiltro() {
        FiltroDashboard f = FiltroDashboard.de(null, "", " ", null, "", null);
        assertThat(f).isEqualTo(FiltroDashboard.VAZIO);
    }

    @Test
    void converteDatasEPadronizaStatusEPrioridade() {
        FiltroDashboard f = FiltroDashboard.de("2026-01-01", "2026-01-31", "em andamento", " Rede ", "João", "alta");
        assertThat(f.dataInicio()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(f.dataFim()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(f.status()).isEqualTo("EM_ANDAMENTO");
        assertThat(f.tipo()).isEqualTo("Rede");
        assertThat(f.prioridade()).isEqualTo("ALTA");
    }

    @Test
    void periodoIncluiODiaFinalInteiroNoFusoDeSaoPaulo() {
        FiltroDashboard f = FiltroDashboard.de("2026-03-10", "2026-03-10", null, null, null, null);
        assertThat(f.inicio(SP)).isEqualTo(Instant.parse("2026-03-10T03:00:00Z"));
        assertThat(f.fimExclusivo(SP)).isEqualTo(Instant.parse("2026-03-11T03:00:00Z"));
    }

    @Test
    void recusaDataEmFormatoInvalido() {
        assertThatThrownBy(() -> FiltroDashboard.de("10/03/2026", null, null, null, null, null))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("AAAA-MM-DD");
    }

    @Test
    void recusaDataInicialDepoisDaFinal() {
        assertThatThrownBy(() -> FiltroDashboard.de("2026-02-01", "2026-01-01", null, null, null, null))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void recusaStatusEPrioridadeDesconhecidos() {
        assertThatThrownBy(() -> FiltroDashboard.de(null, null, "QUALQUER", null, null, null))
                .isInstanceOf(RequisicaoInvalidaException.class);
        assertThatThrownBy(() -> FiltroDashboard.de(null, null, null, null, null, "altissima"))
                .isInstanceOf(RequisicaoInvalidaException.class);
    }
}
