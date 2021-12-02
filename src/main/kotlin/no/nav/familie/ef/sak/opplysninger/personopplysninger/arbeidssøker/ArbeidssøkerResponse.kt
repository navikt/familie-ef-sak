package no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker

import java.time.LocalDate

data class ArbeidssøkerResponse(val perioder: List<ArbeidssøkerPeriode>)

data class ArbeidssøkerPeriode(val fraOgMedDato: LocalDate,
                               val tilOgMedDato: LocalDate)