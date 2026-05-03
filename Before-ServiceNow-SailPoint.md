# CyberAudit7E — Module Intégrations ServiceNow × SailPoint

## Architecture

```
┌─────────────────┐         ┌───────────────────────────────┐         ┌─────────────────┐
│    SailPoint     │         │        CyberAudit7E          │         │   ServiceNow    │
│  Identity Cloud  │         │    (Spring Boot — M7+)       │         │     ITSM        │
│                  │         │                               │         │                 │
│ Event Triggers ──┼──POST──→│ /api/webhooks/sailpoint       │         │                 │
│                  │         │    ↓                          │         │                 │
│  V3 API  ←───────┼──GET───│ SailPointClient               │         │                 │
│                  │         │    ↓                          │         │                 │
│                  │         │ TicketOrchestrator             │         │                 │
│                  │         │    ↓ save SecurityTicket       │         │                 │
│                  │         │    ↓ push to ServiceNow       │         │                 │
│                  │         │ ServiceNowClient ─────POST───→│ Table API /incident │
│                  │         │                               │         │                 │
│                  │         │ /api/webhooks/servicenow ←────┼──POST──│ Business Rule   │
│                  │         │    ↓ update ticket status     │         │ (on state change)│
│                  │         │                               │         │                 │
│                  │         │ /api/tickets                  │         │                 │
│                  │         │    → Dashboard unifié         │         │                 │
└─────────────────┘         └───────────────────────────────┘         └─────────────────┘
```

## Fichiers livrés

```
src/main/java/com/cyberaudit7e/
├── config/
│   ├── IntegrationProperties.java       # Config externalisée ServiceNow + SailPoint
│   └── RestTemplateConfig.java          # Bean RestTemplate (HTTP client)
│
├── domain/
│   ├── entity/
│   │   └── SecurityTicket.java          # Entité JPA — ticket unifié
│   └── enums/
│       ├── TicketSource.java            # AUDIT, SAILPOINT, SERVICENOW, MANUAL
│       ├── TicketStatus.java            # NEW → OPEN → IN_PROGRESS → RESOLVED → CLOSED
│       └── TicketSeverity.java          # CRITICAL, HIGH, MEDIUM, LOW, INFO
│
├── repository/
│   └── SecurityTicketRepository.java    # JPA + requêtes paginées + recherche
│
├── dto/integration/
│   └── TicketDto.java                   # DTO de réponse API
│
├── integration/
│   ├── TicketOrchestrator.java          # Pont central — persiste + pousse vers SNOW
│   ├── TicketController.java            # REST API /api/tickets (CRUD + stats)
│   ├── servicenow/
│   │   └── ServiceNowClient.java        # Client REST — OAuth + Table API
│   ├── sailpoint/
│   │   └── SailPointClient.java         # Client REST — OAuth + V3 API
│   └── webhook/
│       ├── SailPointWebhookController.java     # POST /api/webhooks/sailpoint
│       └── ServiceNowWebhookController.java    # POST /api/webhooks/servicenow
│
src/main/resources/db/migration/
└── V5__security_tickets.sql             # Table security_tickets + index

application-integrations.yml             # Config YAML à fusionner
```

## Installation

### 1. Copier les fichiers

```bash
# Depuis la racine du projet
cp -r src/main/java/com/cyberaudit7e/config/* <projet>/src/main/java/com/cyberaudit7e/config/
cp -r src/main/java/com/cyberaudit7e/domain/* <projet>/src/main/java/com/cyberaudit7e/domain/
cp -r src/main/java/com/cyberaudit7e/repository/* <projet>/src/main/java/com/cyberaudit7e/repository/
cp -r src/main/java/com/cyberaudit7e/dto/* <projet>/src/main/java/com/cyberaudit7e/dto/
cp -r src/main/java/com/cyberaudit7e/integration/* <projet>/src/main/java/com/cyberaudit7e/integration/
cp src/main/resources/db/migration/V5__security_tickets.sql <projet>/src/main/resources/db/migration/
```

### 2. Ajouter la config YAML

Fusionner le contenu de `application-integrations.yml` dans votre `application.yml`.

### 3. Lancer

```bash
mvnw.cmd spring-boot:run
```

Flyway crée automatiquement la table `security_tickets`.

## Test en mode développement (sans ServiceNow/SailPoint)

Les intégrations sont **désactivées par défaut** (`enabled: false`).
Le module fonctionne en mode standalone :

