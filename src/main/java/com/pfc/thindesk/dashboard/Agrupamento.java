package com.pfc.thindesk.dashboard;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.pfc.thindesk.api.RequisicaoInvalidaException;

/** Granularidade do gráfico de chamados por período (RF02). */
public enum Agrupamento {

    DIA, SEMANA, MES;

    /** Limite de pontos por gráfico, para evitar respostas enormes. */
    public static final int MAX_PERIODOS = 400;

    private static final DateTimeFormatter DIA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter SEMANA_FMT = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter MES_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    public static Agrupamento de(String valor) {
        if (valor == null || valor.isBlank()) {
            return MES;
        }
        try {
            return valueOf(valor.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new RequisicaoInvalidaException("agrupamento", "Agrupamento inválido. Use DIA, SEMANA ou MES.");
        }
    }

    /** Primeiro dia do período que contém a data (semanas começam na segunda-feira). */
    public LocalDate inicioDoPeriodo(LocalDate data) {
        return switch (this) {
            case DIA -> data;
            case SEMANA -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case MES -> data.withDayOfMonth(1);
        };
    }

    public LocalDate proximo(LocalDate inicioPeriodo) {
        return switch (this) {
            case DIA -> inicioPeriodo.plusDays(1);
            case SEMANA -> inicioPeriodo.plusWeeks(1);
            case MES -> inicioPeriodo.plusMonths(1);
        };
    }

    public String rotulo(LocalDate inicioPeriodo) {
        return switch (this) {
            case DIA -> DIA_FMT.format(inicioPeriodo);
            case SEMANA -> "Sem. " + SEMANA_FMT.format(inicioPeriodo);
            case MES -> MES_FMT.format(inicioPeriodo);
        };
    }

    /** Todos os períodos entre as datas, inclusive os sem chamados. */
    public List<LocalDate> periodos(LocalDate inicio, LocalDate fim) {
        List<LocalDate> lista = new ArrayList<>();
        for (LocalDate p = inicioDoPeriodo(inicio); !p.isAfter(fim); p = proximo(p)) {
            lista.add(p);
            if (lista.size() > MAX_PERIODOS) {
                throw new RequisicaoInvalidaException("agrupamento",
                        "Intervalo longo demais para o agrupamento " + name() + ". Reduza o período ou use SEMANA ou MES.");
            }
        }
        return lista;
    }
}
