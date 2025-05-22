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
) {
    fun hentInntekt(
        fagsakId: UUID,
        månedFom: YearMonth,
        månedTom: YearMonth,
    ): List<Inntektsmåned> {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val inntekt =
            aMeldingInntektClient.hentInntekt(
                personIdent = aktivIdent,
                månedFom = månedFom,
                månedTom = månedTom,
            )

        return inntekt.inntektsmåneder
    }

    fun hentInntektNyIngress(
        fagsakId: UUID,
        månedFom: YearMonth,
        månedTom: YearMonth,
    ): Map<String, Any> {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val inntekt =
            aMeldingInntektClient.hentInntektNyIngress(
                personIdent = aktivIdent,
                månedFom = månedFom,
                månedTom = månedTom,
            )

        return inntekt
    }

    fun hentÅrsinntekt(
        personIdent: String,
        årstallIFjor: Int,
    ): Int {
        val inntektV2Response = aMeldingInntektClient.hentInntekt(personIdent = personIdent, månedFom = YearMonth.of(årstallIFjor, 1), månedTom = YearMonth.of(årstallIFjor, 12))
        return inntektV2Response.inntektsmåneder.summerTotalInntekt().toInt()
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
