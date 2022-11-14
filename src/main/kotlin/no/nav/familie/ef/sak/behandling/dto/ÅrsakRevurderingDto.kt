package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.domain.Opplysningskilde
import no.nav.familie.ef.sak.behandling.domain.Revurderingsårsak
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import java.time.LocalDateTime
import java.util.UUID

data class ÅrsakRevurderingDto(
    val kilde: Opplysningskilde,
    val årsak: Revurderingsårsak,
    val beskrivelse: String?,
    val endretAv: String,
    val endretTid: LocalDateTime,
)

fun ÅrsakRevurderingDto.tilDomene(behandlingId: UUID) =
    ÅrsakRevurdering(
        behandlingId = behandlingId,
        opplysningskilde = this.kilde,
        årsak = this.årsak,
        beskrivelse = this.beskrivelse
    )

fun ÅrsakRevurdering.tilDto() = ÅrsakRevurderingDto(
    kilde = this.opplysningskilde,
    årsak = this.årsak,
    beskrivelse = this.beskrivelse,
    endretAv = this.sporbar.endret.endretAv,
    endretTid = this.sporbar.endret.endretTid
)