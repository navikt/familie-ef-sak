DROP TABLE barn;

ALTER TABLE grunnlag_soknad
    DROP COLUMN soknad;

ALTER TABLE grunnlag_soknad
    ADD COLUMN soknadsskjema_id UUID;
ALTER TABLE grunnlag_soknad
    ADD COLUMN relaterte_fnr VARCHAR;


CREATE TABLE Soknadsskjema (
    id                                                           UUID PRIMARY KEY,
    type                                                         VARCHAR,
    fodselsnummer                                                VARCHAR,
    navn                                                         VARCHAR,
    telefonnummer                                                VARCHAR,
    dato_mottatt                                                 TIMESTAMP(3),
    sivilstand_er_uformelt_gift                                  BOOLEAN,
    sivilstand_er_uformelt_gift_dokumentasjon                    VARCHAR,
    sivilstand_er_uformelt_separert_eller_skilt                  BOOLEAN,
    sivilstand_er_uformelt_separert_eller_skilt_dokumentasjon    VARCHAR,
    sivilstand_sokt_om_skilsmisse_separasjon                     BOOLEAN,
    sivilstand_dato_sokt_separasjon                              DATE,
    sivilstand_separasjonsbekreftelse                            VARCHAR,
    sivilstand_arsak_enslig                                      VARCHAR,
    sivilstand_samlivsbruddsdokumentasjon                        VARCHAR,
    sivilstand_samlivsbruddsdato                                 DATE,
    sivilstand_fraflytningsdato                                  DATE,
    sivilstand_endring_samversordning_dato                       DATE,
    sivilstand_tidligere_samboer_navn                            VARCHAR,
    sivilstand_tidligere_samboer_fodselsnummer                   VARCHAR,
    sivilstand_tidligere_samboer_fodselsdato                     DATE,
    sivilstand_tidligere_samboer_land                            VARCHAR,

    medlemskap_oppholder_du_deg_i_norge                          BOOLEAN,
    medlemskap_bosatt_norge_siste_arene                          BOOLEAN,

    bosituasjon_deler_du_bolig                                   VARCHAR,
    bosituasjon_samboer_navn                                     VARCHAR,
    bosituasjon_samboer_fodselsnummer                            VARCHAR,
    bosituasjon_samboer_fodselsdato                              DATE,
    bosituasjon_samboer_land                                     VARCHAR,
    bosituasjon_sammenflyttingsdato                              DATE,
    bosituasjon_dato_flyttet_fra_hverandre                       DATE,
    bosituasjon_tidligere_samboer_fortsatt_registrert_pa_adresse VARCHAR,

    sivilstandsplaner_har_planer                                 BOOLEAN,
    sivilstandsplaner_fra_dato                                   DATE,
    sivilstandsplaner_vordende_Samboer_navn                      VARCHAR,
    sivilstandsplaner_vordende_Samboer_fodselsnummer             VARCHAR,
    sivilstandsplaner_vordende_Samboer_fodselsdato               DATE,
    sivilstandsplaner_vordende_Samboer_land                      VARCHAR,

    aktivitet_hvordan_er_arbeidssituasjonen                      VARCHAR,
    aktivitet_virksomhet_virksomhetsbeskrivelse                  VARCHAR,
    aktivitet_virksomhet_dokumentasjon                           VARCHAR,

    aktivitet_arbeidssoker_registrert_som_arbeidssoker_nav       BOOLEAN,
    aktivitet_arbeidssoker_villig_til_a_ta_imot_tilbud_om_arbeid BOOLEAN,
    aktivitet_arbeidssoker_kan_du_begynne_innen_en_uke           BOOLEAN,
    aktivitet_arbeidssoker_kan_du_skaffe_barnepass_innen_en_uke  BOOLEAN,
    aktivitet_arbeidssoker_hvor_onsker_du_arbeid                 VARCHAR,
    aktivitet_arbeidssoker_onsker_du_minst_50_Prosent_stilling   BOOLEAN,
    aktivitet_arbeidssoker_ikke_villig_til_a_ta_tilbud_om_arbeid VARCHAR,

    aktivitet_under_utdanning_skole_utdanningssted               VARCHAR,
    aktivitet_under_utdanning_linje_kurs_grad                    VARCHAR,
    aktivitet_under_utdanning_fra                                DATE,
    aktivitet_under_utdanning_til                                DATE,
    aktivitet_under_utdanning_offentlig_eller_privat             VARCHAR,
    aktivitet_under_utdanning_heltid_eller_deltid                VARCHAR,
    aktivitet_under_utdanning_hvor_mye_skal_du_studere           INT,
    aktivitet_under_utdanning_hva_er_malet_med_utdanningen       VARCHAR,
    aktivitet_under_utdanning_utdanning_etter_grunnskolen        BOOLEAN,
    aktivitet_under_utdanning_semesteravgift                     INT,
    aktivitet_under_utdanning_studieavgift                       INT,
    aktivitet_under_utdanning_eksamensgebyr                      INT,
    aktivitet_er_i_arbeid                                        VARCHAR,
    aktivitet_er_i_arbeid_dokumentasjon                          VARCHAR,
    situasjon_gjelder_dette_deg                                  VARCHAR,
    situasjon_sykdom                                             VARCHAR,
    situasjon_barns_sykdom                                       VARCHAR,
    situasjon_manglende_barnepass                                VARCHAR,
    situasjon_barn_med_serlige_behov                             VARCHAR,
    situasjon_arbeidskontrakt                                    VARCHAR,
    situasjon_lerlingkontrakt                                    VARCHAR,
    situasjon_oppstart_ny_jobb                                   DATE,
    situasjon_utdanningstilbud                                   VARCHAR,
    situasjon_oppstart_utdanning                                 DATE,
    situasjon_sagt_opp_eller_redusert_stilling                   VARCHAR,
    situasjon_oppsigelse_reduksjon_arsak                         VARCHAR,
    situasjon_oppsigelse_reduksjon_tidspunkt                     DATE,
    situasjon_reduksjon_av_arbeidsforhold_dokumentasjon          VARCHAR,
    situasjon_oppsigelse_dokumentasjon                           VARCHAR,
    soker_fra                                                    DATE,
    soker_fra_bestemt_maned                                      BOOLEAN,
    dokumentasjon_barnepassordning_faktura                       VARCHAR,
    dokumentasjon_avtale_barnepasser                             VARCHAR,
    dokumentasjon_arbeidstid                                     VARCHAR,
    dokumentasjon_roterende_arbeidstid                           VARCHAR,
    dokumentasjon_spesielle_behov                                VARCHAR,
    utdanningsutgifter                                           VARCHAR

);



