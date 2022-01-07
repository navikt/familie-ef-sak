package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class InntektService(
    private val aMeldingInntektClient: AMeldingInntektClient,
    private val fagsakService: FagsakService,
    private val inntektMapper: InntektMapper
) {

    fun hentInntekt(fagsakId: UUID, fom: YearMonth, tom: YearMonth): AMeldingInntektDto {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val inntekt = aMeldingInntektClient.hentInntekt(aktivIdent, fom, tom)
        return inntektMapper.mapInntekt(inntekt)
    }

    fun genererAInntektUrl(fagsakId: UUID): String {
        val personIdent = fagsakService.hentAktivIdent(fagsakId)
        return aMeldingInntektClient.genererAInntektUrl(personIdent)
    }
}
