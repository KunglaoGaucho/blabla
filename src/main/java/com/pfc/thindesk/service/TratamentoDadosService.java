package com.pfc.thindesk.service;

import java.time.Instant;
import java.util.Objects;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.pfc.thindesk.dominio.NormalizadorChamado;
import com.pfc.thindesk.dominio.Prioridade;
import com.pfc.thindesk.dominio.StatusChamado;
import com.pfc.thindesk.entity.Chamado;

/**
 * Tratamento dos dados de chamados (RF07). Corrige registros antigos para que
 * entrem corretamente nas contagens do dashboard:
 * campo "Tipo" com maiúscula, status com grafias diferentes, campos vazios
 * e chamados sem data de abertura (recuperada do próprio ObjectId).
 */
@Service
public class TratamentoDadosService {

    private static final Logger log = LoggerFactory.getLogger(TratamentoDadosService.class);

    private static final String COLECAO = "chamados";

    private final MongoTemplate mongo;

    public TratamentoDadosService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public record ResultadoTratamento(long analisados, long camposRenomeados, long statusPadronizados,
            long datasPreenchidas, long camposPreenchidos, long alterados, Instant executadoEm) {
    }

    public ResultadoTratamento executar() {
        // Usa o nome da coleção (e não a classe) para o Spring não converter "Tipo" em "tipo"
        long renomeados = mongo.updateMulti(
                new Query(Criteria.where("Tipo").exists(true).and("tipo").exists(false)),
                new Update().rename("Tipo", "tipo"), COLECAO).getModifiedCount();

        long analisados = 0, status = 0, datas = 0, campos = 0, alterados = 0;
        for (Chamado c : mongo.findAll(Chamado.class)) {
            analisados++;
            boolean mudou = false;

            String novoStatus = NormalizadorChamado.status(c.getStatus()).orElse(StatusChamado.ABERTO).name();
            if (!novoStatus.equals(c.getStatus())) {
                c.setStatus(novoStatus);
                status++;
                mudou = true;
            }

            String novaPrioridade = NormalizadorChamado.prioridade(c.getPrioridade()).orElse(Prioridade.MEDIA).name();
            String novoTipo = NormalizadorChamado.texto(c.getTipo(), NormalizadorChamado.NAO_INFORMADO);
            String novoTecnico = NormalizadorChamado.texto(c.getTecnico(), NormalizadorChamado.NAO_ATRIBUIDO);
            String novoUsuario = NormalizadorChamado.texto(c.getUsuario(), NormalizadorChamado.NAO_INFORMADO);
            if (!novaPrioridade.equals(c.getPrioridade()) || !novoTipo.equals(c.getTipo())
                    || !novoTecnico.equals(c.getTecnico()) || !novoUsuario.equals(c.getUsuario())) {
                c.setPrioridade(novaPrioridade);
                c.setTipo(novoTipo);
                c.setTecnico(novoTecnico);
                c.setUsuario(novoUsuario);
                campos++;
                mudou = true;
            }

            if (c.getDataAbertura() == null) {
                c.setDataAbertura(c.getId() != null && ObjectId.isValid(c.getId())
                        ? new ObjectId(c.getId()).getDate().toInstant()
                        : Instant.now());
                datas++;
                mudou = true;
            }
            if (StatusChamado.CONCLUIDO.name().equals(c.getStatus()) && c.getDataConclusao() == null) {
                c.setDataConclusao(Objects.requireNonNullElse(c.getDataAtualizacao(), c.getDataAbertura()));
                datas++;
                mudou = true;
            }

            if (mudou) {
                c.setDataAtualizacao(Instant.now());
                mongo.save(c);
                alterados++;
            }
        }
        var resultado = new ResultadoTratamento(analisados, renomeados, status, datas, campos, alterados, Instant.now());
        log.info("Tratamento de dados concluído: {}", resultado);
        return resultado;
    }
}
