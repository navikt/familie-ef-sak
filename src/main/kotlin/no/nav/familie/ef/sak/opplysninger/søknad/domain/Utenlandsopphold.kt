package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("soknad_utenlandsopphold")
data class Utenlandsopphold(
    val fradato: LocalDate,
    val tildato: LocalDate,
    val land: String? = null,
    @Column("arsak")
    val årsak: String,
    val personident: String? = null,
    val adresse: String? = null,
)