```bash
REM Créer un ticket manuellement
curl -s -X POST http://localhost:8080/api/tickets ^
  -H "Content-Type: application/json" ^
  -d "{\"title\":\"Test violation SoD\",\"description\":\"Utilisateur avec accès incompatibles\",\"severity\":\"HIGH\",\"category\":\"sod_violation\"}" | jq .

REM Simuler un webhook SailPoint
curl -s -X POST http://localhost:8080/api/webhooks/sailpoint ^
  -H "Content-Type: application/json" ^
  -d "{\"_metadata\":{\"triggerId\":\"idn:policy-violation\",\"invocationId\":\"test-123\"},\"identity\":{\"id\":\"uid-456\",\"name\":\"Jean Dupont\",\"type\":\"IDENTITY\"},\"policyName\":\"SoD Finance-Achats\",\"violatingAccessItems\":[{\"name\":\"Finance Admin\"},{\"name\":\"Procurement Approver\"}]}" | jq .

REM Lister les tickets
curl -s http://localhost:8080/api/tickets | jq .

REM Tickets ouverts
curl -s http://localhost:8080/api/tickets/open | jq .

REM Filtrer par source
curl -s "http://localhost:8080/api/tickets?source=SAILPOINT" | jq .

REM Filtrer par sévérité
curl -s "http://localhost:8080/api/tickets?severity=HIGH" | jq .

REM Recherche
curl -s "http://localhost:8080/api/tickets/search?q=Dupont" | jq .

REM Statistiques
curl -s http://localhost:8080/api/tickets/stats | jq .

REM Résoudre un ticket
curl -s -X POST http://localhost:8080/api/tickets/1/resolve ^
  -H "Content-Type: application/json" ^
  -d "{\"closeNotes\":\"Accès révoqué dans SailPoint\"}" | jq .

REM Statut des intégrations
curl -s http://localhost:8080/api/tickets/integrations | jq .

REM Simuler un callback ServiceNow
curl -s -X POST http://localhost:8080/api/webhooks/servicenow ^
  -H "Content-Type: application/json" ^
  -d "{\"sys_id\":\"abc123\",\"number\":\"INC0012345\",\"state\":3,\"assigned_to\":\"Équipe Sécurité\"}" | jq .

REM Sync manuelle vers ServiceNow
curl -s -X POST http://localhost:8080/api/tickets/sync | jq .

## Configuration ServiceNow (production)

### 1. Créer un utilisateur API

- System Administration → Users → New
- Rôles : `itil`, `rest_service`
- Si OAuth : Admin → OAuth → Application Registry → New

### 2. Configurer le webhook callback (ServiceNow → CyberAudit7E)

- System Web Services → Outbound → REST Message → New
- Endpoint : `https://cyberaudit7e.local/api/webhooks/servicenow`
- Method : POST
- Business Rule sur `incident` :
  - When : after, Update
  - Condition : `current.state.changesTo()`
  - Script : appeler le REST Message avec sys_id, number, state

### 3. Activer dans application.yml

```yaml
cyberaudit7e:
  integrations:
    servicenow:
      enabled: true
      instance: mon-instance.service-now.com
      auth-method: basic
      username: api_user
      password: ${SNOW_PASSWORD}
```

## Configuration SailPoint (production)

### 1. Créer un Personal Access Token

- Identity Security Cloud → Preferences → Personal Access Tokens → New Token
- Noter le Client ID et le Secret

### 2. Configurer les Event Triggers

- Admin → Event Triggers → Sélectionner un trigger (ex: Policy Violation)
- + Subscribe → Type: HTTP
- Integration URL : `https://cyberaudit7e.local/api/webhooks/sailpoint`
- Auth Type : Bearer Token → coller le webhook-secret configuré

### 3. Triggers recommandés

| Trigger | Événement | Sévérité auto |
|---------|-----------|---------------|
| `idn:policy-violation` | Violation SoD | HIGH |
| `idn:identity-deleted` | Identité supprimée | MEDIUM |
| `idn:access-request-pre-approval` | Demande d'accès | MEDIUM |
| `idn:account-aggregation-completed` | Agrégation terminée | LOW |
| `idn:certification-signed-off` | Certification signée | LOW |

### 4. Activer dans application.yml

```yaml
cyberaudit7e:
  integrations:
    sailpoint:
      enabled: true
      tenant: mon-tenant
      client-id: ${SP_CLIENT_ID}
      client-secret: ${SP_CLIENT_SECRET}
      webhook-secret: mon-secret-partage
```

## Endpoints API

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | /api/tickets | Liste paginée (filtres: source, status, severity) |
| GET | /api/tickets/open | Tickets ouverts uniquement |
| GET | /api/tickets/{id} | Détail d'un ticket |
| GET | /api/tickets/search?q=xxx | Recherche full-text |
| GET | /api/tickets/stats | Statistiques (par source, statut, sévérité) |
| GET | /api/tickets/integrations | Statut des connexions SNOW/SP |
| POST | /api/tickets | Créer un ticket manuellement |
| POST | /api/tickets/{id}/resolve | Résoudre (+ sync SNOW) |
| POST | /api/tickets/sync | Synchroniser les tickets non poussés |
| POST | /api/webhooks/sailpoint | Webhook récepteur SailPoint |
| POST | /api/webhooks/servicenow | Webhook callback ServiceNow |
| GET | /api/webhooks/sailpoint/test | Test connectivité webhook SP |
| GET | /api/webhooks/servicenow/test | Test connectivité webhook SNOW |
