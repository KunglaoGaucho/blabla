/*
 * Dashboard administrativo do Thindesk.
 * Todos os dados vêm da API /api/dashboard (RF06). Os filtros do formulário
 * são enviados como query string em todas as chamadas (RF05).
 */
(function () {
    'use strict';

    const API = '/api/dashboard';
    const INTERVALO_MS = 60000;
    const CORES_STATUS = {
        ABERTO: '#0d6efd', EM_ANDAMENTO: '#fd7e14', PENDENTE: '#ffc107',
        CONCLUIDO: '#198754', CANCELADO: '#6c757d'
    };
    const ROTULO_PRIORIDADE = { BAIXA: 'Baixa', MEDIA: 'Média', ALTA: 'Alta', CRITICA: 'Crítica' };

    const form = document.getElementById('filtros');
    const alerta = document.getElementById('alerta');
    const graficos = {};
    let pagina = 0;
    let totalPaginas = 0;
    let temporizador = null;

    // ---------------------------------------------------------------- API

    function parametros(extras) {
        const p = new URLSearchParams();
        new FormData(form).forEach((valor, chave) => { if (valor) p.append(chave, valor); });
        Object.entries(extras || {}).forEach(([chave, valor]) => p.set(chave, valor));
        return p.toString();
    }

    async function obter(caminho, extras) {
        const resposta = await fetch(`${API}${caminho}?${parametros(extras)}`, {
            headers: { Accept: 'application/json' },
            credentials: 'same-origin'
        });
        if (resposta.status === 401) {
            window.location.href = '/login';
            throw new Error('Sessão expirada.');
        }
        if (!resposta.ok) {
            let mensagem = `Erro ${resposta.status} ao consultar ${caminho}.`;
            try { mensagem = (await resposta.json()).mensagem || mensagem; } catch (e) { /* corpo vazio */ }
            throw new Error(mensagem);
        }
        return resposta.json();
    }

    // ---------------------------------------------------------------- carga

    async function carregarOpcoes() {
        const opcoes = await obter('/filtros');
        preencher('status', opcoes.status.map(o => [o.valor, o.rotulo]));
        preencher('prioridade', opcoes.prioridades.map(o => [o.valor, o.rotulo]));
        preencher('tipo', opcoes.tipos.map(t => [t, t]));
        preencher('tecnico', opcoes.tecnicos.map(t => [t, t]));
    }

    function preencher(id, itens) {
        const select = document.getElementById(id);
        const atual = select.value;
        select.length = 1; // mantém "Todos"
        itens.forEach(([valor, rotulo]) => select.add(new Option(rotulo, valor)));
        select.value = atual;
    }

    async function atualizar() {
        esconderAlerta();
        try {
            const agrupamento = document.getElementById('agrupamento').value;
            const [indicadores, periodo, status, tipos, tecnicos] = await Promise.all([
                obter('/indicadores'),
                obter('/periodo', { agrupamento }),
                obter('/status'),
                obter('/tipos', { limite: 7 }),
                obter('/tecnicos')
            ]);
            mostrarIndicadores(indicadores);
            mostrarPeriodo(periodo);
            mostrarStatus(status);
            mostrarTipos(tipos);
            mostrarTecnicos(tecnicos);
            await carregarChamados();
            document.getElementById('ultimaAtualizacao').textContent =
                'Atualizado às ' + new Date().toLocaleTimeString('pt-BR');
        } catch (erro) {
            mostrarAlerta(erro.message);
        }
    }

    async function carregarChamados() {
        const dados = await obter('/chamados', { pagina, tamanho: 10 });
        totalPaginas = dados.totalPaginas;
        const corpo = document.getElementById('tabelaChamados');
        corpo.innerHTML = '';
        if (dados.itens.length === 0) {
            corpo.innerHTML = '<tr><td colspan="7" class="text-center text-muted">Nenhum chamado encontrado.</td></tr>';
        }
        dados.itens.forEach(c => {
            const linha = corpo.insertRow();
            celula(linha, formatarData(c.dataAbertura));
            celula(linha, c.descricao);
            celula(linha, c.tipo);
            celula(linha, ROTULO_PRIORIDADE[c.prioridade] || c.prioridade);
            celula(linha, c.tecnico);
            const badge = document.createElement('span');
            badge.className = 'badge badge-status';
            badge.style.backgroundColor = CORES_STATUS[c.status] || '#adb5bd';
            badge.textContent = c.statusRotulo;
            linha.insertCell().appendChild(badge);
            celula(linha, c.consideracoesTecnicas || '-').className = 'consideracoes';
        });
        document.getElementById('infoPagina').textContent = dados.totalItens === 0 ? ''
            : `Página ${dados.pagina + 1} de ${dados.totalPaginas} (${dados.totalItens} chamados)`;
        document.getElementById('paginaAnterior').disabled = pagina === 0;
        document.getElementById('proximaPagina').disabled = pagina + 1 >= totalPaginas;
    }

    // ---------------------------------------------------------------- exibição

    function mostrarIndicadores(i) {
        const valores = {
            total: i.total,
            emAberto: i.emAberto,
            concluidos: i.concluidos,
            taxaResolucao: `${i.taxaResolucao.toLocaleString('pt-BR')}%`,
            tempoMedioResolucaoHoras: i.tempoMedioResolucaoHoras == null ? '-'
                : `${i.tempoMedioResolucaoHoras.toLocaleString('pt-BR')} h`,
            criticosEmAberto: i.criticosEmAberto
        };
        document.querySelectorAll('[data-kpi]').forEach(el => { el.textContent = valores[el.dataset.kpi]; });
    }

    function mostrarPeriodo(serie) {
        desenhar('graficoPeriodo', {
            type: 'bar',
            data: {
                labels: serie.pontos.map(p => p.periodo),
                datasets: [
                    { label: 'Abertos', data: serie.pontos.map(p => p.abertos), backgroundColor: '#0d6efd' },
                    { label: 'Atendidos (concluídos)', data: serie.pontos.map(p => p.atendidos), backgroundColor: '#198754' }
                ]
            },
            options: opcoesBase({ scales: { y: { beginAtZero: true, ticks: { precision: 0 } } } })
        });
    }

    function mostrarStatus(lista) {
        desenhar('graficoStatus', {
            type: 'doughnut',
            data: {
                labels: lista.map(s => `${s.rotulo} (${s.quantidade})`),
                datasets: [{ data: lista.map(s => s.quantidade), backgroundColor: lista.map(s => CORES_STATUS[s.status] || '#adb5bd') }]
            },
            options: opcoesBase({ plugins: { legend: { position: 'bottom' } } })
        });
    }

    function mostrarTipos(ranking) {
        desenhar('graficoTipos', {
            type: 'bar',
            data: {
                labels: ranking.map(t => `${t.posicao}º ${t.tipo}`),
                datasets: [{ label: 'Chamados', data: ranking.map(t => t.quantidade), backgroundColor: '#6f42c1' }]
            },
            options: opcoesBase({
                indexAxis: 'y',
                plugins: {
                    legend: { display: false },
                    tooltip: { callbacks: { afterLabel: ctx => `${ranking[ctx.dataIndex].percentual}% do total` } }
                },
                scales: { x: { beginAtZero: true, ticks: { precision: 0 } } }
            })
        });
    }

    function mostrarTecnicos(lista) {
        const corpo = document.getElementById('tabelaTecnicos');
        corpo.innerHTML = '';
        if (lista.length === 0) {
            corpo.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Sem dados.</td></tr>';
        }
        lista.forEach(t => {
            const linha = corpo.insertRow();
            celula(linha, t.posicao);
            celula(linha, t.tecnico);
            celula(linha, t.atendidos).className = 'text-end';
            celula(linha, t.concluidos).className = 'text-end';
            celula(linha, t.emAberto).className = 'text-end';
            celula(linha, t.tempoMedioResolucaoHoras == null ? '-' : `${t.tempoMedioResolucaoHoras} h`).className = 'text-end';
        });
    }

    function desenhar(id, configuracao) {
        if (graficos[id]) {
            graficos[id].data = configuracao.data;
            graficos[id].update();
        } else {
            graficos[id] = new Chart(document.getElementById(id), configuracao);
        }
    }

    function opcoesBase(extras) {
        return Object.assign({ responsive: true, maintainAspectRatio: false, animation: { duration: 300 } }, extras);
    }

    function celula(linha, texto) {
        const td = linha.insertCell();
        td.textContent = texto == null ? '-' : texto;
        return td;
    }

    function formatarData(iso) {
        return iso ? new Date(iso).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' }) : '-';
    }

    function mostrarAlerta(mensagem) {
        alerta.textContent = mensagem;
        alerta.classList.remove('d-none');
    }

    function esconderAlerta() {
        alerta.classList.add('d-none');
    }

    // ---------------------------------------------------------------- RF07: atualização automática

    function programarAtualizacao() {
        clearInterval(temporizador);
        if (document.getElementById('autoAtualizar').checked) {
            temporizador = setInterval(atualizar, INTERVALO_MS);
        }
    }

    // ---------------------------------------------------------------- eventos

    form.addEventListener('submit', e => { e.preventDefault(); pagina = 0; atualizar(); });
    document.getElementById('btnLimpar').addEventListener('click', () => { form.reset(); pagina = 0; atualizar(); });
    document.getElementById('btnAtualizar').addEventListener('click', atualizar);
    document.getElementById('agrupamento').addEventListener('change', atualizar);
    document.getElementById('autoAtualizar').addEventListener('change', programarAtualizacao);
    document.getElementById('paginaAnterior').addEventListener('click', () => { pagina--; carregarChamados().catch(e => mostrarAlerta(e.message)); });
    document.getElementById('proximaPagina').addEventListener('click', () => { pagina++; carregarChamados().catch(e => mostrarAlerta(e.message)); });

    carregarOpcoes().catch(e => mostrarAlerta(e.message)).finally(() => { atualizar(); programarAtualizacao(); });
})();
