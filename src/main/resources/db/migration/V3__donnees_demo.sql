-- =========================================================================
-- PROJET SAMA TUR - MIGRATION V3 : Données de démonstration & Utilisateurs
-- Données entièrement fictives. Les dates sont relatives à CURRENT_DATE pour
-- que les retards et les indicateurs existent quel que soit le jour de la démo.
-- =========================================================================

-- 1. Comptes de démo, un par rôle. Hachages BCrypt réels ; les mots de passe
-- circulent dans l'équipe hors du dépôt.
INSERT INTO utilisateur (id, identifiant, mot_de_passe_hash, nom_affiche, role, actif, ecole_id, centre_etat_civil_id, tribunal_instance_id) VALUES
(1, 'ecole1@samatur.sn', '$2a$10$4NtuwuXBkX7ZBBmr7IT9juaagYye9rnPQ6rWE6Ttg3aagReE6WChe', 'Directeur École Bambey Station', 'ECOLE', TRUE, 1, NULL, NULL),
(2, 'etatcivil1@samatur.sn', '$2a$10$W.FX6.7SDSMmKfqSs.B4n.HS.FzZVqsAbJX4LnCLLsVX/nUAFqJWa', 'Officier État Civil Bambey', 'ETAT_CIVIL', TRUE, NULL, 1, NULL),
(3, 'tribunal1@samatur.sn', '$2a$10$ERNf8w/WdGihE5RUHkbjmu9q4OKz93uy4ZtWBFFJA8WX36VXnlX6q', 'Greffier Tribunal Bambey', 'TRIBUNAL', TRUE, NULL, NULL, 1),
(4, 'superviseur@samatur.sn', '$2a$10$E1eGcH9X0Y0JleqzrJhNYe8wS6bkmLBAwBAFKMam9hyqFGwUf.WDm', 'Superviseur IEF Bambey', 'SUPERVISEUR', TRUE, NULL, NULL, NULL);

