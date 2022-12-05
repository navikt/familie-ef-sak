package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.Sanksjonsårsak
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

internal class PatchSanksjonControllerTest : OppslagSpringRunnerTest() {

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var patchSanksjonController: PatchSanksjonController

    @Test
    internal fun `skal oppdatere sanksjoner`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak))
        val vedtaksperiode = Vedtaksperiode(
            Månedsperiode(YearMonth.now()),
            AktivitetType.IKKE_AKTIVITETSPLIKT,
            VedtaksperiodeType.SANKSJON,
            sanksjonsårsak = null
        )
        val sanksjonsårsak = Sanksjonsårsak.SAGT_OPP_STILLING

        vedtakRepository.insert(
            Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.SANKSJONERE,
                sanksjonsårsak = sanksjonsårsak,
                perioder = PeriodeWrapper(listOf(vedtaksperiode))
            )
        )

        patchSanksjonController.oppdater(false)

        val vedtak = vedtakRepository.findByIdOrThrow(behandling.id)
        assertThat(vedtak.perioder?.perioder?.single()?.sanksjonsårsak).isEqualTo(sanksjonsårsak)
    }
}