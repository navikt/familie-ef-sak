package no.nav.familie.ef.sak.behandling.henlegg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.BrevsignaturService
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.brev.dto.Flettefelter
import no.nav.familie.ef.sak.brev.dto.SignaturDto
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.vedtak.domain.VedtakErUtenBeslutter
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class HenleggService(
    private val behandlingService: BehandlingService,
    private val oppgaveService: OppgaveService,
    private val brevClient: BrevClient,
    private val familieDokumentClient: FamilieDokumentClient,
    private val personopplysningerService: PersonopplysningerService,
    private val brevsignaturService: BrevsignaturService,
) {
    @Transactional
    fun henleggBehandling(
        behandlingId: UUID,
        henlagt: HenlagtDto,
    ): Behandling {
        val behandling = behandlingService.henleggBehandling(behandlingId, henlagt)
        ferdigstillOppgaveTask(behandling)
        return behandling
    }

    @Transactional
    fun henleggBehandlingUtenOppgave(
        behandlingId: UUID,
        henlagt: HenlagtDto,
    ): Behandling {
        val behandling = behandlingService.henleggBehandling(behandlingId, henlagt, false)
        settEfOppgaveTilFerdig(behandling)
        return behandling
    }

    private fun ferdigstillOppgaveTask(behandling: Behandling) {
        oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(
            behandlingId = behandling.id,
            Oppgavetype.BehandleSak,
            ignorerFeilregistrert = true,
        )
        oppgaveService.ferdigstillOppgaveHvisOppgaveFinnes(
            behandlingId = behandling.id,
            Oppgavetype.BehandleUnderkjentVedtak,
            ignorerFeilregistrert = true,
        )
    }

    private fun settEfOppgaveTilFerdig(behandling: Behandling) {
        oppgaveService.settEfOppgaveTilFerdig(
            behandlingId = behandling.id,
            Oppgavetype.BehandleSak,
        )
        oppgaveService.settEfOppgaveTilFerdig(
            behandlingId = behandling.id,
            Oppgavetype.BehandleUnderkjentVedtak,
        )
    }

    fun genererHenleggelsesbrev(
        behandlingId: UUID,
        saksbehandlerNavn: String? = null,
        saksbehandlerIdent: String? = null,
    ): ByteArray {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val personIdent = behandlingService.hentAktivIdent(behandlingId)
        val stønadstype = saksbehandling.stønadstype
        val henleggelsesbrev =
            Henleggelsesbrev(
                lagDemalMedFlettefeltForStønadstype(stønadstype),
                lagNavnOgIdentFlettefelt(personIdent),
            )
        val signatur = utledSignatur(saksbehandlerNavn, saksbehandlerIdent, personIdent)

        val html =
            brevClient
                .genererHtml(
                    brevmal = "informasjonsbrevTrukketSoknad",
                    saksbehandlerBrevrequest = objectMapper.valueToTree(henleggelsesbrev),
                    saksbehandlersignatur = signatur.navn,
                    enhet = signatur.enhet,
                    skjulBeslutterSignatur = signatur.skjulBeslutter,
                ).replace(VedtaksbrevService.BESLUTTER_VEDTAKSDATO_PLACEHOLDER, LocalDate.now().norskFormat())

        return familieDokumentClient.genererPdfFraHtml(html)
    }

    private fun utledSignatur(
        saksbehandlerNavn: String?,
        saksbehandlerIdent: String?,
        personIdent: String
    ) = if (saksbehandlerNavn == null || saksbehandlerIdent == null) {
        brevsignaturService.lagSaksbehandlerSignatur(personIdent, VedtakErUtenBeslutter(true))
    } else {
        brevsignaturService.lagSaksbehandlerSignatur(personIdent, VedtakErUtenBeslutter(true), saksbehandlerNavn, saksbehandlerIdent)
    }


    private fun lagNavnOgIdentFlettefelt(personIdent: String): Flettefelter {
        val visningsNavn = personopplysningerService.hentGjeldeneNavn(listOf(personIdent)).getValue(personIdent)
        val navnOgIdentFlettefelt = Flettefelter(navn = listOf(visningsNavn), fodselsnummer = listOf(personIdent))
        return navnOgIdentFlettefelt
    }

    private fun lagDemalMedFlettefeltForStønadstype(stønadstype: StønadType) =
        Delmaler(
            listOf(
                Delmal(
                    DelmalFlettefelt(
                        listOf(
                            lagStringForDelmalFlettefelt(stønadstype),
                        ),
                    ),
                ),
            ),
        )

    private fun lagStringForDelmalFlettefelt(stønadstype: StønadType): String =
        when (stønadstype) {
            StønadType.BARNETILSYN -> "stønad til " + stønadstype.name.lowercase()
            StønadType.SKOLEPENGER -> "stønad til " + stønadstype.name.lowercase()
            else -> stønadstype.name.lowercase()
        }
}

private data class Henleggelsesbrev(
    val delmaler: Delmaler,
    val flettefelter: Flettefelter,
)

private data class Delmal(
    val flettefelter: DelmalFlettefelt,
)

private data class Delmaler(
    val stonadstype: List<Delmal>,
)

private data class DelmalFlettefelt(
    val stonadstype: List<String>,
)
