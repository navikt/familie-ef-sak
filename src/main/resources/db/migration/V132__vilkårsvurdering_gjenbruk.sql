ALTER TABLE vilkarsvurdering
    ADD COLUMN opphavsvilkaar_behandling_id UUID;
ALTER TABLE vilkarsvurdering
    ADD COLUMN opphavsvilkaar_vurderingstidspunkt TIMESTAMP(3);
