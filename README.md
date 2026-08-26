# MiniTricount

MiniTricount est une application simple de gestion de dépenses de groupe, inspirée du principe de Tricount/Splitwise : un groupe de participants enregistre des dépenses communes, et l'application calcule qui doit combien à qui.

Il s'agit d'un projet pédagogique fullstack (Spring Boot + React) destiné à illustrer une architecture backend/frontend simple mais complète.

## Fonctionnalités (V1)

- Création et sélection de groupes
- Ajout de participants à un groupe
- Création, modification et suppression de dépenses
- Partage égal d'une dépense entre les bénéficiaires sélectionnés
- Calcul des balances (qui a trop payé / pas assez payé)
- Calcul des remboursements suggérés pour équilibrer le groupe
- Navigation frontend par onglets (Dépenses / Participants / Balances)

## Règles métier principales

- Devise unique : EUR (aucune notion de multi-devise dans le modèle de données)
- Aucune authentification
- Partage uniquement égal entre bénéficiaires (pas de montants personnalisés)
- Les calculs financiers backend utilisent `BigDecimal` et les montants sont persistés en `NUMERIC(10,2)` afin d'éviter les erreurs d'arrondi dans la logique métier
- Lorsque le montant ne se divise pas exactement, le reste (en centimes) est réparti de façon déterministe sur les premiers participants triés par identifiant
- Balance d'un participant = total payé − total dû
- Les remboursements suggérés sont calculés à partir des balances (pas stockés en base), via un algorithme glouton créditeurs/débiteurs déterministe. Cet algorithme n'est pas garanti minimiser le nombre global de transactions.

## Stack technique

**Backend**
- Java 21
- Spring Boot 3.3.5 (Web, Data JPA, Validation)
- PostgreSQL (driver `org.postgresql:postgresql`)
- Flyway (`flyway-core` + `flyway-database-postgresql`)
- Maven
- Tests : JUnit 5 / Mockito / MockMvc (via `spring-boot-starter-test`)

**Frontend**
- React 19.2
- TypeScript ~6.0
- Vite 8
- Vitest 4 + React Testing Library (`@testing-library/react`, `@testing-library/user-event`, `@testing-library/jest-dom`)
- oxlint (linter)
- CSS natif (aucune librairie UI, pas de Tailwind)

## Architecture

```
Frontend (React/Vite, port 5173)
        │  fetch('/api/...')  — proxifié par Vite vers le backend
        ▼
Backend (Spring Boot, port 8080)
        │  JPA / Hibernate (ddl-auto: validate)
        ▼
PostgreSQL  ◄── schéma géré par les migrations Flyway
```

Le backend est organisé en *package-by-feature* (un package par domaine métier, pas de découpage technique controller/service/repository transverse) :

```
com.minitricount
├── group          (ExpenseGroup)
├── participant
├── expense        (+ EqualSplitCalculator)
├── balance
├── settlement
└── common.exception
```

## Principaux endpoints

| Méthode | Route                                              | Description                     |
|---------|-----------------------------------------------------|----------------------------------|
| POST    | `/api/groups`                                       | Créer un groupe                 |
| GET     | `/api/groups`                                       | Lister les groupes               |
| GET     | `/api/groups/{groupId}`                             | Détail d'un groupe               |
| POST    | `/api/groups/{groupId}/participants`                | Ajouter un participant           |
| GET     | `/api/groups/{groupId}/participants`                | Lister les participants          |
| POST    | `/api/groups/{groupId}/expenses`                    | Créer une dépense                |
| GET     | `/api/groups/{groupId}/expenses`                    | Lister les dépenses              |
| PUT     | `/api/groups/{groupId}/expenses/{expenseId}`        | Modifier une dépense             |
| DELETE  | `/api/groups/{groupId}/expenses/{expenseId}`        | Supprimer une dépense            |
| GET     | `/api/groups/{groupId}/balances`                    | Balances du groupe               |
| GET     | `/api/groups/{groupId}/settlements`                 | Remboursements suggérés          |

