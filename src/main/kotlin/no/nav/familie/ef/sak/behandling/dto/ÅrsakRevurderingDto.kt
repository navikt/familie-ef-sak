package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.domain.KildeOpplysninger
import no.nav.familie.ef.sak.behandling.domain.Årsak
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import java.time.LocalDateTime
import java.util.UUID

data class ÅrsakRevurderingDto(
    val kilde: KildeOpplysninger,
    val årsak: Årsak,
    val beskrivelse: String?,
    val endretAv: String,
    val endretTid: LocalDateTime,
)

fun ÅrsakRevurderingDto.tilDomene(behandlingId: UUID) =
    ÅrsakRevurdering(
        behandlingId = behandlingId,
        kilde = this.kilde,
        årsak = this.årsak,
        beskrivelse = this.beskrivelse
    )

fun ÅrsakRevurdering.tilDto() = ÅrsakRevurderingDto(
    kilde = this.kilde,
    årsak = this.årsak,
    beskrivelse = this.beskrivelse,
    endretAv = this.sporbar.endret.endretAv,
    endretTid = this.sporbar.endret.endretTid
)