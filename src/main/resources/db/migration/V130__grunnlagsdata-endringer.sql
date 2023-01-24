ALTER TABLE grunnlagsdata
    ADD COLUMN endringer JSON;
ALTER TABLE grunnlagsdata
    ADD COLUMN endringer_sjekket TIMESTAMP(3);