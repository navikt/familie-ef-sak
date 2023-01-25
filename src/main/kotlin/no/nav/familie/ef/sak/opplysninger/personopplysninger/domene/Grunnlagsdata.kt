package no.nav.familie.ef.sak.opplysninger.personopplysninger.domene

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.util.UUID

data class Grunnlagsdata(
    @Id
    val behandlingId: UUID,
    val data: GrunnlagsdataDomene,
    val lagtTilEtterFerdigstilling: Boolean = false,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val oppdaterteDataHentetTid: LocalDateTime = SporbarUtils.now(),
    val oppdaterteData: GrunnlagsdataDomene? = null
) {
    fun tilGrunnlagsdataMedMetadata() = GrunnlagsdataMedMetadata(data, sporbar.opprettetTid)
// burde vi ha en tidspunkt for når dataen ble oppdatert? endret tid er kanskje kun når endringer ble sjekket
}
