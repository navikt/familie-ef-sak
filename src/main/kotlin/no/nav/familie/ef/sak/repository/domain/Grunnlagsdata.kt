package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.domene.GrunnlagsdataDomene
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Grunnlagsdata(@Id
                         val behandlingId: UUID,
                         val data: GrunnlagsdataDomene,
                         val lagtTilEtterFerdigstilling: Boolean = false,
                         @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                         val sporbar: Sporbar = Sporbar())
