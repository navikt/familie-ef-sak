package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.Month

data class Stønadsstart(val fraMåned: Søknadsfelt<Month>?,
                        val fraÅr: Søknadsfelt<Int>?,
                        val søkerFraBestemtMåned: Søknadsfelt<Boolean>)
