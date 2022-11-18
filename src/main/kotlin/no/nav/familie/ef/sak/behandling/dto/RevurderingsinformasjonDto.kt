package no.nav.familie.ef.sak.behandling.dto

import no.nav.familie.ef.sak.behandling.domain.Opplysningskilde
import no.nav.familie.ef.sak.behandling.domain.Revurderingsårsak
import no.nav.familie.ef.sak.behandling.domain.ÅrsakRevurdering
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class RevurderingsinformasjonDto(
    val kravMottatt: LocalDate? = null,
    val årsakRevurdering: ÅrsakRevurderingDto? = null,
    val endretTid: LocalDateTime? = null
)

data class ÅrsakRevurderingDto(
    val opplysningskilde: Opplysningskilde,
    val årsak: Revurderingsårsak,
    val beskrivelse: String? = null
)

fun ÅrsakRevurderingDto.tilDomene(behandlingId: UUID) =
    ÅrsakRevurdering(
        behandlingId = behandlingId,
        opplysningskilde = this.opplysningskilde,
        årsak = this.årsak,
        beskrivelse = this.beskrivelse
    )

fun ÅrsakRevurdering.tilDto() = ÅrsakRevurderingDto(
    opplysningskilde = this.opplysningskilde,
    årsak = this.årsak,
    beskrivelse = this.beskrivelse
)