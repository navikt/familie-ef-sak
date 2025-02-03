package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider.objectMapper
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
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
) {
    fun sendBrev(næringsinntektDataForBeregning: NæringsinntektDataForBeregning) {
        val saksbehandling = behandlingService.hentSaksbehandling(næringsinntektDataForBeregning.behandlingId)
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(saksbehandling.ident)
        val brevPdf = genererVarselbrevInntekt(næringsinntektDataForBeregning)
        val varselbrevInntektDto =
            FrittståendeBrevDto(
                personIdent = saksbehandling.ident,
                eksternFagsakId = saksbehandling.eksternFagsakId,
                stønadType = saksbehandling.stønadstype,
                tittel = "Inntekt endret for selvstendig næringsdrivende",
                fil = brevPdf,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandlerIdent = "VL",
                mottakere = lagBrevMottaker(saksbehandling),
            )
        iverksettClient.sendFrittståendeBrev(frittståendeBrevDto = varselbrevInntektDto)
    }

    fun genererVarselbrevInntekt(
        næringsinntektDataForBeregning: NæringsinntektDataForBeregning,
    ): ByteArray {
        val varselbrevEndretInntekt =
            VarselbrevEndretInntekt(
                lagFlettefelt(næringsinntektDataForBeregning.personIdent, næringsinntektDataForBeregning.forventetInntektIFjor),
            )

        val html =
            brevClient
                .genererHtml(
                    brevmal = "varselbrevInntekt",
                    saksbehandlersignatur = "Vedtaksløsningen",
                    saksbehandlerBrevrequest = objectMapper.valueToTree(varselbrevEndretInntekt),
                    enhet = "NAV Arbeid og ytelser",
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

    private fun lagBrevMottaker(saksbehandling: Saksbehandling) =
        listOf(
            Brevmottaker(
                ident = "VL",
                navn = personopplysningerService.hentGjeldeneNavn(listOf(saksbehandling.ident)).getValue(saksbehandling.ident),
                mottakerRolle = Brevmottaker.MottakerRolle.BRUKER,
                identType = Brevmottaker.IdentType.PERSONIDENT,
            ),
        )
}

data class VarselbrevEndretInntekt(
    val flettefelter: Flettefelter,
)

data class Flettefelter(
    val navn: List<String>,
    val fodselsnummer: List<String>,
    val forventetInntekt: List<Int>,
)
