ALTER TABLE vilkarsvurdering
    ADD COLUMN opphavsvilkaar_behandling_id UUID;
ALTER TABLE vilkarsvurdering
    ADD COLUMN opphavsvilkaar_endret_tid TIMESTAMP(3);
