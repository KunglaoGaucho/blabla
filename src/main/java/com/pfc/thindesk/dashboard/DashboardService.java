package com.pfc.thindesk.dashboard;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.limit;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.match;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.sort;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import com.pfc.thindesk.api.RequisicaoInvalidaException;
import com.pfc.thindesk.dashboard.Respostas.ChamadoResumo;
import com.pfc.thindesk.dashboard.Respostas.Indicadores;
import com.pfc.thindesk.dashboard.Respostas.Opcao;
import com.pfc.thindesk.dashboard.Respostas.OpcoesFiltro;
import com.pfc.thindesk.dashboard.Respostas.Pagina;
import com.pfc.thindesk.dashboard.Respostas.PontoPeriodo;
import com.pfc.thindesk.dashboard.Respostas.RankingTecnico;
import com.pfc.thindesk.dashboard.Respostas.RankingTipo;
import com.pfc.thindesk.dashboard.Respostas.SeriePeriodo;
import com.pfc.thindesk.dashboard.Respostas.SituacaoStatus;
import com.pfc.thindesk.dominio.NormalizadorChamado;
import com.pfc.thindesk.dominio.Prioridade;
import com.pfc.thindesk.dominio.StatusChamado;
import com.pfc.thindesk.entity.Chamado;

/**
 * Regras de cálculo do dashboard. As filtragens e contagens simples são feitas
 * no próprio MongoDB (consultas e agregações); métricas que dependem de datas,
 * como tempo de resolução, são calculadas em Java sobre o resultado filtrado.
 */
@Service
public class DashboardService {

    private static final String DATA_ABERTURA = "dataAbertura";
    private static final String DATA_CONCLUSAO = "dataConclusao";

    private final MongoTemplate mongo;
    private final ZoneId zona;

    public DashboardService(MongoTemplate mongo, @Value("${thindesk.fuso-horario:America/Sao_Paulo}") String fuso) {
        this.mongo = mongo;
        this.zona = ZoneId.of(fuso);
    }

    // ------------------------------------------------------------------ RF01

    public Indicadores indicadores(FiltroDashboard filtro) {
        List<Chamado> chamados = buscar(filtro);
        Map<String, Long> porStatus = chamados.stream()
                .collect(Collectors.groupingBy(c -> Objects.toString(c.getStatus(), ""), Collectors.counting()));

        long total = chamados.size();
        long abertos = porStatus.getOrDefault(StatusChamado.ABERTO.name(), 0L);
        long emAndamento = porStatus.getOrDefault(StatusChamado.EM_ANDAMENTO.name(), 0L);
        long pendentes = porStatus.getOrDefault(StatusChamado.PENDENTE.name(), 0L);
        long concluidos = porStatus.getOrDefault(StatusChamado.CONCLUIDO.name(), 0L);
        long cancelados = porStatus.getOrDefault(StatusChamado.CANCELADO.name(), 0L);
        long validos = total - cancelados;
        long criticos = chamados.stream()
                .filter(c -> StatusChamado.emAberto().contains(c.getStatus()))
                .filter(c -> Prioridade.CRITICA.name().equals(c.getPrioridade()))
                .count();

        return new Indicadores(total, abertos, emAndamento, pendentes, concluidos, cancelados,
                abertos + emAndamento + pendentes,
                validos == 0 ? 0.0 : arredondar(concluidos * 100.0 / validos),
                tempoMedioHoras(chamados), criticos, Instant.now());
    }

    // ------------------------------------------------------------------ RF02

    public SeriePeriodo periodo(FiltroDashboard filtro, Agrupamento agrupamento) {
        LocalDate inicio = filtro.dataInicio();
        LocalDate fim = filtro.dataFim();
        if (inicio == null) {
            inicio = dataExtrema(filtro, Sort.Direction.ASC);
        }
        if (fim == null) {
            fim = LocalDate.now(zona);
        }
        if (inicio == null || inicio.isAfter(fim)) {
            return new SeriePeriodo(agrupamento, inicio, fim, List.of());
        }

        FiltroDashboard noIntervalo = filtro.comPeriodo(inicio, fim);
        Map<LocalDate, long[]> contagem = new LinkedHashMap<>();
        for (LocalDate p : agrupamento.periodos(inicio, fim)) {
            contagem.put(p, new long[2]);
        }

        // Abertos no período, pela data de abertura
        for (Chamado c : buscar(noIntervalo)) {
            long[] valores = contagem.get(agrupamento.inicioDoPeriodo(local(c.getDataAbertura())));
            if (valores != null) {
                valores[0]++;
            }
        }

        // Atendidos (concluídos) no período, pela data de conclusão
        if (noIntervalo.status() == null || StatusChamado.CONCLUIDO.name().equals(noIntervalo.status())) {
            Criteria criterio = criterio(noIntervalo, DATA_CONCLUSAO);
            if (noIntervalo.status() == null) {
                criterio = new Criteria().andOperator(criterio, Criteria.where("status").is(StatusChamado.CONCLUIDO.name()));
            }
            for (Chamado c : mongo.find(new Query(criterio), Chamado.class)) {
                long[] valores = contagem.get(agrupamento.inicioDoPeriodo(local(c.getDataConclusao())));
                if (valores != null) {
                    valores[1]++;
                }
            }
        }

        List<PontoPeriodo> pontos = new ArrayList<>();
        contagem.forEach((p, v) -> pontos.add(new PontoPeriodo(agrupamento.rotulo(p), p, v[0], v[1])));
        return new SeriePeriodo(agrupamento, inicio, fim, pontos);
    }

