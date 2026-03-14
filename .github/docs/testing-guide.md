# Guia de Testes — GamifyAPI

---

## Estrutura de Testes

```
src/test/java/com/gamifyapi/
├── unit/
│   ├── service/     @ExtendWith(MockitoExtension.class)
│   └── engine/      @ExtendWith(MockitoExtension.class)
├── integration/
│   ├── controller/  @WebMvcTest
│   └── repository/  @DataJpaTest
└── util/
    ├── TestDataFactory   Builders de entidades/DTOs para testes
    └── JwtTestHelper     Geração de tokens para testes de controller
```

---

## Convenções

- **Nomenclatura:** `should_ExpectedBehavior_When_Condition`
- Usar `@DisplayName("Deve ...")` em português
- Organizar com comentários `// Arrange`, `// Act`, `// Assert`
- Cada classe de teste testa **uma** classe de produção
- Usar `TestDataFactory` para criar objetos (não construir inline)

---

## TestDataFactory

```java
public class TestDataFactory {

    public static Tenant tenant() {
        return Tenant.builder()
                .id(1L)
                .name("Test App")
                .email("test@app.com")
                .passwordHash("hashed")
                .plan(TenantPlan.FREE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Player player() {
        return Player.builder()
                .id(1L)
                .tenant(tenant())
                .externalId("player_1")
                .displayName("Test Player")
                .totalXp(0)
                .currentLevel(1)
                .currentStreak(0)
                .longestStreak(0)
                .lastActivityDate(null)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public static Player playerWithXp(int xp, int level) {
        Player p = player();
        p.setTotalXp(xp);
        p.setCurrentLevel(level);
        return p;
    }

    public static Player playerWithStreak(int streak, LocalDate lastActivity) {
        Player p = player();
        p.setCurrentStreak(streak);
        p.setLongestStreak(streak);
        p.setLastActivityDate(lastActivity);
        return p;
    }

    public static ActionDefinition action(String code, int xp) {
        return ActionDefinition.builder()
                .id(1L)
                .tenant(tenant())
                .code(code)
                .displayName("Test Action")
                .xpValue(xp)
                .cooldownSeconds(0)
                .active(true)
                .build();
    }

    public static ActionDefinition actionWithCooldown(String code, int xp, int cooldown) {
        ActionDefinition a = action(code, xp);
        a.setCooldownSeconds(cooldown);
        return a;
    }

    public static Achievement achievementStreak(int minStreak) {
        return Achievement.builder()
                .id(1L)
                .tenant(tenant())
                .code("STREAK_" + minStreak)
                .name("Streak " + minStreak)
                .criteriaType(CriteriaType.STREAK)
                .criteriaValue("{\"minStreak\": %d}".formatted(minStreak))
                .xpReward(100)
                .active(true)
                .build();
    }

    public static Achievement achievementActionCount(String actionCode, int count) {
        return Achievement.builder()
                .id(2L)
                .tenant(tenant())
                .code("ACTION_COUNT_" + count)
                .name("Did " + count + " actions")
                .criteriaType(CriteriaType.ACTION_COUNT)
                .criteriaValue("{\"actionCode\":\"%s\",\"count\":%d}"
                    .formatted(actionCode, count))
                .xpReward(200)
                .active(true)
                .build();
    }

    public static List<LevelConfig> defaultLevels() {
        return List.of(
                new LevelConfig(1L, tenant(), 1, 0,    "Novato"),
                new LevelConfig(2L, tenant(), 2, 100,  "Aprendiz"),
                new LevelConfig(3L, tenant(), 3, 300,  "Intermediário"),
                new LevelConfig(4L, tenant(), 4, 600,  "Avançado"),
                new LevelConfig(5L, tenant(), 5, 1000, "Expert")
        );
    }

    public static ProcessActionRequest actionRequest(String playerId, String actionCode) {
        return new ProcessActionRequest(playerId, "Test Player", actionCode);
    }
}
```

---

## Cenários de Teste por Service

### StreakService

