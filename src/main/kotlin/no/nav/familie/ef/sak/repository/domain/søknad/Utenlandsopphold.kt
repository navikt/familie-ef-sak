package no.nav.familie.ef.sak.repository.domain.søknad

import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate

data class Utenlandsopphold(val fradato: LocalDate,
                            val tildato: LocalDate,
                            @Column("arsak_utenlandsopphold")
                            val årsakUtenlandsopphold: String)
