package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.service.steg.StegType
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(val id: UUID,
                         val steg: StegType,
                         val type: BehandlingType,
                         val aktiv: Boolean,
                         val status: BehandlingStatus,
                         val sistEndret: LocalDateTime,
                         val resultat: BehandlingResultat,
                         val opprettet: LocalDateTime)

fun Behandling.tilDto(): BehandlingDto =
        BehandlingDto(id = this.id,
                      steg = this.steg,
                      type = this.type,
                      aktiv = this.aktiv,
                      status = this.status,
                      sistEndret = this.sporbar.endret.endretTid,
                      resultat = this.resultat,
                      opprettet = this.sporbar.opprettetTid)