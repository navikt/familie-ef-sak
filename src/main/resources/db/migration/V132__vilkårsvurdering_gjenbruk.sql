ALTER TABLE vilkarsvurdering
    ADD COLUMN gjenbrukt_behandling_id UUID;
ALTER TABLE vilkarsvurdering
    ADD COLUMN gjenbrukt_endret_tid TIMESTAMP(3);
