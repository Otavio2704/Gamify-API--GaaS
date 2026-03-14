# GamifyAPI — Contratos da API

Base URL: `/api/v1`

## Autenticação

### Rotas administrativas (JWT)

- Header: `Authorization: Bearer <token>`
- Obtido via `POST /api/v1/auth/login`

### Rotas de integração (API Key)

- Header: `X-API-Key: gapi_a1b2c3d4e5f6...`
- Obtida pelo Tenant Admin no painel

---

## AUTH (público)

### POST /auth/register

Cria conta de tenant.

**Request:**

```json
{
  "name": "MeuApp Educação",
  "email": "admin@meuapp.com",
  "password": "senhaForte123!"
}
```

**Response 201:**

```json
{
  "id": 1,
  "name": "MeuApp Educação",
  "email": "admin@meuapp.com",
  "plan": "FREE",
  "createdAt": "2025-01-15T10:00:00Z"
}
```

**Erros:**

- `409` — e-mail já cadastrado

---

### POST /auth/login

**Request:**

```json
{
  "email": "admin@meuapp.com",
  "password": "senhaForte123!"
}
```

**Response 200:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresIn": 86400,
  "tenant": {
    "id": 1,
    "name": "MeuApp Educação"
  }
}
```

**Erros:**

- `401` — credenciais inválidas

---

## API KEYS (JWT)

### POST /api-keys

**Request:**

```json
{
  "label": "Produção"
}
```

**Response 201:**

```json
{
  "id": 1,
  "key": "gapi_a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "prefix": "gapi_a1b2",
  "label": "Produção",
  "createdAt": "2025-01-15T10:05:00Z"
}
```

> ⚠️ O campo `key` só é retornado na criação. Depois disso, apenas o `prefix` é visível.

---

### GET /api-keys

**Response 200:**

```json
[
  {
    "id": 1,
    "prefix": "gapi_a1b2",
    "label": "Produção",
    "active": true,
    "createdAt": "2025-01-15T10:05:00Z"
  }
]
```

---

### DELETE /api-keys/{id}

**Response:** `204 No Content`

---

## ACTION DEFINITIONS (JWT)

### POST /actions/definitions

**Request:**

```json
{
  "code": "completed_lesson",
  "displayName": "Aula Completada",
  "description": "Player completou uma aula no curso",
  "xpValue": 50,
  "cooldownSeconds": 60
}
```

**Response 201:**

```json
{
  "id": 1,
  "code": "completed_lesson",
  "displayName": "Aula Completada",
  "description": "Player completou uma aula no curso",
  "xpValue": 50,
  "cooldownSeconds": 60,
  "active": true
}
```

**Erros:**

- `409` — código já existe para este tenant

---

### GET /actions/definitions

**Response 200:** lista de `ActionDefinition` do tenant

---

### PUT /actions/definitions/{id}

Mesmo body do POST. **Response 200** com objeto atualizado.

---

### DELETE /actions/definitions/{id}

**Response:** `204 No Content`

---

## LEVEL CONFIG (JWT)

### POST /levels

**Request:**

```json
{
  "levels": [
    { "level": 1,  "xpRequired": 0,    "title": "Novato" },
    { "level": 2,  "xpRequired": 100,  "title": "Aprendiz" },
    { "level": 3,  "xpRequired": 300,  "title": "Intermediário" },
    { "level": 4,  "xpRequired": 600,  "title": "Avançado" },
    { "level": 5,  "xpRequired": 1000, "title": "Expert" },
    { "level": 6,  "xpRequired": 1500, "title": "Mestre" },
    { "level": 7,  "xpRequired": 2100, "title": "Grão-Mestre" },
    { "level": 8,  "xpRequired": 2800, "title": "Veterano" },
    { "level": 9,  "xpRequired": 3600, "title": "Elite" },
    { "level": 10, "xpRequired": 4500, "title": "Lendário" }
  ]
}
```

**Response 201:** mesma lista com IDs

> O `POST` substitui toda a tabela de níveis do tenant. Deve ter pelo menos o nível `1` com `xpRequired = 0`.

---

### GET /levels

**Response 200:** lista de `LevelConfig` do tenant

---

### PUT /levels

Mesmo comportamento do POST (replace all). **Response 200.**

---

## ACHIEVEMENTS (JWT)

### POST /achievements

**Request:**

```json
{
  "code": "MARATHONER",
  "name": "Maratonista",
  "description": "Manteve atividade por 7 dias consecutivos",
  "badgeImageUrl": "https://cdn.example.com/badges/marathoner.png",
  "xpReward": 200,
  "criteriaType": "STREAK",
  "criteriaValue": {
    "minStreak": 7
  }
}
```

**Exemplos de `criteriaValue` por `criteriaType`:**

```json
// ACTION_COUNT
{ "actionCode": "completed_lesson", "count": 50 }

// STREAK
{ "minStreak": 7 }

// LEVEL_REACHED
{ "level": 10 }

// XP_TOTAL
{ "minXp": 5000 }

