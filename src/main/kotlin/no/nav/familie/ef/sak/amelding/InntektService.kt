package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntektClient
import no.nav.familie.ef.sak.amelding.ekstern.ArbeidOgInntektClient
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class InntektService(
    private val aMeldingInntektClient: AMeldingInntektClient,
    private val arbeidOgInntektClient: ArbeidOgInntektClient,
    private val fagsakService: FagsakService,
    private val fagsakPersonService: FagsakPersonService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

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

    fun hentÅrsinntekt(
        personIdent: String,
        årstallIFjor: Int,
    ): Int {
        val inntektV2Response = aMeldingInntektClient.hentInntekt(personIdent = personIdent, månedFom = YearMonth.of(årstallIFjor, 1), månedTom = YearMonth.of(årstallIFjor, 12))
        return inntektV2Response.inntektsmåneder.summerTotalInntekt().toInt()
    }

    fun genererAInntektUrl(fagsakPersonId: UUID): String {
        val personIdent = fagsakPersonService.hentAktivIdent(fagsakPersonId)
        val url = arbeidOgInntektClient.genererAInntektUrl(personIdent)
        loggGenerertAInntektUrl(kilde = "fagsakPersonId=$fagsakPersonId", personIdent = personIdent, url = url)
        return url
    }

    fun genererAInntektArbeidsforholdUrl(fagsakId: UUID): String {
        val personIdent = fagsakService.hentAktivIdent(fagsakId)
        val url = arbeidOgInntektClient.genererAInntektArbeidsforholdUrl(personIdent)
        loggGenerertAInntektUrl(kilde = "fagsakId=$fagsakId (arbeidsforhold)", personIdent = personIdent, url = url)
        return url
    }

    fun genererAInntektUrlFagsak(fagsakId: UUID): String {
        val personIdent = fagsakService.hentAktivIdent(fagsakId)
        val url = arbeidOgInntektClient.genererAInntektUrl(personIdent)
        loggGenerertAInntektUrl(kilde = "fagsakId=$fagsakId", personIdent = personIdent, url = url)
        return url
    }

    private fun loggGenerertAInntektUrl(
        kilde: String,
        personIdent: String,
        url: String,
    ) {
        val saksbehandler = SikkerhetContext.hentSaksbehandlerEllerSystembruker()
        logger.info("Genererer a-inntekt-url for $kilde (saksbehandler=$saksbehandler)")
        secureLogger.info("Genererer a-inntekt-url for $kilde (saksbehandler=$saksbehandler) personIdent=$personIdent url=$url")
    }
}
