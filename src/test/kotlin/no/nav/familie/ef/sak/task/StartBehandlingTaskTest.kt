package no.nav.familie.ef.sak.no.nav.familie.ef.sak.task

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakPerson
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.task.StartBehandlingTask
import no.nav.familie.ef.sak.util.BehandlingOppsettUtil
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.infotrygd.OpprettStartBehandlingHendelseDto
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.*

class StartBehandlingTaskTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository : FagsakRepository

    val iverksettClient = mockk<IverksettClient>()
    val pdlClient = mockk<PdlClient>()
    val fagsakService = mockk<FagsakService>()

    val opprettStartBehandlingHendelseDtoSlot = slot<OpprettStartBehandlingHendelseDto>()
    val personIdent = "123456789012"
    val fagsak = fagsak(identer = fagsakpersoner(setOf(personIdent)) )

    @BeforeEach
    fun setup() {
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { pdlClient.hentPersonidenter(personIdent, true)} returns PdlIdenter(listOf(PdlIdent(personIdent, false)))
        every { iverksettClient.startBehandling(capture(opprettStartBehandlingHendelseDtoSlot)) } just Runs
    }

    @Test
    fun `skal sende StartBehandling da det ikke finnes tidligere iverksatt sak på person`() {
        val startBehandlingTask = StartBehandlingTask(iverksettClient, pdlClient, fagsakService, behandlingRepository)
        startBehandlingTask.doTask(Task("forrigeTask", fagsak.id.toString(), Properties()))

        Assertions.assertThat(opprettStartBehandlingHendelseDtoSlot.isCaptured).isEqualTo(true)
        Assertions.assertThat(opprettStartBehandlingHendelseDtoSlot.captured.type).isEqualTo(StønadType.OVERGANGSSTØNAD)
        Assertions.assertThat(opprettStartBehandlingHendelseDtoSlot.captured.personIdenter).isEqualTo(setOf(personIdent))
    }

    @Test
    fun `skal ikke sende StartBehandling da det finnes tidligere iverksatt sak på person`() {
        val førstegangsbehandling = BehandlingOppsettUtil.iverksattFørstegangsbehandling
        fagsakRepository.insert(fagsak(setOf(FagsakPerson(personIdent))).copy(id = førstegangsbehandling.fagsakId))

        val behandlinger = BehandlingOppsettUtil.lagBehandlingerForSisteIverksatte()
        behandlingRepository.insertAll(behandlinger)

        val startBehandlingTask = StartBehandlingTask(iverksettClient, pdlClient, fagsakService, behandlingRepository)
        startBehandlingTask.doTask(Task("forrigeTask", fagsak.id.toString(), Properties()))

        Assertions.assertThat(opprettStartBehandlingHendelseDtoSlot.isCaptured).isEqualTo(false)
    }


}