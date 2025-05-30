package no.nav.familie.ef.sak.behandling.henlegg

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.brev.FrittståendeBrevService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendTrukketSøknadHenleggelsesbrevTask.TYPE,
    maxAntallFeil = 3,
    settTilManuellOppfølgning = true,
    triggerTidVedFeilISekunder = 15 * 60L,
    beskrivelse = "Send henleggelsesbrev om trukket søknad",
)
class SendTrukketSøknadHenleggelsesbrevTask(
    private val behandlingService: BehandlingService,
    private val henleggService: HenleggService,
    private val iverksettClient: IverksettClient,
    private val arbeidsfordelingService: ArbeidsfordelingService,
    private val frittståendeBrevService: FrittståendeBrevService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val henleggelsesbrevDto = objectMapper.readValue<HenleggelsesbrevDto>(task.payload)
        val saksbehandlerIdent = henleggelsesbrevDto.saksbehandlerIdent
        val saksbehandling = behandlingService.hentSaksbehandling(henleggelsesbrevDto.behandlingId)
        val journalførendeEnhet = arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(saksbehandling.ident)
        val henleggBrev =
            henleggService.genererHenleggelsesbrev(
                behandlingId = henleggelsesbrevDto.behandlingId,
                saksbehandlerNavn = henleggelsesbrevDto.saksbehandlerSignatur,
                saksbehandlerIdent = saksbehandlerIdent,
            )

        val mottakere = frittståendeBrevService.lagBrevMottaker(saksbehandling = saksbehandling, skalHaSaksbehandlerIdent = true)

        val hennleggbrevDto =
            FrittståendeBrevDto(
                personIdent = saksbehandling.ident,
                eksternFagsakId = saksbehandling.eksternFagsakId,
                stønadType = saksbehandling.stønadstype,
                brevtype = FrittståendeBrevType.INFORMASJONSBREV_TRUKKET_SØKNAD,
                tittel = "Informasjonsbrev - trukket søknad",
                fil = henleggBrev,
                journalførendeEnhet = journalførendeEnhet,
                saksbehandlerIdent = saksbehandlerIdent,
                mottakere = mottakere,
            )
        iverksettClient.sendFrittståendeBrev(frittståendeBrevDto = hennleggbrevDto)
    }

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            saksbehandlerSignatur: String,
            saksbehandlerIdent: String,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(HenleggelsesbrevDto(behandlingId, saksbehandlerSignatur, saksbehandlerIdent)),
            )

        const val TYPE = "SendHenleggelsesbrevOmTrukketSøknadTask"
    }
}
