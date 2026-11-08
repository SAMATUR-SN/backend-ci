CREATE TABLE IF NOT EXISTS alerte (
    id BIGSERIAL PRIMARY KEY,
    dossier_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    date_alerte TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolue BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_alerte_dossier FOREIGN KEY (dossier_id)
        REFERENCES dossier(id) ON DELETE CASCADE
);

ALTER TABLE alerte
    ALTER COLUMN message TYPE TEXT,
    ALTER COLUMN date_alerte SET DEFAULT CURRENT_TIMESTAMP,
    ALTER COLUMN resolue SET DEFAULT FALSE;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_alerte_dossier'
          AND conrelid = 'alerte'::regclass
    ) THEN
        ALTER TABLE alerte
            ADD CONSTRAINT fk_alerte_dossier
            FOREIGN KEY (dossier_id) REFERENCES dossier(id) ON DELETE CASCADE;
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_alerte_dossier ON alerte(dossier_id);
CREATE INDEX IF NOT EXISTS idx_alerte_resolue ON alerte(resolue);