-- 2. Quarante élèves sans acte de naissance, répartis sur les 5 écoles de V2.
-- Téléphones manifestement fictifs (+2217700000NN).
INSERT INTO eleve (id, prenom, nom, sexe, date_naissance, date_naissance_presumee, classe, ecole_id, commune_naissance_id, nom_pere, nom_mere, telephone_parent, langue_contact) VALUES
(1, 'Mamadou', 'Diop', 'M', '2014-05-12', FALSE, 'CM2', 1, 1, 'Abdou Diop', 'Aissatou Ba', '+221770000001', 'WOLOF'),
(2, 'Fatou', 'Sarr', 'F', '2013-09-20', TRUE, 'CM2', 1, 1, 'Ousmane Sarr', 'Sokhna Ndiaye', NULL, 'FRANCAIS'),
(3, 'Modou', 'Faye', 'M', '2014-02-15', FALSE, 'CM2', 3, 2, 'Ibrahima Faye', 'Mariama Diallo', '+221770000003', 'SEREER'),
(4, 'Aminata', 'Ndiaye', 'F', '2015-03-08', FALSE, 'CM1', 2, 1, NULL, 'Khady Sow', '+221770000004', 'WOLOF'),
(5, 'Cheikh', 'Mbaye', 'M', '2014-11-30', TRUE, 'CM2', 4, 3, 'Moussa Mbaye', 'Ndeye Fall', '+221770000005', 'WOLOF'),
(6, 'Astou', 'Gueye', 'F', '2014-07-19', FALSE, 'CM2', 5, 4, 'Lamine Gueye', 'Coumba Thiam', NULL, 'PULAAR'),
(7, 'Babacar', 'Ndour', 'M', '2016-01-25', FALSE, 'CE2', 2, 1, 'Malick Ndour', 'Rokhaya Sene', '+221770000007', 'SEREER'),
(8, 'Mariama', 'Ba', 'F', '2014-04-03', FALSE, 'CM2', 1, 1, 'Alioune Ba', 'Fatimata Sy', '+221770000008', 'PULAAR'),
(9, 'Ousmane', 'Diallo', 'M', '2013-12-11', TRUE, 'CM2', 3, 2, NULL, 'Penda Diallo', '+221770000009', 'PULAAR'),
(10, 'Khady', 'Fall', 'F', '2015-06-22', FALSE, 'CM1', 4, 3, 'Assane Fall', 'Mame Diarra Ndiaye', NULL, 'WOLOF'),
(11, 'Ibrahima', 'Sow', 'M', '2014-08-14', FALSE, 'CM2', 5, 4, 'Samba Sow', 'Dieynaba Ka', '+221770000011', 'PULAAR'),
(12, 'Ndeye', 'Seck', 'F', '2014-10-05', TRUE, 'CM2', 2, 1, 'Pape Seck', 'Mareme Diop', '+221770000012', 'WOLOF'),
(13, 'Abdoulaye', 'Kane', 'M', '2018-02-17', FALSE, 'CI', 1, 1, 'Demba Kane', 'Oumou Kane', NULL, 'FRANCAIS'),
(14, 'Coumba', 'Thiaw', 'F', '2014-01-29', FALSE, 'CM2', 3, 2, 'Saliou Thiaw', 'Bineta Faye', '+221770000014', 'SEREER'),
(15, 'Alioune', 'Ndiaye', 'M', '2013-11-08', TRUE, 'CM2', 4, 3, 'El Hadji Ndiaye', 'Fatou Kine Gaye', '+221770000015', 'WOLOF'),
(16, 'Sokhna', 'Diouf', 'F', '2017-05-30', FALSE, 'CE1', 5, 4, 'Mor Diouf', 'Yacine Sarr', NULL, 'SEREER'),
(17, 'Pape', 'Sarr', 'M', '2014-03-21', TRUE, 'CM2', 1, 2, NULL, 'Ndeye Astou Mbaye', '+221770000017', 'WOLOF'),
(18, 'Dieynaba', 'Sy', 'F', '2014-09-09', FALSE, 'CM2', 2, 1, 'Hamidou Sy', 'Aissata Ly', '+221770000018', 'PULAAR'),
(19, 'Moussa', 'Gaye', 'M', '2015-07-13', FALSE, 'CM1', 3, 2, 'Matar Gaye', 'Seynabou Ndao', '+221770000019', 'WOLOF'),
(20, 'Rama', 'Diagne', 'F', '2014-12-02', TRUE, 'CM2', 4, 3, 'Ndiaga Diagne', 'Adji Fall', NULL, 'WOLOF'),
(21, 'Serigne', 'Mbacke', 'M', '2013-10-16', TRUE, 'CM2', 5, 4, 'Modou Mbacke', 'Sokhna Bousso', '+221770000021', 'WOLOF'),
(22, 'Ndeye Fatou', 'Cisse', 'F', '2014-06-27', FALSE, 'CM2', 1, 1, 'Abdou Cisse', 'Ramatoulaye Diop', '+221770000022', 'FRANCAIS'),
(23, 'Lamine', 'Badji', 'M', '2014-02-08', TRUE, 'CM2', 3, 2, 'Ansou Badji', 'Fatou Diatta', NULL, 'FRANCAIS'),
(24, 'Mareme', 'Ndao', 'F', '2016-04-18', FALSE, 'CE2', 2, 1, 'Gora Ndao', 'Khoudia Sene', '+221770000024', 'SEREER'),
(25, 'Assane', 'Thiam', 'M', '2014-08-01', FALSE, 'CM2', 4, 3, 'Djibril Thiam', 'Maimouna Gueye', '+221770000025', 'WOLOF'),
(26, 'Bineta', 'Kandji', 'F', '2014-05-25', TRUE, 'CM2', 5, 4, 'Youssou Kandji', 'Mbene Diop', '+221770000026', 'WOLOF'),
(27, 'Mouhamed', 'Fall', 'M', '2013-09-03', FALSE, 'CM2', 1, 3, NULL, 'Dior Fall', '+221770000027', 'WOLOF'),
(28, 'Oumou', 'Sall', 'F', '2015-11-12', FALSE, 'CM1', 2, 1, 'Thierno Sall', 'Hawa Ba', NULL, 'PULAAR'),
(29, 'Elhadji', 'Diop', 'M', '2014-07-07', TRUE, 'CM2', 3, 2, 'Souleymane Diop', 'Ngone Faye', '+221770000029', 'SEREER'),
(30, 'Yacine', 'Ndiaye', 'F', '2014-03-14', FALSE, 'CM2', 4, 3, 'Birame Ndiaye', 'Nogaye Mbaye', '+221770000030', 'WOLOF'),
(31, 'Daouda', 'Sene', 'M', '2014-10-28', TRUE, 'CM2', 5, 4, 'Waly Sene', 'Diarra Diouf', NULL, 'SEREER'),
(32, 'Seynabou', 'Diallo', 'F', '2014-01-06', FALSE, 'CM2', 1, 1, 'Boubacar Diallo', 'Kadiatou Barry', '+221770000032', 'PULAAR'),
(33, 'Fallou', 'Ka', 'M', '2017-08-19', FALSE, 'CP', 2, 1, 'Bassirou Ka', 'Astou Ndiaye', '+221770000033', 'WOLOF'),
(34, 'Ndeye Coumba', 'Faye', 'F', '2013-12-24', TRUE, 'CM2', 3, 2, 'Ablaye Faye', 'Mame Fatou Sarr', '+221770000034', 'SEREER'),
(35, 'Saliou', 'Mbengue', 'M', '2014-06-10', FALSE, 'CM2', 4, 3, NULL, 'Daba Mbengue', '+221770000035', 'WOLOF'),
(36, 'Khadidiatou', 'Sarr', 'F', '2014-04-16', FALSE, 'CM2', 5, 4, 'Mbaye Sarr', 'Rokhaya Ndour', '+221770000036', 'WOLOF'),
(37, 'Moustapha', 'Gueye', 'M', '2013-11-27', TRUE, 'CM2', 1, 1, 'Ndiogou Gueye', 'Aminata Seck', '+221770000037', 'WOLOF'),
(38, 'Adja', 'Thiaw', 'F', '2015-02-09', FALSE, 'CM1', 2, 1, 'Cheikh Thiaw', 'Penda Ndiaye', NULL, 'SEREER'),
(39, 'Babacar', 'Diouf', 'M', '2014-09-18', FALSE, 'CM2', 3, 2, 'Insa Diouf', 'Ngone Sene', '+221770000039', 'SEREER'),
(40, 'Sira', 'Ba', 'F', '2014-05-05', TRUE, 'CM2', 4, 3, 'Oumar Ba', 'Aissatou Sy', '+221770000040', 'PULAAR');

