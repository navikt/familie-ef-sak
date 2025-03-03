package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.amelding.ekstern.InntektType
import no.nav.familie.ef.sak.amelding.inntektv2.MånedsInntekt
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
    ): List<MånedsInntekt> {
        val aktivIdent = fagsakService.hentAktivIdent(fagsakId)
        val inntekt = aMeldingInntektClient.hentInntekt(
            personident = aktivIdent,
            fom = fom,
            tom = tom
        )

        // TDDO: Kanskje kaste en feil her om det ikke går.
        return inntekt.maanedsData
    }

    fun hentÅrsinntekt(
        personIdent: String,
        årstallIFjor: Int,
    ): Int {
        val inntektV2Response = aMeldingInntektClient.hentInntekt(personIdent, YearMonth.of(årstallIFjor, 1), YearMonth.of(årstallIFjor, 12))
        val inntektsliste = inntektV2Response.maanedsData.flatMap { månedsdata -> månedsdata.inntektListe}
        val totalBeløp = inntektsliste.filter { it.type != InntektType.YTELSE_FRA_OFFENTLIGE && it.beskrivelse != "feriepenger" }.sumOf { it.beloep }.toInt()

        /*val inntektListeResponse = aMeldingInntektClient.hentInntekt(personIdent, YearMonth.of(årstallIFjor, 1), YearMonth.of(årstallIFjor, 12))
        val inntektListe = inntektListeResponse.arbeidsinntektMåned?.flatMap { it.arbeidsInntektInformasjon?.inntektListe ?: emptyList() }
        val totalBeløp = inntektListe?.filter { it.inntektType != InntektType.YTELSE_FRA_OFFENTLIGE && it.beskrivelse != "feriepenger" }?.sumOf { it.beløp } ?: 0 */

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
