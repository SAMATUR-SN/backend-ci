# Documentation de l'API SamaTur

## Documentation interactive

L'API utilise Springdoc OpenAPI. Après le démarrage de l'application :

- Swagger UI : `http://localhost:8080/swagger-ui/index.html`
- Spécification OpenAPI JSON : `http://localhost:8080/v3/api-docs`

Les deux routes de documentation sont publiques. Pour appeler une route protégée dans Swagger UI, utiliser **Authorize** et fournir le JWT renvoyé par `POST /api/auth/connexion` (sans ajouter manuellement le préfixe `Bearer`).

## Collections de requêtes

- Bruno : ouvrir le dossier `bruno/samatur-api` et sélectionner l'environnement `local`.
- Postman : importer `postman/samatur-api.postman_collection.json` et, si souhaité, `postman/samatur-local.postman_environment.json`.
- Dans les deux outils, renseigner l'identifiant et le mot de passe de l'environnement local. La requête de connexion stocke le JWT pour les appels suivants.

Base URL par défaut : `http://localhost:8080`.

## Configuration locale

Utiliser `.env.example` comme modèle pour définir les variables dans l'environnement de lancement, notamment `SPRING_DATASOURCE_PASSWORD` et `SAMATUR_JWT_SECRET`. Spring Boot ne charge pas automatiquement un fichier `.env`. Générer une clé JWT aléatoire d'au moins 32 octets et ne pas la partager ni la committer. `SAMATUR_CORS_ALLOWED_ORIGINS` accepte une liste d'origines séparées par des virgules ; par défaut, les frontends locaux sur les ports 3000 et 5173 sont autorisés. Les fichiers `.env` sont ignorés par Git ; seul le modèle `.env.example` est versionné.

## Routes exposées actuellement

| Méthode | Route | Accès |
|---|---|---|
| POST | `/api/auth/connexion` | Public |
| GET | `/api/eleves` | Rôle ECOLE, SUPERVISEUR ou ADMIN |
| GET | `/api/eleves/{id}` | Rôle ECOLE, SUPERVISEUR ou ADMIN |
| POST | `/api/eleves` | Rôle ECOLE ou SUPERVISEUR |
| PUT | `/api/eleves/{id}` | Rôle ECOLE ou SUPERVISEUR |
| DELETE | `/api/eleves/{id}` | Rôle ECOLE ou SUPERVISEUR |
| POST | `/api/eleves/import` | Rôle ECOLE ou SUPERVISEUR ; CSV multipart `file` |
| GET | `/api/referentiels/ecoles` | Tout rôle authentifié |
| GET | `/api/referentiels/communes` | Tout rôle authentifié |
| GET | `/api/dossiers` | Rôle ECOLE, ETAT_CIVIL, TRIBUNAL ou SUPERVISEUR |
| POST | `/api/dossiers` | Rôle ECOLE ou SUPERVISEUR ; JSON `reference`, `eleveId` |
| GET | `/api/dossiers/{id}` | Rôle ECOLE, ETAT_CIVIL, TRIBUNAL ou SUPERVISEUR |
| GET | `/api/dossiers/{id}/historique` | Rôle ECOLE, ETAT_CIVIL, TRIBUNAL ou SUPERVISEUR |
| POST | `/api/dossiers/{id}/avancer` | Rôle ECOLE, ETAT_CIVIL ou TRIBUNAL |
| GET | `/api/alertes` | Tout utilisateur authentifié |
| POST | `/api/alertes/verifier` | Tout utilisateur authentifié |
| PATCH | `/api/alertes/{id}/resoudre` | Tout utilisateur authentifié |
| GET | `/api/statistiques/dashboard` | Tout utilisateur authentifié |

Les contrôles de rôle sont ceux déclarés dans les contrôleurs. L'authentification JWT est exigée pour toutes les routes sauf `/api/auth/**`, la documentation Swagger et `/actuator/**`.

## Séquence conseillée

1. Démarrer PostgreSQL et l'application.
2. Vérifier la connexion avec `POST /api/auth/connexion`.
3. Récupérer les écoles et communes via `/api/referentiels/ecoles` et `/api/referentiels/communes`.
4. Créer ou modifier un élève en fournissant son école et sa commune de naissance, ou importer un CSV séparé par `;`.
5. Créer un dossier avec une référence et l'identifiant d'un élève, puis consulter son historique.
6. Faire avancer un dossier ; cette opération modifie son état et écrit une transition dans l'historique.
7. Consulter le tableau de bord ou vérifier les alertes.

Le CSV doit contenir, dans cet ordre, les colonnes `numero_ine;nom;prenom;classe;ecole_id;date_naissance;sexe;commune_naissance_id`. La colonne facultative `date_naissance_presumee` vient en neuvième position. Les identifiants d'école et de commune doivent exister dans les référentiels.

Les mutations ne sont pas toutes réversibles : en particulier, faire avancer un dossier, résoudre une alerte et supprimer un élève modifient les données.

## Notes sur le contrat

- Les statuts du backend suivent ceux de la contrainte SQL : `DETECTE`, `CERTIFICAT_ETABLI`, `DOSSIER_COMPLET`, `JUGEMENT_RENDU`, `TRANSCRIT` et `REMIS`.
- La migration Flyway V5 crée la table `alerte`, nécessaire aux routes d'alertes et aux statistiques.
- Le rôle `ADMIN` est encore accepté par certaines annotations des routes élèves, mais la contrainte SQL de `utilisateur.role` ne permet que `ECOLE`, `ETAT_CIVIL`, `TRIBUNAL` et `SUPERVISEUR`.
- Les contrôleurs exposent directement des entités JPA, pas des DTO dédiés. La forme exacte des réponses dépend donc de la sérialisation des relations.
- La création d'un élève ne crée pas automatiquement de dossier ; créer le dossier séparément via `POST /api/dossiers`.

Les migrations Flyway s'exécutent au démarrage. Vérifier que les identifiants PostgreSQL et `SAMATUR_JWT_SECRET` sont définis dans l'environnement avant de lancer l'application.
