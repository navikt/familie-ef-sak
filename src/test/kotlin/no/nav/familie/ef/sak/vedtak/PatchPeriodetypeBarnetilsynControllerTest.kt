package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.Periodetype
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.Month
import java.util.UUID

internal class PatchPeriodetypeBarnetilsynControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var patchPeriodetypeBarnetilsynController: PatchPeriodetypeBarnetilsynController

    @Test
    internal fun `skal oppdatere sanksjoner`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))

        val barn1 = UUID.randomUUID()
        val barn2 = UUID.randomUUID()

        val barnetilsynPerioder: List<Barnetilsynperiode> = listOf(
            Barnetilsynperiode(
                datoFra = LocalDate.of(2023, Month.JANUARY, 1),
                datoTil = LocalDate.of(2023, Month.JANUARY, 31),
                utgifter = 5000,
                barn = listOf(barn1, barn2),
            ),
            Barnetilsynperiode(
                datoFra = LocalDate.of(2023, Month.FEBRUARY, 1),
                datoTil = LocalDate.of(2023, Month.FEBRUARY, 28),
                utgifter = 5000,
                barn = listOf(barn2),
                erMidlertidigOpphør = true
            ),
            Barnetilsynperiode(
                datoFra = LocalDate.of(2023, Month.MARCH, 1),
                datoTil = LocalDate.of(2023, Month.MARCH, 31),
                utgifter = 7000,
                barn = listOf(barn1),
            )
        )
        val vedtakBarnetilsyn = BarnetilsynWrapper(perioder = barnetilsynPerioder, begrunnelse = "begrunnelse")
        val vedtak = Vedtak(
            behandlingId = behandling.id,
            resultatType = ResultatType.INNVILGE,
            barnetilsyn = vedtakBarnetilsyn
        )

        vedtakRepository.insert(vedtak)

        patchPeriodetypeBarnetilsynController.patchPeriodetyperBarnetilsyn(false)

        val patchetVedtak = vedtakRepository.findByIdOrThrow(behandling.id)
        assertThat(patchetVedtak.barnetilsyn?.perioder?.get(0)).isEqualTo(
            Barnetilsynperiode(
                datoFra = LocalDate.of(2023, Month.JANUARY, 1),
                datoTil = LocalDate.of(2023, Month.JANUARY, 31),
                utgifter = 5000,
                barn = listOf(barn1, barn2),
                periodetype = Periodetype.ORDINÆR
            )
        )
        assertThat(patchetVedtak.barnetilsyn?.perioder?.get(1)).isEqualTo(
            Barnetilsynperiode(
                datoFra = LocalDate.of(2023, Month.FEBRUARY, 1),
                datoTil = LocalDate.of(2023, Month.FEBRUARY, 28),
                utgifter = 5000,
                barn = listOf(barn2),
                erMidlertidigOpphør = true,
                periodetype = Periodetype.OPPHØR
            )
        )
        assertThat(patchetVedtak.barnetilsyn?.perioder?.get(2)).isEqualTo(
            Barnetilsynperiode(
                datoFra = LocalDate.of(2023, Month.MARCH, 1),
                datoTil = LocalDate.of(2023, Month.MARCH, 31),
                utgifter = 7000,
                barn = listOf(barn1),
                periodetype = Periodetype.ORDINÆR
            )
        )
    }
}
