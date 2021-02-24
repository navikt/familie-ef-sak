package no.nav.familie.ef.sak.api.dto

import no.nav.familie.ef.sak.repository.domain.Behandling
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingStatus
import no.nav.familie.ef.sak.repository.domain.BehandlingType
import no.nav.familie.ef.sak.repository.domain.Registergrunnlagsendringer
import no.nav.familie.ef.sak.service.steg.StegType
import java.time.LocalDate
import java.util.UUID

data class BehandlingDto(val id: UUID,
                         val steg: StegType,
                         val type: BehandlingType,
                         val aktiv: Boolean,
                         val status: BehandlingStatus,
                         val sistEndret: LocalDate,
                         val resultat: BehandlingResultat,
                         val opprettet: LocalDate,
                         val endringerIRegistergrunnlag: Registergrunnlagsendringer?)

fun Behandling.tilDto(endringerIRegistergrunnlag: Registergrunnlagsendringer? = null): BehandlingDto =
        BehandlingDto(id = this.id,
                      steg = this.steg,
                      type = this.type,
                      aktiv = this.aktiv,
                      status = this.status,
                      sistEndret = this.sporbar.endret.endretTid.toLocalDate(),
                      resultat = this.resultat,
                      opprettet = this.sporbar.opprettetTid.toLocalDate(),
                      endringerIRegistergrunnlag = endringerIRegistergrunnlag)