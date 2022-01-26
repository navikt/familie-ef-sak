package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDate

@Table("soknad_utenlandsopphold")
data class Utenlandsopphold(val fradato: LocalDate,
                            val tildato: LocalDate,
                            @Column("arsak_utenlandsopphold")
                            val årsakUtenlandsopphold: String)
