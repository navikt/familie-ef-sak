package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class ArbeidssøkerResponse(@JsonProperty("arbeidssokerperioder")
                                val perioder: List<ArbeidssøkerPeriode>)

data class ArbeidssøkerPeriode(val fraOgMedDato: LocalDate,
                               val tilOgMedDato: LocalDate?)