-- =========================================================================
-- PROJET SAMA TUR - MIGRATION V5 : archivage des élèves (fonction facultative)
-- Supprimer un élève l'archive, jamais physiquement (RG-13) : l'élève et son
-- dossier sortent des listes, des alertes et des indicateurs, l'historique reste.
-- =========================================================================

ALTER TABLE eleve ADD COLUMN archive_le TIMESTAMP;

COMMENT ON COLUMN eleve.archive_le IS 'Date d''archivage ; NULL pour un élève actif';
