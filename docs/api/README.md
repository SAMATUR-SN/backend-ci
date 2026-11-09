# Documentation de l'API SamaTur

## Documentation interactive

L'API utilise Springdoc OpenAPI. Après le démarrage de l'application :

- Swagger UI : `http://localhost:8080/swagger-ui/index.html`
- Spécification OpenAPI JSON : `http://localhost:8080/v3/api-docs`

Les deux routes de documentation sont publiques. Pour appeler une route protégée dans Swagger UI, utiliser **Authorize** et fournir le jeton (`jeton`) renvoyé par `POST /api/auth/connexion`, sans ajouter le préfixe `Bearer`.

## Collections de requêtes

- Bruno : ouvrir le dossier `bruno/samatur-api` et sélectionner l'environnement `local`.
- Postman : importer `postman/samatur-api.postman_collection.json` et, si souhaité, `postman/samatur-api.postman_environment.json`.
- Dans les deux outils, renseigner l'identifiant et le mot de passe de l'environnement local. La requête de connexion stocke le jeton pour les appels suivants.

Base URL par défaut : `http://localhost:8080`.

## Configuration locale

Utiliser `.env.example` comme modèle pour définir les variables dans l'environnement de lancement, notamment `SPRING_DATASOURCE_PASSWORD` et `SAMATUR_JWT_SECRET`. Avec `compose.dev.yaml`, le fichier `.env` est lu par Docker Compose : `docker compose -f compose.dev.yaml up` lance la base et l'API. Depuis un IDE, Spring Boot ne charge pas ce fichier : définir les variables dans la configuration de lancement. Générer une clé JWT aléatoire d'au moins 32 octets et ne pas la partager ni la committer. Les fichiers `.env` sont ignorés par Git ; seul le modèle `.env.example` est versionné.

## Routes exposées

Toutes les routes métier sont sous `/api`, en JSON, avec des champs en camelCase et des dates au format ISO (`2026-11-04`).

| Méthode | Route | Accès | Réponse |
|---|---|---|---|
| GET | `/health` | Public | `{"status":"UP"}` |
| POST | `/api/auth/connexion` | Public | `jeton`, `expireA`, `utilisateur` |
| GET | `/api/auth/moi` | Connecté | utilisateur courant : `identifiant`, `nomAffiche`, `role`, `structure` |
| GET | `/api/referentiels/communes` | Connecté | `[{id, nom}]` |
| GET | `/api/referentiels/ecoles` | Connecté | `[{id, code, nom, communeId}]` |
| GET | `/api/referentiels/centres` | Connecté | `[{id, nom}]` |
| GET | `/api/referentiels/tribunaux` | Connecté | `[{id, nom}]` |
| POST | `/api/eleves/import` | Rôle ECOLE | fichier CSV en multipart, champ `file` ; rapport d'import (voir ci-dessous) |
| GET | `/api/eleves` | Rôle ECOLE ou SUPERVISEUR | liste d'élèves (hors contrat initial) |
| GET | `/api/eleves/{id}` | Rôle ECOLE ou SUPERVISEUR | un élève (hors contrat initial) |
| GET | `/api/dossiers` | Connecté | page de résumés de dossiers (voir ci-dessous) |
| GET | `/api/dossiers/{id}` | Connecté | dossier complet : élève, données de chaque étape, témoins, historique |
| POST | `/api/dossiers/{id}/avancer` | Rôle autorisé pour l'étape visée | dossier complet mis à jour |
| GET | `/api/alertes` | Connecté | alertes actives (revues au bloc 6) |
| POST | `/api/alertes/verifier` | Rôle ETAT_CIVIL | lance la vérification des retards |
| PATCH | `/api/alertes/{id}/resoudre` | Rôle ETAT_CIVIL | alerte résolue |
| GET | `/api/indicateurs` | Connecté | indicateurs du tableau de bord (I1, I2, I3 au bloc 6) |

Les élèves arrivent par l'import CSV : il n'y a ni création, ni modification, ni suppression unitaire d'un élève (décision du 09/11), et aucun dossier ni historique n'est jamais supprimé (RG-13).

## Authentification