-- 3. Un dossier par élève. Chaque ligne donne l'étape atteinte et l'âge, en jours,
-- de chaque date d'étape (j_detection = détecté il y a N jours, etc.).
--   1-7 DETECTE · 8-14 CERTIFICAT_OBTENU · 15-20 DOSSIER_COMPLET
--   21-29 JUGEMENT_RENDU (21-25 jugés depuis plus de 15 jours : en retard ;
--         26 jugé il y a exactement 15 jours : pas encore en retard)
--   30-35 TRANSCRIT · 36-40 ACTE_REMIS
-- Le centre d'état civil est celui de la commune de naissance (même id dans V2).
INSERT INTO dossier (id, reference, eleve_id, etape, date_detection,
                     numero_certificat, date_certificat, centre_etat_civil_id, preuve_naissance,
                     numero_jugement, date_jugement, tribunal_instance_id, type_audience,
                     numero_acte, annee_acte, date_transcription, date_remise)
SELECT v.id,
       'ST-' || to_char(CURRENT_DATE, 'YYYY') || '-' || lpad(v.id::text, 6, '0'),
       v.id, v.etape, CURRENT_DATE - v.j_detection,
       CASE WHEN v.j_certificat IS NOT NULL THEN 'CERT-' || lpad(v.id::text, 4, '0') END,
       CURRENT_DATE - v.j_certificat,
       CASE WHEN v.j_certificat IS NOT NULL THEN e.commune_naissance_id END,
       v.preuve,
       CASE WHEN v.j_jugement IS NOT NULL THEN (1000 + v.id) || '/' || to_char(CURRENT_DATE - v.j_jugement, 'YYYY') END,
       CURRENT_DATE - v.j_jugement,
       CASE WHEN v.j_jugement IS NOT NULL THEN 1 END,
       v.audience,
       CASE WHEN v.j_transcription IS NOT NULL THEN (100 + v.id)::text END,
       CAST(extract(YEAR FROM CURRENT_DATE - v.j_transcription) AS INTEGER),
       CURRENT_DATE - v.j_transcription,
       CURRENT_DATE - v.j_remise
