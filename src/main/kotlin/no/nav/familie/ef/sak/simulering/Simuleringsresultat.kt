package no.nav.familie.ef.sak.simulering

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.kontrakter.felles.simulering.BeriketSimuleringsresultat
import no.nav.familie.kontrakter.felles.simulering.DetaljertSimuleringResultat
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table
data class Simuleringsresultat(@Id
                               val behandlingId: UUID,
                               val data: DetaljertSimuleringResultat,
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val sporbar: Sporbar = Sporbar(),
                               val beriketData: BeriketSimuleringsresultat? = null)


