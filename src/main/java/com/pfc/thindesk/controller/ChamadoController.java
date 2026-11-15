package com.pfc.thindesk.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.service.ChamadoService;
import com.pfc.thindesk.service.TratamentoDadosService;
import com.pfc.thindesk.service.TratamentoDadosService.ResultadoTratamento;

@RestController
@RequestMapping("/api/chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;
    private final TratamentoDadosService tratamentoDadosService;

    public ChamadoController(ChamadoService chamadoService, TratamentoDadosService tratamentoDadosService) {
        this.chamadoService = chamadoService;
        this.tratamentoDadosService = tratamentoDadosService;
    }

    @PostMapping
    public ResponseEntity<Chamado> criarChamado(@RequestBody Chamado chamado) {
        Chamado criado = chamadoService.criarChamado(chamado);
        return ResponseEntity.created(URI.create("/api/chamados/" + criado.getId())).body(criado);
    }

    @GetMapping
    public List<Chamado> listarChamados() {
        return chamadoService.listarChamados();
    }

    @GetMapping("/{id}")
    public Chamado buscarChamado(@PathVariable String id) {
        return chamadoService.buscar(id);
    }

    @PutMapping("/{id}")
    public Chamado atualizarChamado(@PathVariable String id, @RequestBody Chamado chamadoAtualizado) {
        return chamadoService.atualizarChamado(id, chamadoAtualizado);
    }

    /** RF07: padroniza registros antigos ou gravados fora do padrão. */
    @PostMapping("/tratamento")
    public ResultadoTratamento tratarDados() {
        return tratamentoDadosService.executar();
    }
}
