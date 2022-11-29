package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Column

data class OpplysningerOmAdresse(
    val adresse: String? = null,
    @Column("soker_bor_pa_adresse")
    val søkerBorPåRegistrertAdresse: Boolean? = null,
    val harMeldtFlytteendring: Boolean? = null,
    val dokumentasjonFlytteendring: Dokumentasjon? = null
)
