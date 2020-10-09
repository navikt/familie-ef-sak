package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.Month

data class MånedÅrPeriode(val fraMåned: Month,
                          val fraÅr: Int,
                          val tilMåned: Month,
                          val tilÅr: Int)
