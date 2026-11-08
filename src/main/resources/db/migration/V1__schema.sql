-- =========================================================================
-- PROJET SAMA TUR - MIGRATION V1 : Schéma initial de la base de données
-- =========================================================================

-- Extension pour la génération d'UUID si nécessaire (optionnel mais utile)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 1. Table des Communes (Référentiel géographique)
CREATE TABLE commune (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(150) NOT NULL UNIQUE,
    departement VARCHAR(100) NOT NULL DEFAULT 'Bambey',
    region VARCHAR(100) NOT NULL DEFAULT 'Diourbel'
);

-- 2. Table des Écoles
CREATE TABLE ecole (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(200) NOT NULL,
    code_ecole VARCHAR(50) NOT NULL UNIQUE,
    ief VARCHAR(100) NOT NULL DEFAULT 'IEF Bambey',
    commune_id BIGINT NOT NULL,
    CONSTRAINT fk_ecole_commune FOREIGN KEY (commune_id) REFERENCES commune(id)
);

-- 3. Table des Centres d'État Civil
CREATE TABLE centre_etat_civil (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(200) NOT NULL,
    commune_id BIGINT NOT NULL,
    CONSTRAINT fk_centre_commune FOREIGN KEY (commune_id) REFERENCES commune(id)
);

-- 4. Table des Tribunaux d'Instance
CREATE TABLE tribunal_instance (
    id BIGSERIAL PRIMARY KEY,
    nom VARCHAR(200) NOT NULL DEFAULT 'Tribunal d''Instance de Bambey',
    ville VARCHAR(100) NOT NULL DEFAULT 'Bambey'
);

-- 5. Table des Utilisateurs & Sécurité (Authentification / Rôles / Périmètre F10)
CREATE TABLE utilisateur (
    id BIGSERIAL PRIMARY KEY,
    identifiant VARCHAR(100) NOT NULL UNIQUE,
    mot_de_passe_hash VARCHAR(255) NOT NULL,
    nom_affiche VARCHAR(150) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('ECOLE', 'ETAT_CIVIL', 'TRIBUNAL', 'SUPERVISEUR')),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    ecole_id BIGINT,
    centre_etat_civil_id BIGINT,
    tribunal_instance_id BIGINT,
    CONSTRAINT fk_user_ecole FOREIGN KEY (ecole_id) REFERENCES ecole(id),
    CONSTRAINT fk_user_centre FOREIGN KEY (centre_etat_civil_id) REFERENCES centre_etat_civil(id),
    CONSTRAINT fk_user_tribunal FOREIGN KEY (tribunal_instance_id) REFERENCES tribunal_instance(id)
);

-- 6. Table des Élèves (avec informations sur les parents)
CREATE TABLE eleve (
    id BIGSERIAL PRIMARY KEY,
    prenom VARCHAR(150) NOT NULL,
    nom VARCHAR(100) NOT NULL,
    sexe CHAR(1) NOT NULL CHECK (sexe IN ('M', 'F')),
    date_naissance DATE NOT NULL,
    date_naissance_presumee BOOLEAN NOT NULL DEFAULT FALSE,
    classe VARCHAR(20) NOT NULL DEFAULT 'CM2',
    ecole_id BIGINT NOT NULL,
    commune_naissance_id BIGINT NOT NULL,
    -- Informations sur les parents
    nom_pere VARCHAR(150),
    profession_pere VARCHAR(100),
    nom_mere VARCHAR(150),
    profession_mere VARCHAR(100),
    CONSTRAINT fk_eleve_ecole FOREIGN KEY (ecole_id) REFERENCES ecole(id),
    CONSTRAINT fk_eleve_commune FOREIGN KEY (commune_naissance_id) REFERENCES commune(id)
);

-- 7. Table des Dossiers (Cœur Métier - Cycle de vie en 6 étapes)
CREATE TABLE dossier (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(100) NOT NULL UNIQUE,
    eleve_id BIGINT NOT NULL UNIQUE,
    etape VARCHAR(50) NOT NULL DEFAULT 'DETECTE' CHECK (etape IN ('DETECTE', 'CERTIFICAT_ETABLI', 'DOSSIER_COMPLET', 'JUGEMENT_RENDU', 'TRANSCRIT', 'REMIS')),
    date_detection TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Métadonnées des étapes successives
    numero_certificat VARCHAR(100),
    date_certificat TIMESTAMP,
    numero_jugement VARCHAR(100),
    date_jugement TIMESTAMP,
    type_audience VARCHAR(100),
    numero_acte VARCHAR(100),
    date_transcription TIMESTAMP,
    date_remise TIMESTAMP,
    
    -- Rattachements opérationnels
    centre_etat_civil_id BIGINT,
    tribunal_instance_id BIGINT,
    
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_dossier_eleve FOREIGN KEY (eleve_id) REFERENCES eleve(id),
    CONSTRAINT fk_dossier_centre FOREIGN KEY (centre_etat_civil_id) REFERENCES centre_etat_civil(id),
    CONSTRAINT fk_dossier_tribunal FOREIGN KEY (tribunal_instance_id) REFERENCES tribunal_instance(id)
);

-- 8. Table des Témoins (0 à 2 par dossier)
CREATE TABLE temoin (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL,
    nom_complet VARCHAR(200) NOT NULL,
    numero_piece VARCHAR(100),
    CONSTRAINT fk_temoin_dossier FOREIGN KEY (dossier_id) REFERENCES dossier(id) ON DELETE CASCADE
);

-- 9. Table d'Historique des Étapes (Traçabilité immuable)
CREATE TABLE historique_etape (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL,
    etape_depart VARCHAR(50),
    etape_arrivee VARCHAR(50) NOT NULL,
    date_heure TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    auteur_id BIGINT,
    role_auteur VARCHAR(50),
    commentaire TEXT,
    CONSTRAINT fk_historique_dossier FOREIGN KEY (dossier_id) REFERENCES dossier(id) ON DELETE CASCADE,
    CONSTRAINT fk_historique_auteur FOREIGN KEY (auteur_id) REFERENCES utilisateur(id)
);

-- Index pour optimiser les performances de recherche et des indicateurs
CREATE INDEX idx_dossier_etape ON dossier(etape);
CREATE INDEX idx_eleve_ecole ON eleve(ecole_id);
CREATE INDEX idx_historique_dossier ON historique_etape(dossier_id);