    // ------------------------------------------------------------------ RF03

    public List<SituacaoStatus> status(FiltroDashboard filtro) {
        Map<String, Long> contagem = contarPor("status", criterio(filtro, DATA_ABERTURA), 0);
        long total = contagem.values().stream().mapToLong(Long::longValue).sum();

        List<SituacaoStatus> lista = new ArrayList<>();
        for (StatusChamado s : StatusChamado.values()) {
            long q = contagem.getOrDefault(s.name(), 0L);
            lista.add(new SituacaoStatus(s.name(), s.rotulo(), q, percentual(q, total)));
        }
        // Valores fora do padrão ainda não tratados aparecem separados, para não sumirem do total
        contagem.forEach((chave, q) -> {
            if (Arrays.stream(StatusChamado.values()).noneMatch(s -> s.name().equals(chave))) {
                lista.add(new SituacaoStatus(chave, "Fora do padrão: " + chave, q, percentual(q, total)));
            }
        });
        return lista;
    }

    // ------------------------------------------------------------------ RF04

    public List<RankingTipo> tipos(FiltroDashboard filtro, int limite) {
        if (limite < 1 || limite > 50) {
            throw new RequisicaoInvalidaException("limite", "O limite deve estar entre 1 e 50.");
        }
        Criteria criterio = criterio(filtro, DATA_ABERTURA);
        long total = mongo.count(new Query(criterio), Chamado.class);
        Map<String, Long> contagem = contarPor("tipo", criterio, limite);

        List<RankingTipo> ranking = new ArrayList<>();
        int posicao = 1;
        for (Map.Entry<String, Long> e : contagem.entrySet()) {
            ranking.add(new RankingTipo(posicao++, e.getKey(), e.getValue(), percentual(e.getValue(), total)));
        }
        return ranking;
    }

    public List<RankingTecnico> tecnicos(FiltroDashboard filtro) {
        Map<String, List<Chamado>> porTecnico = buscar(filtro).stream()
                .collect(Collectors.groupingBy(c -> NormalizadorChamado.texto(c.getTecnico(), NormalizadorChamado.NAO_ATRIBUIDO)));

        List<RankingTecnico> lista = porTecnico.entrySet().stream()
                .map(e -> {
                    List<Chamado> doTecnico = e.getValue();
                    long concluidos = doTecnico.stream().filter(c -> StatusChamado.CONCLUIDO.name().equals(c.getStatus())).count();
                    long emAberto = doTecnico.stream().filter(c -> StatusChamado.emAberto().contains(c.getStatus())).count();
                    return new RankingTecnico(0, e.getKey(), doTecnico.size(), concluidos, emAberto, tempoMedioHoras(doTecnico));
                })
                .sorted(Comparator.comparingLong(RankingTecnico::atendidos).reversed().thenComparing(RankingTecnico::tecnico))
                .toList();

        List<RankingTecnico> numerada = new ArrayList<>();
        for (int i = 0; i < lista.size(); i++) {
            RankingTecnico r = lista.get(i);
            numerada.add(new RankingTecnico(i + 1, r.tecnico(), r.atendidos(), r.concluidos(), r.emAberto(), r.tempoMedioResolucaoHoras()));
        }
        return numerada;
    }

    // ------------------------------------------------------------------ RF03 / RF05

    public Pagina<ChamadoResumo> chamados(FiltroDashboard filtro, int pagina, int tamanho) {
        if (pagina < 0) {
            throw new RequisicaoInvalidaException("pagina", "A página deve ser maior ou igual a 0.");
        }
        if (tamanho < 1 || tamanho > 100) {
            throw new RequisicaoInvalidaException("tamanho", "O tamanho da página deve estar entre 1 e 100.");
        }
        Query query = new Query(criterio(filtro, DATA_ABERTURA));
        long total = mongo.count(query, Chamado.class);
        query.with(Sort.by(Sort.Direction.DESC, DATA_ABERTURA)).skip((long) pagina * tamanho).limit(tamanho);

        List<ChamadoResumo> itens = mongo.find(query, Chamado.class).stream().map(this::resumo).toList();
        int totalPaginas = (int) Math.ceil(total / (double) tamanho);
        return new Pagina<>(itens, pagina, tamanho, total, totalPaginas);
    }

