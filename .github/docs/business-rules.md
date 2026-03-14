# Regras de Negócio — GamifyAPI

---

## 1. Multi-Tenancy

- **REGRA:** Todo dado pertence a um tenant. Toda query **deve** filtrar por `tenantId`.
- **REGRA:** API Key identifica o tenant. O `ApiKeyAuthenticationFilter` busca a key (hash), encontra o tenant associado e seta no `TenantContext`.
- **REGRA:** JWT identifica o tenant admin. O `sub` do JWT contém o ID do tenant.
- **REGRA:** Um tenant **nunca** pode acessar dados de outro tenant. Mesmo que tente passar IDs de outro tenant na URL, o service deve filtrar por `tenantId`.

---

## 2. Player

- **REGRA:** Player é criado automaticamente. Se o `externalId` não existir para o tenant, criar com `totalXp=0`, `currentLevel=1`, `currentStreak=0`.
- **REGRA:** `displayName` é atualizado se enviado. Se o request traz `playerName` e é diferente do atual, atualizar.
- **REGRA:** Player é único por `(tenant_id, external_id)`.

---

## 3. Ações e Cooldown

- **REGRA:** `actionCode` deve existir e estar ativo para o tenant. Caso contrário, retornar `404`.
- **REGRA:** Cooldown por player + ação. Verificar o `timestamp` do último `ActionLog` do player para aquela `ActionDefinition`. Se a diferença for menor que `cooldownSeconds`, rejeitar com `429`.
- **REGRA:** `cooldown = 0` significa sem cooldown.
- **REGRA:** XP concedido = `xpValue` da `ActionDefinition` no momento da execução. Se o admin alterar o XP depois, ações futuras usam o novo valor, mas ações passadas mantêm o XP original registrado no `ActionLog`.

---

## 4. XP e Level Up

- **REGRA:** XP é cumulativo e nunca decresce.
- **REGRA:** Level up é verificado após conceder XP. Comparar `player.totalXp` com a tabela `level_configs` do tenant. O nível do player é o maior nível cujo `xpRequired <= player.totalXp`.
- **REGRA:** Level up múltiplo é possível. Exemplo: player com 90xp (nível 1) ganha 250xp (total 340xp), com níveis configurados em `1=0`, `2=100`, `3=300`, `4=600` — ele pula direto para nível 3. O response deve indicar `previousLevel=1`, `newLevel=3`.
- **REGRA:** Se o tenant não configurou níveis, usar tabela padrão: nível N requer `N*100` xp (`1=0`, `2=100`, `3=200`...).
- **REGRA:** Se uma conquista dá XP bônus (`xpReward`), esse XP também pode causar level up. Processar em loop até estabilizar.

---

## 5. Streak

- **REGRA:** Baseado em dias do calendário (`LocalDate`), não em 24h.
- **REGRA — Incremento:** Se `lastActivityDate == ontem` → `currentStreak++`
- **REGRA — Mesmo dia:** Se `lastActivityDate == hoje` → streak não muda (já contabilizado).
- **REGRA — Reset:** Se `lastActivityDate < ontem` (ou `null`) → `currentStreak = 1`. Antes de resetar, verificar se `currentStreak > longestStreak` e atualizar `longestStreak`.
- **REGRA:** `lastActivityDate` é sempre atualizado para `hoje` após processar a ação.
- **REGRA:** Streak milestones (7, 30, 100 dias) podem disparar webhook `STREAK_MILESTONE`.

---

## 6. Conquistas (Achievements)

- **REGRA:** Uma conquista só pode ser desbloqueada uma vez por player. Se já existe registro em `player_achievements`, pular.
- **REGRA:** Avaliação ocorre **somente** ao processar ação. Não há job em background avaliando conquistas.
- **REGRA:** Avaliar **todas** as conquistas ativas do tenant que o player ainda não desbloqueou.
- **REGRA:** Se a conquista tem `xpReward > 0`, conceder XP ao player **e** verificar level up novamente.

### Critérios por tipo

**ACTION_COUNT**

```json
{ "actionCode": "completed_lesson", "count": 50 }
```

Verificar: `COUNT(*) FROM action_logs WHERE player_id = ? AND action_definition.code = ?`  
Condição: `count >= criteriaValue.count`

---

**STREAK**

```json
{ "minStreak": 7 }
```

Verificar: `player.currentStreak >= criteriaValue.minStreak`

---

**LEVEL_REACHED**

```json
{ "level": 10 }
```

Verificar: `player.currentLevel >= criteriaValue.level`

---

**XP_TOTAL**

```json
{ "minXp": 5000 }
```

Verificar: `player.totalXp >= criteriaValue.minXp`

---

**MULTI_ACTION**

```json
{ "actionCodes": ["completed_lesson", "passed_quiz", "submitted_project"] }
```

Verificar: para **cada** `actionCode` da lista, o player tem pelo menos 1 registro em `action_logs`. Todas devem existir.

---

## 7. Ranking / Leaderboard

- **REGRA:** Ranking `GLOBAL` ordena por `player.totalXp DESC`.
- **REGRA:** Ranking `WEEKLY` filtra `action_logs` da semana corrente (segunda a domingo) e soma `xpGranted`.
- **REGRA:** Ranking `MONTHLY` filtra `action_logs` do mês corrente e soma `xpGranted`.
- **REGRA:** Desempate por `player.createdAt ASC` (quem atingiu o XP primeiro fica na frente).
- **REGRA:** Posição é calculada via query. Para o MVP, calcular on-demand. Otimizar com cache se necessário.

---

## 8. Webhooks

- **REGRA:** Disparados de forma assíncrona (`@Async`) para não atrasar o response da ação.
- **REGRA:** Em caso de falha (timeout, erro HTTP), retry até 3 vezes com backoff exponencial: `1s`, `4s`, `16s`.
- **REGRA:** O payload é assinado com HMAC-SHA256 usando o `secretKey` do webhook. Incluir assinatura no header `X-Gamify-Signature`.
- **REGRA:** Registrar todas as tentativas em `webhook_logs`.
- **REGRA:** Webhook com `active = false` não é disparado.

---

## 9. Dashboard

- **REGRA:** Todas as métricas são filtradas pelo tenant do JWT.
- **REGRA:** "Players ativos 7d" = players com `lastActivityDate` nos últimos 7 dias.
- **REGRA:** "Top action" = `action_definition` com mais registros em `action_logs`.

---

## 10. Validações Gerais

- **REGRA:** Nomes de `code` (actions, achievements) devem ser `snake_case` e ter entre 3–50 caracteres.
- **REGRA:** `xpValue` deve ser `> 0`.
- **REGRA:** `cooldownSeconds` deve ser `>= 0`.
- **REGRA:** URL de webhook deve começar com `https://`.
- **REGRA:** `criteriaValue` deve ser validado conforme o `criteriaType` (validador customizado).