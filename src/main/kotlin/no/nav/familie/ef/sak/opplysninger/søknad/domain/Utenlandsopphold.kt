package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("soknad_utenlandsopphold")
data class Utenlandsopphold(
    val fradato: LocalDate,
    val tildato: LocalDate,
    val land: String? = null,
    @Column("arsak_utenlandsopphold")
    val årsakUtenlandsopphold: String,
    @Column("personident_eos_land")
    val personidentEøsLand: String? = null,
    @Column("adresse_eos_land")
    val adresseEøsLand: String? = null,
    val kanIkkeOppgiPersonident: Boolean? = null,
    @Column("er_eos_land")
    val erEøsland: Boolean? = null,
)
