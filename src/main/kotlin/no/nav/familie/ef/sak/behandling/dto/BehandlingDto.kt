package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(val id: UUID,
                         val fagsakId: UUID,
                         val steg: StegType,
                         val type: BehandlingType,
                         val status: BehandlingStatus,
                         val sistEndret: LocalDateTime,
                         val resultat: BehandlingResultat,
                         val opprettet: LocalDateTime,
                         val behandlingsårsak: BehandlingÅrsak,
                         val stønadstype: StønadType)

fun Behandling.tilDto(stønadstype: StønadType): BehandlingDto =
        BehandlingDto(id = this.id,
                      fagsakId = this.fagsakId,
                      steg = this.steg,
                      type = this.type,
                      status = this.status,
                      sistEndret = this.sporbar.endret.endretTid,
                      resultat = this.resultat,
                      opprettet = this.sporbar.opprettetTid,
                      behandlingsårsak = this.årsak,
                      stønadstype = stønadstype)

fun Saksbehandling.tilDto(): BehandlingDto =
        BehandlingDto(id = this.id,
                      fagsakId = this.fagsakId,
                      steg = this.steg,
                      type = this.type,
                      status = this.status,
                      sistEndret = this.endretTid,
                      resultat = this.resultat,
                      opprettet = this.opprettetTid,
                      behandlingsårsak = this.årsak,
                      stønadstype = stønadstype)