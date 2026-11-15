package com.pfc.thindesk.dashboard;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pfc.thindesk.dashboard.Respostas.ChamadoResumo;
import com.pfc.thindesk.dashboard.Respostas.Indicadores;
import com.pfc.thindesk.dashboard.Respostas.OpcoesFiltro;
import com.pfc.thindesk.dashboard.Respostas.Pagina;
import com.pfc.thindesk.dashboard.Respostas.RankingTecnico;
import com.pfc.thindesk.dashboard.Respostas.RankingTipo;
import com.pfc.thindesk.dashboard.Respostas.SeriePeriodo;
import com.pfc.thindesk.dashboard.Respostas.SituacaoStatus;

/**
 * Endpoints que alimentam o dashboard administrativo (RF06).
 * Todos aceitam os filtros dataInicio, dataFim, status, tipo, tecnico e prioridade (RF05).
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    @GetMapping("/indicadores")
    public Indicadores indicadores(@ParameterObject ParametrosFiltro filtros) {
        return service.indicadores(filtros.validar());
    }

    @GetMapping("/periodo")
    public SeriePeriodo periodo(@ParameterObject ParametrosFiltro filtros, @RequestParam(required = false) String agrupamento) {
        return service.periodo(filtros.validar(), Agrupamento.de(agrupamento));
    }

    @GetMapping("/status")
    public List<SituacaoStatus> status(@ParameterObject ParametrosFiltro filtros) {
        return service.status(filtros.validar());
    }

    @GetMapping("/tipos")
    public List<RankingTipo> tipos(@ParameterObject ParametrosFiltro filtros, @RequestParam(defaultValue = "5") int limite) {
        return service.tipos(filtros.validar(), limite);
    }

    @GetMapping("/tecnicos")
    public List<RankingTecnico> tecnicos(@ParameterObject ParametrosFiltro filtros) {
        return service.tecnicos(filtros.validar());
    }

    @GetMapping("/chamados")
    public Pagina<ChamadoResumo> chamados(@ParameterObject ParametrosFiltro filtros,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int tamanho) {
        return service.chamados(filtros.validar(), pagina, tamanho);
    }

    @GetMapping("/filtros")
    public OpcoesFiltro filtros() {
        return service.opcoesFiltro();
    }
}
