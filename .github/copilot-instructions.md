# Copilot Instructions — GamifyAPI

## Visão Geral

Este projeto é a **GamifyAPI**, uma API REST de **Gamificação como Serviço (GaaS)**.
Qualquer aplicação externa pode integrar via API Key e adicionar mecânicas de
gamificação (XP, níveis, conquistas, rankings, streaks) aos seus usuários.

## Stack Obrigatória

- Java 17+
- Spring Boot 3.x
- Maven (não usar Gradle)
- Spring Data JPA / Hibernate
- Spring Security + JWT (jjwt)
- PostgreSQL (produção) / H2 (testes)
- JUnit 5 + Mockito (testes)
- SpringDoc OpenAPI 3 (Swagger)
- Bean Validation (JSR 380)

## Idioma

- Código (classes, variáveis, métodos): **português brasileiro**
- Comentários e Javadoc: **português brasileiro**
- Mensagens de erro da API: **português brasileiro**
- Documentação (.md): **português brasileiro**

## Regras de Código

- Usar `record` para DTOs sempre que possível
- Nunca expor entidades JPA nos controllers — sempre usar DTOs
- Constructor injection via `@RequiredArgsConstructor` (nunca `@Autowired` em campo)
- Métodos de service que alteram dados devem ser `@Transactional`
- Usar `Optional` com `orElseThrow()` e exceção customizada (nunca `.get()` direto)
- Usar `@Valid` nos controllers para validação de entrada
- Retornar `ResponseEntity` com status HTTP explícito
- Logs com SLF4J (`@Slf4j` do Lombok)

## Lombok

- Usar apenas: `@Slf4j`, `@RequiredArgsConstructor`, `@Getter`, `@Setter`,
  `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`
- NÃO usar `@Data` em entidades JPA

## Entidades JPA

- `@GeneratedValue(strategy = GenerationType.IDENTITY)`
- `equals()` e `hashCode()` baseados apenas no `id`
- `FetchType.LAZY` em todos os relacionamentos
- Tabelas com snake_case via `@Table(name = "...")`
- Auditar com `createdAt`/`updatedAt` via `@PrePersist`/`@PreUpdate`

## Exceções

- Exceções customizadas estendem `RuntimeException`
- Tratamento centralizado em `GlobalExceptionHandler` (`@RestControllerAdvice`)
- Formato: `ErrorResponse(status, message, timestamp, details)`
- Status: 201 criação, 404 não encontrado, 409 duplicidade, 422 regra violada, 429 cooldown

## Segurança

- JWT para rotas administrativas do tenant
- API Key (header `X-API-Key`) para rotas de integração
- API Keys armazenadas como hash SHA-256
- Todas as queries filtram por `tenantId` (multi-tenancy)
- `TenantContext` (ThreadLocal) para acessar o tenant atual

## Testes

- Unitários: `@ExtendWith(MockitoExtension.class)`
- Controllers: `@WebMvcTest`
- Repositories: `@DataJpaTest`
- Nomenclatura: `should_ExpectedBehavior_When_Condition`
- Usar `@DisplayName` em português descritivo
- Dados de teste via `TestDataFactory`
- Cobertura mínima: 80%

---

## 📁 Documentação Detalhada

Os arquivos abaixo contêm detalhamentos de cada aspecto do projeto.
Ao trabalhar em uma parte específica, **referencie o arquivo correspondente**
usando `#file:` no chat do Copilot.

### Arquitetura e estrutura de pastas
> `#file:docs/architecture.md`
>
> Contém: diagrama de camadas, padrões de projeto utilizados,
> estrutura completa de pastas com descrição de cada arquivo,
> fluxo de uma requisição do HTTP ao banco, dependências Maven
> e profiles do Spring.

### Padrões e convenções de código
> `#file:docs/coding-standards.md`
>
> Contém: exemplos completos de como estruturar Entity, DTO (record),
> Controller, Service, Repository, Exception e o padrão Strategy
> usado no engine de conquistas. Tabela de convenções de nomenclatura.

### Endpoints e contratos JSON
> `#file:docs/api-contracts.md`
>
> Contém: todos os endpoints da API com métodos HTTP, request bodies,
> response bodies, status codes e exemplos JSON completos.
> Cobre: Auth, API Keys, Actions, Levels, Achievements, Webhooks,
> Players, Leaderboard e Dashboard.

### Schema do banco de dados
> `#file:docs/database-schema.md`
>
> Contém: todas as tabelas com colunas, tipos, constraints e índices.
> Diagrama de relacionamentos entre entidades.
> Definição dos enums Java mapeados como VARCHAR.

### Regras de negócio
> `#file:docs/business-rules.md`
>
> Contém: regras detalhadas de multi-tenancy, player, cooldown,
> XP, level up (incluindo múltiplo), streak, conquistas (todos
> os tipos de critério), ranking, webhooks e dashboard.

### Guia de testes
> `#file:docs/testing-guide.md`
>
> Contém: estrutura de testes, convenções, TestDataFactory,
> tabelas de cenários por service (StreakService, LevelService,
> CooldownService, AchievementEngine, GamificationService),
> cenários por evaluator e exemplos de testes de controller
> e repository.