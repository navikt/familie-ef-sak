package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.fagsak.domain.Stønadstype

class ForekomsterPerUke(val år: Int,
                        val uke: Int,
                        val stonadstype: Stønadstype,
                        val antall: Int)
