package no.nav.familie.ef.sak.brev

import com.fasterxml.jackson.databind.JsonNode
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.brev.BrevsignaturService.Companion.NAV_ENHET_NAY
import no.nav.familie.ef.sak.brev.domain.BrevmottakerOrganisasjon
import no.nav.familie.ef.sak.brev.domain.BrevmottakerPerson
import no.nav.familie.ef.sak.brev.dto.FrittståendeSanitybrevDto
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.tilIverksettDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.felles.objectMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto as FrittståendeBrevDtoIverksetting

@Service
class FrittståendeBrevService(
    private val brevClient: BrevClient,
    private val fagsakService: FagsakService,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val iverksettClient: IverksettClient,
    private val brevsignaturService: BrevsignaturService,
    private val mellomlagringBrevService: MellomlagringBrevService,
    private val familieDokumentClient: FamilieDokumentClient,
    private val brevmottakereService: BrevmottakereService,
    private val personopplysningerService: PersonopplysningerService,
) {
    fun lagFrittståendeSanitybrev(
        fagsakId: UUID,
        brevmal: String,
        brevrequest: JsonNode,
    ): ByteArray {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val signatur = brevsignaturService.lagSaksbehandlerSignatur(fagsak.hentAktivIdent(), VedtakErUtenBeslutter(true))

        val html =
            brevClient
                .genererHtml(
                    brevmal = brevmal,
                    saksbehandlerBrevrequest = brevrequest,
                    saksbehandlersignatur = signatur.navn,
                    saksbehandlerEnhet = signatur.enhet,
                    enhet = signatur.enhet,
                    skjulBeslutterSignatur = signatur.skjulBeslutter,
                ).replace(VedtaksbrevService.BESLUTTER_VEDTAKSDATO_PLACEHOLDER, LocalDate.now().norskFormat())

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    fun sendFrittståendeSanitybrev(
        fagsakId: UUID,
        sendBrevRequest: FrittståendeSanitybrevDto,
    ) {
        val saksbehandlerIdent = SikkerhetContext.hentSaksbehandler()
        val brevmottakere = validerOgMapBrevmottakere(sendBrevRequest.mottakere)
        val fagsak = fagsakService.fagsakMedOppdatertPersonIdent(fagsakId)
        val ident = fagsak.hentAktivIdent()
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(ident)
        iverksettClient.sendFrittståendeBrev(
            FrittståendeBrevDtoIverksetting(
                personIdent = ident,
                eksternFagsakId = fagsak.eksternId,
                stønadType = fagsak.stønadstype,
                tittel = sendBrevRequest.tittel,
                fil = sendBrevRequest.pdf,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandlerIdent = saksbehandlerIdent,
                mottakere = brevmottakere,
            ),
        )
        mellomlagringBrevService.slettMellomlagretFrittståendeBrev(fagsakId, saksbehandlerIdent)
        brevmottakereService.slettBrevmottakereForFagsakOgSaksbehandlerHvisFinnes(fagsakId, saksbehandlerIdent)
    }

    fun lagBrevForInnhentingAvAktivitetsplikt(
        visningsnavn: String,
        personIdent: String,
    ): ByteArray {
        val brevRequest = BrevRequest(flettefelter = Flettefelter(navn = listOf(visningsnavn), fodselsnummer = listOf(personIdent)))

        val html =
            brevClient
                .genererHtml(
                    brevmal = "innhentingOpplysningerAktivitetEtterUtdanning",
                    saksbehandlerBrevrequest = objectMapper.valueToTree(brevRequest),
                    saksbehandlersignatur = "",
                    saksbehandlerEnhet = NAV_ENHET_NAY,
                    enhet = NAV_ENHET_NAY,
                    skjulBeslutterSignatur = true,
                ).replace(VedtaksbrevService.BESLUTTER_VEDTAKSDATO_PLACEHOLDER, LocalDate.now().norskFormat())

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    fun lagFrittståendeBrevDto(
        saksbehandling: Saksbehandling,
        tittel: String,
        fil: ByteArray,
        brevmottakere: BrevmottakereDto? = null,
    ): FrittståendeBrevDto {
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(saksbehandling.ident)

        val mottakere: List<Brevmottaker> =
            if (brevmottakere != null) {
                validerOgMapBrevmottakere(brevmottakere)
            } else {
                lagBrevMottaker(saksbehandling, skalHaSaksbehandlerIdent = true)
            }

        val brevDto =
            FrittståendeBrevDto(
                personIdent = saksbehandling.ident,
                eksternFagsakId = saksbehandling.eksternFagsakId,
                stønadType = saksbehandling.stønadstype,
                tittel = tittel,
                fil = fil,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandlerIdent = "VL",
                mottakere = mottakere,
            )

        return brevDto
    }

    fun lagBrevMottaker(
        saksbehandling: Saksbehandling,
        skalHaSaksbehandlerIdent: Boolean = false,
    ) = listOf(
        Brevmottaker(
            ident = if (skalHaSaksbehandlerIdent) saksbehandling.ident else "VL",
            navn = personopplysningerService.hentGjeldeneNavn(listOf(saksbehandling.ident)).getValue(saksbehandling.ident),
            mottakerRolle = Brevmottaker.MottakerRolle.BRUKER,
            identType = Brevmottaker.IdentType.PERSONIDENT,
        ),
    )

    private fun mapMottakere(mottakere: BrevmottakereDto): List<Brevmottaker> {
        val personer = mottakere.personer.map(BrevmottakerPerson::tilIverksettDto)
        val organisasjoner = mottakere.organisasjoner.map(BrevmottakerOrganisasjon::tilIverksettDto)
        return personer + organisasjoner
    }

    private fun validerOgMapBrevmottakere(mottakere: BrevmottakereDto?): List<Brevmottaker> {
        brukerfeilHvis(mottakere == null || (mottakere.personer.isEmpty() && mottakere.organisasjoner.isEmpty())) {
            "Kan ikke sende frittstående brev uten at minst en brevmottaker er lagt til"
        }
        return mapMottakere(mottakere)
    }
}