`POST /api/auth/connexion` reçoit `{"identifiant": "...", "motDePasse": "..."}` et répond :

```json
{
  "jeton": "eyJhbGciOiJIUzI1NiJ9...",
  "expireA": "2026-11-05T11:00:00Z",
  "utilisateur": { "identifiant": "tribunal1@samatur.sn", "nomAffiche": "Greffier Tribunal Bambey", "role": "TRIBUNAL", "structure": "Tribunal d'Instance de Bambey" }
}
```

Toutes les autres requêtes `/api/**` portent l'en-tête `Authorization: Bearer <jeton>`. Les jetons sont signés en HS256 et valables 8 heures. Sans jeton, ou avec un jeton expiré ou invalide, l'API répond 401 ; avec un rôle non autorisé, 403. Une connexion refusée (identifiant inconnu, mauvais mot de passe ou compte désactivé) répond toujours 401 avec le même message. `structure` vaut `null` pour le superviseur, qui n'est rattaché à aucune structure.

## Lister les dossiers

`GET /api/dossiers` accepte des filtres facultatifs : `etape`, `communeId` (commune de naissance), `ecoleId`, `classe`, `enRetard` (`true` : jugement non transcrit au-delà du délai cible), `q` (recherche dans la référence, le nom et le prénom). La pagination se règle avec `page` (à partir de 0) et `taille` (20 par défaut, 100 au maximum) ; `tri` vaut `dateDetection,desc` par défaut, ou `reference`, `dateDetection`, `dateJugement` suivi de `,asc` ou `,desc`.

```json
{
  "contenu": [{
    "id": 42,
    "reference": "ST-2026-000042",
    "eleve": { "prenom": "Awa", "nom": "Diouf", "classe": "CM2", "ecole": "École A de Bambey" },
    "communeNaissance": "Bambey",
    "etape": "JUGEMENT_RENDU",
    "dateDerniereEtape": "2026-11-03",
    "enRetard": false,
    "joursDepuisJugement": 1
  }],
  "page": 0, "taille": 20, "totalElements": 137, "totalPages": 7
}
```

`dateDerniereEtape` est la date la plus récente parmi celles des étapes franchies.

## Importer des élèves (CSV)

`POST /api/eleves/import` (rôle ECOLE) reçoit un fichier dans le champ multipart `file` : CSV UTF-8, séparateur point-virgule, une ligne d'en-tête, 2 Mo et 1 000 lignes au maximum. Depuis Excel, enregistrer au format **« CSV UTF-8 (délimité par des points-virgules) »** : l'ancien format « CSV » n'est pas en UTF-8 et le fichier est refusé. Les colonnes sont reconnues par leur nom, dans n'importe quel ordre.

| Colonne | Obligatoire | Format |
|---|---|---|
| `prenom`, `nom` | oui | texte |
| `sexe` | oui | `F` ou `M` |
| `date_naissance` | oui | `JJ/MM/AAAA`, dans le passé |
| `date_naissance_presumee` | non | `oui` ou `non`, défaut `non` |
| `commune_naissance` | oui | nom d'une commune du référentiel (`GET /api/referentiels/communes`) |
| `code_ecole` | oui | code d'une école du référentiel (`GET /api/referentiels/ecoles`) |
| `classe` | oui | `PRESCOLAIRE`, `CI`, `CP`, `CE1`, `CE2`, `CM1` ou `CM2` |
| `nom_mere` | oui | texte |
| `nom_pere` | non | texte |
| `telephone_parent` | non | `+221` suivi de 9 chiffres |
| `langue_contact` | non | `FRANCAIS` (défaut), `WOLOF`, `PULAAR` ou `SEREER` |
| `numero_ine` | non | numéro INE, unique |

L'import est ligne à ligne : une ligne invalide est rejetée avec son numéro (en-tête compris, comme dans Excel) et son motif ; chaque ligne valide crée un élève et son dossier à l'étape `DETECTE`, avec sa référence et sa première ligne d'historique. Une ligne est un doublon si un élève de même prénom, nom, date de naissance et école existe déjà (RG-07).

