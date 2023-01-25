ALTER TABLE grunnlagsdata
    ADD COLUMN endringer JSON;
ALTER TABLE grunnlagsdata
    ADD COLUMN endringer_sjekket TIMESTAMP(3) NOT NULL DEFAULT opprettet_tid;
