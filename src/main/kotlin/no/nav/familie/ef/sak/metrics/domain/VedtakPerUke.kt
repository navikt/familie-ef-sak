package no.nav.familie.ef.sak.metrics.domain

import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.dto.HenlagtÅrsak
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType

data class VedtakPerUke(
    val år: Int,
    val uke: Int,
    val stonadstype: StønadType,
    val resultat: BehandlingResultat,
    val arsak: BehandlingÅrsak,
    val henlagt_arsak: HenlagtÅrsak? = null,
    val antall: Int
)
