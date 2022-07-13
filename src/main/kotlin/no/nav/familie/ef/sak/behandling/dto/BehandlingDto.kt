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

data class BehandlingDto(
    val id: UUID,
    val forrigeBehandlingId: UUID?,
    val fagsakId: UUID,
    val steg: StegType,
    val type: BehandlingType,
    val status: BehandlingStatus,
    val sistEndret: LocalDateTime,
    val resultat: BehandlingResultat,
    val opprettet: LocalDateTime,
    val behandlingsårsak: BehandlingÅrsak,
    val stønadstype: StønadType,
    val vedtaksdato: LocalDateTime? = null,
    val henlagtÅrsak: HenlagtÅrsak? = null
)

fun Behandling.tilDto(stønadstype: StønadType, vedtaksdato: LocalDateTime? = null): BehandlingDto =
    BehandlingDto(
        id = this.id,
        forrigeBehandlingId = this.forrigeBehandlingId,
        fagsakId = this.fagsakId,
        steg = this.steg,
        type = this.type,
        status = this.status,
        sistEndret = this.sporbar.endret.endretTid,
        resultat = this.resultat,
        opprettet = this.sporbar.opprettetTid,
        behandlingsårsak = this.årsak,
        henlagtÅrsak = this.henlagtÅrsak,
        stønadstype = stønadstype,
        vedtaksdato = vedtaksdato
            ?: (if (this.status == BehandlingStatus.FERDIGSTILT) this.sporbar.endret.endretTid else null)
    )

fun Saksbehandling.tilDto(): BehandlingDto =
    BehandlingDto(
        id = this.id,
        forrigeBehandlingId = this.forrigeBehandlingId,
        fagsakId = this.fagsakId,
        steg = this.steg,
        type = this.type,
        status = this.status,
        sistEndret = this.endretTid,
        resultat = this.resultat,
        opprettet = this.opprettetTid,
        behandlingsårsak = this.årsak,
        henlagtÅrsak = this.henlagtÅrsak,
        stønadstype = stønadstype,
        vedtaksdato = if (this.status == BehandlingStatus.FERDIGSTILT) this.endretTid else null
    )
