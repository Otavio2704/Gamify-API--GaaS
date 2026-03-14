# GamifyAPI

API REST de Gamificacao como Servico (GaaS), multi-tenant, para adicionar XP, niveis, conquistas, rankings e streaks em aplicacoes externas.

## Sumario

- Visao Geral
- Principais Funcionalidades
- Stack Tecnologica
- Arquitetura
- Estrutura do Projeto
- Como Executar
- Variaveis de Ambiente
- Documentacao da API
- Autenticacao e Seguranca
- Endpoints Principais
- Exemplo de Fluxo de Integracao
- Formato Padrao de Erro
- Testes
- Perfis Spring
- Docker
- Documentacao Complementar

## Visao Geral

A GamifyAPI foi criada para funcionar como camada de gamificacao desacoplada do produto cliente. Em vez de implementar regras de XP e conquistas em cada sistema, a aplicacao cliente envia eventos de acao para esta API e recebe o estado atualizado do player.

A plataforma oferece:

- multi-tenancy por tenant (empresa/produto)
- autenticacao administrativa com JWT
- autenticacao de integracao com API Key
- processamento de acao com cooldown, XP, level up, streak, conquistas e ranking
- webhooks assinados para eventos de gamificacao

## Principais Funcionalidades

- Cadastro e login de tenant admin
- Gerenciamento de API Keys por tenant
- CRUD de definicoes de acoes (codigo, XP, cooldown)
- Configuracao de niveis e progressao
- CRUD de conquistas com criterios dinamicos
- Processamento de acao em endpoint unico de integracao
- Consultas de perfil do player, conquistas e timeline
- Leaderboard global, semanal e mensal
- Dashboard com metricas agregadas
- Webhooks assinados com retentativa assincrona

## Stack Tecnologica

- Java 17
- Spring Boot 3.2.x
- Maven
- Spring Data JPA / Hibernate
- Spring Security (JWT + API Key)
- PostgreSQL (producao)
- H2 (desenvolvimento e testes)
- JUnit 5 / Mockito
- SpringDoc OpenAPI 3 (Swagger UI)
- Bean Validation

## Arquitetura

Arquitetura em camadas:

- Security Layer: filtros de JWT e API Key + TenantContext
- Controller Layer: validacao de entrada e exposicao HTTP
- Service Layer: regras de negocio e orquestracao
- Repository Layer: acesso a dados com Spring Data
- Database Layer: PostgreSQL/H2

Fluxo principal de processamento de acao:

1. Recebe evento no endpoint de acoes.
2. Resolve/valida tenant e player.
3. Verifica cooldown da acao.
4. Concede XP.
5. Calcula level up (inclusive multiplo).
6. Atualiza streak.
7. Avalia e desbloqueia conquistas.
8. Atualiza ranking.
9. Dispara webhooks assinados de forma assincrona.

## Estrutura do Projeto

```text
gamify-api/
|- src/main/java/com/gamifyapi/
|  |- achievement/      # Engine de conquistas e evaluators
|  |- config/           # Configuracoes Spring (Security, OpenAPI, etc.)
|  |- controller/       # Endpoints REST
|  |- dto/              # DTOs de request/response
|  |- entity/           # Entidades JPA
|  |- enums/            # Enumeracoes de dominio
|  |- exception/        # Excecoes customizadas e handler global
|  |- repository/       # Repositorios Spring Data
|  |- security/         # JWT, API Key, TenantContext e filtros
|  |- service/          # Regras de negocio
|- src/main/resources/
|  |- application.yml   # Perfis dev/test/prod
|- src/test/java/com/gamifyapi/
|  |- unit/             # Testes unitarios
|  |- util/             # Fabrica de dados auxiliares
|- docker-compose.yml
|- Dockerfile
|- .env.example
|- pom.xml
```

## Como Executar

### Pre-requisitos

- Java 17+
- Maven 3.9+
- Docker e Docker Compose (opcional)

### 1) Rodar localmente em desenvolvimento (H2)

```bash
mvn spring-boot:run
```

Como o perfil dev e o padrao, a aplicacao sobe com H2 em memoria.

URLs uteis:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- H2 Console (dev): http://localhost:8080/h2-console

### 2) Rodar testes

```bash
mvn test
```

### 3) Build do artefato

```bash
mvn clean package
```

### 4) Rodar com Docker Compose (app em prod + banco externo via env)

```bash
cp .env.example .env
# edite o .env com os dados reais do seu banco

docker compose up --build
```

### 5) Rodar PostgreSQL local auxiliar (perfil local-db)

```bash
docker compose --profile local-db up --build
```

Esse perfil sobe um PostgreSQL local para apoio em desenvolvimento.

## Variaveis de Ambiente

Arquivo base: .env.example

| Variavel | Obrigatoria | Descricao |
|---|---|---|
| DB_URL | Sim (prod) | URL JDBC do PostgreSQL |
| DB_USERNAME | Sim (prod) | Usuario do banco |
| DB_PASSWORD | Sim (prod) | Senha do banco |
| JWT_SECRET | Sim (prod) | Segredo JWT (minimo recomendado: 256 bits de entropia) |
| JWT_EXPIRATION_MS | Nao | Expiracao do token em ms (padrao 86400000) |
| PORT | Nao | Porta HTTP da aplicacao (padrao 8080) |

