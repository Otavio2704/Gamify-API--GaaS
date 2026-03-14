# Schema do Banco de Dados — GamifyAPI

## Diagrama de Relacionamentos

```
tenants
│
├── 1:N → api_keys
├── 1:N → players
│         ├── 1:N → action_logs
│         └── N:N → achievements (via player_achievements)
├── 1:N → action_definitions
│         └── 1:N → action_logs
├── 1:N → level_configs
├── 1:N → achievements
├── 1:N → webhook_configs
│         └── 1:N → webhook_logs
└── 1:N → ranking_entries
```

---

## Tabelas

### tenants

| Coluna        | Tipo         | Constraints              |
|---------------|--------------|--------------------------|
| id            | BIGINT       | PK, AUTO_INCREMENT       |
| name          | VARCHAR(100) | NOT NULL                 |
| email         | VARCHAR(150) | NOT NULL, UNIQUE         |
| password_hash | VARCHAR(255) | NOT NULL                 |
| plan          | VARCHAR(20)  | NOT NULL, DEFAULT 'FREE' |
| created_at    | TIMESTAMP    | NOT NULL                 |
| updated_at    | TIMESTAMP    | NULLABLE                 |

---

### api_keys

| Coluna     | Tipo         | Constraints             |
|------------|--------------|-------------------------|
| id         | BIGINT       | PK, AUTO_INCREMENT      |
| tenant_id  | BIGINT       | FK → tenants, NOT NULL  |
| key_hash   | VARCHAR(255) | NOT NULL, UNIQUE        |
| prefix     | VARCHAR(20)  | NOT NULL                |
| label      | VARCHAR(50)  | NOT NULL                |
| active     | BOOLEAN      | NOT NULL, DEFAULT TRUE  |
| created_at | TIMESTAMP    | NOT NULL                |

> Índice: `idx_api_keys_key_hash` em `key_hash` (busca na autenticação)

---

### players

| Coluna             | Tipo         | Constraints             |
|--------------------|--------------|-------------------------|
| id                 | BIGINT       | PK, AUTO_INCREMENT      |
| tenant_id          | BIGINT       | FK → tenants, NOT NULL  |
| external_id        | VARCHAR(100) | NOT NULL                |
| display_name       | VARCHAR(100) | NULLABLE                |
| total_xp           | INT          | NOT NULL, DEFAULT 0     |
| current_level      | INT          | NOT NULL, DEFAULT 1     |
| current_streak     | INT          | NOT NULL, DEFAULT 0     |
| longest_streak     | INT          | NOT NULL, DEFAULT 0     |
| last_activity_date | DATE         | NULLABLE                |
| created_at         | TIMESTAMP    | NOT NULL                |
| updated_at         | TIMESTAMP    | NULLABLE                |

> Índice UNIQUE: `uk_player_tenant_external` em `(tenant_id, external_id)`  
> Índice: `idx_player_xp` em `(tenant_id, total_xp DESC)` (para leaderboard)

---

### action_definitions

| Coluna           | Tipo         | Constraints             |
|------------------|--------------|-------------------------|
| id               | BIGINT       | PK, AUTO_INCREMENT      |
| tenant_id        | BIGINT       | FK → tenants, NOT NULL  |
| code             | VARCHAR(50)  | NOT NULL                |
| display_name     | VARCHAR(100) | NOT NULL                |
| description      | VARCHAR(255) | NULLABLE                |
| xp_value         | INT          | NOT NULL                |
| cooldown_seconds | INT          | NOT NULL, DEFAULT 0     |
| active           | BOOLEAN      | NOT NULL, DEFAULT TRUE  |
| created_at       | TIMESTAMP    | NOT NULL                |

> Índice UNIQUE: `uk_action_tenant_code` em `(tenant_id, code)`

---

### action_logs

| Coluna               | Tipo      | Constraints                       |
|----------------------|-----------|-----------------------------------|
| id                   | BIGINT    | PK, AUTO_INCREMENT                |
| player_id            | BIGINT    | FK → players, NOT NULL            |
| action_definition_id | BIGINT    | FK → action_definitions, NOT NULL |
| xp_granted           | INT       | NOT NULL                          |
| timestamp            | TIMESTAMP | NOT NULL                          |

> Índice: `idx_action_log_player_time` em `(player_id, timestamp DESC)`  
> Índice: `idx_action_log_player_action` em `(player_id, action_definition_id)`

---

### level_configs

