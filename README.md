# Thindesk - Dashboard Administrativo

Sistema de chamados técnicos **Thindesk** com um **dashboard administrativo** que consome uma API REST própria,
integrada ao **MongoDB Atlas**.

Autor: **André Novaes da Silva Junior**
Documentação completa (requisitos, endpoints e testes): [`docs/documentacao.pdf`](docs/documentacao.pdf)

## Tecnologias

Java 21 · Spring Boot 3.4 (Web, Data MongoDB, Security, Thymeleaf) · MongoDB Atlas · Bootstrap 5 · Chart.js 4 · Swagger UI (springdoc)

## Funcionalidades do dashboard

| Requisito | Onde |
|---|---|
| RF01 Indicadores gerais | Cartões no topo (`GET /api/dashboard/indicadores`) |
| RF02 Chamados por período | Gráfico de barras por dia, semana ou mês (`GET /api/dashboard/periodo`) |
| RF03 Situação dos chamados | Gráfico de rosca e tabela com considerações técnicas (`/status`, `/chamados`) |
| RF04 Tipos mais atendidos | Ranking em barras (`GET /api/dashboard/tipos`) e desempenho por técnico (`/tecnicos`) |
| RF05 Filtros | Período, status, tipo, técnico e prioridade, aplicados a todos os endpoints |
| RF06 Integração com a API | Toda a tela é carregada por `fetch` na API |
| RF07 Atualização e tratamento | Atualização automática a cada 60 s e padronização dos dados (`POST /api/chamados/tratamento`) |

## Como executar

Pré-requisitos: **JDK 21** e uma conta no **MongoDB Atlas**.

1. No Atlas, use um cluster existente ou crie um gratuito (M0). Em *Network Access*, libere o seu IP.
   Em *Connect › Drivers*, copie a string `mongodb+srv://...`.
2. Configure o ambiente:
   ```bash
   cp .env.example .env
   # edite o .env e preencha MONGODB_URI
   ```
3. Execute:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Acesse <http://localhost:8080>, entre com `admin` / `thindesk` e clique em **Dashboard** no menu.

Na primeira execução a aplicação:
- padroniza os chamados já existentes (status, tipo, datas);
- cria **160 chamados de exemplo** nos últimos seis meses, se a coleção tiver menos de 20 registros
  (desative com `THINDESK_DADOS_EXEMPLO=false`).

O banco usado é o `thindesk` (altere com `MONGODB_DATABASE`), na coleção `chamados`.

## API

Todas as rotas exigem login. Para testar pelo navegador, entre no sistema e abra o **Swagger UI** em
<http://localhost:8080/swagger-ui.html> (também no menu, em **API (Swagger)**): lá é possível executar
GET, POST e PUT com o botão *Try it out*. Por curl, use Basic Auth.

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/dashboard/indicadores` | Totais, taxa de resolução, tempo médio e críticos em aberto |
| GET | `/api/dashboard/periodo?agrupamento=MES` | Abertos e atendidos por período (`DIA`, `SEMANA`, `MES`) |
| GET | `/api/dashboard/status` | Quantidade e percentual por situação |
| GET | `/api/dashboard/tipos?limite=5` | Ranking dos tipos de chamado |
| GET | `/api/dashboard/tecnicos` | Atendimentos e tempo médio por técnico |
| GET | `/api/dashboard/chamados?pagina=0&tamanho=10` | Lista paginada com considerações técnicas |
| GET | `/api/dashboard/filtros` | Opções disponíveis para os filtros |
| GET | `/api/chamados` · `/api/chamados/{id}` | Consulta de chamados |
| POST | `/api/chamados` | Abertura de chamado |
| PUT | `/api/chamados/{id}` | Atualização parcial (status, técnico, considerações...) |
| POST | `/api/chamados/tratamento` | Padroniza registros antigos ou fora do padrão |

Filtros aceitos pelos endpoints do dashboard: `dataInicio`, `dataFim` (AAAA-MM-DD), `status`, `tipo`, `tecnico`, `prioridade`.

Exemplo:

```bash
curl -u admin:thindesk "http://localhost:8080/api/dashboard/indicadores?dataInicio=2026-10-01&dataFim=2026-10-31"
```

Erros seguem um formato único:

```json
{ "timestamp": "2026-11-14T22:10:00Z", "status": 400, "erro": "Bad Request",
  "mensagem": "Data inválida em 'dataInicio'. Use o formato AAAA-MM-DD.", "campo": "dataInicio",
  "caminho": "/api/dashboard/indicadores" }
```

## Testes

```bash
./mvnw test
```

## Fluxo de branches (Gitflow)

- `main`: versões entregues
- `develop`: integração
- `feature/dashboard-api`: implementação da API e do dashboard
