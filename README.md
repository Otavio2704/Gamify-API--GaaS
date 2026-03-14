# GamifyAPI 🎮

> **Gamification as a Service (GaaS)** — REST API multi-tenant para integrar XP, níveis, conquistas, streaks e rankings em qualquer aplicação.

---

## Sobre o projeto

A GamifyAPI é uma plataforma de gamificação pronta para ser integrada em apps, plataformas educacionais, sistemas de fidelidade ou qualquer produto que queira engajar usuários via mecânicas de jogo.

Cada empresa que se cadastra recebe um **tenant isolado** com sua própria configuração de ações, conquistas, níveis e webhooks. A integração do produto final é feita por **API Key**, sem expor credenciais de admin.

---

## Funcionalidades

- **XP e Progressão** — ações concedем XP configurável; o motor de níveis é customizável por tenant
- **Conquistas (Badges)** — 5 tipos de critério: `ACTION_COUNT`, `STREAK`, `LEVEL_REACHED`, `XP_TOTAL`, `MULTI_ACTION`
- **Streaks** — contagem de dias consecutivos com reset automático e rastreamento do recorde
- **Leaderboard** — rankings global, semanal e mensal com paginação
- **Webhooks assíncronos** — notificações para `LEVEL_UP`, `ACHIEVEMENT_UNLOCKED`, `STREAK_MILESTONE`, `RANK_CHANGED` com 3 tentativas e backoff exponencial
- **Dual Auth** — JWT para admins do tenant + API Key para integração dos produtos
- **Multi-tenant** — isolamento completo por ThreadLocal; dados nunca se cruzam entre tenants
- **Dashboard** — métricas agregadas e gráfico de ações por dia

---

## Stack

| Camada | Tecnologia |
|--------|-----------|
| Linguagem | Java 17 |
| Framework | Spring Boot 3.2 |
| Segurança | Spring Security + JJWT 0.12 |
| Persistência | Spring Data JPA + PostgreSQL |
| Documentação | SpringDoc OpenAPI 3 (Swagger UI) |
| Testes | JUnit 5 + Mockito + AssertJ |
| Build | Maven 3.9 |
| Container | Docker + Docker Compose |

---

## Arquitetura

```
┌─────────────────────────────────────────────────────┐
│                   GamifyAPI                         │
│                                                     │
│  Controller → Service → Repository → PostgreSQL     │
│                                                     │
│  ┌─────────────────────────────────────┐            │
│  │         GamificationService         │            │
│  │  (orquestrador principal)           │            │
│  │                                     │            │
│  │  PlayerService → XpService          │            │
│  │  LevelService  → StreakService      │            │
│  │  AchievementEngine (Strategy)       │            │
│  │  RankingService → WebhookService    │            │
│  └─────────────────────────────────────┘            │
│                                                     │
│  Segurança:                                         │
│  ApiKeyFilter → JwtFilter → SecurityContext         │
│  TenantContext (ThreadLocal)                        │
└─────────────────────────────────────────────────────┘
```

O **AchievementEngine** usa o padrão **Strategy** — cada `CriteriaType` tem seu próprio `AchievementCriteriaEvaluator`, desacoplado via `EvaluatorFactory`.

---

## Endpoints

### Autenticação (JWT)
| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/auth/register` | Cadastra novo tenant |
| `POST` | `/api/v1/auth/login` | Autentica e retorna JWT |

### Integração (API Key — `X-API-Key`)
| Método | Rota | Descrição |
|--------|------|-----------|
| `POST` | `/api/v1/actions` | **Endpoint principal** — processa ação do player |
| `GET` | `/api/v1/players/{id}` | Perfil completo do player |
| `GET` | `/api/v1/players/{id}/achievements` | Conquistas desbloqueadas e bloqueadas |
| `GET` | `/api/v1/players/{id}/timeline` | Histórico de ações (paginado) |
| `GET` | `/api/v1/leaderboard` | Ranking global |
| `GET` | `/api/v1/leaderboard/weekly` | Ranking semanal |
| `GET` | `/api/v1/leaderboard/monthly` | Ranking mensal |

### Administração (JWT)
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

- Java 17+
- Maven 3.9+
- Docker (opcional, para banco local)

### Modo dev (H2 em memória)

```bash
# Clone o repositório
git clone https://github.com/seu-usuario/gamify-api.git
cd gamify-api

