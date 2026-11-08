-- =========================================================================
-- PROJET SAMA TUR - MIGRATION V2 : Données de référence (Bambey)
-- =========================================================================

-- 1. Insertion des Communes du département de Bambey
INSERT INTO commune (id, nom, departement, region) VALUES
(1, 'Bambey (Commune)', 'Bambey', 'Diourbel'),
(2, 'Ngoye', 'Bambey', 'Diourbel'),
(3, 'Baba Garage', 'Bambey', 'Diourbel'),
(4, 'Ndangalma', 'Bambey', 'Diourbel');

-- 2. Insertion des Tribunaux d'Instance
INSERT INTO tribunal_instance (id, nom, ville) VALUES
(1, 'Tribunal d''Instance de Bambey', 'Bambey');

-- 3. Insertion des Centres d'État Civil
INSERT INTO centre_etat_civil (id, nom, commune_id) VALUES
(1, 'Centre Principal d''État Civil de Bambey', 1),
(2, 'Centre d''État Civil de Ngoye', 2),
(3, 'Centre d''État Civil de Baba Garage', 3),
(4, 'Centre d''État Civil de Ndangalma', 4);

-- 4. Insertion des Écoles primaires de référence (Cible CM2)
INSERT INTO ecole (id, nom, code_ecole, ief, commune_id) VALUES
(1, 'École Publique Bambey Station 1', 'ECO-BAM-01', 'IEF Bambey', 1),
(2, 'École Publique Bambey Serigne Mor', 'ECO-BAM-02', 'IEF Bambey', 1),
(3, 'École Élémentaire de Ngoye Centre', 'ECO-NGO-01', 'IEF Bambey', 2),
(4, 'École Élémentaire de Baba Garage 1', 'ECO-BAB-01', 'IEF Bambey', 3),
(5, 'École Élémentaire de Ndangalma Gare', 'ECO-NDA-01', 'IEF Bambey', 4);