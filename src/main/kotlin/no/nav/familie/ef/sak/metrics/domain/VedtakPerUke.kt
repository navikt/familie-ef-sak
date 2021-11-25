package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype

data class VedtakPerUke(val år: Int,
                        val uke: Int,
                        val stonadstype: Stønadstype,
                        val resultat: BehandlingResultat,
                        val antall: Int)
