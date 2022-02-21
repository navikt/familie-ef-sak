package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.AvslagÅrsak
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Avslå
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Ressurs
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BeregningControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var vedtakService: VedtakService
    @Autowired private lateinit var vilkårsvurderingService: VurderingService
    @Autowired private lateinit var søknadService: SøknadService
    @Autowired private lateinit var grunnlagsdataService: GrunnlagsdataService
    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository


    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal klare å inserte ett vedtak med resultatet avslå`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VEDTA_BLANKETT,
                                                                type = BehandlingType.BLANKETT,
                                                                status = BehandlingStatus.UTREDES))
        val vedtakDto = Avslå(avslåBegrunnelse = "avslår vedtaket", avslåÅrsak = AvslagÅrsak.VILKÅR_IKKE_OPPFYLT)
        val vedtak = Vedtak(behandlingId = behandling.id, avslåBegrunnelse = "avslår vedtaket", avslåÅrsak = AvslagÅrsak.VILKÅR_IKKE_OPPFYLT, resultatType = ResultatType.AVSLÅ)
        val respons: ResponseEntity<Ressurs<UUID>> = fatteVedtak(behandling.id, vedtakDto)


        assertThat(vedtakService.hentVedtak(respons.body.data!!)).isEqualTo(vedtak)
    }

    @Test
    internal fun `Skal klare å inserte ett vedtak med resultatet innvilge`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.VEDTA_BLANKETT,
                                                                type = BehandlingType.BLANKETT,
                                                                status = BehandlingStatus.UTREDES))
        val vedtakDto = Innvilget(resultatType = ResultatType.INNVILGE,
                                  periodeBegrunnelse = "periode begrunnelse",
                                  inntektBegrunnelse = "inntekt begrunnelse")

        val respons: ResponseEntity<Ressurs<UUID>> = fatteVedtak(behandling.id, vedtakDto)
        val vedtak = Vedtak(behandlingId = behandling.id,
                            periodeBegrunnelse = "periode begrunnelse",
                            inntektBegrunnelse = "inntekt begrunnelse",
                            resultatType = ResultatType.INNVILGE,
                            inntekter = InntektWrapper(emptyList()),
                            avslåBegrunnelse = null,
                            perioder = PeriodeWrapper(emptyList()))

        assertThat(vedtakService.hentVedtak(respons.body.data!!)).isEqualTo(vedtak)
    }

    @Test
    internal fun `Skal returnere riktig feilmelding i response når fullfør ikke er mulig pga valideringsfeil`() {
        val (_, behandling) = lagFagsakOgBehandling()

        val vedtakDto = Innvilget(resultatType = ResultatType.INNVILGE,
                                  periodeBegrunnelse = "periode begrunnelse",
                                  inntektBegrunnelse = "inntekt begrunnelse")

        vilkårsvurderingService.hentEllerOpprettVurderinger(behandlingId = behandling.id) // ingen ok.

        val respons: ResponseEntity<Ressurs<UUID>> = fullførVedtak(behandling.id, vedtakDto)

        assertThat(respons.body.frontendFeilmelding).isEqualTo("Kan ikke fullføre en behandling med resultat innvilget hvis ikke alle vilkår er oppfylt")

    }

    @Test
    internal fun `Skal kun hente beløpsperioder for det relevante vedtaket`() {
        val (fagsak, behandling) = lagFagsakOgBehandling(StegType.BEHANDLING_FERDIGSTILT)
        val revurdering = lagRevurdering(stegType = StegType.BESLUTTE_VEDTAK, fagsak)

        val responsFørstegangsbehandling: ResponseEntity<Ressurs<List<Beløpsperiode>>> = hentBeløpsperioderForBehandling(behandling.id)
        val beløpsperioderFørstegangsbehandling = responsFørstegangsbehandling.body.data
        assertThat(beløpsperioderFørstegangsbehandling).hasSize(1)
        assertThat(beløpsperioderFørstegangsbehandling?.first()?.periode?.fradato).isEqualTo(LocalDate.of(2022, 1, 1))
        assertThat(beløpsperioderFørstegangsbehandling?.first()?.periode?.tildato).isEqualTo(LocalDate.of(2022, 4, 30))
        assertThat(beløpsperioderFørstegangsbehandling?.first()?.beløp).isEqualTo(BigDecimal(10_000))

        val responsRevurdering: ResponseEntity<Ressurs<List<Beløpsperiode>>> = hentBeløpsperioderForBehandling(revurdering.id)
        val beløpsperioderRevurdering = responsRevurdering.body.data
        assertThat(beløpsperioderRevurdering).hasSize(1)
        assertThat(beløpsperioderRevurdering?.first()?.periode?.fradato).isEqualTo(LocalDate.of(2022, 3, 1))
        assertThat(beløpsperioderRevurdering?.first()?.periode?.tildato).isEqualTo(LocalDate.of(2022, 6, 30))
        assertThat(beløpsperioderRevurdering?.first()?.beløp).isEqualTo(BigDecimal(12_000))
    }

    private fun lagFagsakOgBehandling(stegType: StegType = StegType.VEDTA_BLANKETT): Pair<Fagsak,Behandling> {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("12345678910"))))
        val førstegangsbehandling = behandlingRepository.insert(behandling(fagsak,
                                                                           steg = stegType,
                                                                           type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                           status = BehandlingStatus.FERDIGSTILT))

        val søknad = SøknadMedVedlegg(Testsøknad.søknadOvergangsstønad, emptyList())
        val tilkjentYtelse = lagTilkjentYtelse(behandlingId = førstegangsbehandling.id,
                                               andelerTilkjentYtelse = listOf(lagAndelTilkjentYtelse(
                                                       fraOgMed = LocalDate.of(2022,
                                                                               1,
                                                                               1),
                                                       kildeBehandlingId = førstegangsbehandling.id,
                                                       beløp = 10_000,
                                                       tilOgMed = LocalDate.of(2022,
                                                                               4,
                                                                               30),
                                               )))
        val vedtakDto = Innvilget(resultatType = ResultatType.INNVILGE,
                                  periodeBegrunnelse = "periode begrunnelse",
                                  inntektBegrunnelse = "inntekt begrunnelse",
                                  perioder = listOf(VedtaksperiodeDto(årMånedFra = YearMonth.of(2022, 1),
                                                                      årMånedTil = YearMonth.of(2022, 4),
                                                                      aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                                                                      periodeType = VedtaksperiodeType.HOVEDPERIODE)),
                                  inntekter = emptyList())



        søknadService.lagreSøknadForOvergangsstønad(søknad.søknad, førstegangsbehandling.id, fagsak.id, "1234")
        tilkjentYtelseRepository.insert(tilkjentYtelse)
        vedtakService.lagreVedtak(vedtakDto, førstegangsbehandling.id)
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandling.id)

        return Pair(fagsak, førstegangsbehandling)
    }

    private fun lagRevurdering(stegType: StegType = StegType.BESLUTTE_VEDTAK, fagsak: Fagsak): Behandling {
        val revurdering = behandlingRepository.insert(behandling(fagsak,
                                                                 steg = stegType,
                                                                 type = BehandlingType.REVURDERING,
                                                                 status = BehandlingStatus.UTREDES))
        val tilkjentYtelse = lagTilkjentYtelse(behandlingId = revurdering.id, andelerTilkjentYtelse = listOf(
                lagAndelTilkjentYtelse(fraOgMed = LocalDate.of(2022,
                                                               1,
                                                               1),
                                       beløp = 10_000,
                                       kildeBehandlingId = revurdering.id,
                                       tilOgMed = LocalDate.of(2022,
                                                               2, 28)),
                lagAndelTilkjentYtelse(fraOgMed = LocalDate.of(2022,
                                                               3,
                                                               1),
                                       beløp = 12_000,
                                       kildeBehandlingId = revurdering.id,
                                       tilOgMed = LocalDate.of(2022,
                                                               6, 30))))

        val vedtakDto = Innvilget(resultatType = ResultatType.INNVILGE,
                                  periodeBegrunnelse = "periode begrunnelse",
                                  inntektBegrunnelse = "inntekt begrunnelse",
                                  perioder = listOf(VedtaksperiodeDto(årMånedFra = YearMonth.of(2022, 3),
                                                                      årMånedTil = YearMonth.of(2022, 6),
                                                                      aktivitet = AktivitetType.BARN_UNDER_ETT_ÅR,
                                                                      periodeType = VedtaksperiodeType.HOVEDPERIODE)),
                                  inntekter = emptyList())
        tilkjentYtelseRepository.insert(tilkjentYtelse)
        vedtakService.lagreVedtak(vedtakDto, revurdering.id)
        return revurdering
    }

    private fun fatteVedtak(id: UUID, vedtakDto: VedtakDto): ResponseEntity<Ressurs<UUID>> {
        return restTemplate.exchange(localhost("/api/beregning/$id/lagre-blankettvedtak"),
                                     HttpMethod.POST,
                                     HttpEntity(vedtakDto, headers))
    }


    private fun fullførVedtak(id: UUID, vedtakDto: VedtakDto): ResponseEntity<Ressurs<UUID>> {
        return restTemplate.exchange(localhost("/api/beregning/$id/fullfor"),
                                     HttpMethod.POST,
                                     HttpEntity(vedtakDto, headers))
    }

    private fun hentBeløpsperioderForBehandling(id: UUID): ResponseEntity<Ressurs<List<Beløpsperiode>>> {
        return restTemplate.exchange(localhost("/api/beregning/$id"),
                                     HttpMethod.GET,
                                     HttpEntity<Ressurs<List<Beløpsperiode>>>(headers))
    }


}