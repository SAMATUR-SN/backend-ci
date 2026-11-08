<!-- This is a Kanban Board file created with MD Kanban extension -->
<!-- GitHub: https://github.com/jebakumarj/md-kanban -->
<!-- VS Code Extension: Search "MD Kanban" in the extension store (ID: jeddak.md-kanban) -->

# Brojet_Backend_Spring_SamaTur Board

## To Do

## In Progress

### Backends

#### Conteneurisation
<!-- id: task-1794095569939-79 -->
Rédiger un Dockerfile multi-stage : une première étape utilise l’image de build Java 25 et Maven
pour produire l’artefact; l’étape finale utilise une image JRE 25 légère, exécute le processus avec
un utilisateur non root et ne contient ni sources ni outils de compilation inutiles.
Le conteneur doit recevoir la configuration par variables d’environnement, exposer le port prévu,
arrêter proprement l’application et fournir un point de santé exploitable. Les secrets ne sont ni
copiés dans l’image ni écrits dans les logs.
Tags: `urgent`
<!-- due: 2026-11-07 -->
<!-- assignee: Gana -->

## Done

### Backends

#### Étape 1– Initialisation du projet et de la base DB
<!-- id: task-1794048551495-0 -->
- [x] La génération et la configuration de la structure du projet Spring Boot
- [x] La mise en place de la base de données PostgreSQL et des scripts de migration Flyway
Tags: `urgent`
<!-- priority: high -->
<!-- due: 2026-11-07 -->
<!-- assignee: Gana -->

#### Étape 2 : Sécurité et Authentification JWT pour le backend de Sama Tur.
<!-- id: task-1794058944939-14 -->
Protéger les endpoints et appliquer les autorisations par rôle et par structure dès le début des
fonctions métier
- [x] Configurer Spring Security en mode stateless : aucune session HTTP serveur, et CSRF désactivé pour l’API utilisant des jetons Bearer. Définir une politique CORS explicite pour le client Angular
- [x] Implémenter le filtre JWT : lire Authorization: Bearer, vérifier signature, expiration et for mat, puis créer le contexte d’authentification Spring avec le rôle et le rattachement organisa tionnel validés.
- [x] Créer l’endpoint public POST /api/auth/connexion. Vérifier le mot de passe avec BCrypt et renvoyer un DTO de connexion contenant le jeton et les informations utilisateur nécessaires au client, sans jamais exposer le hash.
- [x] Protéger les routes privées selon les rôles autorisés. Les règles de périmètre F10 sont appliquées côté serveur dans les services et requêtes de données; elles ne reposent pas sur un identifiant d’école fourni par le navigateur.
- [x] Configurer le secret JWT par variable d’environnement, sans valeur par défaut utilisable en production. Définir une durée d’expiration et documenter la procédure de rotation du secret.
Tags: `urgent`
<!-- priority: high -->
<!-- workload: hard -->
<!-- due: 2026-11-07 -->
<!-- assignee: Gana -->

#### Étape 3 : Implémentation des Modules Métiers (Package-by-Feature)
<!-- id: task-1794059363891-23 -->
Coder la logique métier en suivant strictement le contrat d'API et les règles de gestion
- [x] Module Référentiel & Élève :  Exposer les routes de lecture des référentiels (communes, écoles, centres d'état civil, tribunaux).  Mettre en place l'importation de fichiers CSV pour les élèves (F1) avec gestion du rapport d'erreurs ligne par ligne.
- [x] Module Dossier (Le Cœur Métier) :Implémenter la consultation paginée et filtrée des dossiers (GET /api/dossiers).Coder la machine à états pour gérer le cycle de vie du dossier à travers ses 6 étapes strictes (DETECTE ➔ CERTIFICAT OBTENU ➔ DOSSIER COMPLET ➔ JUGEMENT RENDU ➔ TRANSCRIT ➔ ACTE REMIS) via l'endpoint d'avancement (POST /api/dossiers/{id}/avancer), tout en vérifiant les rôles et les champs obligatoires.   Assurer l'enregistrement systématique de chaque transition dans la table HistoriqueEtape au sein de la même transaction.
- [x] Module Indicateurs & Alertes :  Implémenter le calcul automatique des retards par rapport au délai cible configuré (par exemple, 15 jours pour l'étape JUGEMENT RENDU).  Exposer les endpoints des indicateurs globaux/communaux et la liste des élèves bloqués en CM2 (F9).
Tags: `urgent`
<!-- priority: high -->
<!-- workload: hard -->
<!-- due: 2026-11-07 -->
<!-- assignee: Gana -->

#### Étape 4– Documentation, tests de régression et déploiement
<!-- id: task-1794092225116-54 -->
Documentation et vérification fonctionnelle
— vérifier la génération de la spécification OpenAPI et l’accès à Swagger UI
Tags: `urgent`
<!-- due: 2026-11-07 -->
<!-- assignee: Gana -->
