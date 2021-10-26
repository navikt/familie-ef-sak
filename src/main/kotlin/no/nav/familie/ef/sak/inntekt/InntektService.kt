package no.nav.familie.ef.sak.inntekt

import no.nav.familie.ef.sak.fagsak.FagsakService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class InntektService(
        private val inntektClient: InntektClient,
        private val fagsakService: FagsakService
) {

    fun hentInntekt(fagsakId: UUID): Map<String, Any> {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        return inntektClient.hentInntekt(aktivIdent)
    }
}
