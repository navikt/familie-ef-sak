package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.dto.SettPåVentRequest
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatus
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.oppgave.OppgaveSubtype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.oppgave.OppgavePrioritet
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

internal class BehandlingPåVentServiceIntegrationTest : OppslagSpringRunnerTest() {
    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var behandlingPåVentService: BehandlingPåVentService

    @Autowired
    lateinit var revurderingService: RevurderingService

    @Autowired
    lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    val fagsak = fagsak()
    val behandling = behandling(fagsak).ferdigstill()
    val saksbehandling = saksbehandling(fagsak, behandling)
    lateinit var vilkårsvurdering: Vilkårsvurdering

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext()
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vilkårsvurdering = vilkårsvurderingRepository.insert(vilkårsvurdering(behandling.id))
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `revurdering med behandling på vent skal peke til førstegangsbehandling`() {
        val settPåVentRequest = settPåVentRequest(1, emptyList())
        val behandling2 = opprettRevurdering()
        behandlingPåVentService.settPåVent(behandling2.id, settPåVentRequest)
        val behandling3 = opprettRevurdering()

        assertThat(behandling2.forrigeBehandlingId).isEqualTo(behandling.id)
        assertThat(behandling3.forrigeBehandlingId).isEqualTo(behandling.id)
        val vilkårForBehandling3 = vilkårsvurderingRepository.findByBehandlingId(behandling3.id)
        val gjenbruktVilkår = vilkårForBehandling3.find { it.type == vilkårsvurdering.type }
        assertThat(gjenbruktVilkår?.opphavsvilkår?.behandlingId).isEqualTo(behandling.id)
        assertThat(gjenbruktVilkår?.opphavsvilkår?.vurderingstidspunkt).isEqualTo(vilkårsvurdering.sporbar.endret.endretTid)
    }

    @Test
    internal fun `behandling2 settes på vent og det gjøres et vedtak på behandling3 i mellomtiden`() {
        val settPåVentRequest = settPåVentRequest(1, emptyList())
        val behandling2 = opprettRevurdering()
        behandlingPåVentService.settPåVent(behandling2.id, settPåVentRequest)
        val behandling3 = opprettRevurdering()

        assertKanTaAvVent(behandling2, TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES)

        behandlingRepository.update(behandling3.ferdigstill())
        assertKanTaAvVent(behandling2, TaAvVentStatus.MÅ_NULSTILLE_VEDTAK)

        behandlingPåVentService.taAvVent(behandling2.id)
        assertThat(behandlingRepository.findByIdOrThrow(behandling2.id).forrigeBehandlingId)
            .`as`("Behandling 2 sin forrigeBehandlingId skal pekes om til behandling3 sin id")
            .isEqualTo(behandling3.id)

        assertSisteIverksatteBehandling(behandling3)

        behandlingRepository.update(behandling2.ferdigstill())
        assertSisteIverksatteBehandling(behandling2)

        assertThat(opprettRevurdering().forrigeBehandlingId)
            .`as`("Ny revurdering skal peke til behandling2 som er den sist iverksatte behandlingen")
            .isEqualTo(behandling2.id)
    }

    private fun assertSisteIverksatteBehandling(behandling: Behandling) {
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)?.id).isEqualTo(behandling.id)
    }

    private fun assertKanTaAvVent(
        behandling: Behandling,
        taAvVentStatus: TaAvVentStatus,
    ) {
        val kanTaAvVent = behandlingPåVentService.kanTaAvVent(behandling.id)
        assertThat(kanTaAvVent.status).isEqualTo(taAvVentStatus)
    }

    private fun Behandling.ferdigstill() =
        this.copy(
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.INNVILGET,
            vedtakstidspunkt = LocalDateTime.now(),
        )

    private fun opprettRevurdering() =
        revurderingService.opprettRevurderingManuelt(
            RevurderingDto(
                fagsak.id,
                BehandlingÅrsak.NYE_OPPLYSNINGER,
                kravMottatt = LocalDate.now(),
                vilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE,
            ),
        )

    private fun settPåVentRequest(
        oppgaveId: Long,
        oppfølgingsoppgaver: List<OppgaveSubtype>,
    ) = SettPåVentRequest(
        oppgaveId = oppgaveId,
        saksbehandler = "ny saksbehandler",
        prioritet = OppgavePrioritet.HOY,
        frist = LocalDate.of(2002, Month.MARCH, 24).toString(),
        mappe = 102,
        beskrivelse = "Her er litt tekst fra saksbehandler",
        oppgaveVersjon = 1,
        oppfølgingsoppgaverMotLokalKontor = oppfølgingsoppgaver,
        innstillingsoppgaveBeskjed = "",
    )
}