FROM (VALUES
    ( 1, 'DETECTE',             3, NULL::int, NULL, NULL::int, NULL, NULL::int, NULL::int),
    ( 2, 'DETECTE',             5, NULL, NULL, NULL, NULL, NULL, NULL),
    ( 3, 'DETECTE',             8, NULL, NULL, NULL, NULL, NULL, NULL),
    ( 4, 'DETECTE',            12, NULL, NULL, NULL, NULL, NULL, NULL),
    ( 5, 'DETECTE',            20, NULL, NULL, NULL, NULL, NULL, NULL),
    ( 6, 'DETECTE',             2, NULL, NULL, NULL, NULL, NULL, NULL),
    ( 7, 'DETECTE',            30, NULL, NULL, NULL, NULL, NULL, NULL),
    ( 8, 'CERTIFICAT_OBTENU',  40,  30, NULL, NULL, NULL, NULL, NULL),
    ( 9, 'CERTIFICAT_OBTENU',  35,  20, NULL, NULL, NULL, NULL, NULL),
    (10, 'CERTIFICAT_OBTENU',  25,  10, NULL, NULL, NULL, NULL, NULL),
    (11, 'CERTIFICAT_OBTENU',  50,  41, NULL, NULL, NULL, NULL, NULL),
    (12, 'CERTIFICAT_OBTENU',  18,   4, NULL, NULL, NULL, NULL, NULL),
    (13, 'CERTIFICAT_OBTENU',  60,  45, NULL, NULL, NULL, NULL, NULL),
    (14, 'CERTIFICAT_OBTENU',  22,  15, NULL, NULL, NULL, NULL, NULL),
    (15, 'DOSSIER_COMPLET',    70,  55, 'TEMOINS', NULL, NULL, NULL, NULL),
    (16, 'DOSSIER_COMPLET',    65,  50, 'CERTIFICAT_ACCOUCHEMENT', NULL, NULL, NULL, NULL),
    (17, 'DOSSIER_COMPLET',    80,  60, 'TEMOINS', NULL, NULL, NULL, NULL),
    (18, 'DOSSIER_COMPLET',    45,  35, 'CERTIFICAT_ACCOUCHEMENT', NULL, NULL, NULL, NULL),
    (19, 'DOSSIER_COMPLET',    55,  40, 'TEMOINS', NULL, NULL, NULL, NULL),
    (20, 'DOSSIER_COMPLET',    90,  70, 'CERTIFICAT_ACCOUCHEMENT', NULL, NULL, NULL, NULL),
    (21, 'JUGEMENT_RENDU',    120, 100, 'TEMOINS',                  60, 'FORAINE',   NULL, NULL),
    (22, 'JUGEMENT_RENDU',    100,  85, 'CERTIFICAT_ACCOUCHEMENT',  45, 'ORDINAIRE', NULL, NULL),
    (23, 'JUGEMENT_RENDU',     90,  70, 'TEMOINS',                  32, 'FORAINE',   NULL, NULL),
    (24, 'JUGEMENT_RENDU',     80,  60, 'TEMOINS',                  25, 'SPECIALE',  NULL, NULL),
    (25, 'JUGEMENT_RENDU',     70,  50, 'CERTIFICAT_ACCOUCHEMENT',  20, 'ORDINAIRE', NULL, NULL),
    (26, 'JUGEMENT_RENDU',     60,  40, 'TEMOINS',                  15, 'FORAINE',   NULL, NULL),
    (27, 'JUGEMENT_RENDU',     50,  35, 'CERTIFICAT_ACCOUCHEMENT',  12, 'ORDINAIRE', NULL, NULL),
    (28, 'JUGEMENT_RENDU',     45,  30, 'TEMOINS',                   8, 'FORAINE',   NULL, NULL),
    (29, 'JUGEMENT_RENDU',     40,  25, 'CERTIFICAT_ACCOUCHEMENT',   3, 'SPECIALE',  NULL, NULL),
    (30, 'TRANSCRIT',         150, 130, 'TEMOINS',                 100, 'FORAINE',     90, NULL),
    (31, 'TRANSCRIT',         140, 120, 'CERTIFICAT_ACCOUCHEMENT',  90, 'ORDINAIRE',   60, NULL),
    (32, 'TRANSCRIT',         130, 110, 'TEMOINS',                  80, 'FORAINE',     68, NULL),
    (33, 'TRANSCRIT',         120, 100, 'TEMOINS',                  70, 'SPECIALE',    45, NULL),
    (34, 'TRANSCRIT',         110,  90, 'CERTIFICAT_ACCOUCHEMENT',  50, 'ORDINAIRE',   35, NULL),
    (35, 'TRANSCRIT',         100,  80, 'TEMOINS',                  40, 'FORAINE',     20, NULL),
    (36, 'ACTE_REMIS',        200, 180, 'TEMOINS',                 150, 'FORAINE',    140, 130),
    (37, 'ACTE_REMIS',        190, 170, 'CERTIFICAT_ACCOUCHEMENT', 140, 'ORDINAIRE',  110, 100),
    (38, 'ACTE_REMIS',        180, 160, 'TEMOINS',                 130, 'FORAINE',    120, 115),
    (39, 'ACTE_REMIS',        170, 150, 'TEMOINS',                 120, 'SPECIALE',   100,  90),
    (40, 'ACTE_REMIS',        160, 140, 'CERTIFICAT_ACCOUCHEMENT', 110, 'ORDINAIRE',   98,  85)
) AS v(id, etape, j_detection, j_certificat, preuve, j_jugement, audience, j_transcription, j_remise)
JOIN eleve e ON e.id = v.id;