    public OpcoesFiltro opcoesFiltro() {
        List<Opcao> status = Arrays.stream(StatusChamado.values()).map(s -> new Opcao(s.name(), s.rotulo())).toList();
        List<Opcao> prioridades = Arrays.stream(Prioridade.values()).map(p -> new Opcao(p.name(), p.rotulo())).toList();
        return new OpcoesFiltro(status, prioridades, distintos("tipo"), distintos("tecnico"),
                dataExtrema(FiltroDashboard.VAZIO, Sort.Direction.ASC),
                dataExtrema(FiltroDashboard.VAZIO, Sort.Direction.DESC));
    }

    // ------------------------------------------------------------------ apoio

    private List<Chamado> buscar(FiltroDashboard filtro) {
        return mongo.find(new Query(criterio(filtro, DATA_ABERTURA)), Chamado.class);
    }

    /** Monta o critério de consulta do MongoDB a partir dos filtros, aplicando o período no campo indicado. */
    Criteria criterio(FiltroDashboard filtro, String campoData) {
        List<Criteria> partes = new ArrayList<>();
        if (filtro.status() != null) {
            partes.add(Criteria.where("status").is(filtro.status()));
        }
        if (filtro.tipo() != null) {
            partes.add(Criteria.where("tipo").is(filtro.tipo()));
        }
        if (filtro.tecnico() != null) {
            partes.add(Criteria.where("tecnico").is(filtro.tecnico()));
        }
        if (filtro.prioridade() != null) {
            partes.add(Criteria.where("prioridade").is(filtro.prioridade()));
        }
        Instant inicio = filtro.inicio(zona);
        Instant fim = filtro.fimExclusivo(zona);
        if (inicio != null || fim != null) {
            Criteria periodo = Criteria.where(campoData);
            if (inicio != null) {
                periodo = periodo.gte(Date.from(inicio));
            }
            if (fim != null) {
                periodo = periodo.lt(Date.from(fim));
            }
            partes.add(periodo);
        }
        return partes.isEmpty() ? new Criteria() : new Criteria().andOperator(partes);
    }

    /** Contagem agrupada feita pelo MongoDB ($match → $group → $sort → $limit). */
    private Map<String, Long> contarPor(String campo, Criteria criterio, int limite) {
        List<AggregationOperation> etapas = new ArrayList<>();
        etapas.add(match(criterio));
        etapas.add(group(campo).count().as("quantidade"));
        etapas.add(sort(Sort.by(Sort.Direction.DESC, "quantidade").and(Sort.by(Sort.Direction.ASC, "_id"))));
        if (limite > 0) {
            etapas.add(limit(limite));
        }
        Map<String, Long> resultado = new LinkedHashMap<>();
        for (Document d : mongo.aggregate(newAggregation(etapas), Chamado.class, Document.class)) {
            Object chave = d.get("_id");
            Number quantidade = (Number) d.get("quantidade");
            resultado.merge(chave == null ? NormalizadorChamado.NAO_INFORMADO : chave.toString(),
                    quantidade == null ? 0L : quantidade.longValue(), Long::sum);
        }
        return resultado;
    }

    private LocalDate dataExtrema(FiltroDashboard filtro, Sort.Direction direcao) {
        Query query = new Query(new Criteria().andOperator(
                criterio(filtro, DATA_ABERTURA), Criteria.where(DATA_ABERTURA).ne(null)));
        query.with(Sort.by(direcao, DATA_ABERTURA)).limit(1);
        Chamado c = mongo.findOne(query, Chamado.class);
        return c == null ? null : local(c.getDataAbertura());
    }

    private List<String> distintos(String campo) {
        return mongo.findDistinct(new Query(), campo, Chamado.class, String.class).stream()
                .filter(Objects::nonNull)
                .filter(v -> !v.isBlank())
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private ChamadoResumo resumo(Chamado c) {
        String rotulo = NormalizadorChamado.status(c.getStatus()).map(StatusChamado::rotulo).orElse(c.getStatus());
        return new ChamadoResumo(c.getId(), c.getDescricao(), c.getStatus(), rotulo, c.getTipo(), c.getPrioridade(),
                c.getTecnico(), c.getUsuario(), c.getConsideracoesTecnicas(), c.getDataAbertura(), c.getDataConclusao(),
                horasResolucao(c));
    }

    private Double tempoMedioHoras(List<Chamado> chamados) {
        OptionalDouble media = chamados.stream()
                .filter(c -> StatusChamado.CONCLUIDO.name().equals(c.getStatus()))
                .map(this::horasResolucao)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average();
        return media.isPresent() ? arredondar(media.getAsDouble()) : null;
    }

    private Double horasResolucao(Chamado c) {
        if (c.getDataAbertura() == null || c.getDataConclusao() == null || c.getDataConclusao().isBefore(c.getDataAbertura())) {
            return null;
        }
        return arredondar(Duration.between(c.getDataAbertura(), c.getDataConclusao()).toMinutes() / 60.0);
    }

    private LocalDate local(Instant instante) {
        return instante == null ? null : instante.atZone(zona).toLocalDate();
    }

    private static double percentual(long parte, long total) {
        return total == 0 ? 0.0 : arredondar(parte * 100.0 / total);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
