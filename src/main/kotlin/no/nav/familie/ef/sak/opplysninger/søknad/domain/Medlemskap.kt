package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.MappedCollection

data class Medlemskap(
    val oppholderDuDegINorge: Boolean,
    @Column("bosatt_norge_siste_arene")
    val bosattNorgeSisteÅrene: Boolean,
    val oppholdsland: String? = null,
    @MappedCollection(idColumn = "soknadsskjema_id")
    val utenlandsopphold: Set<Utenlandsopphold> = emptySet(),
)