CREATE TABLE Selvstendig (
    soknadsskjema_id           UUID REFERENCES Soknadsskjema (id),
    firmanavn                  VARCHAR,
    organisasjonsnummer        VARCHAR,
    etableringsdato            DATE,
    arbeidsmengde              INT,
    hvordan_ser_arbeidsuken_ut VARCHAR
);

CREATE TABLE Arbeidsgiver (
    soknadsskjema_id       UUID REFERENCES Soknadsskjema (id),
    arbeidsgivernavn       VARCHAR,
    arbeidsmengde          INT,
    fast_eller_midlertidig VARCHAR,
    har_sluttdato          BOOLEAN,
    sluttdato              DATE
);

CREATE TABLE Aksjeselskap (
    soknadsskjema_id UUID REFERENCES Soknadsskjema (id),
    navn             VARCHAR,
    arbeidsmengde    INT
);

CREATE TABLE Utenlandsopphold (
    soknadsskjema_id       UUID REFERENCES Soknadsskjema (id),
    fradato                DATE,
    tildato                DATE,
    arsak_utenlandsopphold VARCHAR
);

CREATE TABLE Tidligere_utdanning (
    soknadsskjema_id UUID REFERENCES Soknadsskjema (id),
    linje_kurs_grad  VARCHAR,
    fra              DATE,
    til              DATE
);

CREATE TABLE barn (
    id                                                            UUID PRIMARY KEY,
    soknadsskjema_id                                              UUID,
    navn                                                          VARCHAR,
    fodselsnummer                                                 VARCHAR,
    har_skal_ha_samme_adresse                                     BOOLEAN,
    ikke_registrert_pa_sokers_adresse_beskrivelse                 VARCHAR,
    er_barnet_fodt                                                BOOLEAN,
    fodsel_termindato                                             DATE,
    terminbekreftelse                                             VARCHAR,
    annen_forelder_ikke_oppgitt_annen_forelder_begrunnelse        VARCHAR,
    annen_forelder_bosatt_norge                                   BOOLEAN,
    annen_forelder_land                                           VARCHAR,
    annen_forelder_navn                                           VARCHAR,
    annen_forelder_fodselsnummer                                  VARCHAR,
    annen_forelder_fodselsdato                                    DATE,
    samver_sporsmal_avtale_om_delt_bosted                         BOOLEAN,
    samver_avtale_om_delt_bosted                                  VARCHAR,
    samver_skal_annen_forelder_ha_samver                          VARCHAR,
    samver_har_dere_skriftlig_avtale_om_samver                    VARCHAR,
    samver_samversavtale                                          VARCHAR,
    samver_barn_skal_bo_hos_soker_annen_forelder_samarbeider_ikke VARCHAR,
    samver_hvordan_praktiseres_samveret                           VARCHAR,
    samver_bor_annen_forelder_i_samme_hus                         VARCHAR,
    samver_bor_annen_forelder_i_samme_hus_beskrivelse             VARCHAR,
    samver_har_dere_tidligere_bodd_sammen                         BOOLEAN,
    samver_nar_flyttet_dere_fra_hverandre                         DATE,
    samver_erklering_om_samlivsbrudd                              VARCHAR,
    samver_hvor_mye_er_du_sammen_med_annen_forelder               VARCHAR,
    samver_beskriv_samver_uten_barn                               VARCHAR,
    skal_ha_barnepass                                             BOOLEAN,
    serlige_tilsynsbehov                                          VARCHAR,
    barnepass_arsak_barnepass                                     VARCHAR
);

CREATE TABLE Barnepassordning (
    barn_id                    UUID REFERENCES barn (Id),
    hva_slags_barnepassordning VARCHAR,
    navn                       VARCHAR,
    fra                        DATE,
    til                        DATE,
    belop                      INT
);
