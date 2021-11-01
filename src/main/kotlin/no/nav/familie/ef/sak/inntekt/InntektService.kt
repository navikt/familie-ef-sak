package no.nav.familie.ef.sak.inntekt

import no.nav.familie.ef.sak.fagsak.FagsakService
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class InntektService(
        private val inntektClient: InntektClient,
        private val fagsakService: FagsakService,
        private val inntektMapper: InntektMapper
) {

    fun hentInntekt(fagsakId: UUID, fom: YearMonth, tom: YearMonth): InntektResponseDto {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val inntekt = inntektClient.hentInntekt(aktivIdent, fom, tom)
        return inntektMapper.map(inntekt)
    }
}
