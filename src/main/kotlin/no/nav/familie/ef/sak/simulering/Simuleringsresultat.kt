package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.repository.domain.Sporbar
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table
data class Simuleringsresultat(@Id
                               val behandlingId: UUID,
                               val data: DetaljertSimuleringResultat,
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val sporbar: Sporbar = Sporbar())


