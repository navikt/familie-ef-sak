package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class InntektService(
    private val aMeldingInntektClient: AMeldingInntektClient,
    private val fagsakService: FagsakService,
    private val fagsakPersonService: FagsakPersonService,
    private val inntektMapper: InntektMapper,
) {
    fun hentInntekt(
        fagsakId: UUID,
        fom: YearMonth,
        tom: YearMonth,
    ): AMeldingInntektDto {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val inntekt = aMeldingInntektClient.hentInntekt(aktivIdent, fom, tom)
        return inntektMapper.mapInntekt(inntekt)
    }

    fun genererAInntektUrl(fagsakPersonId: UUID): String {
        val personIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        return aMeldingInntektClient.genererAInntektUrl(personIdent)
    }

    fun genererAInntektArbeidsforholdUrl(fagsakId: UUID): String {
        val personIdent = fagsakService.hentAktivIdent(fagsakId)
        return aMeldingInntektClient.genererAInntektArbeidsforholdUrl(personIdent)
    }

    fun genererAInntektUrlFagsak(fagsakId: UUID): String {
        val personIdent = fagsakService.hentAktivIdent(fagsakId)
        return aMeldingInntektClient.genererAInntektUrl(personIdent)
    }
}
