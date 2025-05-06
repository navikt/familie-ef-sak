package no.nav.familie.ef.sak.næringsinntektskontroll

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevRequest
import no.nav.familie.ef.sak.brev.BrevsignaturService.Companion.NAV_ENHET_NAY
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.Flettefelter
import no.nav.familie.ef.sak.brev.FrittståendeBrevService
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class NæringsinntektKontrollBrev(
    val behandlingService: BehandlingService,
    val personopplysningerService: PersonopplysningerService,
    val arbeidsfordelingService: ArbeidsfordelingService,
    val brevClient: BrevClient,
    val familieDokumentClient: FamilieDokumentClient,
    val iverksettClient: IverksettClient,
    val frittståendeBrevService: FrittståendeBrevService,
) {
    fun sendBrev(næringsinntektDataForBeregning: NæringsinntektDataForBeregning) {
        val saksbehandling = behandlingService.hentSaksbehandling(næringsinntektDataForBeregning.behandlingId)
        val brevPdf = genererVarselbrevInntekt(næringsinntektDataForBeregning)

        val tittel = "Inntekt endret for selvstendig næringsdrivende"

        val varselbrevInntektDto =
            frittståendeBrevService.lagFrittståendeBrevDto(
                saksbehandling,
                tittel,
                brevPdf,
            )

        iverksettClient.sendFrittståendeBrev(frittståendeBrevDto = varselbrevInntektDto)
    }

    fun genererVarselbrevInntekt(
        næringsinntektDataForBeregning: NæringsinntektDataForBeregning,
    ): ByteArray {
        val varselbrevEndretInntekt =
            BrevRequest(
                lagFlettefelt(næringsinntektDataForBeregning.personIdent, næringsinntektDataForBeregning.forventetInntektIFjor),
            )

        val html =
            brevClient
                .genererHtml(
                    brevmal = "varselbrevInntekt",
                    saksbehandlersignatur = "Vedtaksløsningen",
                    saksbehandlerBrevrequest = objectMapper.valueToTree(varselbrevEndretInntekt),
                    saksbehandlerEnhet = NAV_ENHET_NAY,
                    enhet = NAV_ENHET_NAY,
                    skjulBeslutterSignatur = true,
                ).replace(VedtaksbrevService.BESLUTTER_VEDTAKSDATO_PLACEHOLDER, LocalDate.now().norskFormat())
        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun lagFlettefelt(
        personIdent: String,
        forventetInntekt: Int,
    ): Flettefelter {
        val visningsNavn = personopplysningerService.hentGjeldeneNavn(listOf(personIdent)).getValue(personIdent)
        val navnOgIdentFlettefelt = Flettefelter(navn = listOf(visningsNavn), fodselsnummer = listOf(personIdent), forventetInntekt = listOf(forventetInntekt))
        return navnOgIdentFlettefelt
    }
}
