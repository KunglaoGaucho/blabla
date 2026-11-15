package com.pfc.thindesk.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import com.pfc.thindesk.dominio.Prioridade;
import com.pfc.thindesk.dominio.StatusChamado;
import com.pfc.thindesk.entity.Chamado;

/**
 * Na inicialização: trata os dados existentes (RF07) e, se a coleção estiver
 * praticamente vazia, cria chamados de exemplo distribuídos nos últimos seis
 * meses para que os gráficos do dashboard tenham o que mostrar.
 * Pode ser desligado com THINDESK_DADOS_EXEMPLO=false.
 */
@Component
public class InicializacaoDados implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InicializacaoDados.class);
    private static final int MINIMO_PARA_NAO_GERAR = 20;

    private static final String[][] TIPOS = {
        {"Hardware", "Computador não liga", "Monitor sem imagem", "Teclado com teclas falhando", "Troca de mouse"},
        {"Software", "Erro ao abrir o sistema financeiro", "Instalação do pacote Office", "Atualização do antivírus", "Sistema lento ao gerar relatório"},
        {"Rede", "Sem acesso à internet", "Wi-Fi caindo na sala de reunião", "Ponto de rede sem sinal", "VPN não conecta"},
        {"Acesso e senha", "Senha do e-mail expirada", "Usuário bloqueado no sistema", "Liberação de acesso a pasta compartilhada"},
        {"Impressora", "Impressora não imprime", "Papel atolado", "Toner acabando", "Instalar impressora no computador"},
        {"E-mail", "Caixa de e-mail cheia", "E-mails não chegam", "Configurar e-mail no celular"},
        {"Manutenção", "Manutenção preventiva do servidor", "Limpeza de equipamento", "Troca de nobreak"}
    };
    private static final int[] PESO_TIPOS = {22, 20, 18, 14, 12, 8, 6};

    private static final String[] CONSIDERACOES = {
        "Problema resolvido remotamente.",
        "Equipamento substituído por outro do estoque.",
        "Cabo de rede trocado e conexão testada.",
        "Senha redefinida e usuário orientado.",
        "Driver reinstalado e teste de impressão realizado.",
        "Atualização aplicada e sistema reiniciado.",
        "Chamado encaminhado ao fornecedor; peça trocada na garantia."
    };

    private static final String[] TECNICOS = {"João", "Mariana", "Carlos", "Fernanda", "Rafael"};
    private static final String[] USUARIOS = {"Alberto", "Beatriz", "Camila", "Diego", "Eduarda", "Felipe", "Gabriela",
        "Henrique", "Isabela", "Lucas", "Marina", "Paulo", "Renata", "Sérgio", "Tatiane"};

    private final MongoTemplate mongo;
    private final TratamentoDadosService tratamento;
    private final boolean gerarExemplos;
    private final int quantidade;
    private final ZoneId zona;

    public InicializacaoDados(MongoTemplate mongo, TratamentoDadosService tratamento,
            @Value("${thindesk.dados-exemplo.habilitado:true}") boolean gerarExemplos,
            @Value("${thindesk.dados-exemplo.quantidade:160}") int quantidade,
            @Value("${thindesk.fuso-horario:America/Sao_Paulo}") String fuso) {
        this.mongo = mongo;
        this.tratamento = tratamento;
        this.gerarExemplos = gerarExemplos;
        this.quantidade = quantidade;
        this.zona = ZoneId.of(fuso);
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            tratamento.executar();
        } catch (RuntimeException e) {
            // Uma falha no tratamento não deve impedir o sistema de subir; pode ser repetido por POST /api/chamados/tratamento
            log.warn("Não foi possível tratar os dados na inicialização: {}", e.getMessage());
        }
        long existentes = mongo.count(new org.springframework.data.mongodb.core.query.Query(), Chamado.class);
        if (gerarExemplos && existentes < MINIMO_PARA_NAO_GERAR) {
            mongo.insertAll(gerar(quantidade));
            log.info("{} chamados de exemplo criados na coleção 'chamados'.", quantidade);
        }
    }

    List<Chamado> gerar(int total) {
        Random r = new Random(2026);
        LocalDate hoje = LocalDate.now(zona);
        Instant agora = Instant.now();
        List<Chamado> lista = new ArrayList<>();

        for (int i = 0; i < total; i++) {
            int diasAtras = r.nextInt(180);
            LocalDate dia = hoje.minusDays(diasAtras);
            Instant abertura = dia.atTime(LocalTime.of(8 + r.nextInt(10), r.nextInt(60))).atZone(zona).toInstant();
            if (abertura.isAfter(agora)) {
                abertura = agora.minus(Duration.ofMinutes(30 + r.nextInt(240)));
            }

            String[] tipo = TIPOS[sortear(r, PESO_TIPOS)];
            Prioridade prioridade = Prioridade.values()[sortear(r, new int[] {25, 45, 22, 8})];
            StatusChamado status = sortearStatus(r, diasAtras);

            Chamado c = new Chamado();
            c.setDescricao(tipo[1 + r.nextInt(tipo.length - 1)]);
            c.setTipo(tipo[0]);
            c.setPrioridade(prioridade.name());
            c.setStatus(status.name());
            c.setTecnico(TECNICOS[r.nextInt(TECNICOS.length)]);
            c.setUsuario(USUARIOS[r.nextInt(USUARIOS.length)]);
            c.setDataAbertura(abertura);
            c.setDataAtualizacao(abertura);

            if (status == StatusChamado.CONCLUIDO) {
                Instant conclusao = abertura.plus(Duration.ofMinutes(horasResolucao(r, prioridade) * 60L + r.nextInt(60)));
                if (conclusao.isAfter(agora)) {
                    conclusao = agora;
                }
                c.setDataConclusao(conclusao);
                c.setDataAtualizacao(conclusao);
                c.setConsideracoesTecnicas(CONSIDERACOES[r.nextInt(CONSIDERACOES.length)]);
            } else if (status == StatusChamado.PENDENTE) {
                c.setConsideracoesTecnicas("Aguardando retorno do usuário ou peça de reposição.");
            } else if (status == StatusChamado.CANCELADO) {
                c.setConsideracoesTecnicas("Cancelado a pedido do usuário.");
            }
            lista.add(c);
        }
        return lista;
    }

    /** Chamados antigos tendem a estar concluídos; os recentes, em aberto. */
    private static StatusChamado sortearStatus(Random r, int diasAtras) {
        if (diasAtras > 20) {
            return new StatusChamado[] {StatusChamado.CONCLUIDO, StatusChamado.CONCLUIDO, StatusChamado.CONCLUIDO,
                StatusChamado.CONCLUIDO, StatusChamado.CONCLUIDO, StatusChamado.CONCLUIDO, StatusChamado.CONCLUIDO,
                StatusChamado.CONCLUIDO, StatusChamado.CANCELADO, StatusChamado.PENDENTE}[r.nextInt(10)];
        }
        return StatusChamado.values()[sortear(r, new int[] {30, 25, 15, 25, 5})];
    }

    private static int horasResolucao(Random r, Prioridade p) {
        return switch (p) {
            case CRITICA -> 1 + r.nextInt(4);
            case ALTA -> 2 + r.nextInt(10);
            case MEDIA -> 4 + r.nextInt(30);
            case BAIXA -> 8 + r.nextInt(64);
        };
    }

    private static int sortear(Random r, int[] pesos) {
        int soma = 0;
        for (int p : pesos) {
            soma += p;
        }
        int valor = r.nextInt(soma);
        for (int i = 0; i < pesos.length; i++) {
            valor -= pesos[i];
            if (valor < 0) {
                return i;
            }
        }
        return pesos.length - 1;
    }
}
