package no.nav.familie.ef.sak.behandlingsflyt.task

import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.BehandlingOppsettUtil
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.Properties

class StartBehandlingTaskTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var fagsakService: FagsakService

    private val iverksettClient = mockk<IverksettClient>()
    private val pdlClient = mockk<PdlClient>()

    private lateinit var startBehandlingTask: StartBehandlingTask

    private val opprettStartBehandlingHendelseDtoSlot = slot<OpprettStartBehandlingHendelseDto>()
    private val personIdent = "123456789012"
    private val fagsak = fagsak(identer = fagsakpersoner(setOf(personIdent)))

    @BeforeEach
    fun setup() {
        testoppsettService.lagreFagsak(fagsak)

        every { pdlClient.hentPersonidenter(personIdent, true) } returns PdlIdenter(listOf(PdlIdent(personIdent, false)))
        justRun { iverksettClient.startBehandling(capture(opprettStartBehandlingHendelseDtoSlot)) }

        startBehandlingTask = StartBehandlingTask(iverksettClient, pdlClient, fagsakService, behandlingRepository)
    }

    @Test
    fun `skal sende StartBehandling da det ikke finnes tidligere iverksatt sak på person`() {
        val behandling = behandling(fagsak)
        behandlingRepository.insert(behandling)

        startBehandlingTask.doTask(Task("forrigeTask", behandling.id.toString(), Properties()))

        assertThat(opprettStartBehandlingHendelseDtoSlot.isCaptured).isEqualTo(true)
        assertThat(opprettStartBehandlingHendelseDtoSlot.captured.type).isEqualTo(StønadType.OVERGANGSSTØNAD)
        assertThat(opprettStartBehandlingHendelseDtoSlot.captured.personIdenter).isEqualTo(setOf(personIdent))
    }

    @Test
    fun `skal ikke sende StartBehandling da det finnes tidligere iverksatt sak på person`() {
        val behandling = BehandlingOppsettUtil.iverksattFørstegangsbehandling.copy(fagsakId = fagsak.id)
        behandlingRepository.insert(behandling)

        startBehandlingTask.doTask(Task("forrigeTask", behandling.id.toString(), Properties()))

        assertThat(opprettStartBehandlingHendelseDtoSlot.isCaptured).isEqualTo(false)
    }

}