| # | Cenário | Input | Expected |
|---|---------|-------|----------|
| 1 | Deve iniciar streak quando player nunca jogou | `lastActivity=null` | `streak=1, wasReset=false` |
| 2 | Deve incrementar streak quando jogou ontem | `lastActivity=ontem, streak=5` | `streak=6, wasReset=false` |
| 3 | Deve manter streak quando já jogou hoje | `lastActivity=hoje, streak=5` | `streak=5, wasReset=false` |
| 4 | Deve resetar streak quando pulou um dia | `lastActivity=anteontem, streak=15` | `streak=1, wasReset=true` |
| 5 | Deve atualizar longestStreak ao resetar se for maior | `lastActivity=anteontem, streak=20, longest=10` | `longest=20, streak=1` |
| 6 | Deve não atualizar longestStreak se for menor | `lastActivity=anteontem, streak=3, longest=20` | `longest=20, streak=1` |

---

### LevelService

| # | Cenário | Input | Expected |
|---|---------|-------|----------|
| 1 | Deve não subir de nível quando XP insuficiente | `totalXp=50, níveis=[1:0, 2:100]` | `level=1, levelUp.happened=false` |
| 2 | Deve subir um nível quando XP suficiente | `totalXp=150, level atual=1, níveis=[2:100]` | `level=2, levelUp from 1 to 2` |
| 3 | Deve subir múltiplos níveis de uma vez | `totalXp=650, level=1, níveis=[2:100,3:300,4:600]` | `level=4, levelUp from 1 to 4` |
| 4 | Deve retornar título do novo nível | `totalXp=100, nível 2 title="Aprendiz"` | `levelUp.title="Aprendiz"` |
| 5 | Deve usar tabela padrão quando tenant não configurou | sem `LevelConfig` pro tenant | `nível = totalXp / 100 + 1` (simplificado) |

---

### CooldownService

| # | Cenário | Input | Expected |
|---|---------|-------|----------|
| 1 | Deve permitir quando não há ação anterior | nenhum `ActionLog` para player+action | permitido (sem exceção) |
| 2 | Deve permitir quando cooldown expirou | último log há 120s, `cooldown=60s` | permitido |
| 3 | Deve bloquear quando cooldown ativo | último log há 30s, `cooldown=60s` | `CooldownActiveException(remaining=30)` |
| 4 | Deve permitir quando cooldown é zero | `cooldown=0`, último log há 1s | permitido |

---

### AchievementEngine

| # | Cenário | Input | Expected |
|---|---------|-------|----------|
| 1 | Deve desbloquear conquista de streak quando atingido | `streak=7, achievement minStreak=7` | `[achievement]` na lista |
| 2 | Deve não desbloquear quando streak insuficiente | `streak=5, achievement minStreak=7` | lista vazia |
| 3 | Deve ignorar conquista já desbloqueada | player já tem achievement | lista vazia |
| 4 | Deve desbloquear múltiplas conquistas de uma vez | player atinge critério de 2 achievements | lista com 2 achievements |
| 5 | Deve usar evaluator correto por `CriteriaType` | `tipo=ACTION_COUNT` | `ActionCountEvaluator` é chamado |
| 6 | Deve conceder `xpReward` ao desbloquear | `achievement.xpReward=200` | `player.totalXp` aumenta 200 |

---

### GamificationService (orquestrador)

| # | Cenário | Expected |
|---|---------|----------|
| 1 | Deve criar player automaticamente se não existir | player criado com `XP=0, level=1` |
| 2 | Deve processar fluxo completo (XP + level + streak + conquistas) | response com todos os campos preenchidos |
| 3 | Deve rejeitar ação com cooldown ativo | `CooldownActiveException` |
| 4 | Deve rejeitar ação inexistente | `ResourceNotFoundException` |
| 5 | Deve conceder XP bônus de conquista e re-checar level | se conquista dá 500xp e isso causa level up, ambos no response |
| 6 | Deve disparar webhook de level up | `webhookService.notify` chamado com evento `LEVEL_UP` |
| 7 | Deve atualizar `displayName` se diferente | `player.displayName` atualizado |

---

## Cenários de Teste por Evaluator

### ActionCountEvaluator

| # | Cenário | count no banco | `criteriaValue.count` | Result |
|---|---------|----------------|-----------------------|--------|
| 1 | Count exato | 50 | 50 | `true` |
| 2 | Count acima | 75 | 50 | `true` |
| 3 | Count abaixo | 49 | 50 | `false` |
| 4 | Nenhuma ação registrada | 0 | 1 | `false` |

