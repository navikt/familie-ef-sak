package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.kontrakter.felles.ef.StønadType

data class ForekomsterPerUke(val år: Int,
                             val uke: Int,
                             val stonadstype: StønadType,
                             val antall: Int)
