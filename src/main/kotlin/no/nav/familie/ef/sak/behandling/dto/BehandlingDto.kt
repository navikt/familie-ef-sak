package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.tilbakekreving.Behandlingsårsakstype
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(val id: UUID,
                         val fagsakId: UUID,
                         val steg: StegType,
                         val type: BehandlingType,
                         val status: BehandlingStatus,
                         val årsak: BehandlingÅrsak,
                         val sistEndret: LocalDateTime,
                         val resultat: BehandlingResultat,
                         val opprettet: LocalDateTime,
                         val behandlingsårsak: BehandlingÅrsak)

fun Behandling.tilDto(): BehandlingDto =
        BehandlingDto(id = this.id,
                      fagsakId = this.fagsakId,
                      steg = this.steg,
                      type = this.type,
                      status = this.status,
                      årsak = this.årsak,
                      sistEndret = this.sporbar.endret.endretTid,
                      resultat = this.resultat,
                      opprettet = this.sporbar.opprettetTid,
                      behandlingsårsak = this.årsak)