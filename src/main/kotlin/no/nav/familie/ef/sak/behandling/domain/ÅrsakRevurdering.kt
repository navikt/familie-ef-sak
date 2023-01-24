package no.nav.familie.ef.sak.behandling.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("arsak_revurdering")
data class ÅrsakRevurdering(
    @Id
    val behandlingId: UUID,
    val opplysningskilde: Opplysningskilde,
    @Column("arsak")
    val årsak: Revurderingsårsak,
    val beskrivelse: String?,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)
