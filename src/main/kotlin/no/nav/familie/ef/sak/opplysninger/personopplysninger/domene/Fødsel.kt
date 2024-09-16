package no.nav.familie.ef.sak.opplysninger.personopplysninger.domene

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class Fødsel(
    @JsonProperty("foedselsaar") val fødselsår: Int?,
    @JsonProperty("foedselsdato") val fødselsdato: LocalDate?,
    @JsonProperty("foedeland") val fødeland: String?,
    @JsonProperty("foedested") val fødested: String?,
    @JsonProperty("foedekommune") val fødekommune: String?,
) {
    fun erUnder18År() =
        this.fødselsdato?.let { LocalDate.now() < it.plusYears(18) }
            ?: this.fødselsår?.let { LocalDate.now() < LocalDate.of(it, 1, 1).plusYears(18) }
            ?: true
}
