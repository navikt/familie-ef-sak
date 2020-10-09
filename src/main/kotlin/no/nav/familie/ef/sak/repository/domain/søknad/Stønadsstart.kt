package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.Month

data class Stønadsstart(val fraMåned: Month?,
                        val fraÅr: Int?,
                        val søkerFraBestemtMåned: Boolean)
