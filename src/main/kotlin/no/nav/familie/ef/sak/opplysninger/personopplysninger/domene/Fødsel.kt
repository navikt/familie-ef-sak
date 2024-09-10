package no.nav.familie.ef.sak.opplysninger.personopplysninger.domene

import java.time.LocalDate

data class Fødsel(
    val foedselsaar: Int?,
    val foedselsdato: LocalDate?,
    val foedeland: String?,
    val foedested: String?,
    val foedekommune: String?,
) {
    fun erUnder18År() =
        this.foedselsdato?.let { LocalDate.now() < it.plusYears(18) }
            ?: this.foedselsaar?.let { LocalDate.now() < LocalDate.of(it, 1, 1).plusYears(18) }
            ?: true
}

fun List<Fødsel>.gjeldende(): Fødsel = this.first()
