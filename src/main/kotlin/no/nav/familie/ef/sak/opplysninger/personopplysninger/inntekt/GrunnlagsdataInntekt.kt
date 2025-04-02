package no.nav.familie.ef.sak.opplysninger.personopplysninger.inntekt

import no.nav.familie.ef.sak.amelding.InntektResponse
import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.ef.sak.felles.domain.SporbarUtils
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDateTime
import java.util.UUID

data class GrunnlagsdataInntekt(
    @Id
    val behandlingId: UUID,
    val inntektsdata: InntektResponse,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
    val oppdaterteInntektsdataHentetTid: LocalDateTime = SporbarUtils.now(),
    val oppdaterteInntektsdata: InntektResponse? = null,
)
