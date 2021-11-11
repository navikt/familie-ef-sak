package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype

class BehandlingerPerSteg(val stonadstype: Stønadstype,
                          val steg: StegType,
                          val antall: Int)
