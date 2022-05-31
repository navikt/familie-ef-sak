package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.avslagDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseBarnetilsynDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseOvergangsstønadDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.innvilgelseSkolepengerDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.opphørDto
import no.nav.familie.ef.sak.vedtak.VedtakDtoUtil.sanksjonertDto
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vedtak.erVedtakAktivtForDato
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
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
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak,
                steg = StegType.VILKÅR,
                status = BehandlingStatus.UTREDES,
                type = BehandlingType.BLANKETT
            )
        )

        val tomBegrunnelse = ""
        val vedtakRequest = InnvilgelseOvergangsstønad(
            tomBegrunnelse,
            tomBegrunnelse, emptyList(), emptyList()
        )

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
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak,
                steg = StegType.VILKÅR,
                status = BehandlingStatus.UTREDES,
                type = BehandlingType.BLANKETT
            )
        )

        val tomBegrunnelse = ""
        val vedtakDto = InnvilgelseOvergangsstønad(tomBegrunnelse, tomBegrunnelse, emptyList(), emptyList())

        vedtakService.lagreVedtak(vedtakDto, behandling.id, fagsak.stønadstype)

        assertThat(vedtakService.hentVedtakHvisEksisterer(behandling.id)).usingRecursiveComparison().isEqualTo(vedtakDto)
    }

    @Test
    internal fun `skal oppdatere saksbehandler på vedtaket`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak,
                steg = StegType.VILKÅR,
                status = BehandlingStatus.UTREDES,
                type = BehandlingType.BLANKETT
            )
        )

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
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak,
                steg = StegType.VILKÅR,
                status = BehandlingStatus.UTREDES,
                type = BehandlingType.BLANKETT
            )
        )

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
        val vedtakDto = InnvilgelseOvergangsstønad(
            periodeBegrunnelse = "",
            inntektBegrunnelse = "tomBegrunnelse",
            perioder = emptyList(),
            inntekter = emptyList()
        )
        vedtakService.lagreVedtak(vedtakDto, behandling, fagsak.stønadstype)
        vedtakService.lagreVedtak(vedtakDto, behandling2, fagsak.stønadstype)

        assertThat(vedtakService.hentVedtakForBehandlinger(setOf(behandling, behandling2))).hasSize(2)
    }

    @Test
    internal fun `hentForventetInntektForVedtakOgDato - filtrer vekk vedtak som slutter en måned frem i tid`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak())
        val behandling = behandlingRepository.insert(behandling(fagsak, status = BehandlingStatus.FERDIGSTILT))
        val vedtak = vedtakRepository.insert(vedtak(behandling.id))
        val behandlingIds = listOf(behandling.id)
        val behandlingIdToForventetInntektMap = vedtakService.hentForventetInntektForBehandlingIds(behandlingIds)

        assertThat(behandlingIdToForventetInntektMap[behandling.id]).isNull()
    }

    @Test
    internal fun `hentForventetInntektForVedtakOgDato - finn riktig forventet inntekt`() {
        val forrigeMåned = YearMonth.now().minusMonths(1)
        val behandlingIdInnenforPeriode = insertVedtakMedPeriode(forrigeMåned.atDay(1), YearMonth.now().atEndOfMonth())
        val behandlingIdMedInntektForrigeMåned = insertVedtakMedPeriode(forrigeMåned.atDay(1), forrigeMåned.atEndOfMonth())

        val behandlingIdToForventetInntektMap = vedtakService.hentForventetInntektForBehandlingIds(listOf(behandlingIdInnenforPeriode, behandlingIdMedInntektForrigeMåned))

        assertThat(behandlingIdToForventetInntektMap[behandlingIdInnenforPeriode]).isEqualTo(500_000)
        assertThat(behandlingIdToForventetInntektMap[behandlingIdMedInntektForrigeMåned]).isNull()
    }

    @Test
    internal fun `er vedtak aktivt`() {
        // Vedtak som varer fra 1.1.2021 - 31.12-2021
        assertThat(vedtak(UUID.randomUUID()).erVedtakAktivtForDato(LocalDate.of(2021, 6, 1))).isTrue
        assertThat(vedtak(UUID.randomUUID()).erVedtakAktivtForDato(LocalDate.of(2020, 12, 31))).isFalse
        assertThat(vedtak(UUID.randomUUID()).erVedtakAktivtForDato(LocalDate.of(2022, 1, 1))).isFalse
    }

    private fun insertVedtakMedPeriode(fraOgMedDato: LocalDate, tilOgMedDato: LocalDate): UUID {
        val fagsakInnenforPeriode = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(UUID.randomUUID().toString()))))
        val behandlingIdMedInntektInnenforPeriode = behandlingRepository.insert(behandling(fagsakInnenforPeriode, status = BehandlingStatus.FERDIGSTILT)).id

        val inntektsperiodeTilOgMedDenneMåneden =
            Inntektsperiode(fraOgMedDato, tilOgMedDato, BigDecimal(500_000), BigDecimal.ZERO)
        val vedtaksperiodeTilOgMedDenneMåneden = Vedtaksperiode(
            fraOgMedDato,
            tilOgMedDato,
            AktivitetType.BARN_UNDER_ETT_ÅR,
            VedtaksperiodeType.HOVEDPERIODE
        )
        vedtakRepository.insert(
            vedtak(
                behandlingIdMedInntektInnenforPeriode,
                inntekter = InntektWrapper(listOf(inntektsperiodeTilOgMedDenneMåneden)),
                perioder = PeriodeWrapper(
                    listOf(vedtaksperiodeTilOgMedDenneMåneden)
                )
            )
        )
        return behandlingIdMedInntektInnenforPeriode
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
        internal fun `innvilgelse skolepenger`() {
            val behandling = opprettBehandling()
            val vedtak = innvilgelseSkolepengerDto()

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

        private fun assertInnsendtVedtakErLikHentetVedtak(
            vedtak: VedtakDto,
            behandling: Saksbehandling
        ) {
            vedtakService.lagreVedtak(vedtak, behandling.id, behandling.stønadstype)
            val hentetVedtak = vedtakService.hentVedtak(behandling.id).tilVedtakDto()
            assertThat(vedtak).isEqualTo(hentetVedtak)
        }
    }
}
