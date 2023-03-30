package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import no.nav.familie.ef.sak.behandling.BehandlingPåVentService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.dto.SettPåVentRequest
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.cucumber.domeneparser.Domenenøkkel
import no.nav.familie.ef.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ef.sak.cucumber.domeneparser.parseEnum
import no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parseString
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriEnum
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriString
import no.nav.familie.ef.sak.felles.util.DatoUtil
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.vedtak.NullstillVedtakService
import no.nav.familie.kontrakter.felles.oppgave.MappeDto
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import java.time.LocalDateTime

class SettPåVentStepDefinitions {

    lateinit var eksisterendeOppgave: Oppgave
    lateinit var settOppgavePåVentRequest: SettPåVentRequest
    lateinit var mapper: List<MappeDto>

    val featureToggleService = mockFeatureToggleService()
    val behandlingService = mockk<BehandlingService>()
    val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    val taskService = mockk<TaskService>()
    val nullstillVedtakService = mockk<NullstillVedtakService>()
    val oppgaveService = mockk<OppgaveService>()

    val påVentService = BehandlingPåVentService(
        behandlingService,
        behandlingshistorikkService,
        taskService,
        nullstillVedtakService,
        featureToggleService,
        oppgaveService,
    )

    val behandling = behandling()
    val oppgaveSlot = slot<Oppgave>()

    @Gitt("eksisterende oppgave")
    fun eksisterendeOppgave(dataTable: DataTable) {
        val verdier = dataTable.asMap()
        eksisterendeOppgave =
            Oppgave(
                tilordnetRessurs = parseString(SettPåVentDomeneBegrep.SAKSBEHANDLER, verdier),
                fristFerdigstillelse = parseValgfriDato(SettPåVentDomeneBegrep.FRIST, verdier)?.toString(),
                mappeId = parseValgfriString(SettPåVentDomeneBegrep.MAPPE, verdier)?.toLong(),
                prioritet = parseValgfriEnum<OppgavePrioritet>(SettPåVentDomeneBegrep.PRIORITET, verdier),
                beskrivelse = parseValgfriString(SettPåVentDomeneBegrep.BESKRIVELSE, verdier),
                tildeltEnhetsnr = "4489"
            )
    }

    @Gitt("mapper")
    fun gjeldendeMapper(dataTable: DataTable) {
        mapper = dataTable.asMaps().map {
            MappeDto(
                id = parseInt(SettPåVentDomeneBegrep.MAPPE_ID, it),
                navn = parseString(SettPåVentDomeneBegrep.MAPPE_NAVN, it),
                "4489"
            )
        }
    }

    @Gitt("sett på vent request")
    fun settPåVentRequest(dataTable: DataTable) {
        val verdier = dataTable.asMap()
        settOppgavePåVentRequest =
            SettPåVentRequest(
                oppgaveId = 123,
                saksbehandler = parseValgfriString(SettPåVentDomeneBegrep.SAKSBEHANDLER, verdier).orEmpty(),
                frist = parseDato(SettPåVentDomeneBegrep.FRIST, verdier).toString(),
                mappe = parseValgfriString(SettPåVentDomeneBegrep.MAPPE, verdier)?.toLong(),
                prioritet = parseEnum(SettPåVentDomeneBegrep.PRIORITET, verdier),
                beskrivelse = parseValgfriString(SettPåVentDomeneBegrep.BESKRIVELSE, verdier).orEmpty()
            )
    }

    @Når("vi setter behandling på vent")
    fun settBehandlingPåVent() {

        mockkObject(SikkerhetContext)
        mockkObject(DatoUtil)
        every { DatoUtil.dagensDatoMedTid() } returns LocalDateTime.of(2020, 10, 25, 13, 34)

        every { SikkerhetContext.hentSaksbehandler() } returns "bob"
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { behandlingService.oppdaterStatusPåBehandling(any(), any()) } returns behandling
        every { oppgaveService.hentOppgave(any()) } returns eksisterendeOppgave
        every { behandlingshistorikkService.opprettHistorikkInnslag(any(), any(), any(), any()) } just Runs
        every { oppgaveService.oppdaterOppgave(capture(oppgaveSlot)) } just Runs
        every { taskService.save(any()) } returns mockk()
        every { oppgaveService.finnMapper("4489") } returns mapper

        påVentService.settPåVent(behandling.id, settOppgavePåVentRequest)
        unmockkObject(SikkerhetContext)
        unmockkObject(DatoUtil)
    }

    @Så("forventer vi følgende beskrivelse på oppgaven")
    fun forventOppgavebeskrivelse(beskrivelse: String) {
        assertThat(oppgaveSlot.captured.beskrivelse).isEqualTo(beskrivelse)

    }

    @Så("forventer vi at oppgaven er oppdatert med")
    fun forventOppdatertOppgave(dataTable: DataTable) {
        val verdier = dataTable.asMap()

        assertThat(oppgaveSlot.captured.tilordnetRessurs).isEqualTo(parseValgfriString(SettPåVentDomeneBegrep.SAKSBEHANDLER, verdier).orEmpty())
        assertThat(oppgaveSlot.captured.fristFerdigstillelse).isEqualTo(parseValgfriDato(SettPåVentDomeneBegrep.FRIST, verdier)?.toString())
        assertThat(oppgaveSlot.captured.mappeId).isEqualTo(parseValgfriString(SettPåVentDomeneBegrep.MAPPE, verdier)?.toLong())
        assertThat(oppgaveSlot.captured.prioritet).isEqualTo(parseValgfriEnum<OppgavePrioritet>(SettPåVentDomeneBegrep.PRIORITET, verdier))
    }

    enum class SettPåVentDomeneBegrep(val nøkkel: String) : Domenenøkkel {
        MAPPE_ID("Mappeid"),
        MAPPE_NAVN("Mappenavn"),

        // SettPåVentRequest
        SAKSBEHANDLER("saksbehandler"),
        FRIST("frist"),
        MAPPE("mappe"),
        PRIORITET("prioritet"),
        BESKRIVELSE("beskrivelse");

        override fun nøkkel(): String {
            return nøkkel
        }
    }
}
