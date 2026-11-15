package com.pfc.thindesk.dominio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Padroniza os valores textuais dos chamados (RF07). Dados antigos foram gravados
 * com grafias diferentes ("Aberto", "em andamento", "Concluído"), o que quebraria
 * as contagens do dashboard. Classe sem dependências, testada isoladamente.
 */
public final class NormalizadorChamado {

    public static final String NAO_INFORMADO = "Não informado";
    public static final String NAO_ATRIBUIDO = "Não atribuído";

    private static final Map<String, StatusChamado> SINONIMOS_STATUS = Map.ofEntries(
            Map.entry("ABERTO", StatusChamado.ABERTO),
            Map.entry("NOVO", StatusChamado.ABERTO),
            Map.entry("EM_ANDAMENTO", StatusChamado.EM_ANDAMENTO),
            Map.entry("ANDAMENTO", StatusChamado.EM_ANDAMENTO),
            Map.entry("EM_ATENDIMENTO", StatusChamado.EM_ANDAMENTO),
            Map.entry("PENDENTE", StatusChamado.PENDENTE),
            Map.entry("AGUARDANDO", StatusChamado.PENDENTE),
            Map.entry("CONCLUIDO", StatusChamado.CONCLUIDO),
            Map.entry("FECHADO", StatusChamado.CONCLUIDO),
            Map.entry("RESOLVIDO", StatusChamado.CONCLUIDO),
            Map.entry("FINALIZADO", StatusChamado.CONCLUIDO),
            Map.entry("CANCELADO", StatusChamado.CANCELADO));

    private static final Map<String, Prioridade> SINONIMOS_PRIORIDADE = Map.of(
            "BAIXA", Prioridade.BAIXA,
            "MEDIA", Prioridade.MEDIA,
            "NORMAL", Prioridade.MEDIA,
            "ALTA", Prioridade.ALTA,
            "CRITICA", Prioridade.CRITICA,
            "URGENTE", Prioridade.CRITICA);

    private NormalizadorChamado() {
    }

    /** "Em andamento", "em_andamento", "EM ANDAMENTO" → EM_ANDAMENTO. Vazio se não reconhecido. */
    public static Optional<StatusChamado> status(String valor) {
        return Optional.ofNullable(SINONIMOS_STATUS.get(chave(valor)));
    }

    public static Optional<Prioridade> prioridade(String valor) {
        return Optional.ofNullable(SINONIMOS_PRIORIDADE.get(chave(valor)));
    }

    /** Remove espaços extras; texto vazio vira o valor padrão. */
    public static String texto(String valor, String padrao) {
        if (valor == null) {
            return padrao;
        }
        String limpo = valor.trim().replaceAll("\\s+", " ");
        return limpo.isEmpty() ? padrao : limpo;
    }

    /** Maiúsculas, sem acentos e com "_" no lugar de espaços e hífens. */
    static String chave(String valor) {
        if (valor == null) {
            return "";
        }
        String semAcento = Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
    }
}