// MULTI_ACTION
{ "actionCodes": ["completed_lesson", "passed_quiz", "submitted_project"] }
```

**Response 201:** objeto com ID

---

### GET /achievements
### PUT /achievements/{id}
### DELETE /achievements/{id}

---

## WEBHOOKS (JWT)

### POST /webhooks

**Request:**

```json
{
  "url": "https://meuapp.com/webhooks/gamify",
  "eventType": "LEVEL_UP",
  "secretKey": "minha_chave_secreta_123"
}
```

**`eventType` possíveis:** `LEVEL_UP`, `ACHIEVEMENT_UNLOCKED`, `STREAK_MILESTONE`, `RANK_CHANGED`

**Response 201**

---

### GET /webhooks
### PUT /webhooks/{id}
### DELETE /webhooks/{id}

---

## PROCESS ACTION (API Key) ⭐ Endpoint principal

### POST /actions

**Header:** `X-API-Key: gapi_a1b2c3d4...`

**Request:**

```json
{
  "playerId": "user_42",
  "playerName": "João Silva",
  "actionCode": "completed_lesson"
}
```

**Response 200:**

```json
{
  "playerId": "user_42",
  "action": "completed_lesson",
  "xpGranted": 50,
  "totalXp": 1350,
  "currentLevel": 8,
  "levelUp": {
    "happened": true,
    "previousLevel": 7,
    "newLevel": 8,
    "title": "Veterano"
  },
  "streak": {
    "currentStreak": 12,
    "longestStreak": 15,
    "wasReset": false
  },
  "newAchievements": [
    {
      "code": "DEDICATED_LEARNER",
      "name": "Aprendiz Dedicado",
      "description": "Completou 50 aulas",
      "badgeImageUrl": "https://cdn.example.com/badges/dedicated.png",
      "xpBonus": 200
    }
  ],
  "leaderboardPosition": 23,
  "processedAt": "2025-01-15T14:32:10Z"
}
```

**Erros:**

- `404` — `actionCode` não encontrado para este tenant
- `429` — cooldown ativo (inclui header `Retry-After`)

---

## PLAYERS (API Key)

### GET /players/{externalId}

**Response 200:**

```json
{
  "externalId": "user_42",
  "displayName": "João Silva",
  "totalXp": 1350,
  "currentLevel": 8,
  "levelTitle": "Veterano",
  "xpToNextLevel": 150,
  "progressPercent": 62.5,
  "currentStreak": 12,
  "longestStreak": 15,
  "totalActions": 87,
  "achievementsUnlocked": 5,
  "leaderboardPosition": 23,
  "memberSince": "2024-11-20T08:30:00Z"
}
```

---

### GET /players/{externalId}/achievements

**Response 200:**

```json
{
  "unlocked": [
    {
      "code": "MARATHONER",
      "name": "Maratonista",
      "description": "7 dias seguidos",
      "badgeImageUrl": "...",
      "unlockedAt": "2025-01-10T16:20:00Z"
    }
  ],
  "locked": [
    {
      "code": "LEGENDARY",
      "name": "Lendário",
      "description": "Atingir nível 10",
      "badgeImageUrl": "...",
      "progressDescription": "Nível atual: 8 / Necessário: 10"
    }
  ]
}
```

---

### GET /players/{externalId}/timeline?page=0&size=20

**Response 200:**

```json
{
  "content": [
    {
      "type": "ACTION",
      "description": "Completou aula",
      "xp": 50,
      "timestamp": "2025-01-15T14:32:10Z"
    },
    {
      "type": "LEVEL_UP",
      "description": "Subiu para nível 8 — Veterano",
      "xp": null,
      "timestamp": "2025-01-15T14:32:10Z"
    },
    {
      "type": "ACHIEVEMENT",
      "description": "Desbloqueou: Aprendiz Dedicado",
      "xp": 200,
      "timestamp": "2025-01-15T14:32:10Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 142,
  "totalPages": 8
}
```

---

### GET /players/{externalId}/stats

**Response 200:**

```json
{
  "totalActions": 87,
  "actionBreakdown": {
    "completed_lesson": 52,
    "passed_quiz": 20,
    "submitted_project": 15
  },
  "xpBreakdown": {
    "fromActions": 3100,
    "fromAchievements": 600,
    "total": 3700
  },
  "averageActionsPerDay": 4.2,
  "mostActiveDay": "WEDNESDAY",
  "mostActiveHour": 14,
  "daysActive": 21,
  "currentStreak": 12,
  "longestStreak": 15
}
```

---

## LEADERBOARD (API Key)

### GET /leaderboard?page=0&size=10

**Response 200:**

```json
{
  "period": "GLOBAL",
  "entries": [
    {
      "position": 1,
      "externalId": "user_99",
      "displayName": "Maria Souza",
      "totalXp": 8200,
      "level": 10,
      "levelTitle": "Lendário"
    },
    {
      "position": 2,
      "externalId": "user_42",
      "displayName": "João Silva",
      "totalXp": 3700,
      "level": 8,
      "levelTitle": "Veterano"
    }
  ],
  "page": 0,
  "size": 10,
  "totalPlayers": 156
}
```

---

### GET /leaderboard/weekly?page=0&size=10
### GET /leaderboard/monthly?page=0&size=10

Mesma estrutura. O campo `period` muda e `totalXp` reflete apenas o período selecionado.

---

## DASHBOARD (JWT)

### GET /dashboard/overview

**Response 200:**

```json
{
  "totalPlayers": 156,
  "activePlayers7d": 89,
  "totalActionsAllTime": 12450,
  "totalActions7d": 1830,
  "averageLevel": 4.2,
  "achievementsUnlockedTotal": 340,
  "topAction": {
    "code": "completed_lesson",
    "count": 6200
  }
}
```

---

### GET /dashboard/top-players?limit=5

**Response 200:** lista dos top players com XP e nível

---

### GET /dashboard/actions-chart?days=30

**Response 200:**

```json
{
  "data": [
    { "date": "2024-12-16", "count": 45 },
    { "date": "2024-12-17", "count": 62 },
    { "date": "2024-12-18", "count": 38 }
  ]
}
```