```json
{"total": 10, "importes": 8, "rejetes": 2, "erreurs": [
  {"ligne": 10, "motif": "commune inconnue : Bambeye"},
  {"ligne": 11, "motif": "doublon : un élève de même prénom, nom, date de naissance et école existe déjà"}
]}
```

Un fichier d'exemple est versionné dans `src/test/resources/import-exemple.csv` : Awa, l'élève du scénario de démo, et une ligne volontairement fausse.

## Faire avancer un dossier

`POST /api/dossiers/{id}/avancer` reçoit `etapeCible`, qui doit être l'étape suivant l'étape courante, et les champs exigés par cette étape. Le rôle vient du jeton, jamais de la requête.

| Étape visée | Rôle | Champs obligatoires |
|---|---|---|
| `CERTIFICAT_OBTENU` | ETAT_CIVIL | `numeroCertificat`, `dateCertificat`, `centreEtatCivilId` |
| `DOSSIER_COMPLET` | ECOLE | `preuveNaissance` ; si `TEMOINS`, `temoins` = deux `{nomComplet, numeroPiece}` |
| `JUGEMENT_RENDU` | TRIBUNAL | `numeroJugement`, `dateJugement`, `tribunalId`, `typeAudience` (`ORDINAIRE`, `SPECIALE`, `FORAINE`) |
| `TRANSCRIT` | ETAT_CIVIL | `numeroActe`, `anneeActe`, `dateTranscription` |
| `ACTE_REMIS` | ECOLE | `dateRemise` |

`commentaire` est facultatif et rejoint l'historique. Les dates doivent se suivre (détection, certificat, jugement, transcription, remise) et aucune ne peut être dans le futur. La réponse est le dossier complet, comme `GET /api/dossiers/{id}` : élève, données de chaque étape, témoins, historique, et `enRetard` calculé avec le délai cible.

## Erreurs

Toutes les erreurs suivent le format `application/problem+json` (RFC 9457) :

| Code | Quand |
|---|---|
| 400 | JSON illisible, paramètre mal typé ou valeur inconnue, tri inconnu |
| 401 | jeton absent, expiré ou invalide ; identifiants de connexion refusés |
| 403 | connecté, mais rôle non autorisé pour cette action |
| 404 | dossier, élève ou alerte inexistant |
| 409 | étape déjà franchie, dossier terminé, doublon ou dossier modifié entre-temps |
| 422 | saut d'étape, champ manquant (liste dans `erreurs`) ou dates incohérentes |

```json
{
  "type": "https://samatur.sn/erreurs/champ-obligatoire",
  "title": "Champ obligatoire manquant",
  "status": 422,
  "detail": "Le passage à JUGEMENT_RENDU exige : dateJugement, tribunalId, typeAudience.",
  "erreurs": [ { "champ": "dateJugement", "message": "obligatoire" } ]
}
```

## Séquence conseillée

1. Démarrer PostgreSQL et l'application.
2. Se connecter avec `POST /api/auth/connexion`, puis vérifier le profil avec `GET /api/auth/moi`.
3. Charger les référentiels, puis la liste des dossiers.
4. Faire avancer un dossier avec le compte du rôle de l'étape ; cette opération modifie son état et écrit un historique.
5. Consulter les alertes et les indicateurs.

Faire avancer un dossier et résoudre une alerte modifient les données ; ces opérations ne se défont pas.

## Données de démonstration

Le dossier suit six étapes, identiques dans l'enum `StatutDossier` et dans la contrainte SQL : `DETECTE`, `CERTIFICAT_OBTENU`, `DOSSIER_COMPLET`, `JUGEMENT_RENDU`, `TRANSCRIT`, `ACTE_REMIS`.

La migration V3 crée 40 élèves fictifs du département de Bambey, répartis sur les six étapes, dont 5 dossiers en retard (jugement non transcrit depuis plus de 15 jours). Les dates sont relatives au jour du démarrage. Les mots de passe des quatre comptes de démo (`ecole1@samatur.sn`, `etatcivil1@samatur.sn`, `tribunal1@samatur.sn`, `superviseur@samatur.sn`) sont communiqués hors du dépôt.
