package com.pfc.thindesk.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import com.pfc.thindesk.api.RequisicaoInvalidaException;
import com.pfc.thindesk.dominio.NormalizadorChamado;

/**
 * Filtros aceitos por todos os endpoints do dashboard (RF05), já validados.
 * Campos nulos significam "sem filtro".
 */
public record FiltroDashboard(
        LocalDate dataInicio,
        LocalDate dataFim,
        String status,
        String tipo,
        String tecnico,
        String prioridade) {

    public static final FiltroDashboard VAZIO = new FiltroDashboard(null, null, null, null, null, null);

    /** Converte e valida os parâmetros recebidos como texto na URL. */
    public static FiltroDashboard de(String dataInicio, String dataFim, String status,
            String tipo, String tecnico, String prioridade) {
        LocalDate inicio = data("dataInicio", dataInicio);
        LocalDate fim = data("dataFim", dataFim);
        if (inicio != null && fim != null && inicio.isAfter(fim)) {
            throw new RequisicaoInvalidaException("dataInicio", "A data inicial não pode ser posterior à data final.");
        }
        String statusValido = vazio(status) ? null
                : NormalizadorChamado.status(status).map(Enum::name).orElseThrow(() -> new RequisicaoInvalidaException(
                        "status", "Status inválido. Use ABERTO, EM_ANDAMENTO, PENDENTE, CONCLUIDO ou CANCELADO."));
        String prioridadeValida = vazio(prioridade) ? null
                : NormalizadorChamado.prioridade(prioridade).map(Enum::name).orElseThrow(() -> new RequisicaoInvalidaException(
                        "prioridade", "Prioridade inválida. Use BAIXA, MEDIA, ALTA ou CRITICA."));
        return new FiltroDashboard(inicio, fim, statusValido, texto(tipo), texto(tecnico), prioridadeValida);
    }

    public FiltroDashboard comPeriodo(LocalDate novoInicio, LocalDate novoFim) {
        return new FiltroDashboard(novoInicio, novoFim, status, tipo, tecnico, prioridade);
    }

    /** Início do primeiro dia, no fuso informado. */
    public Instant inicio(ZoneId zona) {
        return dataInicio == null ? null : dataInicio.atStartOfDay(zona).toInstant();
    }

    /** Início do dia seguinte à data final (limite exclusivo). */
    public Instant fimExclusivo(ZoneId zona) {
        return dataFim == null ? null : dataFim.plusDays(1).atStartOfDay(zona).toInstant();
    }

    private static LocalDate data(String campo, String valor) {
        if (vazio(valor)) {
            return null;
        }
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new RequisicaoInvalidaException(campo, "Data inválida em '" + campo + "'. Use o formato AAAA-MM-DD.");
        }
    }

    private static String texto(String valor) {
        return vazio(valor) ? null : valor.trim();
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
