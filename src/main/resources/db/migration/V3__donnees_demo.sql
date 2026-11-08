-- =========================================================================
-- PROJET SAMA TUR - MIGRATION V3 : Données de démonstration & Utilisateurs
-- =========================================================================

-- 1. Insertion des Utilisateurs de test (Mots de passe hachés BCrypt par défaut pour 'password123')
-- Hash BCrypt de 'password123' : $2a$10$Xptv41.8h0m8.8m8.8m8.8u8.8m8.8m8.8m8.8m8.8m8.8m8.8m8m
INSERT INTO utilisateur (id, identifiant, mot_de_passe_hash, nom_affiche, role, actif, ecole_id, centre_etat_civil_id, tribunal_instance_id) VALUES
(1, 'ecole1@samatur.sn', '$2a$10$8.8m8.8m8.8m8.8m8.8m8.u8.8m8.8m8.8m8.8m8.8m8.8m8.8m8m', 'Directeur École Bambey Station', 'ECOLE', TRUE, 1, NULL, NULL),
(2, 'etatcivil1@samatur.sn', '$2a$10$8.8m8.8m8.8m8.8m8.8m8.u8.8m8.8m8.8m8.8m8.8m8.8m8.8m8m', 'Officier État Civil Bambey', 'ETAT_CIVIL', TRUE, NULL, 1, NULL),
(3, 'tribunal1@samatur.sn', '$2a$10$8.8m8.8m8.8m8.8m8.8m8.u8.8m8.8m8.8m8.8m8.8m8.8m8.8m8m', 'Greffier Tribunal Bambey', 'TRIBUNAL', TRUE, NULL, NULL, 1),
(4, 'superviseur@samatur.sn', '$2a$10$8.8m8.8m8.8m8.8m8.8m8.u8.8m8.8m8.8m8.8m8.8m8.8m8.8m8m', 'Superviseur IEF Bambey', 'SUPERVISEUR', TRUE, NULL, NULL, NULL);

-- 2. Insertion d'élèves de test en classe de CM2 sans acte de naissance
INSERT INTO eleve (id, prenom, nom, sexe, date_naissance, date_naissance_presumee, classe, ecole_id, commune_naissance_id, nom_pere, profession_pere, nom_mere, profession_mere) VALUES
(1, 'Mamadou', 'Diop', 'M', '2014-05-12', FALSE, 'CM2', 1, 1, 'Abdou Diop', 'Agriculteur', 'Aissatou Ba', 'Ménagère'),
(2, 'Fatou', 'Sarr', 'F', '2013-09-20', TRUE, 'CM2', 1, 1, 'Ousmane Sarr', 'Commerçant', 'Sokhna Ndiaye', 'Couturière'),
(3, 'Modou', 'Faye', 'M', '2014-02-15', FALSE, 'CM2', 2, 2, 'Ibrahima Faye', 'Éleveur', 'Mariama Diallo', 'Agricultrice');

-- 3. Insertion de dossiers associés aux élèves (Cycle de vie initial)
INSERT INTO dossier (id, reference, eleve_id, etape, date_detection, centre_etat_civil_id, tribunal_instance_id) VALUES
(1, 'DOS-2026-0001', 1, 'DETECTE', CURRENT_TIMESTAMP, 1, 1),
(2, 'DOS-2026-0002', 2, 'CERTIFICAT_ETABLI', CURRENT_TIMESTAMP, 1, 1),
(3, 'DOS-2026-0003', 3, 'DOSSIER_COMPLET', CURRENT_TIMESTAMP, 2, 1);


UPDATE utilisateur 
SET mot_de_passe_hash = '$2a$12$W/ePSrQOwWVtifh6O8Icb.cKvWbPaZt1Z9jksZ99V0cD2uDrULeZ6' 
WHERE identifiant = 'ecole1@samatur.sn';