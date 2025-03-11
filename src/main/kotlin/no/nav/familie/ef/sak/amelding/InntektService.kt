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
        fom: YearMonth,
        tom: YearMonth,
    ): List<Inntektsmåned> {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val inntekt =
            aMeldingInntektClient.hentInntekt(
                personIdent = aktivIdent,
                månedFom = fom,
                månedTom = tom,
            )

        return inntekt.inntektsmåneder
    }

    fun hentÅrsinntekt(
        personIdent: String,
        årstallIFjor: Int,
    ): Int {
        val inntektV2Response = aMeldingInntektClient.hentInntekt(personIdent = personIdent, månedFom = YearMonth.of(årstallIFjor, 1), månedTom = YearMonth.of(årstallIFjor, 12))

        val inntektsliste = inntektV2Response.inntektsmåneder.flatMap { månedsdata -> månedsdata.inntektListe }
        val totalBeløp = inntektsliste.filter { it.type != InntektType.YTELSE_FRA_OFFENTLIGE && it.beskrivelse != "feriepenger" }.sumOf { it.beløp }.toInt()

        return totalBeløp
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
