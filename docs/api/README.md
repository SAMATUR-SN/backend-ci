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

Utiliser `.env.example` comme modèle pour définir les variables dans l'environnement de lancement, notamment `SPRING_DATASOURCE_PASSWORD` et `SAMATUR_JWT_SECRET`. Spring Boot ne charge pas automatiquement un fichier `.env`. Générer une clé JWT aléatoire d'au moins 32 octets et ne pas la partager ni la committer. Les fichiers `.env` sont ignorés par Git ; seul le modèle `.env.example` est versionné.

## Routes exposées actuellement

| Méthode | Route | Accès |
|---|---|---|
| POST | `/api/auth/connexion` | Public |
| GET | `/api/eleves` | Rôle ECOLE, SUPERVISEUR ou ADMIN |
| GET | `/api/eleves/{id}` | Rôle ECOLE, SUPERVISEUR ou ADMIN |
| POST | `/api/eleves` | Rôle ECOLE ou SUPERVISEUR |
| PUT | `/api/eleves/{id}` | Rôle ECOLE ou SUPERVISEUR |
| DELETE | `/api/eleves/{id}` | Rôle ECOLE ou SUPERVISEUR |
| POST | `/api/eleves/import` | Rôle ECOLE ou SUPERVISEUR ; multipart `file` |
| GET | `/api/dossiers` | Rôle ECOLE, ETAT_CIVIL, TRIBUNAL ou SUPERVISEUR |
| GET | `/api/dossiers/{id}` | Rôle ECOLE, ETAT_CIVIL, TRIBUNAL ou SUPERVISEUR |
| POST | `/api/dossiers/{id}/avancer` | Rôle ECOLE, ETAT_CIVIL ou TRIBUNAL |
| GET | `/api/alertes` | Tout utilisateur authentifié |
| POST | `/api/alertes/verifier` | Tout utilisateur authentifié |
| PATCH | `/api/alertes/{id}/resoudre` | Tout utilisateur authentifié |
| GET | `/api/statistiques/dashboard` | Tout utilisateur authentifié |

Les contrôles de rôle sont ceux déclarés dans les contrôleurs. L'authentification JWT est exigée pour toutes les routes sauf `/api/auth/**`, la documentation Swagger et `/actuator/**`.

## Séquence conseillée

1. Démarrer PostgreSQL et l'application.
2. Vérifier la connexion avec `POST /api/auth/connexion`.
3. Lire les élèves et les dossiers.
4. Créer ou modifier un élève, ou importer un CSV dont le séparateur est `;`.
5. Faire avancer un dossier ; cette opération modifie son état et écrit un historique.
6. Consulter le tableau de bord ou vérifier les alertes.

Les mutations ne sont pas toutes réversibles : en particulier, faire avancer un dossier, résoudre une alerte et supprimer un élève modifient les données.

## Limites connues du contrat actuel

- `ReferentielController.java` est vide : aucune route de lecture des écoles, communes, centres d'état civil ou tribunaux n'est exposée. La création d'un élève attend pourtant des objets `ecole` et éventuellement `communeNaissance`.
- Les statuts permis par la contrainte SQL de la migration V1 ne correspondent pas tous à `StatutDossier` et aux transitions du service. Une transition peut donc échouer en base.
- `Alerte` et ses routes existent dans le code Java, mais les migrations V1 à V4 ne créent pas la table `alerte`. Le schéma doit être complété avant de valider les routes d'alertes et les statistiques associées.
- Le rôle `ADMIN` est accepté par certaines annotations de contrôleur, mais la contrainte SQL de `utilisateur.role` ne permet que `ECOLE`, `ETAT_CIVIL`, `TRIBUNAL` et `SUPERVISEUR`.
- Les contrôleurs exposent directement des entités JPA, pas des DTO dédiés. La forme exacte des réponses dépend donc de la sérialisation des relations.
- La création d'un élève ne crée pas automatiquement de dossier ; aucun endpoint HTTP de création de dossier n'est exposé.

La collection inclut les routes existantes, mais les opérations touchant les tables ou statuts signalés ci-dessus peuvent échouer jusqu'à la correction du schéma et du modèle.