# Sobe a aplicação com perfil dev (H2)
./mvnw spring-boot:run
```

A aplicação sobe em `http://localhost:8080`. O banco H2 é criado em memória automaticamente.

### Com Docker Compose (banco local PostgreSQL)

```bash
# Copia o arquivo de variáveis
cp .env.example .env
# Edite o .env com as credenciais do seu banco externo (se for usar prod)

# Sobe com banco PostgreSQL local
docker compose --profile local-db up --build
```

### Produção (banco externo)

```bash
# Configure as variáveis de ambiente
cp .env.example .env
# Edite .env com DB_URL, DB_USERNAME, DB_PASSWORD e JWT_SECRET

# Sobe apenas a aplicação (banco externo)
docker compose up --build
```

---

## Variáveis de ambiente

| Variável | Descrição | Obrigatório em prod |
|----------|-----------|---------------------|
| `DB_URL` | JDBC URL do banco PostgreSQL | ✅ |
| `DB_USERNAME` | Usuário do banco | ✅ |
| `DB_PASSWORD` | Senha do banco | ✅ |
| `JWT_SECRET` | Chave secreta JWT (mín. 256 bits) | ✅ |
| `JWT_EXPIRATION_MS` | Expiração do token em ms (padrão: 86400000) | ❌ |

---

## Documentação interativa

Com a aplicação rodando:

- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON:** `http://localhost:8080/v3/api-docs`
- **H2 Console (dev):** `http://localhost:8080/h2-console`

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

### 3. Criar uma definição de ação (autenticado com JWT)
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

### 5. Processar ação do usuário (via API Key)
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

| Tipo | Estrutura do `criteriaValue` | Descrição |
|------|------------------------------|-----------|
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

# Roda apenas testes de um pacote
./mvnw test -Dtest="com.gamifyapi.unit.*"
```

A suíte cobre os serviços críticos com testes unitários via Mockito:

- `GamificationServiceTest` — orquestração do fluxo principal
- `AchievementEngineTest` — avaliação e desbloqueio de conquistas
- `LevelServiceTest` — cálculo de nível e XP
- `StreakServiceTest` — regras de streak
- `CooldownServiceTest` — validação de cooldown

---

## Segurança

- **Senhas** armazenadas com BCrypt
- **API Keys** nunca persistidas — apenas o hash SHA-256 fica no banco
- **JWT** com `sub` = tenant ID, assinado com HMAC-SHA256
- **Webhooks** assinados com HMAC-SHA256 no header `X-Gamify-Signature`
- **Multi-tenancy** garantido por ThreadLocal — nenhuma query acessa dados de outro tenant

---

## Estrutura do projeto

```
src/
├── main/java/com/gamifyapi/
│   ├── achievement/          # Strategy pattern — engine de conquistas
│   ├── config/               # Spring Security, Async, Jackson, OpenAPI
│   ├── controller/           # REST controllers
│   ├── dto/                  # Request e Response records
│   ├── entity/               # Entidades JPA
│   ├── enums/                # CriteriaType, RankingPeriod, etc.
│   ├── exception/            # Exceções de domínio + GlobalExceptionHandler
│   ├── repository/           # Interfaces JPA + queries customizadas
│   ├── security/             # JWT, ApiKey, TenantContext
│   └── service/              # Lógica de negócio
└── test/java/com/gamifyapi/
    ├── unit/                 # Testes unitários por serviço
    └── util/                 # TestDataFactory
```

---

## Licença

Este projeto está sob a licença MIT.
