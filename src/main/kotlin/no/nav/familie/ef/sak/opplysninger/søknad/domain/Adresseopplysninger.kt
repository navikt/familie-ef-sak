package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Column

data class Adresseopplysninger(
    val adresse: String? = null,
    @Column("soker_bor_pa_adresse")
    val søkerBorPåRegistrertAdresse: Boolean? = null,
    val harMeldtAdresseendring: Boolean? = null,
    val dokumentasjonAdresseendring: Dokumentasjon? = null
)