| Coluna      | Tipo        | Constraints             |
|-------------|-------------|-------------------------|
| id          | BIGINT      | PK, AUTO_INCREMENT      |
| tenant_id   | BIGINT      | FK → tenants, NOT NULL  |
| level       | INT         | NOT NULL                |
| xp_required | INT         | NOT NULL                |
| title       | VARCHAR(50) | NULLABLE                |

> Índice UNIQUE: `uk_level_tenant_level` em `(tenant_id, level)`

---

### achievements

| Coluna          | Tipo         | Constraints             |
|-----------------|--------------|-------------------------|
| id              | BIGINT       | PK, AUTO_INCREMENT      |
| tenant_id       | BIGINT       | FK → tenants, NOT NULL  |
| code            | VARCHAR(50)  | NOT NULL                |
| name            | VARCHAR(100) | NOT NULL                |
| description     | VARCHAR(255) | NULLABLE                |
| badge_image_url | VARCHAR(500) | NULLABLE                |
| xp_reward       | INT          | NOT NULL, DEFAULT 0     |
| criteria_type   | VARCHAR(30)  | NOT NULL                |
| criteria_value  | JSON / TEXT  | NOT NULL                |
| active          | BOOLEAN      | NOT NULL, DEFAULT TRUE  |
| created_at      | TIMESTAMP    | NOT NULL                |

> Índice UNIQUE: `uk_achievement_tenant_code` em `(tenant_id, code)`

---

### player_achievements

| Coluna         | Tipo      | Constraints                  |
|----------------|-----------|------------------------------|
| id             | BIGINT    | PK, AUTO_INCREMENT           |
| player_id      | BIGINT    | FK → players, NOT NULL       |
| achievement_id | BIGINT    | FK → achievements, NOT NULL  |
| unlocked_at    | TIMESTAMP | NOT NULL                     |

> Índice UNIQUE: `uk_player_achievement` em `(player_id, achievement_id)`

---

### webhook_configs

| Coluna     | Tipo         | Constraints             |
|------------|--------------|-------------------------|
| id         | BIGINT       | PK, AUTO_INCREMENT      |
| tenant_id  | BIGINT       | FK → tenants, NOT NULL  |
| url        | VARCHAR(500) | NOT NULL                |
| event_type | VARCHAR(30)  | NOT NULL                |
| secret_key | VARCHAR(255) | NOT NULL                |
| active     | BOOLEAN      | NOT NULL, DEFAULT TRUE  |
| created_at | TIMESTAMP    | NOT NULL                |

---

### webhook_logs

| Coluna            | Tipo        | Constraints                    |
|-------------------|-------------|--------------------------------|
| id                | BIGINT      | PK, AUTO_INCREMENT             |
| webhook_config_id | BIGINT      | FK → webhook_configs, NOT NULL |
| event_type        | VARCHAR(30) | NOT NULL                       |
| payload           | TEXT        | NOT NULL                       |
| response_status   | INT         | NULLABLE                       |
| success           | BOOLEAN     | NOT NULL                       |
| attempt_count     | INT         | NOT NULL, DEFAULT 1            |
| sent_at           | TIMESTAMP   | NOT NULL                       |

---

### ranking_entries

| Coluna     | Tipo        | Constraints                              |
|------------|-------------|------------------------------------------|
| id         | BIGINT      | PK, AUTO_INCREMENT                       |
| tenant_id  | BIGINT      | FK → tenants, NOT NULL                   |
| player_id  | BIGINT      | FK → players, NOT NULL                   |
| period     | VARCHAR(10) | NOT NULL (`GLOBAL`, `WEEKLY`, `MONTHLY`) |
| period_key | VARCHAR(10) | NOT NULL (ex: `"2025-W03"`, `"2025-01"`) |
| score      | INT         | NOT NULL                                 |
| position   | INT         | NOT NULL                                 |
| updated_at | TIMESTAMP   | NOT NULL                                 |

> Índice: `idx_ranking_lookup` em `(tenant_id, period, period_key, position)`

---

## Enums no Java (mapeados como VARCHAR)

```java
public enum CriteriaType {
    ACTION_COUNT,    // Contagem de ações específicas
    STREAK,          // Streak mínimo
    LEVEL_REACHED,   // Nível atingido
    XP_TOTAL,        // XP total acumulado
    MULTI_ACTION     // Combinação de ações diferentes
}

public enum WebhookEventType {
    LEVEL_UP,
    ACHIEVEMENT_UNLOCKED,
    STREAK_MILESTONE,
    RANK_CHANGED
}

public enum RankingPeriod {
    GLOBAL,
    WEEKLY,
    MONTHLY
}

public enum TenantPlan {
    FREE,
    PRO,
    ENTERPRISE
}
```