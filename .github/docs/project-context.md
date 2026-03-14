# Contexto do Projeto — GamifyAPI

## O Que É

Uma API REST multi-tenant que fornece gamificação como serviço.
Apps externos se cadastram, configuram regras de gamificação e integram
via API Key. A cada ação do usuário final, a API processa XP, níveis,
streaks, conquistas e rankings automaticamente.

## Glossário do Domínio

| Termo              | Significado                                                       |
|--------------------|-------------------------------------------------------------------|
| Tenant             | Empresa/app cliente que se cadastra na GamifyAPI                  |
| API Key            | Chave de autenticação do tenant para integração                   |
| Player             | Usuário final do app cliente, rastreado pela GamifyAPI            |
| External ID        | ID do player no sistema do cliente (nós não geramos esse ID)      |
| Action Definition  | Template de ação configurado pelo tenant (ex: "completou_aula")   |
| Action Log         | Registro de uma ação executada por um player                      |
| XP                 | Pontos de experiência acumulados pelo player                      |
| Level              | Nível atual do player, determinado pela tabela de XP do tenant    |
| Level Up           | Evento de subida de nível                                         |
| Streak             | Dias consecutivos de atividade do player                          |
| Achievement        | Conquista/badge desbloqueável por critérios configurados          |
| Criteria Type      | Tipo de regra para desbloquear conquista                          |
| Cooldown           | Tempo mínimo entre execuções da mesma ação pelo mesmo player      |
| Leaderboard        | Ranking de players por XP (global, semanal ou mensal)             |
| Webhook            | Notificação HTTP enviada ao tenant quando algo relevante acontece |
| Surge/Multiplier   | Não se aplica a este projeto (era do projeto de pricing)          |

## Atores do Sistema

### Tenant Admin
- Se cadastra na plataforma (registro + login com JWT)
- Configura: ações, níveis, conquistas, webhooks
- Gera e revoga API Keys
- Consulta dashboard com métricas

### App Cliente (via API Key)
- Envia ações dos players (`POST /api/v1/actions`)
- Consulta perfil, conquistas e timeline dos players
- Consulta leaderboard

### Player
- Não interage diretamente com a GamifyAPI
- É representado por um `externalId` que o app cliente envia
- Seus dados são criados automaticamente na primeira ação

## Fluxo Principal

1. Tenant Admin se registra → recebe JWT
2. Configura ações: "completed_lesson" = 50xp, cooldown 60s
3. Configura níveis: 1=0xp, 2=100xp, 3=300xp, 4=600xp...
4. Configura conquistas: "Maratonista" = streak >= 7 dias
5. Gera API Key: gapi_a1b2c3d4...
6. App cliente integra usando a API Key
7. Player completa uma aula no app
8. App chama: POST /api/v1/actions { playerId: "u1", actionCode: "completed_lesson" }
9. GamifyAPI processa: concede XP → checa level up → atualiza streak
   → avalia conquistas → atualiza ranking → dispara webhooks
10. Retorna resultado completo pro app exibir ao player

## Multi-Tenancy

    Estratégia: isolamento por coluna (tenant_id em todas as tabelas)
    Todas as queries JPA filtram por tenant automaticamente
    Um player com externalId "user_1" no Tenant A é completamente
    independente de um "user_1" no Tenant B
    API Key pertence a um tenant — ao autenticar via API Key,
    o tenant é identificado e setado no TenantContext

## Pacotes e Responsabilidades

Pacote	- Responsabilidade
config	- Configurações do Spring (Security, CORS, Async, OpenAPI)
security - JWT, API Key filter, TenantContext
controller - Endpoints REST, validação de entrada
dto - Objetos de transferência (request/response)
entity - Entidades JPA mapeadas para o banco
enums - Enumerações do domínio
repository - Interfaces Spring Data JPA
service - Regras de negócio e orquestração
engine - Evaluators de conquistas (Strategy Pattern)
exception - Exceções customizadas e handler global
mapper - Conversão Entity ↔ DTO
validation - Validadores customizados (Bean Validation)
scheduler - Tarefas agendadas (@Scheduled)