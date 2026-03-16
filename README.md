<div align="center">

```
  ██████╗  █████╗ ███╗   ███╗██╗███████╗██╗   ██╗ █████╗ ██████╗ ██╗
 ██╔════╝ ██╔══██╗████╗ ████║██║██╔════╝╚██╗ ██╔╝██╔══██╗██╔══██╗██║
 ██║  ███╗███████║██╔████╔██║██║█████╗   ╚████╔╝ ███████║██████╔╝██║
 ██║   ██║██╔══██║██║╚██╔╝██║██║██╔══╝    ╚██╔╝  ██╔══██║██╔═══╝ ██║
 ╚██████╔╝██║  ██║██║ ╚═╝ ██║██║██║        ██║   ██║  ██║██║     ██║
  ╚═════╝ ╚═╝  ╚═╝╚═╝     ╚═╝╚═╝╚═╝        ╚═╝   ╚═╝  ╚═╝╚═╝     ╚═╝
```

**Gamification as a Service (GaaS)** — REST API multi-tenant para integrar XP, níveis, conquistas, streaks e rankings em qualquer aplicação.

---

[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/17/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=for-the-badge&logo=apache-maven&logoColor=white)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](./LICENSE)


</div>

---

## O que é a GamifyAPI?

A GamifyAPI é uma plataforma de gamificação pronta para ser plugada em apps, plataformas educacionais, sistemas de fidelidade ou qualquer produto que queira engajar usuários via mecânicas de jogo.

Cada empresa que se cadastra recebe um **tenant isolado** com sua própria configuração de ações, conquistas, níveis e webhooks. A integração do produto final é feita por **API Key**, sem expor credenciais de admin.

---

## Funcionalidades

| Recurso | Descrição |
|---|---|
| 🏅 **XP e Progressão** | Ações concedem XP configurável; motor de níveis customizável por tenant |
| 🏆 **Conquistas (Badges)** | 5 tipos de critério: `ACTION_COUNT`, `STREAK`, `LEVEL_REACHED`, `XP_TOTAL`, `MULTI_ACTION` |
| 🔥 **Streaks** | Contagem de dias consecutivos com reset automático e rastreamento de recorde |
| 📊 **Leaderboard** | Rankings global, semanal e mensal com paginação |
| 🔔 **Webhooks Assíncronos** | Notificações para `LEVEL_UP`, `ACHIEVEMENT_UNLOCKED`, `STREAK_MILESTONE`, `RANK_CHANGED` com 3 tentativas e backoff exponencial |
| 🔐 **Dual Auth** | JWT para admins do tenant + API Key para integração dos produtos |
| 🏢 **Multi-tenant** | Isolamento completo por ThreadLocal; dados nunca se cruzam entre tenants |
| 📈 **Dashboard** | Métricas agregadas e gráfico de ações por dia |

---

## Stack

<div align="center">

