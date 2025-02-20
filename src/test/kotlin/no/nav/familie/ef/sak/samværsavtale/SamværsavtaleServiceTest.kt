package no.nav.familie.ef.sak.samværsavtale

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FATTER_VEDTAK
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.FERDIGSTILT
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.IVERKSETTER_VEDTAK
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus.SATT_PÅ_VENT
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.samværsavtale
import no.nav.familie.ef.sak.samværsavtale.dto.tilDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mock
import org.springframework.http.HttpStatus
import java.util.UUID

internal class SamværsavtaleServiceTest {
    private val samværsavtaleRepository: SamværsavtaleRepository = mockk()
    private val behandlingService: BehandlingService = mockk()
    private val tilordnetRessursService: TilordnetRessursService = mockk()
    private val barnService: BarnService = mockk()


    private val samværsavtaleService: SamværsavtaleService = SamværsavtaleService(
        samværsavtaleRepository = samværsavtaleRepository,
        behandlingService = behandlingService,
        tilordnetRessursService = tilordnetRessursService,
        barnService = barnService,
    )

    @ParameterizedTest
    @EnumSource(
        value = BehandlingStatus::class,
        names = ["FATTER_VEDTAK", "IVERKSETTER_VEDTAK", "FERDIGSTILT", "SATT_PÅ_VENT"],
        mode = EnumSource.Mode.INCLUDE,
    )
    internal fun `skal ikke kunne redigere samværsavtale dersom tilhørende behandling ikke er redigerbar`(behandlingStatus: BehandlingStatus) {
        val behandlingId = UUID.randomUUID()
        val samværsavtale = samværsavtale(behandlingId = behandlingId).tilDto()

        every { behandlingService.hentBehandling(behandlingId) } returns behandling(status = behandlingStatus)
        every { barnService.finnBarnPåBehandling(behandlingId) } returns listOf(behandlingBarn(behandlingId = behandlingId))

        val feil: ApiFeil = assertThrows { samværsavtaleService.opprettEllerErstattSamværsavtale(samværsavtale) }

        assertThat(feil.message).isEqualTo("Behandlingen er låst for videre redigering")
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
    }


}