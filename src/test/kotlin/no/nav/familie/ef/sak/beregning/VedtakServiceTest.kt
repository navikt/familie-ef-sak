package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.avslagDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseBarnetilsynDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseOvergangsstønadDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.opphørDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.sanksjonertDto
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.util.UUID

internal class VedtakServiceTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var vedtakService: VedtakService
    @Autowired private lateinit var vedtakRepository: VedtakRepository

    @Autowired private lateinit var fagsakRepository: FagsakRepository

    @Test
    fun `lagre og hent vedtak, lagre igjen - da skal første slettes`() {

        /** Pre */
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VILKÅR,
                                                                status = BehandlingStatus.UTREDES,
                                                                type = BehandlingType.BLANKETT))

        val tomBegrunnelse = ""
        val vedtakRequest = InnvilgelseOvergangsstønad(tomBegrunnelse,
                                                       tomBegrunnelse, emptyList(), emptyList())

        /** Skal ikke gjøre noe når den ikke er opprettet **/
        vedtakService.slettVedtakHvisFinnes(behandling.id)

        /** Opprett */
        vedtakService.lagreVedtak(vedtakRequest, behandling.id, fagsak.stønadstype)

        /** Verifiser opprettet */
        val vedtakLagret = vedtakRepository.findByIdOrNull(behandling.id)
        assertThat(vedtakLagret?.resultatType).isEqualTo(ResultatType.INNVILGE)
        assertThat(vedtakLagret?.periodeBegrunnelse).isEqualTo(tomBegrunnelse)

        /** Slett og opprett ny **/
        val vedtakRequestMedPeriodeBegrunnelse =
                InnvilgelseOvergangsstønad("Begrunnelse", tomBegrunnelse, emptyList(), emptyList())
        vedtakService.slettVedtakHvisFinnes(behandling.id)
        assertThat(vedtakRepository.findAll()).isEmpty()
        vedtakService.lagreVedtak(vedtakRequestMedPeriodeBegrunnelse, behandling.id, fagsak.stønadstype)

        /** Verifiser nytt **/
        val nyttVedtakLagret = vedtakRepository.findByIdOrNull(behandling.id)
        assertThat(nyttVedtakLagret?.resultatType).isEqualTo(ResultatType.INNVILGE)
        assertThat(nyttVedtakLagret?.periodeBegrunnelse).isEqualTo(vedtakRequestMedPeriodeBegrunnelse.periodeBegrunnelse)

    }

    @Test
    fun `skal hente lagret vedtak hvis finnes`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VILKÅR,
                                                                status = BehandlingStatus.UTREDES,
                                                                type = BehandlingType.BLANKETT))

        val tomBegrunnelse = ""
        val vedtakDto = InnvilgelseOvergangsstønad(tomBegrunnelse, tomBegrunnelse, emptyList(), emptyList())

        vedtakService.lagreVedtak(vedtakDto, behandling.id, fagsak.stønadstype)

        assertThat(vedtakService.hentVedtakHvisEksisterer(behandling.id)).usingRecursiveComparison().isEqualTo(vedtakDto)
    }

    @Test
    internal fun `skal oppdatere saksbehandler på vedtaket`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VILKÅR,
                                                                status = BehandlingStatus.UTREDES,
                                                                type = BehandlingType.BLANKETT))

        val tomBegrunnelse = ""
        val vedtakDto = InnvilgelseOvergangsstønad(tomBegrunnelse, tomBegrunnelse, emptyList(), emptyList())

        vedtakService.lagreVedtak(vedtakDto, behandling.id, fagsak.stønadstype)
        val saksbehandlerIdent = "S123456"
        vedtakService.oppdaterSaksbehandler(behandlingId = behandling.id, saksbehandlerIdent = saksbehandlerIdent)
        assertThat(vedtakService.hentVedtak(behandling.id).saksbehandlerIdent).isEqualTo(saksbehandlerIdent)
    }

    @Test
    internal fun `skal oppdatere beslutter på vedtaket`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VILKÅR,
                                                                status = BehandlingStatus.UTREDES,
                                                                type = BehandlingType.BLANKETT))

        val tomBegrunnelse = ""
        val vedtakDto = InnvilgelseOvergangsstønad(tomBegrunnelse, tomBegrunnelse, emptyList(), emptyList())

        vedtakService.lagreVedtak(vedtakDto, behandling.id, fagsak.stønadstype)
        val beslutterIdent = "B123456"
        vedtakService.oppdaterBeslutter(behandlingId = behandling.id, beslutterIdent = beslutterIdent)
        assertThat(vedtakService.hentVedtak(behandling.id).beslutterIdent).isEqualTo(beslutterIdent)
    }

    @Test
    internal fun `hentVedtakForBehandlinger - skal kaste feil hvis vedtak ikke finnes`() {
        assertThatThrownBy { vedtakService.hentVedtakForBehandlinger(setOf(UUID.randomUUID())) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("Finner ikke Vedtak for")
    }

    @Test
    internal fun `hentVedtakForBehandlinger - skal returnere vedtak`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT)).id
        val behandling2 = behandlingRepository.insert(behandling(fagsak)).id
        val vedtakDto = InnvilgelseOvergangsstønad(periodeBegrunnelse = "",
                                                   inntektBegrunnelse = "tomBegrunnelse",
                                                   perioder = emptyList(),
                                                   inntekter = emptyList())
        vedtakService.lagreVedtak(vedtakDto, behandling, fagsak.stønadstype)
        vedtakService.lagreVedtak(vedtakDto, behandling2, fagsak.stønadstype)

        assertThat(vedtakService.hentVedtakForBehandlinger(setOf(behandling, behandling2))).hasSize(2)
    }

    @Nested
    inner class DtoTilDomeneTilDto {

        @Test
        internal fun `innvilgelse overgangsstønad`() {
            val behandling = opprettBehandling()
            val vedtak = innvilgelseOvergangsstønadDto()

            assertInnsendtVedtakErLikHentetVedtak(vedtak, behandling)
        }

        @Test
        internal fun `innvilgelse barnetilsyn`() {
            val behandling = opprettBehandling()
            val vedtak = innvilgelseBarnetilsynDto()

            assertInnsendtVedtakErLikHentetVedtak(vedtak, behandling)
        }

        @Test
        internal fun `avslag`() {
            val behandling = opprettBehandling()
            val vedtak = avslagDto()

            assertInnsendtVedtakErLikHentetVedtak(vedtak, behandling)
        }

        @Test
        internal fun `opphør`() {
            val behandling = opprettBehandling()
            val vedtak = opphørDto()

            assertInnsendtVedtakErLikHentetVedtak(vedtak, behandling)
        }

        @Test
        internal fun `sanksjonering`() {
            val behandling = opprettBehandling()
            val vedtak = sanksjonertDto()

            assertInnsendtVedtakErLikHentetVedtak(vedtak, behandling)
        }

        @Test
        internal fun `innvilgelse barnetilsyn og innvilgelse overgangsstønad er ikke lik`() {
            assertThat(innvilgelseBarnetilsynDto())
                    .isNotEqualTo(innvilgelseOvergangsstønadDto())
        }

        private fun opprettBehandling(): Saksbehandling {
            val fagsak = testoppsettService.lagreFagsak(fagsak())
            val behandlingId = behandlingRepository.insert(behandling(fagsak)).id
            return behandlingRepository.finnSaksbehandling(behandlingId)
        }

        private fun assertInnsendtVedtakErLikHentetVedtak(vedtak: VedtakDto,
                                                          behandling: Saksbehandling) {
            vedtakService.lagreVedtak(vedtak, behandling.id, behandling.stønadstype)
            val hentetVedtak = vedtakService.hentVedtak(behandling.id).tilVedtakDto()
            assertThat(vedtak).isEqualTo(hentetVedtak)
        }
    }

}