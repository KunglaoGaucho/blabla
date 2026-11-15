package com.pfc.thindesk.entity;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Chamado técnico. Os campos de data, prioridade e considerações técnicas
 * foram acrescentados para alimentar o dashboard administrativo.
 */
@Document(collection = "chamados")
public class Chamado {

    @Id
    private String id;
    private String descricao;
    @Indexed
    private String status;
    @Indexed
    private String tipo;
    private String prioridade;
    @Indexed
    private String tecnico;
    private String usuario;
    private String consideracoesTecnicas;
    @Indexed
    private Instant dataAbertura;
    private Instant dataAtualizacao;
    private Instant dataConclusao;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getPrioridade() { return prioridade; }
    public void setPrioridade(String prioridade) { this.prioridade = prioridade; }

    public String getTecnico() { return tecnico; }
    public void setTecnico(String tecnico) { this.tecnico = tecnico; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getConsideracoesTecnicas() { return consideracoesTecnicas; }
    public void setConsideracoesTecnicas(String consideracoesTecnicas) { this.consideracoesTecnicas = consideracoesTecnicas; }

    public Instant getDataAbertura() { return dataAbertura; }
    public void setDataAbertura(Instant dataAbertura) { this.dataAbertura = dataAbertura; }

    public Instant getDataAtualizacao() { return dataAtualizacao; }
    public void setDataAtualizacao(Instant dataAtualizacao) { this.dataAtualizacao = dataAtualizacao; }

    public Instant getDataConclusao() { return dataConclusao; }
    public void setDataConclusao(Instant dataConclusao) { this.dataConclusao = dataConclusao; }
}