| ![Java](https://img.shields.io/badge/Java_17-ED8B00?logo=openjdk&logoColor=white&style=for-the-badge) | ![Spring Boot](https://img.shields.io/badge/Spring_Boot_3.2-6DB33F?logo=spring-boot&logoColor=white&style=for-the-badge) | ![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?logo=spring&logoColor=white&style=for-the-badge) | ![PostgreSQL](https://img.shields.io/badge/PostgreSQL_16-316192?logo=postgresql&logoColor=white&style=for-the-badge) | ![H2](https://img.shields.io/badge/H2_Database-0076BD?logo=h2&logoColor=white&style=for-the-badge) |
|:---:|:---:|:---:|:---:|:---:|
| Linguagem principal | Framework web | JWT + API Key auth | Banco de dados | Dev e testes (in-memory) |

| ![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?logo=docker&logoColor=white&style=for-the-badge) | ![Swagger](https://img.shields.io/badge/Swagger_UI-85EA2D?logo=swagger&logoColor=black&style=for-the-badge) | ![JUnit 5](https://img.shields.io/badge/JUnit_5-25A162?logo=junit5&logoColor=white&style=for-the-badge) | ![Mockito](https://img.shields.io/badge/Mockito-78A641?style=for-the-badge) | ![Maven](https://img.shields.io/badge/Maven_3.9-C71A36?logo=apache-maven&logoColor=white&style=for-the-badge) |
|:---:|:---:|:---:|:---:|:---:|
| Containerização | Documentação OpenAPI 3 | Testes unitários | Mocks e stubs | Build e dependências |

</div>

---

## Arquitetura

```
┌─────────────────────────────────────────────────────────────────┐
│                          GamifyAPI                              │
│                                                                 │
│   HTTP Request                                                  │
│       │                                                         │
│       ▼                                                         │
│  ┌─────────────────────────────────────┐                        │
│  │          Security Filters           │                        │
│  │  ApiKeyFilter → JwtFilter           │                        │
│  │  TenantContext (ThreadLocal)        │                        │
│  └──────────────────┬──────────────────┘                        │
│                     │                                           │
│                     ▼                                           │
│       Controller → Service → Repository → PostgreSQL            │
│                                                                 │
│  ┌─────────────────────────────────────┐                        │
│  │         GamificationService         │                        │
│  │       (orquestrador principal)      │                        │
│  │                                     │                        │
│  │  PlayerService  →  XpService        │                        │
│  │  LevelService   →  StreakService    │                        │
│  │  AchievementEngine (Strategy)       │                        │
│  │  RankingService →  WebhookService   │                        │
│  └─────────────────────────────────────┘                        │
│                                                                 │
│  ┌─────────────────────────────────────┐                        │
│  │         AchievementEngine           │                        │
│  │  EvaluatorFactory (Factory Pattern) │                        │
│  │                                     │                        │
│  │  ACTION_COUNT  → ActionCountEval    │                        │
│  │  STREAK        → StreakEval         │                        │
│  │  LEVEL_REACHED → LevelReachedEval   │                        │
│  │  XP_TOTAL      → XpTotalEval        │                        │
│  │  MULTI_ACTION  → MultiActionEval    │                        │
│  └─────────────────────────────────────┘                        │
└─────────────────────────────────────────────────────────────────┘
```

O **AchievementEngine** usa o padrão **Strategy** — cada `CriteriaType` tem seu próprio `AchievementCriteriaEvaluator`, desacoplado via `EvaluatorFactory`.

---

## Fluxo de processamento de uma ação

```
POST /api/v1/actions
        │
        ▼
  [1] Busca ou cria o player
        │
        ▼
  [2] Valida que a ação existe e está ativa
        │
        ▼
  [3] Verifica cooldown  ──► 429 Too Many Requests (se ativo)
        │
        ▼
  [4] Concede XP + registra ActionLog
        │
        ▼
  [5] Verifica level up  ──► atualiza nível do player
        │
        ▼
  [6] Atualiza streak do dia
        │
        ▼
  [7] Avalia conquistas (Strategy)  ──► XP bônus se desbloqueou
        │
        ▼
  [8] Atualiza posição no ranking (ROW_NUMBER no banco)
        │
        ▼
  [9] Dispara webhooks assíncronos (3 tentativas + backoff exp.)
        │
        ▼
  [200] ActionResultResponse
```

---

## Endpoints

### 🔓 Autenticação (pública)
| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/auth/register` | Cadastra novo tenant |
| `POST` | `/api/v1/auth/login` | Autentica e retorna JWT |

### 🔑 Integração (API Key — `X-API-Key`)
| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/actions` | **Endpoint principal** — processa ação do player |
| `GET` | `/api/v1/players/{id}` | Perfil completo do player |
| `GET` | `/api/v1/players/{id}/achievements` | Conquistas desbloqueadas e bloqueadas |
| `GET` | `/api/v1/players/{id}/timeline` | Histórico de ações (paginado) |
| `GET` | `/api/v1/leaderboard` | Ranking global |
| `GET` | `/api/v1/leaderboard/weekly` | Ranking semanal |
| `GET` | `/api/v1/leaderboard/monthly` | Ranking mensal |

### 🛡️ Administração (JWT)
| Método | Rota | Descrição |
|--------|------|-----------|
| `POST/GET/PUT/DELETE` | `/api/v1/actions/definitions` | CRUD de definições de ações |
| `POST/GET/PUT/DELETE` | `/api/v1/achievements` | CRUD de conquistas |
| `POST/GET/PUT/DELETE` | `/api/v1/webhooks` | CRUD de webhooks |
| `POST/GET/DELETE` | `/api/v1/api-keys` | Gerenciamento de API Keys |
| `POST/GET` | `/api/v1/levels` | Configuração da tabela de níveis |
| `GET` | `/api/v1/dashboard/overview` | Métricas do tenant |
| `GET` | `/api/v1/dashboard/actions-chart` | Gráfico de ações por dia |

---

## Rodando localmente

### Pré-requisitos

![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&style=flat-square)
![Maven](https://img.shields.io/badge/Maven-3.9%2B-C71A36?logo=apache-maven&style=flat-square)
![Docker](https://img.shields.io/badge/Docker-opcional-2496ED?logo=docker&style=flat-square)

### Modo dev (H2 em memória)

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/gamify-api.git
cd gamify-api

# Sobe com perfil dev (H2 in-memory, sem Docker)
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O banco H2 é criado automaticamente.

### Com Docker Compose (PostgreSQL local)

```bash
cp .env.example .env

# Sobe com banco PostgreSQL local
docker compose --profile local-db up --build
```

### Produção (banco externo)

```bash
cp .env.example .env
# Edite .env com DB_URL, DB_USERNAME, DB_PASSWORD e JWT_SECRET

docker compose up --build
```

---

## Variáveis de ambiente

| Variável | Descrição | Obrigatório em prod |
|----------|-----------|:-------------------:|
| `DB_URL` | JDBC URL do banco PostgreSQL | ✅ |
| `DB_USERNAME` | Usuário do banco | ✅ |
| `DB_PASSWORD` | Senha do banco | ✅ |
| `JWT_SECRET` | Chave secreta JWT (mín. 256 bits) | ✅ |
| `JWT_EXPIRATION_MS` | Expiração do token em ms (padrão: 86400000) | ❌ |

---

## Documentação interativa

Com a aplicação rodando:

| Interface | URL |
|-----------|-----|
| 📖 Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| 📄 OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| 🗄️ H2 Console (dev) | `http://localhost:8080/h2-console` |

---

## Exemplo de integração

### 1. Cadastrar tenant
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Minha Plataforma",
  "email": "admin@plataforma.com",
  "password": "senha123"
}
```

### 2. Fazer login e obter JWT
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@plataforma.com",
  "password": "senha123"
}
```

### 3. Criar uma definição de ação (JWT)
```http
POST /api/v1/actions/definitions
Authorization: Bearer {token}
Content-Type: application/json

{
  "code": "completed_lesson",
  "displayName": "Aula concluída",
  "xpValue": 50,
  "cooldownSeconds": 0
}
```

### 4. Criar uma API Key para integração
```http
POST /api/v1/api-keys
Authorization: Bearer {token}
Content-Type: application/json

{
  "label": "App Mobile"
}
```

### 5. Processar ação do usuário (API Key)
```http
POST /api/v1/actions
X-API-Key: gapi_sua_chave_aqui
Content-Type: application/json

{
  "playerId": "user-456",
  "playerName": "Maria",
  "actionCode": "completed_lesson"
}
```

**Resposta:**
```json
{
  "playerId": "user-456",
  "action": "completed_lesson",
  "xpGranted": 50,
  "totalXp": 150,
  "currentLevel": 2,
  "levelUp": {
    "happened": true,
    "previousLevel": 1,
    "newLevel": 2,
    "title": "Aprendiz"
  },
  "streak": {
    "currentStreak": 3,
    "longestStreak": 3,
    "wasReset": false
  },
  "newAchievements": [],
  "leaderboardPosition": 1,
  "processedAt": "2025-03-14T12:00:00Z"
}
```

---

## Tipos de critério para conquistas

| Tipo | `criteriaValue` | Descrição |
|------|-----------------|-----------|
| `ACTION_COUNT` | `{"actionCode": "login", "count": 10}` | Executou ação N vezes |
| `STREAK` | `{"minStreak": 7}` | Streak mínimo de dias |
| `LEVEL_REACHED` | `{"level": 5}` | Atingiu nível mínimo |
| `XP_TOTAL` | `{"minXp": 5000}` | Acumulou XP total mínimo |
| `MULTI_ACTION` | `{"actionCodes": ["a", "b", "c"]}` | Executou cada ação ao menos uma vez |

---

## Testes

```bash
# Roda todos os testes
./mvnw test

# Roda apenas um pacote
./mvnw test -Dtest="com.gamifyapi.unit.*"
```

A suíte cobre os serviços críticos com testes unitários via Mockito:

| Classe de Teste | Cobertura |
|-----------------|-----------|
| `GamificationServiceTest` | Orquestração do fluxo principal |
| `AchievementEngineTest` | Avaliação e desbloqueio de conquistas |
| `LevelServiceTest` | Cálculo de nível e XP |
| `StreakServiceTest` | Regras de streak |
| `CooldownServiceTest` | Validação de cooldown |

---

## Segurança

| Mecanismo | Detalhe |
|-----------|---------|
| 🔒 **Senhas** | Armazenadas com BCrypt |
| 🔑 **API Keys** | Nunca persistidas — apenas o hash SHA-256 fica no banco |
| 🪙 **JWT** | `sub` = tenant ID, assinado com HMAC-SHA256 |
| 🪝 **Webhooks** | Assinados com HMAC-SHA256 no header `X-Gamify-Signature` |
| 🏢 **Multi-tenancy** | Garantido por ThreadLocal — nenhuma query acessa dados de outro tenant |

---

## Estrutura do projeto

```
src/
├── main/java/com/gamifyapi/
│   │
│   ├── achievement/                          # Padrão Strategy para avaliação de conquistas
│   │   ├── AchievementCriteriaEvaluator.java # Interface contrato do Strategy
│   │   ├── AchievementEngine.java            # Motor: itera candidatas, delega ao evaluator, desbloqueia
│   │   ├── EvaluatorFactory.java             # Factory: resolve o evaluator pelo CriteriaType
│   │   ├── ActionCountEvaluator.java         # Critério ACTION_COUNT  — N execuções de uma ação
│   │   ├── StreakEvaluator.java              # Critério STREAK        — streak mínimo de dias
│   │   ├── LevelReachedEvaluator.java        # Critério LEVEL_REACHED — nível mínimo atingido
│   │   ├── XpTotalEvaluator.java             # Critério XP_TOTAL      — XP acumulado mínimo
│   │   └── MultiActionEvaluator.java         # Critério MULTI_ACTION  — N ações distintas executadas
│   │
│   ├── config/                               # Configurações Spring e infra
│   │   ├── SecurityConfig.java               # Filtros JWT/ApiKey, regras de acesso, sessão STATELESS
│   │   ├── AsyncConfig.java                  # ThreadPoolTaskExecutor para webhooks assíncronos
│   │   ├── JacksonConfig.java                # ObjectMapper com JavaTimeModule e ISO 8601
│   │   ├── OpenApiConfig.java                # Swagger UI com esquemas de segurança JWT e ApiKey
│   │   └── StartupLinksLogger.java           # Loga links úteis (Swagger, H2, API) no startup
│   │
│   ├── controller/                           # Camada HTTP — recebe requests, delega aos services
│   │   ├── AuthController.java               # POST /auth/register e /auth/login
│   │   ├── ActionController.java             # POST /actions — endpoint principal (via API Key)
│   │   ├── ActionDefinitionController.java   # CRUD /actions/definitions (via JWT)
│   │   ├── AchievementController.java        # CRUD /achievements (via JWT)
│   │   ├── ApiKeyController.java             # CRUD /api-keys (via JWT)
│   │   ├── LevelController.java              # POST/GET /levels — tabela de progressão (via JWT)
│   │   ├── WebhookController.java            # CRUD /webhooks (via JWT)
│   │   ├── LeaderboardController.java        # GET /leaderboard, /weekly, /monthly (via API Key)
│   │   ├── PlayerController.java             # GET /players/{id}, /achievements, /timeline
│   │   └── DashboardController.java          # GET /dashboard/overview e /actions-chart (via JWT)
│   │
│   ├── dto/
│   │   ├── request/                          # Records de entrada validados com Bean Validation
│   │   │   ├── RegisterRequest.java          # name, email, password
│   │   │   ├── LoginRequest.java             # email, password
│   │   │   ├── ProcessActionRequest.java     # playerId, playerName, actionCode
│   │   │   ├── ActionDefinitionRequest.java  # code, displayName, xpValue, cooldownSeconds
│   │   │   ├── AchievementRequest.java       # code, name, criteriaType, criteriaValue, xpReward
│   │   │   ├── CreateApiKeyRequest.java      # label
│   │   │   ├── LevelConfigRequest.java       # lista de LevelEntry (level, xpRequired, title)
│   │   │   └── WebhookConfigRequest.java     # url, eventType, secretKey
│   │   │
│   │   └── response/                         # Records de saída serializados como JSON
│   │       ├── ActionResultResponse.java     # Resposta completa do /actions (XP, level, streak…)
│   │       ├── AuthResponse.java             # token JWT + expiresIn + TenantInfo
│   │       ├── TenantResponse.java           # id, name, email, plan, createdAt
│   │       ├── ApiKeyResponse.java           # id, key (só na criação), prefix, label
│   │       ├── ActionDefinitionResponse.java # id, code, displayName, xpValue, cooldown
│   │       ├── AchievementResponse.java      # id, code, name, criteriaType, criteriaValue
│   │       ├── AchievementUnlockedResponse.java # conquista desbloqueada na ação (xpBonus, unlockedAt)
│   │       ├── PlayerProfileResponse.java    # perfil completo com XP, nível, streak, ranking
│   │       ├── PlayerAchievementsResponse.java  # listas de unlocked e locked por player
│   │       ├── LeaderboardResponse.java      # ranking paginado com LeaderboardEntry[]
│   │       ├── DashboardOverviewResponse.java   # métricas agregadas do tenant
│   │       └── WebhookConfigResponse.java    # id, url, eventType, active
│   │
│   ├── entity/                               # Entidades JPA mapeadas para o banco
│   │   ├── Tenant.java                       # Empresa/app cliente — raiz do multi-tenancy
│   │   ├── Player.java                       # Usuário final (externalId + tenantId)
│   │   ├── ActionDefinition.java             # Template de ação (code, xpValue, cooldown)
│   │   ├── ActionLog.java                    # Log imutável de cada execução de ação
│   │   ├── Achievement.java                  # Conquista configurada (criteriaValue como JSON)
│   │   ├── PlayerAchievement.java            # Relação N:N Player ↔ Achievement com unlockedAt
│   │   ├── LevelConfig.java                  # Tabela de progressão de níveis do tenant
│   │   ├── RankingEntry.java                 # Entrada no ranking (score, position, periodKey)
│   │   ├── WebhookConfig.java                # URL + eventType + secretKey por tenant
│   │   └── WebhookLog.java                   # Registro de tentativas de disparo de webhook
│   │
│   ├── enums/                                # Tipos enumerados do domínio
│   │   ├── CriteriaType.java                 # ACTION_COUNT | STREAK | LEVEL_REACHED | XP_TOTAL | MULTI_ACTION
│   │   ├── RankingPeriod.java                # GLOBAL | WEEKLY | MONTHLY
│   │   ├── TenantPlan.java                   # FREE | PRO | ENTERPRISE
│   │   └── WebhookEventType.java             # LEVEL_UP | ACHIEVEMENT_UNLOCKED | STREAK_MILESTONE | RANK_CHANGED
│   │
│   ├── exception/                            # Exceções de domínio e tratamento centralizado
│   │   ├── GlobalExceptionHandler.java       # @RestControllerAdvice — converte exceções em HTTP
│   │   ├── ErrorResponse.java                # Record padrão de resposta de erro (status, message, details)
│   │   ├── RecursoNaoEncontradoException.java # → HTTP 404
│   │   ├── ConflitoException.java            # → HTTP 409 (e-mail ou código duplicado)
│   │   ├── RegraNegocioException.java        # → HTTP 422 (regra de negócio violada)
│   │   ├── CooldownAtivoException.java       # → HTTP 429 (com header Retry-After)
│   │   └── AutenticacaoException.java        # → HTTP 401
│   │
│   ├── repository/                           # Interfaces Spring Data JPA com queries customizadas
│   │   ├── TenantRepository.java             # findByEmail, existsByEmail
│   │   ├── PlayerRepository.java             # findByTenant_IdAndExternalId, avg level, top XP
│   │   ├── ActionDefinitionRepository.java   # findByTenantIdAndCodeAndActiveTrue
│   │   ├── ActionLogRepository.java          # countByActionCode, sumXp, groupByDay (dashboard)
│   │   ├── AchievementRepository.java        # findAtivasNaoDesbloqueasPorPlayer (query JPQL)
│   │   ├── PlayerAchievementRepository.java  # countByTenantId, findAllByPlayerId
│   │   ├── ApiKeyRepository.java             # findByKeyHashAndActiveTrue (autenticação)
│   │   ├── LevelConfigRepository.java        # findAllByTenantIdOrderByLevelAsc, deleteAllByTenantId
│   │   ├── RankingEntryRepository.java       # recalcularPosicoes (ROW_NUMBER nativo), findByTenantPlayerAndPeriod
│   │   ├── WebhookConfigRepository.java      # findAllByTenantIdAndEventTypeAndActiveTrue
│   │   └── WebhookLogRepository.java         # persistência de logs de disparo
│   │
│   ├── security/                             # Autenticação, contexto de tenant e filtros
│   │   ├── JwtTokenProvider.java             # Gera e valida JWTs (HMAC-SHA256, sub = tenantId)
│   │   ├── JwtAuthenticationFilter.java      # Intercepta Bearer token, seta TenantContext
│   │   ├── ApiKeyAuthenticationFilter.java   # Intercepta X-API-Key, faz SHA-256, seta TenantContext
│   │   ├── TenantContext.java                # ThreadLocal<Tenant> — isolamento por requisição
│   │   ├── SecurityUtils.java                # getTenantAtual() / getTenantIdAtual() com erro claro
│   │   ├── CustomUserDetailsService.java     # Carrega Tenant por e-mail para o Spring Security
│   │   └── TenantUserDetails.java            # Adapta Tenant para UserDetails (ROLE_TENANT)
│   │
│   └── service/                              # Lógica de negócio
│       ├── GamificationService.java          # Orquestrador principal — coordena todos os serviços
│       ├── PlayerService.java                # buscarOuCriar — upsert do player por externalId
│       ├── XpService.java                    # concederXp (com ActionLog) e concederXpBonus
│       ├── LevelService.java                 # calcularNivel, verificarLevelUp (suporta multi-level-up)
│       ├── StreakService.java                # Regras de streak por dia de calendário (LocalDate)
│       ├── CooldownService.java              # Valida janela de cooldown entre execuções
│       ├── AchievementService.java           # CRUD de conquistas + obterConquistasDoPlayer
│       ├── ActionDefinitionService.java      # CRUD de definições de ações
│       ├── ApiKeyService.java                # Gera chave segura (SecureRandom), armazena só o hash
│       ├── AuthService.java                  # Registro de tenant e login com JWT
│       ├── LevelConfigService.java           # Salva/substitui tabela de níveis do tenant
│       ├── RankingService.java               # Atualiza ranking e recalcula posições via ROW_NUMBER
│       ├── WebhookService.java               # Disparo assíncrono com 3 tentativas + backoff exponencial
│       ├── WebhookConfigService.java         # CRUD de configurações de webhook
│       ├── DashboardService.java             # Métricas agregadas e gráfico de ações por dia
│       └── PlayerProfileService.java         # Perfil, conquistas e timeline do player
│
└── test/java/com/gamifyapi/
    ├── unit/
    │   ├── engine/
    │   │   └── AchievementEngineTest.java    # Avaliação, desbloqueio e resiliência a erros
    │   └── service/
    │       ├── GamificationServiceTest.java  # Fluxo principal, level up, conquista com XP bônus
    │       ├── LevelServiceTest.java         # Fórmula padrão, multi-level-up, XP faltante
    │       ├── StreakServiceTest.java         # Incremento, reset, longestStreak, idempotência
    │       └── CooldownServiceTest.java      # Cooldown zero, primeira vez, ativo, expirado
    └── util/
        └── TestDataFactory.java              # Fábrica de objetos de teste (Tenant, Player, Achievement…)
```

---

## Licença

Este projeto está sob a licença [MIT](./LICENSE).

<div align="center">

Made with ☕ and Java

[![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)

</div>
