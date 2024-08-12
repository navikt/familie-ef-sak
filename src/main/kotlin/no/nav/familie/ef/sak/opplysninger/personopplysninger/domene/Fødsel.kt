package no.nav.familie.ef.sak.opplysninger.personopplysninger.domene

import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import java.time.LocalDate

data class Fødsel(
    val fødselsår: Int?,
    val fødselsdato: LocalDate?,
    val fødeland: String?,
    val fødested: String?,
    val fødekommune: String?,
    val metadata: Metadata,
) {
    fun erUnder18År() =
        this.fødselsdato?.let { LocalDate.now() < it.plusYears(18) }
            ?: this.fødselsår?.let { LocalDate.now() < LocalDate.of(it, 1, 1).plusYears(18) }
            ?: true
}

fun List<Fødsel>.gjeldende(): Fødsel = this.first()
