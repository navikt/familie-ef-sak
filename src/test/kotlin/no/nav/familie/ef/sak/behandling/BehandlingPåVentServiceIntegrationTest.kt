package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.dto.RevurderingDto
import no.nav.familie.ef.sak.behandling.dto.TaAvVentStatus
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.clearBrukerContext
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.mockBrukerContext
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

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

    @BeforeEach
    internal fun setUp() {
        mockBrukerContext()
        testoppsettService.lagreFagsak(fagsak)
        behandlingRepository.insert(behandling)
        grunnlagsdataService.opprettGrunnlagsdata(behandling.id)
        vilkårsvurderingRepository.insert(vilkårsvurdering(behandling.id))
    }

    @AfterEach
    internal fun tearDown() {
        clearBrukerContext()
    }

    @Test
    internal fun `behandling settes på vent og det gjøres et vedtak på en annen behandling i mellomtiden`() {
        val behandling2 = opprettRevurdering()
        behandlingPåVentService.settPåVent(behandling2.id)
        val behandling3 = opprettRevurdering()

        assertThat(behandling2.forrigeBehandlingId).isEqualTo(behandling.id)
        assertThat(behandling3.forrigeBehandlingId).isEqualTo(behandling.id)

        assertKanTaAvVent(behandling2, TaAvVentStatus.ANNEN_BEHANDLING_MÅ_FERDIGSTILLES)

        behandlingRepository.update(behandling3.ferdigstill())
        assertKanTaAvVent(behandling2, TaAvVentStatus.MÅ_NULSTILLE_VEDTAK)

        behandlingPåVentService.taAvVent(behandling2.id)
        val nyForrigeBehandlingId = behandlingRepository.findByIdOrThrow(behandling2.id).forrigeBehandlingId
        assertThat(nyForrigeBehandlingId).isEqualTo(behandling3.id)

        assertSisteIverksatteBehandling(behandling3)

        behandlingRepository.update(behandling2.ferdigstill())
        assertSisteIverksatteBehandling(behandling2)

        val revurdering = opprettRevurdering()
        assertThat(revurdering.forrigeBehandlingId).isEqualTo(behandling2.id)
    }

    private fun assertSisteIverksatteBehandling(behandling: Behandling) {
        assertThat(behandlingRepository.finnSisteIverksatteBehandling(fagsak.id)?.id).isEqualTo(behandling.id)
    }

    private fun assertKanTaAvVent(behandling: Behandling, taAvVentStatus: TaAvVentStatus) {
        val kanTaAvVent = behandlingPåVentService.kanTaAvVent(behandling.id)
        assertThat(kanTaAvVent.status).isEqualTo(taAvVentStatus)
    }

    private fun Behandling.ferdigstill() =
        this.copy(
            status = BehandlingStatus.FERDIGSTILT,
            resultat = BehandlingResultat.INNVILGET,
            vedtakstidspunkt = LocalDateTime.now()
        )

    private fun opprettRevurdering() =
        revurderingService.opprettRevurderingManuelt(
            RevurderingDto(
                fagsak.id,
                BehandlingÅrsak.NYE_OPPLYSNINGER,
                kravMottatt = LocalDate.now()
            )
        )
}