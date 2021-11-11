package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

class VedtakPerUke(val år: Int,
                   val uke: Int,
                   val stonadstype: Stønadstype,
                   val resultat: BehandlingResultat,
                   val antall: Int)