---

### StreakEvaluator

| # | Cenário | `currentStreak` | `minStreak` | Result |
|---|---------|-----------------|-------------|--------|
| 1 | Streak exato | 7 | 7 | `true` |
| 2 | Streak acima | 15 | 7 | `true` |
| 3 | Streak abaixo | 6 | 7 | `false` |

---

### LevelReachedEvaluator

| # | Cenário | `currentLevel` | `criteriaLevel` | Result |
|---|---------|----------------|-----------------|--------|
| 1 | Nível exato | 10 | 10 | `true` |
| 2 | Nível acima | 12 | 10 | `true` |
| 3 | Nível abaixo | 9 | 10 | `false` |

---

### MultiActionEvaluator

| # | Cenário | actions do player | required | Result |
|---|---------|-------------------|----------|--------|
| 1 | Todas as ações realizadas | `[A, B, C]` | `[A, B, C]` | `true` |
| 2 | Ações extras além das requeridas | `[A, B, C, D]` | `[A, B, C]` | `true` |
| 3 | Falta uma ação | `[A, B]` | `[A, B, C]` | `false` |
| 4 | Nenhuma ação realizada | `[]` | `[A, B]` | `false` |

---

## Testes de Controller

Usar `@WebMvcTest` com security mockado:

```java
@WebMvcTest(ActionController.class)
@Import(SecurityConfig.class)
class ActionControllerIntTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GamificationService gamificationService;

    @MockBean
    private ApiKeyService apiKeyService;

    @Test
    @DisplayName("Deve retornar 200 ao processar ação válida")
    void should_Return200_When_ValidAction() throws Exception {
        // Arrange
        when(apiKeyService.validateAndGetTenant("gapi_test"))
                .thenReturn(TestDataFactory.tenant());

        when(gamificationService.processAction(any()))
                .thenReturn(/* mock response */);

        // Act & Assert
        mockMvc.perform(post("/api/v1/actions")
                        .header("X-API-Key", "gapi_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "playerId": "user_1",
                                "actionCode": "completed_lesson"
                            }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerId").value("user_1"))
                .andExpect(jsonPath("$.xpGranted").isNumber());
    }

    @Test
    @DisplayName("Deve retornar 429 quando cooldown ativo")
    void should_Return429_When_CooldownActive() throws Exception {
        // Arrange
        when(apiKeyService.validateAndGetTenant(any()))
                .thenReturn(TestDataFactory.tenant());

        when(gamificationService.processAction(any()))
                .thenThrow(new CooldownActiveException(45));

        // Act & Assert
        mockMvc.perform(post("/api/v1/actions")
                        .header("X-API-Key", "gapi_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "playerId": "user_1",
                                "actionCode": "completed_lesson"
                            }
                        """))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "45"));
    }

    @Test
    @DisplayName("Deve retornar 400 quando playerId ausente")
    void should_Return400_When_MissingPlayerId() throws Exception {
        mockMvc.perform(post("/api/v1/actions")
                        .header("X-API-Key", "gapi_test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "actionCode": "completed_lesson"
                            }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.playerId").exists());
    }
}
```

---

## Testes de Repository

```java
@DataJpaTest
@ActiveProfiles("test")
class PlayerRepositoryTest {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    @DisplayName("Deve encontrar player por tenant e externalId")
    void should_FindPlayer_When_TenantAndExternalIdMatch() {
        // Arrange
        Tenant tenant = entityManager.persist(TestDataFactory.tenantWithoutId());
        Player player = TestDataFactory.playerWithoutId();
        player.setTenant(tenant);
        player.setExternalId("user_42");
        entityManager.persist(player);
        entityManager.flush();

        // Act
        Optional<Player> found = playerRepository
                .findByTenantIdAndExternalId(tenant.getId(), "user_42");

        // Assert
        assertTrue(found.isPresent());
        assertEquals("user_42", found.get().getExternalId());
    }

    @Test
    @DisplayName("Deve retornar leaderboard ordenado por XP decrescente")
    void should_ReturnPlayersOrderedByXpDesc() {
        // Arrange: criar 3 players com XPs diferentes
        // Act: chamar findLeaderboard
        // Assert: verificar ordem
    }
}
```