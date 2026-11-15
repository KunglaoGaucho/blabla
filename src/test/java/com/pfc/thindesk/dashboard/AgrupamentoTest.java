package com.pfc.thindesk.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.pfc.thindesk.api.RequisicaoInvalidaException;

class AgrupamentoTest {

    @Test
    void padraoEhMensalEAceitaMinusculas() {
        assertThat(Agrupamento.de(null)).isEqualTo(Agrupamento.MES);
        assertThat(Agrupamento.de("semana")).isEqualTo(Agrupamento.SEMANA);
    }

    @Test
    void recusaAgrupamentoInvalido() {
        assertThatThrownBy(() -> Agrupamento.de("ANO")).isInstanceOf(RequisicaoInvalidaException.class);
    }

    @Test
    void semanaComecaNaSegundaFeira() {
        // 2026-11-14 é um sábado
        assertThat(Agrupamento.SEMANA.inicioDoPeriodo(LocalDate.of(2026, 11, 14))).isEqualTo(LocalDate.of(2026, 11, 9));
    }

    @Test
    void geraTodosOsMesesDoIntervaloInclusiveVazios() {
        var meses = Agrupamento.MES.periodos(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 4, 2));
        assertThat(meses).containsExactly(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));
        assertThat(Agrupamento.MES.rotulo(meses.get(0))).isEqualTo("01/2026");
    }

    @Test
    void limitaQuantidadeDePontos() {
        assertThatThrownBy(() -> Agrupamento.DIA.periodos(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 1, 1)))
                .isInstanceOf(RequisicaoInvalidaException.class)
                .hasMessageContaining("SEMANA ou MES");
    }
}
