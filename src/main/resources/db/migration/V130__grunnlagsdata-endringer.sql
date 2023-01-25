ALTER TABLE grunnlagsdata
    ADD COLUMN oppdaterte_data JSON,
    ADD COLUMN oppdaterte_data_hentet_tid TIMESTAMP(3);
UPDATE grunnlagsdata
    SET oppdaterte_data_hentet_tid = opprettet_tid;
ALTER TABLE grunnlagsdata
    ALTER COLUMN oppdaterte_data_hentet_tid SET NOT NULL;