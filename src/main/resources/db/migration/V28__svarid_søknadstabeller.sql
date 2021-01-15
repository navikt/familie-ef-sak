
ALTER table Soknadsskjema
    ADD COLUMN bosituasjon_deler_du_bolig_verdi VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN bosituasjon_deler_du_bolig_svar_id VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN sivilstand_arsak_enslig_verdi VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN sivilstand_arsak_enslig_svar_id VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN aktivitet_hvordan_er_arbeidssituasjonen_verdi VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN aktivitet_hvordan_er_arbeidssituasjonen_svar_id VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN aktivitet_er_i_arbeid_verdi VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN aktivitet_er_i_arbeid_svar_id VARCHAR;

ALTER table Soknadsskjema
    ADD COLUMN situasjon_gjelder_dette_deg_verdi VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN situasjon_gjelder_dette_deg_svar_id VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN situasjon_sagt_opp_eller_redusert_stilling_verdi VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN situasjon_sagt_opp_eller_redusert_stilling_svar_id VARCHAR;

ALTER table Soknadsskjema
    ADD COLUMN aktivitet_under_utdanning_heltid_eller_deltid_svar_id VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN aktivitet_under_utdanning_heltid_eller_deltid_verdi VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN aktivitet_under_utdanning_offentlig_eller_privat_verdi VARCHAR;
ALTER table Soknadsskjema
    ADD COLUMN aktivitet_under_utdanning_offentlig_eller_privat_svar_id VARCHAR;
ALTER table barn
    ADD COLUMN annen_forelder_ikke_oppgitt_annen_forelder_begrunnelse_verdi VARCHAR;
ALTER table barn
    ADD COLUMN annen_forelder_ikke_oppgitt_annen_forelder_begrunnelse_svar_id VARCHAR;
ALTER table barn
    ADD COLUMN samver_bor_annen_forelder_i_samme_hus_verdi VARCHAR;
ALTER table barn
    ADD COLUMN samver_bor_annen_forelder_i_samme_hus_svar_id VARCHAR;
ALTER table barn
    ADD COLUMN samver_har_dere_skriftlig_avtale_om_samver_verdi VARCHAR;
ALTER table barn
    ADD COLUMN samver_har_dere_skriftlig_avtale_om_samver_svar_id VARCHAR;
ALTER table barn
    ADD COLUMN samver_hvor_mye_er_du_sammen_med_annen_forelder_verdi VARCHAR;
ALTER table barn
    ADD COLUMN samver_hvor_mye_er_du_sammen_med_annen_forelder_svar_id VARCHAR;
ALTER table barn
    ADD COLUMN samver_skal_annen_forelder_ha_samver_verdi VARCHAR;
ALTER table barn
    ADD COLUMN samver_skal_annen_forelder_ha_samver_svar_id VARCHAR;

ALTER table arbeidsgiver
    ADD COLUMN fast_eller_midlertidig_verdi VARCHAR;
ALTER table arbeidsgiver
    ADD COLUMN fast_eller_midlertidig_svar_id VARCHAR;


ALTER table barn
    ADD COLUMN barnepass_arsak_barnepass_verdi VARCHAR;
ALTER table barn
    ADD COLUMN barnepass_arsak_barnepass_svar_id VARCHAR;

ALTER table Barnepassordning
    ADD COLUMN hva_slags_barnepassordning_verdi VARCHAR;
ALTER table Barnepassordning
    ADD COLUMN hva_slags_barnepassordning_svar_id VARCHAR;
