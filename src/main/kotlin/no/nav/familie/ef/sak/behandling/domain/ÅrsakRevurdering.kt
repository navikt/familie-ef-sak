package no.nav.familie.ef.sak.behandling.domain

import no.nav.familie.ef.sak.felles.domain.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("arsak_revurdering")
data class ÅrsakRevurdering(
    @Id
    val behandlingId: UUID,
    val kilde: KildeOpplysninger,
    @Column("arsak")
    val årsak: Årsak,
    val beskrivelse: String?,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)

enum class KildeOpplysninger {
    MELDING_MODIA,
    INNSENDT_DOKUMENTASJON,
    BESKJED_ANNEN_ENHET,
    LIVSHENDELSER,
    OPPLYSNINGER_INTERNE_KONTROLLER
}

enum class Årsak {
    ENDRING_INNTEKT
}