## Lancement local

### Prérequis

- Java 21
- Maven 3.8+
- Node.js (compatible Vite 8 / TypeScript ~6.0)
- Une instance PostgreSQL accessible

### Base de données

Par défaut (`backend/src/main/resources/application.yml`), le backend se connecte à :

- hôte : `localhost:5432`
- base : `minitricount`
- utilisateur / mot de passe : `minitricount` / `minitricount`

Ces valeurs sont surchargeables via les variables d'environnement `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` (et `SERVER_PORT` pour le port du backend).

Créer l'utilisateur et la base correspondants avant le premier lancement, en faisant de l'utilisateur applicatif le propriétaire de la base (nécessaire pour que Flyway puisse créer le schéma sans ambiguïté de permissions) :

```sql
CREATE USER minitricount WITH PASSWORD 'minitricount';
CREATE DATABASE minitricount OWNER minitricount;
```

Le schéma est ensuite créé automatiquement par Flyway au démarrage du backend (voir plus bas).

### Backend

```bash
cd backend
mvn spring-boot:run
```

Le backend démarre sur `http://localhost:8080`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Le frontend démarre sur `http://localhost:5173` et proxifie les appels `/api/*` vers `http://localhost:8080` (voir `frontend/vite.config.ts`).

## Tests

**Backend**

```bash
cd backend
mvn test
```

**Frontend**

```bash
cd frontend
npm run test    # Vitest
npm run build   # tsc -b && vite build
npm run lint    # oxlint
```

## Base de données / migrations

Le schéma est entièrement géré par Flyway ; Hibernate est configuré en `ddl-auto: validate` et ne fait donc que vérifier la cohérence entre les entités JPA et le schéma existant, sans jamais le générer ni le modifier.

Migrations actuelles (`backend/src/main/resources/db/migration/`) :
- `V1__init_schema.sql` : tables `expense_group`, `participant`
- `V2__add_expense_tables.sql` : tables `expense`, `expense_share`

## Choix techniques notables

- `BigDecimal` de bout en bout côté backend pour tous les montants (colonnes `NUMERIC(10,2)`).
- Le calcul du partage égal est isolé dans `EqualSplitCalculator`, testé indépendamment de toute logique de persistance.
- Les mutations (création/modification/suppression de dépense, ajout de participant) sont annotées `@Transactional`.
- Les balances sont agrégées côté base de données (requêtes SQL de somme), pas recalculées en Java à partir de toutes les dépenses chargées en mémoire.
- Aucun calcul financier côté frontend : le frontend affiche uniquement les valeurs déjà calculées par le backend.
- Après une mutation, le frontend ne rafraîchit que les balances et remboursements (`refreshFinancials()`), pas l'ensemble des données du groupe.
- Ce rafraîchissement ciblé est protégé contre les réponses réseau obsolètes (compteur de séquence côté frontend), pour éviter qu'une réponse arrivée en retard n'écrase un état plus récent.

## Limites de la V1 / hors scope

- Pas d'authentification ni de gestion d'utilisateurs
- Pas de multi-devise
- Pas de partage inégal entre bénéficiaires
- Pas de persistance des remboursements (recalculés à la volée depuis les balances)
- Pas de modification ni de suppression d'un participant
- Pas de modification ni de suppression d'un groupe
- Pas de routing par URL côté frontend (navigation entièrement pilotée par l'état React local)

## Structure du repository

```
miniTricount/
├── backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/minitricount/   (group, participant, expense, balance, settlement, common)
│       │   └── resources/
│       │       ├── application.yml
│       │       └── db/migration/
│       └── test/java/com/minitricount/
└── frontend/
    ├── package.json
    ├── vite.config.ts
    └── src/
        ├── api/
        ├── components/
        └── types/
```
