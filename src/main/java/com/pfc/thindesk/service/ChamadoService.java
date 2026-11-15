package com.pfc.thindesk.service;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.pfc.thindesk.api.RecursoNaoEncontradoException;
import com.pfc.thindesk.api.RequisicaoInvalidaException;
import com.pfc.thindesk.dominio.NormalizadorChamado;
import com.pfc.thindesk.dominio.Prioridade;
import com.pfc.thindesk.dominio.StatusChamado;
import com.pfc.thindesk.entity.Chamado;
import com.pfc.thindesk.repository.ChamadoRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository chamadoRepository;

    public ChamadoService(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    public Chamado criarChamado(Chamado chamado) {
        if (chamado == null || NormalizadorChamado.texto(chamado.getDescricao(), null) == null) {
            throw new RequisicaoInvalidaException("descricao", "A descrição do chamado é obrigatória.");
        }
        Instant agora = Instant.now();
        chamado.setId(null);
        chamado.setDescricao(NormalizadorChamado.texto(chamado.getDescricao(), null));
        chamado.setStatus(statusValido(chamado.getStatus(), StatusChamado.ABERTO).name());
        chamado.setPrioridade(prioridadeValida(chamado.getPrioridade(), Prioridade.MEDIA).name());
        chamado.setTipo(NormalizadorChamado.texto(chamado.getTipo(), NormalizadorChamado.NAO_INFORMADO));
        chamado.setTecnico(NormalizadorChamado.texto(chamado.getTecnico(), NormalizadorChamado.NAO_ATRIBUIDO));
        chamado.setUsuario(NormalizadorChamado.texto(chamado.getUsuario(), NormalizadorChamado.NAO_INFORMADO));
        chamado.setConsideracoesTecnicas(NormalizadorChamado.texto(chamado.getConsideracoesTecnicas(), null));
        if (chamado.getDataAbertura() == null) {
            chamado.setDataAbertura(agora);
        }
        chamado.setDataAtualizacao(agora);
        chamado.setDataConclusao(StatusChamado.CONCLUIDO.name().equals(chamado.getStatus()) ? agora : null);
        return chamadoRepository.save(chamado);
    }

    public List<Chamado> listarChamados() {
        return chamadoRepository.findAll(Sort.by(Sort.Direction.DESC, "dataAbertura"));
    }

    public Chamado buscar(String id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado com id: " + id));
    }

    /** Atualização parcial: só os campos enviados são alterados. */
    public Chamado atualizarChamado(String id, Chamado novo) {
        if (novo == null) {
            throw new RequisicaoInvalidaException(null, "Informe ao menos um campo para atualizar.");
        }
        Chamado chamado = buscar(id);
        Instant agora = Instant.now();

        if (novo.getDescricao() != null) {
            String descricao = NormalizadorChamado.texto(novo.getDescricao(), null);
            if (descricao == null) {
                throw new RequisicaoInvalidaException("descricao", "A descrição do chamado não pode ficar vazia.");
            }
            chamado.setDescricao(descricao);
        }
        if (novo.getStatus() != null) {
            StatusChamado status = statusValido(novo.getStatus(), null);
            boolean concluindoAgora = status == StatusChamado.CONCLUIDO && !status.name().equals(chamado.getStatus());
            chamado.setStatus(status.name());
            if (concluindoAgora) {
                chamado.setDataConclusao(agora);
            } else if (status != StatusChamado.CONCLUIDO) {
                chamado.setDataConclusao(null);
            }
        }
        if (novo.getPrioridade() != null) {
            chamado.setPrioridade(prioridadeValida(novo.getPrioridade(), null).name());
        }
        if (novo.getTipo() != null) {
            chamado.setTipo(NormalizadorChamado.texto(novo.getTipo(), NormalizadorChamado.NAO_INFORMADO));
        }
        if (novo.getTecnico() != null) {
            chamado.setTecnico(NormalizadorChamado.texto(novo.getTecnico(), NormalizadorChamado.NAO_ATRIBUIDO));
        }
        if (novo.getUsuario() != null) {
            chamado.setUsuario(NormalizadorChamado.texto(novo.getUsuario(), NormalizadorChamado.NAO_INFORMADO));
        }
        if (novo.getConsideracoesTecnicas() != null) {
            chamado.setConsideracoesTecnicas(NormalizadorChamado.texto(novo.getConsideracoesTecnicas(), null));
        }
        chamado.setDataAtualizacao(agora);
        return chamadoRepository.save(chamado);
    }

    private static StatusChamado statusValido(String valor, StatusChamado padrao) {
        if (valor == null || valor.isBlank()) {
            if (padrao != null) {
                return padrao;
            }
        }
        return NormalizadorChamado.status(valor).orElseThrow(() -> new RequisicaoInvalidaException("status",
                "Status inválido. Use ABERTO, EM_ANDAMENTO, PENDENTE, CONCLUIDO ou CANCELADO."));
    }

    private static Prioridade prioridadeValida(String valor, Prioridade padrao) {
        if (valor == null || valor.isBlank()) {
            if (padrao != null) {
                return padrao;
            }
        }
        return NormalizadorChamado.prioridade(valor).orElseThrow(() -> new RequisicaoInvalidaException("prioridade",
                "Prioridade inválida. Use BAIXA, MEDIA, ALTA ou CRITICA."));
    }
}
