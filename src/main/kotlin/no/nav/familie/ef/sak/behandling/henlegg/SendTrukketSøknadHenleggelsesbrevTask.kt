package no.nav.familie.ef.sak.behandling.henlegg

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendTrukketSøknadHenleggelsesbrevTask.TYPE,
    maxAntallFeil = 1,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Send henleggelsesbrev om trukket søknad",
)
class SendTrukketSøknadHenleggelsesbrevTask(
    private val behandlingService: BehandlingService,
    private val henleggService: HenleggService,
    private val iverksettClient: IverksettClient,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val personopplysningerService: PersonopplysningerService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val payload = objectMapper.readValue<HenleggTaskDto>(task.payload)
        val behandlingId = payload.behandlingId
        val saksbehandlerSignatur = payload.saksbehandlerSignatur
        val saksbehandlerIdent = payload.saksbehandlerIdent
        val brukerIdent = behandlingService.hentAktivIdent(behandlingId)
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        val genererHenleggBrev = henleggService.genererHenleggBrev(saksbehandling, saksbehandlerSignatur)
        val journalFørendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(brukerIdent)
        val mottaker =
            Brevmottaker(
                ident = brukerIdent,
                navn = personopplysningerService.hentGjeldeneNavn(listOf(brukerIdent)).getValue(brukerIdent),
                mottakerRolle = Brevmottaker.MottakerRolle.BRUKER,
                identType = Brevmottaker.IdentType.PERSONIDENT,
            )

        val hennleggbrevDTO =
            FrittståendeBrevDto(
                personIdent = saksbehandling.ident,
                eksternFagsakId = saksbehandling.eksternFagsakId,
                stønadType = saksbehandling.stønadstype,
                brevtype = FrittståendeBrevType.INFORMASJONSBREV_TRUKKET_SØKNAD,
                tittel = "Brev søknad trukket og derfor henlagt", // TODO sjekk med Miria @Endre
                fil = genererHenleggBrev,
                journalførendeEnhet = journalFørendeEnhet,
                saksbehandlerIdent = saksbehandlerIdent,
                mottakere = listOf(mottaker),
            )
        iverksettClient.sendFrittståendeBrev(
            frittståendeBrevDto = hennleggbrevDTO,
        )
    }

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            saksbehandlerSignatur: String,
            saksbehandlerIdent: String,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(HenleggTaskDto(behandlingId, saksbehandlerSignatur, saksbehandlerIdent)),
            )

        const val TYPE = "SendHenleggelsesbrevOmTrukkedSøknadTask"
    }
}

private fun String.toUUID(): UUID = UUID.fromString(this)
