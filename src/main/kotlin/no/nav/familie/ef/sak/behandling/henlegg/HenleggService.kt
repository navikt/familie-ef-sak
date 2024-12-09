package no.nav.familie.ef.sak.behandling.henlegg

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.dto.HenlagtDto
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.brev.FamilieDokumentClient
import no.nav.familie.ef.sak.brev.VedtaksbrevService
import no.nav.familie.ef.sak.brev.dto.Flettefelter
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
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

    fun genererHenleggBrev(
        stønadstype: StønadType,
        saksbehandlerSignatur: String,
        personIdent: String,
    ): ByteArray {
        val visningsNavn = personopplysningerService.hentGjeldeneNavn(listOf(personIdent)).getValue(personIdent)
        val flettefelter = Flettefelter(navn = listOf(visningsNavn), fodselsnummer = listOf(personIdent))

        val henleggelsesbrev = Henleggelsesbrev(
            Delmaler(
                listOf(
                    Stonadstype(
                        Flettefelter2(
                            listOf(stønadstype.toString()
                            )
                        )
                    )
                )
            ),
            flettefelter
        )

        val html =
            brevClient
                .genererHtml(
                    brevmal = "informasjonsbrevTrukketSoknad",
                    saksbehandlerBrevrequest = objectMapper.valueToTree(henleggelsesbrev),
                    saksbehandlersignatur = saksbehandlerSignatur,
                    enhet = "Nav Arbeid og ytelser",
                    skjulBeslutterSignatur = true,
                ).replace(VedtaksbrevService.BESLUTTER_VEDTAKSDATO_PLACEHOLDER, LocalDate.now().norskFormat())

        return familieDokumentClient.genererPdfFraHtml(html)
    }
}

private data class Henleggelsesbrev(
    val delmaler: Delmaler,
    val flettefelter: Flettefelter,
)

private data class Stonadstype(
    val flettefelter: Flettefelter2,
)

private data class Delmaler(
    val stonadstype: List<Stonadstype>,
)

private data class Flettefelter2(
    val stonadstype: List<String>,
)
