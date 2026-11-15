package com.pfc.thindesk.dashboard;

/** Parâmetros de filtro como chegam na query string (ex.: ?dataInicio=2026-01-01&status=ABERTO). */
public record ParametrosFiltro(
        String dataInicio,
        String dataFim,
        String status,
        String tipo,
        String tecnico,
        String prioridade) {

    public FiltroDashboard validar() {
        return FiltroDashboard.de(dataInicio, dataFim, status, tipo, tecnico, prioridade);
    }
}