## Documentacao da API

Documentacao interativa:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Autenticacao e Seguranca

A API possui dois modos de autenticacao:

- JWT (rotas administrativas do tenant)
- API Key (rotas de integracao)

Headers:

- JWT: Authorization: Bearer <token>
- API Key: X-API-Key: <sua-chave>

Regras importantes:

- API Keys sao armazenadas com hash SHA-256
- toda consulta e filtrada por tenant
- TenantContext (ThreadLocal) define o tenant atual da requisicao

## Endpoints Principais

Base path: /api/v1

### Publicos

- POST /auth/register
- POST /auth/login

### Admin (JWT)

- POST /api-keys
- GET /api-keys
- DELETE /api-keys/{id}

- POST /actions/definitions
- GET /actions/definitions
- PUT /actions/definitions/{id}
- DELETE /actions/definitions/{id}

- POST /levels
- GET /levels

- POST /achievements
- GET /achievements
- PUT /achievements/{id}
- DELETE /achievements/{id}

- POST /webhooks
- GET /webhooks
- PUT /webhooks/{id}
- DELETE /webhooks/{id}

- GET /dashboard/overview
- GET /dashboard/actions-chart?dias=30

### Integracao (API Key)

- POST /actions
- GET /players/{externalId}
- GET /players/{externalId}/achievements
- GET /players/{externalId}/timeline?page=0&size=20
- GET /leaderboard?page=0&size=20
- GET /leaderboard/weekly?page=0&size=20
- GET /leaderboard/monthly?page=0&size=20

## Exemplo de Fluxo de Integracao

### 1) Registrar tenant

```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name":"Meu App",
    "email":"admin@meuapp.com",
    "password":"senhaForte123"
  }'
```

### 2) Fazer login e obter JWT

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email":"admin@meuapp.com",
    "password":"senhaForte123"
  }'
```

Resposta esperada (resumo):

```json
{
  "token": "<jwt>",
  "expiresIn": 86400000,
  "tenant": { "id": 1, "name": "Meu App" }
}
```

### 3) Criar API Key

```bash
curl -X POST http://localhost:8080/api/v1/api-keys \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{"label":"producao"}'
```

Guarde o campo key retornado na criacao.

### 4) Criar definicao de acao

```bash
curl -X POST http://localhost:8080/api/v1/actions/definitions \
  -H "Authorization: Bearer <jwt>" \
  -H "Content-Type: application/json" \
  -d '{
    "code":"completed_lesson",
    "displayName":"Aula completada",
    "description":"Player concluiu uma aula",
    "xpValue":50,
    "cooldownSeconds":60
  }'
```

### 5) Processar acao do player

```bash
curl -X POST http://localhost:8080/api/v1/actions \
  -H "X-API-Key: <api_key>" \
  -H "Content-Type: application/json" \
  -d '{
    "playerId":"player-123",
    "playerName":"Otavio",
    "actionCode":"completed_lesson"
  }'
```

Resposta esperada (resumo):

```json
{
  "playerId": "player-123",
  "action": "completed_lesson",
  "xpGranted": 50,
  "totalXp": 50,
  "currentLevel": 1,
  "levelUp": { "happened": false },
  "streak": { "currentStreak": 1, "longestStreak": 1, "wasReset": false },
  "newAchievements": [],
  "leaderboardPosition": 1,
  "processedAt": "2026-03-14T12:00:00Z"
}
```

## Formato Padrao de Erro

A API usa um contrato padrao de erro:

```json
{
  "status": 422,
  "message": "Regra de negocio violada",
  "timestamp": "2026-03-14T12:00:00Z",
  "details": []
}
```

Mapeamentos principais:

- 400: validacao de entrada
- 401: autenticacao invalida
- 404: recurso nao encontrado
- 409: conflito/duplicidade
- 422: regra de negocio
- 429: cooldown ativo (com header Retry-After)
- 500: erro interno

## Testes

Testes unitarios existentes cobrem engine e servicos centrais.

Executar:

```bash
mvn test
```

Pasta principal de testes:

- src/test/java/com/gamifyapi/unit

## Perfis Spring

- dev (padrao): H2 em memoria, SQL logado, console H2 habilitado
- test: H2 em memoria para execucao de testes
- prod: PostgreSQL, configuracao via variaveis de ambiente

Forcar perfil manualmente:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

## Docker

### Build da imagem

```bash
docker build -t gamify-api:local .
```

### Rodar container diretamente

```bash
docker run --rm -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e DB_URL="jdbc:postgresql://host:5432/gamifyapi" \
  -e DB_USERNAME="gamify" \
  -e DB_PASSWORD="segredo" \
  -e JWT_SECRET="secret-forte-com-minimo-256-bits" \
  gamify-api:local
```

## Documentacao Complementar

Para aprofundar, consulte:

- .github/docs/project-context.md
- .github/docs/architecture.md
- .github/docs/coding-standarts.md
- .github/docs/api-contracts.md
- .github/docs/datebase-schema.md
- .github/docs/business-rules.md
- .github/docs/testing-guide.md

## Licenca

Defina aqui a licenca oficial do projeto (ex.: MIT, Apache-2.0, proprietaria).
