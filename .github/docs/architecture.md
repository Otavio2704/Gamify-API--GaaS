# Arquitetura do Projeto — GamifyAPI

## Visão Geral

A GamifyAPI segue uma arquitetura em camadas (Layered Architecture) com separação clara de responsabilidades. Cada camada só conhece a camada imediatamente abaixo, garantindo baixo acoplamento e alta testabilidade.

---

## Diagrama de Camadas

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT REQUEST                              │
│              (App externo via API Key ou Admin via JWT)              │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        SECURITY LAYER                               │
│                                                                     │
│  Intercepta toda requisição antes de chegar ao controller.          │
│  Dois filtros atuam em paralelo:                                    │
│                                                                     │
│  ┌──────────────────────────┐  ┌──────────────────────────────┐     │
│  │  JwtAuthenticationFilter │  │  ApiKeyAuthenticationFilter  │     │
│  │                          │  │                              │     │
│  │  Rotas: /api/v1/auth/**  │  │  Rotas: /api/v1/actions,    │     │
│  │  (exceto login/register) │  │  /api/v1/players/**,         │     │
│  │  /api/v1/api-keys/**     │  │  /api/v1/leaderboard/**      │     │
│  │  /api/v1/actions/def.**  │  │                              │     │
│  │  /api/v1/levels/**       │  │  Lê header X-API-Key,        │     │
│  │  /api/v1/achievements/** │  │  busca hash no banco,        │     │
│  │  /api/v1/webhooks/**     │  │  identifica o tenant.        │     │
│  │  /api/v1/dashboard/**    │  │                              │     │
│  └──────────────────────────┘  └──────────────────────────────┘     │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐       │
│  │  TenantContext (ThreadLocal)                              │       │
│  │  Armazena o tenant autenticado durante o ciclo de vida    │       │
│  │  da requisição. Toda a aplicação consulta esse contexto.  │       │
│  └──────────────────────────────────────────────────────────┘       │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       CONTROLLER LAYER                              │
│                                                                     │
│  - Recebe a requisição HTTP já autenticada                          │
│  - Valida o body com Bean Validation (@Valid)                       │
│  - Delega toda lógica ao Service                                    │
│  - Converte o retorno do Service em ResponseEntity                  │
│  - NÃO contém regras de negócio                                     │
│  - NÃO acessa Repository diretamente                                │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        SERVICE LAYER                                │
│                                                                     │
│  Contém toda a lógica de negócio da aplicação.                      │
│  O GamificationService atua como orquestrador central:              │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────────┐   │
│  │                   GamificationService (orquestrador)         │   │
│  │                                                              │   │
│  │  processAction()                                             │   │
│  │  ├── 1. PlayerService.findOrCreate()                         │   │
│  │  ├── 2. CooldownService.validate()                           │   │
│  │  ├── 3. XpService.grant()                                    │   │
│  │  ├── 4. LevelService.checkLevelUp()                          │   │
│  │  ├── 5. StreakService.update()                               │   │
│  │  ├── 6. AchievementEngine.evaluate()                         │   │
│  │  │       └── EvaluatorFactory → [Strategy Evaluators]        │   │
│  │  ├── 7. RankingService.updatePosition()                      │   │
│  │  └── 8. WebhookService.notifyAsync()                         │   │
│  └──────────────────────────────────────────────────────────────┘   │
│                                                                     │
│  Cada service específico é independente e testável isoladamente.    │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                       REPOSITORY LAYER                              │
│                                                                     │
│  - Interfaces Spring Data JPA                                       │
│  - Queries derivadas do nome do método                              │
│  - Queries customizadas com @Query (JPQL)                           │
│  - Paginação nativa via Pageable                                    │
│  - TODAS as queries filtram por tenantId                            │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                         DATABASE                                    │
│                                                                     │
│  PostgreSQL (produção) — H2 (testes e desenvolvimento)              │
│  Hibernate gerencia o schema via ddl-auto                           │
│  Flyway gerencia migrações versionadas                              │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Padrões de Projeto Utilizados

| Padrão | Onde é usado |
|--------|-------------|
| **Strategy** | `AchievementCriteriaEvaluator` e suas implementações (`ActionCountEvaluator`, `StreakEvaluator`, etc.). Permite adicionar novos tipos de critério sem alterar código existente (Open/Closed Principle). |
| **Factory** | `EvaluatorFactory` seleciona o evaluator correto com base no `CriteriaType` da conquista. |
| **Facade** | `GamificationService` atua como fachada, orquestrando múltiplos services internos em um único fluxo. |
| **Template Method** | A interface `AchievementCriteriaEvaluator` define o contrato; cada implementação define a avaliação. |
| **DTO Pattern** | Requests e Responses isolam a API das entidades internas. Entidades JPA nunca são expostas. |
| **Repository Pattern** | Spring Data JPA abstrai o acesso a dados. |
| **Builder** | Lombok `@Builder` nas entidades e DTOs para construção fluente de objetos. |
| **Observer (async)** | `WebhookService` dispara notificações de forma assíncrona quando eventos ocorrem (level up, etc.). |

---

## Estrutura Completa de Pastas e Arquivos

```
gamify-api/
│
│  ── Arquivos da raiz ──────────────────────────────────────────────────
│
├── pom.xml                                    # Dependências Maven e plugins
├── docker-compose.yml                         # PostgreSQL + app containerizados
├── Dockerfile                                 # Build da imagem da aplicação
├── README.md                                  # Apresentação, como rodar, endpoints
├── .gitignore                                 # Arquivos ignorados pelo Git
├── .env.example                               # Variáveis de ambiente de exemplo
│
│  ── CI/CD ─────────────────────────────────────────────────────────────
│
├── .github/
│   ├── workflows/
│   │   └── ci.yml                             # GitHub Actions: build + testes
│   ├── copilot-instructions.md                # Instruções gerais pro Copilot
│   ├── project-context.md                     # Domínio, glossário, fluxos
│   ├── coding-standards.md                    # Convenções e exemplos de código
│   ├── api-contracts.md                       # Endpoints e contratos JSON
│   ├── database-schema.md                     # Tabelas, colunas, índices
│   ├── business-rules.md                      # Regras de negócio detalhadas
│   └── testing-guide.md                       # Padrões de teste e cenários
│
│  ── Documentação auxiliar ─────────────────────────────────────────────
│
├── docs/
│   ├── ARCHITECTURE.md                        # Este arquivo
│   ├── API-GUIDE.md                           # Guia de integração para devs externos
│   └── gamify-api.postman_collection.json     # Collection Postman com exemplos
│
│  ── Código-fonte ──────────────────────────────────────────────────────
│
└── src/
    ├── main/
    │   ├── java/com/gamifyapi/
    │   │   │
    │   │   ├── GamifyApiApplication.java      # Classe principal (main)
    │   │   │
    │   │   │
    │   │   │  ── CONFIG ────────────────────────────────────────────────
    │   │   │  Configurações do Spring Framework.
    │   │   │  Cada arquivo configura um aspecto da aplicação.
    │   │   │
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java
    │   │   │   │   # Configura Spring Security.
    │   │   │   │   # Define quais rotas são públicas, quais exigem JWT
    │   │   │   │   # e quais exigem API Key.
    │   │   │   │   # Registra os dois filtros de autenticação.
    │   │   │   │   # Desabilita CSRF (API stateless).
    │   │   │   │   # Define política de sessão STATELESS.
    │   │   │   │
    │   │   │   ├── CorsConfig.java
    │   │   │   │   # Configuração de CORS.
    │   │   │   │   # Permite origens, métodos e headers específicos.
    │   │   │   │   # Em dev: permite tudo. Em prod: restrito.
    │   │   │   │
    │   │   │   ├── OpenApiConfig.java
    │   │   │   │   # Configuração do Swagger/SpringDoc.
    │   │   │   │   # Define título, descrição, versão da API.
    │   │   │   │   # Configura esquemas de autenticação (JWT e API Key)
    │   │   │   │   # para aparecerem no Swagger UI.
    │   │   │   │
    │   │   │   ├── AsyncConfig.java
    │   │   │   │   # Habilita @Async no Spring.
    │   │   │   │   # Configura o ThreadPoolTaskExecutor para webhooks.
    │   │   │   │   # Define: core pool, max pool, queue capacity.
    │   │   │   │
    │   │   │   └── JacksonConfig.java
    │   │   │       # Configuração de serialização JSON.
    │   │   │       # Define formato de datas (ISO 8601).
    │   │   │       # Configura para ignorar campos nulos no response.
    │   │   │       # Registra módulos (JavaTimeModule).
    │   │   │
    │   │   │
    │   │   │  ── SECURITY ──────────────────────────────────────────────
    │   │   │  Autenticação, autorização e contexto do tenant.
    │   │   │  Dois mecanismos: JWT (admin) e API Key (integração).
    │   │   │
    │   │   ├── security/
    │   │   │   ├── JwtTokenProvider.java
    │   │   │   │   # Responsável por gerar e validar tokens JWT.
    │   │   │   │   # Métodos: generateToken(tenant), validateToken(token),
    │   │   │   │   # getTenantIdFromToken(token).
    │   │   │   │   # Usa a lib jjwt. Chave secreta via application.yml.
    │   │   │   │   # Expiração configurável (padrão: 24h).
    │   │   │   │
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   │   # Filtro que intercepta requisições com header
    │   │   │   │   # Authorization: Bearer <token>.
    │   │   │   │   # Extrai o token, valida via JwtTokenProvider,
    │   │   │   │   # carrega o tenant e seta no SecurityContext
    │   │   │   │   # e no TenantContext.
    │   │   │   │   # Extends OncePerRequestFilter.
    │   │   │   │
    │   │   │   ├── ApiKeyAuthenticationFilter.java
    │   │   │   │   # Filtro que intercepta requisições com header
    │   │   │   │   # X-API-Key: gapi_xxxx.
    │   │   │   │   # Faz hash da key recebida (SHA-256),
    │   │   │   │   # busca no banco (ApiKeyRepository),
    │   │   │   │   # valida se está ativa,
    │   │   │   │   # seta o tenant no TenantContext.
    │   │   │   │   # Extends OncePerRequestFilter.
    │   │   │   │
    │   │   │   ├── TenantContext.java
    │   │   │   │   # ThreadLocal que armazena o tenant autenticado.
    │   │   │   │   # Métodos estáticos: setTenant(), getTenant(), clear().
    │   │   │   │   # Limpo automaticamente ao final da requisição.
    │   │   │   │   # Usado por todos os services para saber qual tenant
    │   │   │   │   # está fazendo a requisição.
    │   │   │   │
    │   │   │   ├── CustomUserDetailsService.java
    │   │   │   │   # Implementa UserDetailsService do Spring Security.
    │   │   │   │   # Carrega o tenant pelo email (para login JWT).
    │   │   │   │   # Converte Tenant em UserDetails.
    │   │   │   │
    │   │   │   └── SecurityUtils.java
    │   │   │       # Métodos utilitários estáticos.
    │   │   │       # getCurrentTenantId(): retorna ID do tenant logado.
    │   │   │       # getCurrentTenant(): retorna entidade Tenant.
    │   │   │       # Encapsula acesso ao TenantContext e SecurityContext.
    │   │   │
    │   │   │
    │   │   │  ── ENTITY ────────────────────────────────────────────────
    │   │   │  Entidades JPA mapeadas para tabelas do banco.
    │   │   │  Cada entidade representa um conceito do domínio.
    │   │   │  Todas possuem tenant_id (multi-tenancy).
    │   │   │
    │   │   ├── entity/
    │   │   │   ├── Tenant.java
    │   │   │   │   # Representa uma empresa/app cliente.
    │   │   │   │   # Campos: id, name, email, passwordHash, plan,
    │   │   │   │   # createdAt, updatedAt.
    │   │   │   │   # Relacionamentos: 1:N com todas as outras entidades.
    │   │   │   │
    │   │   │   ├── ApiKey.java
    │   │   │   │   # Chave de API para integração.
    │   │   │   │   # Campos: id, tenant (FK), keyHash, prefix, label,
    │   │   │   │   # active, createdAt.
    │   │   │   │   # keyHash: SHA-256 da chave original.
    │   │   │   │   # prefix: primeiros 8 chars para identificação visual.
    │   │   │   │
    │   │   │   ├── Player.java
    │   │   │   │   # Usuário final do app cliente dentro da GamifyAPI.
    │   │   │   │   # Campos: id, tenant (FK), externalId, displayName,
    │   │   │   │   # totalXp, currentLevel, currentStreak, longestStreak,
    │   │   │   │   # lastActivityDate, createdAt, updatedAt.
    │   │   │   │   # Métodos de domínio: addXp(int).
    │   │   │   │   # Unique constraint: (tenant_id, external_id).
    │   │   │   │
    │   │   │   ├── ActionDefinition.java
    │   │   │   │   # Template de ação configurado pelo tenant.
    │   │   │   │   # Campos: id, tenant (FK), code, displayName,
    │   │   │   │   # description, xpValue, cooldownSeconds, active,
    │   │   │   │   # createdAt.
    │   │   │   │   # Unique constraint: (tenant_id, code).
    │   │   │   │
    │   │   │   ├── ActionLog.java
    │   │   │   │   # Registro de cada ação executada por um player.
    │   │   │   │   # Campos: id, player (FK), actionDefinition (FK),
    │   │   │   │   # xpGranted, timestamp.
    │   │   │   │   # Imutável: uma vez criado, nunca é alterado.
    │   │   │   │
    │   │   │   ├── LevelConfig.java
    │   │   │   │   # Tabela de progressão de níveis do tenant.
    │   │   │   │   # Campos: id, tenant (FK), level, xpRequired, title.
    │   │   │   │   # Unique constraint: (tenant_id, level).
    │   │   │   │   # Ordenação natural: por level ASC.
    │   │   │   │
    │   │   │   ├── Achievement.java
    │   │   │   │   # Conquista/badge configurada pelo tenant.
    │   │   │   │   # Campos: id, tenant (FK), code, name, description,
    │   │   │   │   # badgeImageUrl, xpReward, criteriaType (enum),
    │   │   │   │   # criteriaValue (JSON string), active, createdAt.
    │   │   │   │   # Método auxiliar: getCriteriaValueAsMap().
    │   │   │   │   # Método auxiliar: getCriteriaValueAsInt(key).
    │   │   │   │
    │   │   │   ├── PlayerAchievement.java
    │   │   │   │   # Relação N:N entre Player e Achievement.
    │   │   │   │   # Campos: id, player (FK), achievement (FK),
    │   │   │   │   # unlockedAt.
    │   │   │   │   # Unique constraint: (player_id, achievement_id).
    │   │   │   │
    │   │   │   ├── WebhookConfig.java
    │   │   │   │   # Configuração de webhook do tenant.
    │   │   │   │   # Campos: id, tenant (FK), url, eventType (enum),
    │   │   │   │   # secretKey, active, createdAt.
    │   │   │   │   # Um tenant pode ter vários webhooks para eventos
    │   │   │   │   # diferentes ou até para o mesmo evento.
    │   │   │   │
    │   │   │   ├── WebhookLog.java
    │   │   │   │   # Log de cada tentativa de disparo de webhook.
    │   │   │   │   # Campos: id, webhookConfig (FK), eventType,
    │   │   │   │   # payload (JSON), responseStatus, success,
    │   │   │   │   # attemptCount, sentAt.
    │   │   │   │   # Usado para debug e retry de webhooks falhados.
    │   │   │   │
    │   │   │   └── RankingEntry.java
    │   │   │       # Entrada no ranking/leaderboard.
    │   │   │       # Campos: id, tenant (FK), player (FK), period (enum),
    │   │   │       # periodKey (ex: "2025-W03"), score, position,
    │   │   │       # updatedAt.
    │   │   │       # Índice composto: (tenant_id, period, period_key,
    │   │   │       # position) para consultas eficientes.
    │   │   │
    │   │   │
    │   │   │  ── ENUMS ─────────────────────────────────────────────────
    │   │   │  Enumerações do domínio. Persistidas como VARCHAR no banco.
    │   │   │
    │   │   ├── enums/
    │   │   │   ├── CriteriaType.java
    │   │   │   │   # Valores: ACTION_COUNT, STREAK, LEVEL_REACHED,
    │   │   │   │   # XP_TOTAL, MULTI_ACTION.
    │   │   │   │
    │   │   │   ├── WebhookEventType.java
    │   │   │   │   # Valores: LEVEL_UP, ACHIEVEMENT_UNLOCKED,
    │   │   │   │   # STREAK_MILESTONE, RANK_CHANGED.
    │   │   │   │
    │   │   │   ├── RankingPeriod.java
    │   │   │   │   # Valores: GLOBAL, WEEKLY, MONTHLY.
    │   │   │   │
    │   │   │   └── TenantPlan.java
    │   │   │       # Valores: FREE, PRO, ENTERPRISE.
    │   │   │       # (Para uso futuro em limitações de features.)
    │   │   │
    │   │   │
    │   │   │  ── DTO ───────────────────────────────────────────────────
    │   │   │  Objetos de transferência. Isolam a API das entidades.
    │   │   │  Usar Java records sempre que possível.
    │   │   │  Requests contêm anotações de validação.
    │   │   │
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   │   ├── RegisterRequest.java
    │   │   │   │   │   # Campos: name, email, password.
    │   │   │   │   │   # Validações: @NotBlank, @Email, @Size(min=8).
    │   │   │   │   │
    │   │   │   │   ├── LoginRequest.java
    │   │   │   │   │   # Campos: email, password.
    │   │   │   │   │   # Validações: @NotBlank, @Email.
    │   │   │   │   │
    │   │   │   │   ├── CreateApiKeyRequest.java
    │   │   │   │   │   # Campos: label.
    │   │   │   │   │   # Validações: @NotBlank, @Size(max=50).
    │   │   │   │   │
    │   │   │   │   ├── ActionDefinitionRequest.java
    │   │   │   │   │   # Campos: code, displayName, description,
    │   │   │   │   │   # xpValue, cooldownSeconds.
    │   │   │   │   │   # Validações: @NotBlank, @Positive, @Min(0).
    │   │   │   │   │
    │   │   │   │   ├── ProcessActionRequest.java
    │   │   │   │   │   # Campos: playerId, playerName, actionCode.
    │   │   │   │   │   # Validações: @NotBlank em playerId e actionCode.
    │   │   │   │   │   # playerName é opcional.
    │   │   │   │   │
    │   │   │   │   ├── LevelConfigRequest.java
    │   │   │   │   │   # Campos: levels (List<LevelEntry>).
    │   │   │   │   │   # LevelEntry: level, xpRequired, title.
    │   │   │   │   │   # Validações: @NotEmpty, nível 1 deve ter xp=0.
    │   │   │   │   │
    │   │   │   │   ├── AchievementRequest.java
    │   │   │   │   │   # Campos: code, name, description, badgeImageUrl,
    │   │   │   │   │   # xpReward, criteriaType, criteriaValue (Map).
    │   │   │   │   │   # Validações: @NotBlank, @NotNull.
    │   │   │   │   │   # @ValidCriteriaValue (validador customizado).
    │   │   │   │   │
    │   │   │   │   └── WebhookConfigRequest.java
    │   │   │   │       # Campos: url, eventType, secretKey.
    │   │   │   │       # Validações: @NotBlank, @URL, @NotNull.
    │   │   │   │
    │   │   │   └── response/
    │   │   │       ├── AuthResponse.java
    │   │   │       │   # Campos: token, expiresIn, tenant (TenantInfo).
    │   │   │       │
    │   │   │       ├── ApiKeyResponse.java
    │   │   │       │   # Campos: id, key (nullable), prefix, label,
    │   │   │       │   # active, createdAt.
    │   │   │       │   # key só é preenchido na criação.
    │   │   │       │
    │   │   │       ├── ActionResultResponse.java
    │   │   │       │   # Response principal do POST /actions.
    │   │   │       │   # Campos: playerId, action, xpGranted, totalXp,
    │   │   │       │   # currentLevel, levelUp (LevelUpDetails),
    │   │   │       │   # streak (StreakInfo),
    │   │   │       │   # newAchievements (List<AchievementResponse>),
    │   │   │       │   # leaderboardPosition, processedAt.
    │   │   │       │
    │   │   │       ├── PlayerProfileResponse.java
    │   │   │       │   # Campos: externalId, displayName, totalXp,
    │   │   │       │   # currentLevel, levelTitle, xpToNextLevel,
    │   │   │       │   # progressPercent, currentStreak, longestStreak,
    │   │   │       │   # totalActions, achievementsUnlocked,
    │   │   │       │   # leaderboardPosition, memberSince.
    │   │   │       │
    │   │   │       ├── PlayerStatsResponse.java
    │   │   │       │   # Campos: totalActions, actionBreakdown (Map),
    │   │   │       │   # xpBreakdown, averageActionsPerDay,
    │   │   │       │   # mostActiveDay, mostActiveHour, daysActive,
    │   │   │       │   # currentStreak, longestStreak.
    │   │   │       │
    │   │   │       ├── AchievementResponse.java
    │   │   │       │   # Campos: code, name, description,
    │   │   │       │   # badgeImageUrl, xpBonus, unlockedAt.
    │   │   │       │
    │   │   │       ├── LeaderboardResponse.java
    │   │   │       │   # Campos: period, entries (List),
    │   │   │       │   # page, size, totalPlayers.
    │   │   │       │
    │   │   │       ├── LeaderboardEntryResponse.java
    │   │   │       │   # Campos: position, externalId, displayName,
    │   │   │       │   # totalXp, level, levelTitle.
    │   │   │       │
    │   │   │       ├── LevelUpDetails.java
    │   │   │       │   # Campos: happened (boolean), previousLevel,
    │   │   │       │   # newLevel, title.
    │   │   │       │   # Métodos estáticos: none(), of().
    │   │   │       │
    │   │   │       ├── StreakInfo.java
    │   │   │       │   # Campos: currentStreak, longestStreak,
    │   │   │       │   # wasReset (boolean).
    │   │   │       │
    │   │   │       ├── TimelineEntryResponse.java
    │   │   │       │   # Campos: type (ACTION/LEVEL_UP/ACHIEVEMENT),
    │   │   │       │   # description, xp, timestamp.
    │   │   │       │
    │   │   │       ├── DashboardOverviewResponse.java
    │   │   │       │   # Campos: totalPlayers, activePlayers7d,
    │   │   │       │   # totalActionsAllTime, totalActions7d,
    │   │   │       │   # averageLevel, achievementsUnlockedTotal,
    │   │   │       │   # topAction.
    │   │   │       │
    │   │   │       └── ErrorResponse.java
    │   │   │           # Campos: status (int), message, timestamp,
    │   │   │           # details (Map<String, String>).
    │   │   │           # Formato padronizado para todos os erros.
    │   │   │
    │   │   │
    │   │   │  ── REPOSITORY ────────────────────────────────────────────
    │   │   │  Interfaces Spring Data JPA para acesso a dados.
    │   │   │  Queries customizadas com @Query quando necessário.
    │   │   │
    │   │   ├── repository/
    │   │   │   ├── TenantRepository.java
    │   │   │   │   # findByEmail(String email): Optional<Tenant>
    │   │   │   │   # existsByEmail(String email): boolean
    │   │   │   │
    │   │   │   ├── ApiKeyRepository.java
    │   │   │   │   # findByKeyHash(String keyHash): Optional<ApiKey>
    │   │   │   │   # findAllByTenantId(Long tenantId): List<ApiKey>
    │   │   │   │
    │   │   │   ├── PlayerRepository.java
    │   │   │   │   # findByTenantIdAndExternalId(Long, String): Optional
    │   │   │   │   # existsByTenantIdAndExternalId(Long, String): boolean
    │   │   │   │   # findLeaderboard(Long tenantId, Pageable): Page
    │   │   │   │   # findPlayerRank(Long tenantId, int xp): int
    │   │   │   │   # countByTenantId(Long tenantId): long
    │   │   │   │   # countActiveSince(Long tenantId, LocalDate): long
    │   │   │   │
    │   │   │   ├── ActionDefinitionRepository.java
    │   │   │   │   # findByTenantIdAndCode(Long, String): Optional
    │   │   │   │   # findAllByTenantId(Long tenantId): List
    │   │   │   │   # existsByTenantIdAndCode(Long, String): boolean
    │   │   │   │
    │   │   │   ├── ActionLogRepository.java
    │   │   │   │   # findLastByPlayerAndAction(Long playerId,
    │   │   │   │   #   Long actionDefId): Optional<ActionLog>
    │   │   │   │   # countByPlayerIdAndActionDefinitionCode(Long,
    │   │   │   │   #   String): long
    │   │   │   │   # findDistinctActionCodesByPlayerId(Long): List<String>
    │   │   │   │   # sumXpByPlayerIdAndTimestampBetween(Long,
    │   │   │   │   #   Instant, Instant): int
    │   │   │   │   # findByPlayerIdOrderByTimestampDesc(Long,
    │   │   │   │   #   Pageable): Page
    │   │   │   │   # countByTenantIdGroupedByActionCode(Long): List<Object[]>
    │   │   │   │   # countByTenantIdAndTimestampBetween(Long,
    │   │   │   │   #   Instant, Instant): long
    │   │   │   │
    │   │   │   ├── LevelConfigRepository.java
    │   │   │   │   # findByTenantIdOrderByLevelAsc(Long): List
    │   │   │   │   # deleteAllByTenantId(Long tenantId): void
    │   │   │   │
    │   │   │   ├── AchievementRepository.java
    │   │   │   │   # findByTenantIdAndActiveTrue(Long): List
    │   │   │   │   # existsByTenantIdAndCode(Long, String): boolean
    │   │   │   │
    │   │   │   ├── PlayerAchievementRepository.java
    │   │   │   │   # findByPlayerId(Long playerId): List
    │   │   │   │   # existsByPlayerIdAndAchievementId(Long, Long): boolean
    │   │   │   │   # countByAchievementTenantId(Long tenantId): long
    │   │   │   │
    │   │   │   ├── WebhookConfigRepository.java
    │   │   │   │   # findByTenantIdAndEventTypeAndActiveTrue(Long,
    │   │   │   │   #   WebhookEventType): List
    │   │   │   │   # findAllByTenantId(Long tenantId): List
    │   │   │   │
    │   │   │   ├── WebhookLogRepository.java
    │   │   │   │   # findByWebhookConfigIdAndSuccessFalse(): List
    │   │   │   │
    │   │   │   └── RankingEntryRepository.java
    │   │   │       # findByTenantIdAndPeriodAndPeriodKeyOrderByPositionAsc(
    │   │   │       #   Long, RankingPeriod, String, Pageable): Page
    │   │   │       # findByTenantIdAndPlayerIdAndPeriod(Long, Long,
    │   │   │       #   RankingPeriod): Optional
    │   │   │
    │   │   │
    │   │   │  ── SERVICE ───────────────────────────────────────────────
    │   │   │  Camada de regras de negócio.
    │   │   │  Cada service tem responsabilidade única.
    │   │   │  GamificationService orquestra todos os outros.
    │   │   │
    │   │   ├── service/
    │   │   │   ├── AuthService.java
    │   │   │   │   # register(RegisterRequest): TenantResponse
    │   │   │   │   #   → Valida e-mail único, faz hash da senha,
    │   │   │   │   #   persiste Tenant.
    │   │   │   │   # login(LoginRequest): AuthResponse
    │   │   │   │   #   → Valida credenciais, gera JWT.
    │   │   │   │
    │   │   │   ├── ApiKeyService.java
    │   │   │   │   # generate(CreateApiKeyRequest): ApiKeyResponse
    │   │   │   │   #   → Gera chave aleatória, salva hash SHA-256,
    │   │   │   │   #   retorna chave em texto (única vez).
    │   │   │   │   # validateAndGetTenant(String rawKey): Tenant
    │   │   │   │   #   → Faz hash, busca no banco, valida active=true.
    │   │   │   │   # list(): List<ApiKeyResponse>
    │   │   │   │   # revoke(Long id): void
    │   │   │   │
    │   │   │   ├── GamificationService.java ⭐
    │   │   │   │   # processAction(ProcessActionRequest): ActionResultResponse
    │   │   │   │   #   → Orquestra todo o fluxo:
    │   │   │   │   #   1. Busca/cria player
    │   │   │   │   #   2. Valida cooldown
    │   │   │   │   #   3. Concede XP
    │   │   │   │   #   4. Checa level up
    │   │   │   │   #   5. Atualiza streak
    │   │   │   │   #   6. Avalia conquistas
    │   │   │   │   #   7. Atualiza ranking
    │   │   │   │   #   8. Dispara webhooks
    │   │   │   │   #   9. Monta e retorna response completo.
    │   │   │   │   #   Método @Transactional.
    │   │   │   │
    │   │   │   ├── PlayerService.java
    │   │   │   │   # findOrCreate(String externalId, String name): Player
    │   │   │   │   #   → Busca por (tenantId, externalId).
    │   │   │   │   #   Se não existe, cria com valores iniciais.
    │   │   │   │   #   Se existe e name mudou, atualiza displayName.
    │   │   │   │   # getProfile(String externalId): PlayerProfileResponse
    │   │   │   │   # getTimeline(String externalId, Pageable): Page
    │   │   │   │   # getStats(String externalId): PlayerStatsResponse
    │   │   │   │
    │   │   │   ├── XpService.java
    │   │   │   │   # grant(Player, ActionDefinition): int
    │   │   │   │   #   → Adiciona XP ao player, registra ActionLog.
    │   │   │   │   #   Retorna quantidade de XP concedido.
    │   │   │   │
    │   │   │   ├── LevelService.java
    │   │   │   │   # checkLevelUp(Player): LevelUpDetails
    │   │   │   │   #   → Busca tabela de níveis do tenant.
    │   │   │   │   #   Compara totalXp com xpRequired.
    │   │   │   │   #   Se subiu, atualiza player.currentLevel.
    │   │   │   │   #   Suporta level up múltiplo.
    │   │   │   │   # configureLevels(LevelConfigRequest): List
    │   │   │   │   # getLevels(): List<LevelConfig>
    │   │   │   │
    │   │   │   ├── StreakService.java
    │   │   │   │   # update(Player): StreakInfo
    │   │   │   │   #   → Compara lastActivityDate com hoje.
    │   │   │   │   #   Se ontem: streak++
    │   │   │   │   #   Se hoje: noop
    │   │   │   │   #   Se antes de ontem ou null: reset para 1.
    │   │   │   │   #   Atualiza longestStreak se necessário.
    │   │   │   │   #   Atualiza lastActivityDate para hoje.
    │   │   │   │
    │   │   │   ├── AchievementService.java
    │   │   │   │   # CRUD de conquistas para o painel admin.
    │   │   │   │   # create(AchievementRequest): AchievementResponse
    │   │   │   │   # list(): List<AchievementResponse>
    │   │   │   │   # update(Long id, AchievementRequest): AchievementResponse
    │   │   │   │   # delete(Long id): void
    │   │   │   │   # getPlayerAchievements(String externalId): unlocked+locked
    │   │   │   │
    │   │   │   ├── AchievementEngine.java ⭐
    │   │   │   │   # evaluate(Player): List<Achievement>
    │   │   │   │   #   → Busca conquistas ativas do tenant.
    │   │   │   │   #   Filtra as que o player ainda não desbloqueou.
    │   │   │   │   #   Para cada uma, usa EvaluatorFactory para
    │   │   │   │   #   obter o evaluator correto e avalia.
    │   │   │   │   #   Se desbloqueou, cria PlayerAchievement
    │   │   │   │   #   e concede xpReward (se houver).
    │   │   │   │   #   Retorna lista de conquistas novas.
    │   │   │   │
    │   │   │   ├── RankingService.java
    │   │   │   │   # updatePosition(Player): int
    │   │   │   │   #   → Recalcula posição do player no ranking.
    │   │   │   │   #   Retorna a posição atual.
    │   │   │   │   # getLeaderboard(RankingPeriod, Pageable):
    │   │   │   │   #   LeaderboardResponse
    │   │   │   │
    │   │   │   ├── WebhookService.java
    │   │   │   │   # notifyAsync(WebhookEventType, Object payload): void
    │   │   │   │   #   → Método @Async.
    │   │   │   │   #   Busca webhooks ativos do tenant para o eventType.
    │   │   │   │   #   Envia POST HTTP para cada URL.
    │   │   │   │   #   Assina payload com HMAC-SHA256.
    │   │   │   │   #   Em caso de falha, registra em WebhookLog
    │   │   │   │   #   para retry posterior.
    │   │   │   │
    │   │   │   ├── DashboardService.java
    │   │   │   │   # getOverview(): DashboardOverviewResponse
    │   │   │   │   # getTopPlayers(int limit): List<PlayerProfileResponse>
    │   │   │   │   # getActionsChart(int days): List<DailyCount>
    │   │   │   │
    │   │   │   └── CooldownService.java
    │   │   │       # validate(Player, ActionDefinition): void
    │   │   │       #   → Busca último ActionLog do player para essa ação.
    │   │   │       #   Calcula diferença em segundos.
    │   │   │       #   Se < cooldownSeconds: lança CooldownActiveException
    │   │   │       #   com secondsRemaining.
    │   │   │       #   Se cooldownSeconds == 0: sempre permite.
    │   │   │
    │   │   │
    │   │   │  ── ENGINE ────────────────────────────────────────────────
    │   │   │  Implementação do padrão Strategy para avaliação de
    │   │   │  critérios de conquistas. Cada tipo de critério tem
    │   │   │  seu próprio evaluator. Novos tipos são adicionados
    │   │   │  criando uma nova classe, sem alterar código existente.
    │   │   │
    │   │   ├── engine/
    │   │   │   ├── AchievementCriteriaEvaluator.java
    │   │   │   │   # Interface com dois métodos:
    │   │   │   │   # getType(): CriteriaType
    │   │   │   │   # evaluate(Player, Achievement, Long tenantId): boolean
    │   │   │   │
    │   │   │   ├── ActionCountEvaluator.java
    │   │   │   │   # Implementa: CriteriaType.ACTION_COUNT
    │   │   │   │   # Conta quantas vezes o player executou uma ação
    │   │   │   │   # específica (via ActionLogRepository).
    │   │   │   │   # Compara com criteriaValue.count.
    │   │   │   │
    │   │   │   ├── StreakEvaluator.java
    │   │   │   │   # Implementa: CriteriaType.STREAK
    │   │   │   │   # Compara player.currentStreak com
    │   │   │   │   # criteriaValue.minStreak.
    │   │   │   │
    │   │   │   ├── LevelReachedEvaluator.java
    │   │   │   │   # Implementa: CriteriaType.LEVEL_REACHED
    │   │   │   │   # Compara player.currentLevel com
    │   │   │   │   # criteriaValue.level.
    │   │   │   │
    │   │   │   ├── TotalXpEvaluator.java
    │   │   │   │   # Implementa: CriteriaType.XP_TOTAL
    │   │   │   │   # Compara player.totalXp com
    │   │   │   │   # criteriaValue.minXp.
    │   │   │   │
    │   │   │   ├── MultiActionEvaluator.java
    │   │   │   │   # Implementa: CriteriaType.MULTI_ACTION
    │   │   │   │   # Verifica se o player executou TODAS as ações
    │   │   │   │   # listadas em criteriaValue.actionCodes.
    │   │   │   │   # Usa ActionLogRepository.findDistinctActionCodes.
    │   │   │   │
    │   │   │   └── EvaluatorFactory.java
    │   │   │       # Recebe List<AchievementCriteriaEvaluator> via
    │   │   │       # constructor injection (Spring injeta todos os
    │   │   │       # @Component que implementam a interface).
    │   │   │       # getEvaluator(CriteriaType): retorna o evaluator
    │   │   │       # correspondente ou lança exceção.
    │   │   │
    │   │   │
    │   │   │  ── CONTROLLER ────────────────────────────────────────────
    │   │   │  Endpoints REST. Validam entrada, delegam ao service,
    │   │   │  retornam ResponseEntity com status HTTP correto.
    │   │   │
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   │   # POST /api/v1/auth/register → 201
    │   │   │   │   # POST /api/v1/auth/login    → 200
    │   │   │   │
    │   │   │   ├── ApiKeyController.java
    │   │   │   │   # POST   /api/v1/api-keys     → 201
    │   │   │   │   # GET    /api/v1/api-keys      → 200
    │   │   │   │   # DELETE /api/v1/api-keys/{id} → 204
    │   │   │   │
    │   │   │   ├── ActionDefinitionController.java
    │   │   │   │   # POST   /api/v1/actions/definitions     → 201
    │   │   │   │   # GET    /api/v1/actions/definitions      → 200
    │   │   │   │   # PUT    /api/v1/actions/definitions/{id} → 200
    │   │   │   │   # DELETE /api/v1/actions/definitions/{id} → 204
    │   │   │   │
    │   │   │   ├── ActionController.java
    │   │   │   │   # POST /api/v1/actions → 200
    │   │   │   │   # Autenticado via API Key.
    │   │   │   │   # Endpoint principal da aplicação.
    │   │   │   │
    │   │   │   ├── LevelConfigController.java
    │   │   │   │   # POST /api/v1/levels → 201
    │   │   │   │   # GET  /api/v1/levels → 200
    │   │   │   │   # PUT  /api/v1/levels → 200
    │   │   │   │
    │   │   │   ├── AchievementController.java
    │   │   │   │   # POST   /api/v1/achievements     → 201
    │   │   │   │   # GET    /api/v1/achievements      → 200
    │   │   │   │   # PUT    /api/v1/achievements/{id} → 200
    │   │   │   │   # DELETE /api/v1/achievements/{id} → 204
    │   │   │   │
    │   │   │   ├── PlayerController.java
    │   │   │   │   # GET /api/v1/players/{externalId}              → 200
    │   │   │   │   # GET /api/v1/players/{externalId}/achievements → 200
    │   │   │   │   # GET /api/v1/players/{externalId}/timeline     → 200
    │   │   │   │   # GET /api/v1/players/{externalId}/stats        → 200
    │   │   │   │   # Autenticado via API Key.
    │   │   │   │
    │   │   │   ├── LeaderboardController.java
    │   │   │   │   # GET /api/v1/leaderboard         → 200
    │   │   │   │   # GET /api/v1/leaderboard/weekly  → 200
    │   │   │   │   # GET /api/v1/leaderboard/monthly → 200
    │   │   │   │   # Autenticado via API Key.
    │   │   │   │
    │   │   │   ├── WebhookController.java
    │   │   │   │   # POST   /api/v1/webhooks     → 201
    │   │   │   │   # GET    /api/v1/webhooks      → 200
    │   │   │   │   # PUT    /api/v1/webhooks/{id} → 200
    │   │   │   │   # DELETE /api/v1/webhooks/{id} → 204
    │   │   │   │
    │   │   │   └── DashboardController.java
    │   │   │       # GET /api/v1/dashboard/overview     → 200
    │   │   │       # GET /api/v1/dashboard/top-players  → 200
    │   │   │       # GET /api/v1/dashboard/actions-chart → 200
    │   │   │
    │   │   │
    │   │   │  ── EXCEPTION ─────────────────────────────────────────────
    │   │   │  Exceções customizadas e tratamento global centralizado.
    │   │   │  Toda exceção é convertida em ErrorResponse padronizado.
    │   │   │
    │   │   ├── exception/
    │   │   │   ├── GlobalExceptionHandler.java
    │   │   │   │   # @RestControllerAdvice
    │   │   │   │   # Trata: ResourceNotFoundException → 404
    │   │   │   │   # Trata: DuplicateResourceException → 409
    │   │   │   │   # Trata: BusinessException → 422
    │   │   │   │   # Trata: CooldownActiveException → 429
    │   │   │   │   # Trata: UnauthorizedException → 401
    │   │   │   │   # Trata: MethodArgumentNotValidException → 400
    │   │   │   │   # Trata: Exception genérica → 500
    │   │   │   │   # Todos retornam ErrorResponse com mesmo formato.
    │   │   │   │
    │   │   │   ├── ResourceNotFoundException.java   # HTTP 404
    │   │   │   ├── BusinessException.java           # HTTP 422
    │   │   │   ├── CooldownActiveException.java     # HTTP 429 + Retry-After
    │   │   │   ├── DuplicateResourceException.java  # HTTP 409
    │   │   │   └── UnauthorizedException.java       # HTTP 401
    │   │   │
    │   │   │
    │   │   │  ── MAPPER ────────────────────────────────────────────────
    │   │   │  Conversão entre entidades JPA e DTOs.
    │   │   │
    │   │   ├── mapper/
    │   │   │   ├── PlayerMapper.java
    │   │   │   ├── AchievementMapper.java
    │   │   │   ├── ActionDefinitionMapper.java
    │   │   │   ├── LeaderboardMapper.java
    │   │   │   └── WebhookMapper.java
    │   │   │
    │   │   │
    │   │   │  ── VALIDATION ────────────────────────────────────────────
    │   │   │  Validadores customizados do Bean Validation.
    │   │   │
    │   │   ├── validation/
    │   │   │   ├── ValidCriteriaValue.java
    │   │   │   │   # @interface customizada para o campo criteriaValue.
    │   │   │   │   # Valida campos obrigatórios conforme o criteriaType.
    │   │   │   │
    │   │   │   └── CriteriaValueValidator.java
    │   │   │       # Implementa ConstraintValidator<ValidCriteriaValue, ...>
    │   │   │       # STREAK → requer "minStreak"
    │   │   │       # ACTION_COUNT → requer "actionCode" e "count"
    │   │   │       # LEVEL_REACHED → requer "level"
    │   │   │       # XP_TOTAL → requer "minXp"
    │   │   │       # MULTI_ACTION → requer "actionCodes" (array)
    │   │   │
    │   │   │
    │   │   │  ── SCHEDULER ─────────────────────────────────────────────
    │   │   │  Tarefas agendadas que rodam automaticamente.
    │   │   │
    │   │   └── scheduler/
    │   │       ├── RankingRecalculationScheduler.java
    │   │       │   # @Scheduled(cron = "0 0 * * * *") → a cada hora
    │   │       │   # Recalcula rankings WEEKLY e MONTHLY para todos
    │   │       │   # os tenants. O GLOBAL é calculado em tempo real.
    │   │       │
    │   │       └── WebhookRetryScheduler.java
    │   │           # @Scheduled(fixedDelay = 60000) → a cada 1 minuto
    │   │           # Busca WebhookLogs com success=false e attemptCount < 3.
    │   │           # Retenta com backoff exponencial.
    │   │
    │   │
    │   └── resources/
    │       ├── application.yml          # Configurações gerais
    │       ├── application-dev.yml      # H2, ddl create-drop, log DEBUG
    │       ├── application-prod.yml     # PostgreSQL, ddl validate, log INFO
    │       ├── application-test.yml     # H2, ddl create-drop, log WARN
    │       └── db/migration/
    │           ├── V1__create_tenant_and_api_key_tables.sql
    │           ├── V2__create_player_and_action_tables.sql
    │           ├── V3__create_gamification_tables.sql
    │           └── V4__create_webhook_tables.sql
    │
    │
    └── test/
        └── java/com/gamifyapi/
            ├── GamifyApiApplicationTests.java
            ├── unit/
            │   ├── service/
            │   │   ├── GamificationServiceTest.java
            │   │   ├── XpServiceTest.java
            │   │   ├── LevelServiceTest.java
            │   │   ├── StreakServiceTest.java
            │   │   ├── AchievementEngineTest.java
            │   │   ├── CooldownServiceTest.java
            │   │   ├── RankingServiceTest.java
            │   │   └── WebhookServiceTest.java
            │   └── engine/
            │       ├── ActionCountEvaluatorTest.java
            │       ├── StreakEvaluatorTest.java
            │       ├── LevelReachedEvaluatorTest.java
            │       ├── TotalXpEvaluatorTest.java
            │       └── MultiActionEvaluatorTest.java
            ├── integration/
            │   ├── controller/
            │   │   ├── AuthControllerIntTest.java
            │   │   ├── ActionControllerIntTest.java
            │   │   ├── PlayerControllerIntTest.java
            │   │   └── LeaderboardControllerIntTest.java
            │   └── repository/
            │       ├── PlayerRepositoryTest.java
            │       ├── ActionLogRepositoryTest.java
            │       └── RankingEntryRepositoryTest.java
            └── util/
                ├── TestDataFactory.java
                └── JwtTestHelper.java
```

---

## Fluxo de uma Requisição (do HTTP ao banco)

> Exemplo: `POST /api/v1/actions`

```
1. REQUEST chega no servidor
   │
2. ├── ApiKeyAuthenticationFilter intercepta
   │   ├── Lê header X-API-Key
   │   ├── Faz SHA-256 da key
   │   ├── Busca em ApiKeyRepository.findByKeyHash()
   │   ├── Valida active == true
   │   ├── Seta tenant no TenantContext
   │   └── Seta Authentication no SecurityContext
   │
3. ├── ActionController.processAction() recebe o request
   │   ├── @Valid valida ProcessActionRequest
   │   └── Delega para GamificationService.processAction()
   │
4. ├── GamificationService.processAction() [⭐ @Transactional]
   │   │
   │   ├── 4.1 PlayerService.findOrCreate("user_42", "João")
   │   │   └── PlayerRepository.findByTenantIdAndExternalId()
   │   │       → se não existe: cria e salva
   │   │       → se existe: atualiza displayName se mudou
   │   │
   │   ├── 4.2 ActionDefinitionRepository.findByTenantIdAndCode()
   │   │   → se não existe: lança ResourceNotFoundException
   │   │
   │   ├── 4.3 CooldownService.validate(player, actionDef)
   │   │   └── ActionLogRepository.findLastByPlayerAndAction()
   │   │       → se cooldown ativo: lança CooldownActiveException
   │   │
   │   ├── 4.4 XpService.grant(player, actionDef)
   │   │   ├── player.addXp(50)
   │   │   ├── PlayerRepository.save(player)
   │   │   └── ActionLogRepository.save(new ActionLog(...))
   │   │
   │   ├── 4.5 LevelService.checkLevelUp(player)
   │   │   ├── LevelConfigRepository.findByTenantIdOrderByLevelAsc()
   │   │   ├── Calcula novo nível baseado no totalXp
   │   │   └── Se mudou: player.setCurrentLevel(newLevel)
   │   │
   │   ├── 4.6 StreakService.update(player)
   │   │   ├── Compara lastActivityDate com LocalDate.now()
   │   │   ├── Incrementa, mantém ou reseta streak
   │   │   └── Atualiza lastActivityDate
   │   │
   │   ├── 4.7 AchievementEngine.evaluate(player)
   │   │   ├── AchievementRepository.findByTenantIdAndActiveTrue()
   │   │   ├── PlayerAchievementRepository (filtra já desbloqueadas)
   │   │   ├── Para cada conquista pendente:
   │   │   │   ├── EvaluatorFactory.getEvaluator(criteriaType)
   │   │   │   └── evaluator.evaluate(player, achievement, tenantId)
   │   │   ├── Se desbloqueou: cria PlayerAchievement
   │   │   └── Se xpReward > 0: player.addXp() + re-checa level up
   │   │
   │   ├── 4.8 RankingService.updatePosition(player)
   │   │   └── PlayerRepository.findPlayerRank()
   │   │
   │   └── 4.9 WebhookService.notifyAsync(events) [@Async]
   │       ├── WebhookConfigRepository.findByTenantIdAndEventType()
   │       ├── Envia HTTP POST para cada URL
   │       └── Registra em WebhookLogRepository
   │
5. ├── GamificationService monta ActionResultResponse
   │
6. ├── ActionController retorna ResponseEntity.ok(response)
   │
7. └── RESPONSE 200 enviado ao cliente
```

---

## Dependências Maven (pom.xml)

| Dependência | Finalidade |
|-------------|------------|
| `spring-boot-starter-web` | API REST, Jackson, Tomcat embutido |
| `spring-boot-starter-data-jpa` | JPA, Hibernate, Spring Data |
| `spring-boot-starter-security` | Spring Security |
| `spring-boot-starter-validation` | Bean Validation (JSR 380) |
| `jjwt-api` + `jjwt-impl` + `jjwt-jackson` | Geração e validação de JWT |
| `postgresql` | Driver PostgreSQL (runtime) |
| `h2` | Banco em memória (test/dev) |
| `springdoc-openapi-starter-webmvc-ui` | Swagger UI + OpenAPI 3 |
| `lombok` | Redução de boilerplate |
| `flyway-core` | Migrações de banco versionadas |
| `spring-boot-starter-test` | JUnit 5, Mockito, MockMvc |

---

## Profiles do Spring

| Aspecto | `dev` | `test` | `prod` |
|---------|-------|--------|--------|
| Banco | H2 memory | H2 memory | PostgreSQL |
| DDL | `create-drop` | `create-drop` | `validate` |
| SQL log | `true` | `false` | `false` |
| Log level | `DEBUG` | `WARN` | `INFO` |
| Flyway | desabilitado | desabilitado | habilitado |
| Swagger | habilitado | N/A | configurável |