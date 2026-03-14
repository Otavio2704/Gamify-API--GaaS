# Padrões de Código — GamifyAPI

---

## Estrutura de uma Entidade JPA

```java
@Entity
@Table(name = "players")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private String externalId;

    private String displayName;

    @Column(nullable = false)
    private Integer totalXp = 0;

    @Column(nullable = false)
    private Integer currentLevel = 1;

    @Column(nullable = false)
    private Integer currentStreak = 0;

    @Column(nullable = false)
    private Integer longestStreak = 0;

    private LocalDate lastActivityDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Métodos de domínio (lógica que pertence à entidade)
    public void addXp(int xp) {
        this.totalXp += xp;
    }

    // equals e hashCode baseados SOMENTE no id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Player player = (Player) o;
        return id != null && id.equals(player.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
```

---

## Estrutura de um DTO (usar record quando possível)

```java
// Request
public record ProcessActionRequest(
    @NotBlank(message = "playerId é obrigatório")
    String playerId,

    String playerName,

    @NotBlank(message = "actionCode é obrigatório")
    String actionCode
) {}

// Response
public record ActionResultResponse(
    String playerId,
    String action,
    Integer xpGranted,
    Integer totalXp,
    Integer currentLevel,
    LevelUpDetails levelUp,
    StreakInfo streak,
    List<AchievementResponse> newAchievements,
    Integer leaderboardPosition,
    Instant processedAt
) {}

// Response auxiliar
public record LevelUpDetails(
    boolean happened,
    Integer previousLevel,
    Integer newLevel,
    String title
) {
    public static LevelUpDetails none(int currentLevel) {
        return new LevelUpDetails(false, currentLevel, currentLevel, null);
    }

    public static LevelUpDetails of(int from, int to, String title) {
        return new LevelUpDetails(true, from, to, title);
    }
}
```

---

## Estrutura de um Controller

```java
@RestController
@RequestMapping("/api/v1/actions")
@RequiredArgsConstructor
@Tag(name = "Actions", description = "Processamento de ações dos players")
@Slf4j
public class ActionController {

    private final GamificationService gamificationService;

    @PostMapping
    @Operation(summary = "Processar ação de um player")
    public ResponseEntity<ActionResultResponse> processAction(
            @Valid @RequestBody ProcessActionRequest request,
            @RequestHeader("X-API-Key") String apiKey) {

        log.info("Processando ação '{}' para player '{}'",
                request.actionCode(), request.playerId());

        ActionResultResponse result = gamificationService.processAction(request);

        return ResponseEntity.ok(result);
    }
}
```

---

## Estrutura de um Service

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class XpService {

    private final PlayerRepository playerRepository;
    private final ActionLogRepository actionLogRepository;

    @Transactional
    public int grantXp(Player player, ActionDefinition action) {
        int xp = action.getXpValue();
        player.addXp(xp);
        playerRepository.save(player);

        ActionLog logEntry = ActionLog.builder()
                .player(player)
                .actionDefinition(action)
                .xpGranted(xp)
                .timestamp(Instant.now())
                .build();

        actionLogRepository.save(logEntry);

        log.debug("Concedidos {} XP ao player '{}'. Total: {}",
                xp, player.getExternalId(), player.getTotalXp());

        return xp;
    }
}
```

---

## Estrutura de um Repository

```java
public interface PlayerRepository extends JpaRepository<Player, Long> {

    Optional<Player> findByTenantIdAndExternalId(Long tenantId, String externalId);

    @Query("""
        SELECT p FROM Player p
        WHERE p.tenant.id = :tenantId
        ORDER BY p.totalXp DESC
    """)
    Page<Player> findLeaderboard(@Param("tenantId") Long tenantId, Pageable pageable);

    @Query("""
        SELECT COUNT(p) + 1 FROM Player p
        WHERE p.tenant.id = :tenantId AND p.totalXp > :xp
    """)
    int findPlayerRank(@Param("tenantId") Long tenantId, @Param("xp") int xp);

    boolean existsByTenantIdAndExternalId(Long tenantId, String externalId);
}
```

---

## Estrutura de Exceção Customizada

```java
public class CooldownActiveException extends RuntimeException {

    private final long remainingSeconds;

    public CooldownActiveException(long remainingSeconds) {
        super("Ação em cooldown. Tente novamente em %d segundos.".formatted(remainingSeconds));
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
```

---

## Estrutura do GlobalExceptionHandler

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage(), Instant.now(), null));
    }

    @ExceptionHandler(CooldownActiveException.class)
    public ResponseEntity<ErrorResponse> handleCooldown(CooldownActiveException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRemainingSeconds()))
                .body(new ErrorResponse(429, ex.getMessage(), Instant.now(), null));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse(422, ex.getMessage(), Instant.now(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        f -> f.getDefaultMessage() != null ? f.getDefaultMessage() : "inválido"
                ));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(400, "Erro de validação", Instant.now(), details));
    }
}
```

---

## Padrão Strategy (Engine de Conquistas)

```java
// Interface
public interface AchievementCriteriaEvaluator {
    CriteriaType getType();
    boolean evaluate(Player player, Achievement achievement, Long tenantId);
}

// Implementação
@Component
public class StreakEvaluator implements AchievementCriteriaEvaluator {

    @Override
    public CriteriaType getType() {
        return CriteriaType.STREAK;
    }

    @Override
    public boolean evaluate(Player player, Achievement achievement, Long tenantId) {
        int requiredStreak = achievement.getCriteriaValueAsInt("minStreak");
        return player.getCurrentStreak() >= requiredStreak;
    }
}

// Factory
@Component
@RequiredArgsConstructor
public class EvaluatorFactory {

    private final List<AchievementCriteriaEvaluator> evaluators;

    public AchievementCriteriaEvaluator getEvaluator(CriteriaType type) {
        return evaluators.stream()
                .filter(e -> e.getType() == type)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                    "Evaluator não encontrado para: " + type));
    }
}
```

---

## Convenções de Nomenclatura

| Elemento | Convenção | Exemplo |
|----------|-----------|---------|
| Classe Entity | Substantivo | `Player`, `Achievement` |
| Classe Controller | `NomeController` | `ActionController` |
| Classe Service | `NomeService` | `GamificationService` |
| Classe Repository | `NomeRepository` | `PlayerRepository` |
| DTO Request | `AçãoRequest` | `ProcessActionRequest` |
| DTO Response | `NomeResponse` | `ActionResultResponse` |
| Exceção | `NomeException` | `CooldownActiveException` |
| Enum | PascalCase | `CriteriaType`, `RankingPeriod` |
| Tabela (banco) | `snake_case` plural | `players`, `action_logs` |
| Coluna (banco) | `snake_case` | `total_xp`, `created_at` |
| Endpoint | `kebab-case` plural | `/api/v1/api-keys` |
| Método de teste | `should_X_When_Y` | `should_ResetStreak_When_DaySkipped` |