-- 4. Deux témoins pour chaque dossier dont la preuve de naissance est TEMOINS
INSERT INTO temoin (dossier_id, nom_complet, numero_piece)
SELECT d.id,
       (ARRAY['Ibrahima Ndiaye', 'Mame Diarra Sow', 'Ousmane Fall', 'Aida Mbaye', 'Cheikh Gueye', 'Fatou Diop',
              'Samba Sarr', 'Khady Seck', 'Modou Diouf', 'Ndeye Faye', 'Abdou Sene', 'Rokhaya Ba'])[1 + (d.id * 2 + t.n) % 12],
       'CNI-' || lpad((d.id * 10 + t.n)::text, 6, '0')
FROM dossier d CROSS JOIN generate_series(1, 2) AS t(n)
WHERE d.preuve_naissance = 'TEMOINS'
ORDER BY d.id, t.n;

-- 5. Historique : une ligne par étape franchie, par le compte de démo du rôle autorisé.
-- La complétude du dossier n'a pas de date propre : elle est placée peu après le certificat.
INSERT INTO historique_etape (dossier_id, etape_depart, etape_arrivee, date_heure, auteur_id, role_auteur, commentaire)
SELECT id, NULL, 'DETECTE', date_detection + TIME '09:00', 1, 'ECOLE', 'Dossier créé (données de démonstration)'
FROM dossier
UNION ALL
SELECT id, 'DETECTE', 'CERTIFICAT_OBTENU', date_certificat + TIME '10:00', 2, 'ETAT_CIVIL', NULL
FROM dossier WHERE date_certificat IS NOT NULL
UNION ALL
SELECT id, 'CERTIFICAT_OBTENU', 'DOSSIER_COMPLET',
       LEAST(date_certificat + 3, COALESCE(date_jugement, CURRENT_DATE)) + TIME '11:00', 1, 'ECOLE', NULL
FROM dossier WHERE preuve_naissance IS NOT NULL
UNION ALL
SELECT id, 'DOSSIER_COMPLET', 'JUGEMENT_RENDU', date_jugement + TIME '12:00', 3, 'TRIBUNAL', NULL
FROM dossier WHERE date_jugement IS NOT NULL
UNION ALL
SELECT id, 'JUGEMENT_RENDU', 'TRANSCRIT', date_transcription + TIME '14:00', 2, 'ETAT_CIVIL', NULL
FROM dossier WHERE date_transcription IS NOT NULL
UNION ALL
SELECT id, 'TRANSCRIT', 'ACTE_REMIS', date_remise + TIME '15:00', 1, 'ECOLE', NULL
FROM dossier WHERE date_remise IS NOT NULL;

-- 6. Les lignes ci-dessus et celles de V2 portent des id écrits en dur : on recale
-- chaque séquence, sinon le premier insert par l'API échouerait sur une clé en double.
SELECT setval(pg_get_serial_sequence('commune', 'id'),           (SELECT max(id) FROM commune));
SELECT setval(pg_get_serial_sequence('ecole', 'id'),             (SELECT max(id) FROM ecole));
SELECT setval(pg_get_serial_sequence('centre_etat_civil', 'id'), (SELECT max(id) FROM centre_etat_civil));
SELECT setval(pg_get_serial_sequence('tribunal_instance', 'id'), (SELECT max(id) FROM tribunal_instance));
SELECT setval(pg_get_serial_sequence('utilisateur', 'id'),       (SELECT max(id) FROM utilisateur));
SELECT setval(pg_get_serial_sequence('eleve', 'id'),             (SELECT max(id) FROM eleve));
SELECT setval(pg_get_serial_sequence('dossier', 'id'),           (SELECT max(id) FROM dossier));
SELECT setval('dossier_reference_seq',                           (SELECT max(id) FROM dossier));
