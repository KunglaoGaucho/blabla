package com.pfc.thindesk.dashboard;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Formatos das respostas da API do dashboard. */
public final class Respostas {

    private Respostas() {
    }

    /** RF01 */
    public record Indicadores(
            long total,
            long abertos,
            long emAndamento,
            long pendentes,
            long concluidos,
            long cancelados,
            long emAberto,
            double taxaResolucao,
            Double tempoMedioResolucaoHoras,
            long criticosEmAberto,
            Instant atualizadoEm) {
    }

    /** RF02 */
    public record SeriePeriodo(Agrupamento agrupamento, LocalDate inicio, LocalDate fim, List<PontoPeriodo> pontos) {
    }

    public record PontoPeriodo(String periodo, LocalDate inicio, long abertos, long atendidos) {
    }

    /** RF03 */
    public record SituacaoStatus(String status, String rotulo, long quantidade, double percentual) {
    }

    /** RF04 */
    public record RankingTipo(int posicao, String tipo, long quantidade, double percentual) {
    }

    public record RankingTecnico(int posicao, String tecnico, long atendidos, long concluidos, long emAberto,
            Double tempoMedioResolucaoHoras) {
    }

    /** RF05 */
    public record OpcoesFiltro(List<Opcao> status, List<Opcao> prioridades, List<String> tipos, List<String> tecnicos,
            LocalDate primeiraData, LocalDate ultimaData) {
    }

    public record Opcao(String valor, String rotulo) {
    }

    public record ChamadoResumo(String id, String descricao, String status, String statusRotulo, String tipo,
            String prioridade, String tecnico, String usuario, String consideracoesTecnicas,
            Instant dataAbertura, Instant dataConclusao, Double horasResolucao) {
    }

    public record Pagina<T>(List<T> itens, int pagina, int tamanho, long totalItens, int totalPaginas) {
    }
}
