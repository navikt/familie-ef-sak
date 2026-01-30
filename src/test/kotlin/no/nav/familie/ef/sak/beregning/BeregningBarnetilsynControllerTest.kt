package no.nav.familie.ef.sak.beregning

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeløpsperiodeBarnetilsynDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.søknad.SøknadMedVedlegg
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class BeregningBarnetilsynControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var vilkårsvurderingService: VurderingService

    @Autowired
    private lateinit var søknadService: SøknadService

    @Autowired
    private lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal kun hente beløpsperioder for det relevante vedtaket`() {
        val (fagsak, behandling) = lagFagsakOgBehandling(StegType.BEHANDLING_FERDIGSTILT)
        val revurdering = lagRevurdering(stegType = StegType.BESLUTTE_VEDTAK, fagsak, behandling.id)

        val responsFørstegangsbehandling: ResponseEntity<Ressurs<List<BeløpsperiodeBarnetilsynDto>>> =
            hentBeløpsperioderForBehandling(behandling.id)
        val beløpsperioderFørstegangsbehandling = responsFørstegangsbehandling.body?.data
        Assertions.assertThat(beløpsperioderFørstegangsbehandling).hasSize(1)
        Assertions.assertThat(beløpsperioderFørstegangsbehandling?.first()?.periode?.fom).isEqualTo(YearMonth.of(2022, 1))
        Assertions.assertThat(beløpsperioderFørstegangsbehandling?.first()?.periode?.tom).isEqualTo(YearMonth.of(2022, 4))
        Assertions.assertThat(beløpsperioderFørstegangsbehandling?.first()?.beløp).isEqualTo(2000)

        val responsRevurdering: ResponseEntity<Ressurs<List<BeløpsperiodeBarnetilsynDto>>> =
            hentBeløpsperioderForBehandling(revurdering.id)
        val beløpsperioderRevurdering = responsRevurdering.body?.data
        Assertions.assertThat(beløpsperioderRevurdering).hasSize(1)
        Assertions.assertThat(beløpsperioderRevurdering?.first()?.periode?.fomDato).isEqualTo(LocalDate.of(2022, 3, 1))
        Assertions.assertThat(beløpsperioderRevurdering?.first()?.periode?.tomDato).isEqualTo(LocalDate.of(2022, 6, 30))
        Assertions.assertThat(beløpsperioderRevurdering?.first()?.beløp).isEqualTo(3000)
    }

    private fun lagFagsakOgBehandling(stegType: StegType = StegType.BESLUTTE_VEDTAK): Pair<Fagsak, Behandling> {
        val fagsak =
            testoppsettService.lagreFagsak(
                fagsak(
                    stønadstype = StønadType.BARNETILSYN,
                    identer = setOf(PersonIdent("12345678910")),
                ),
            )
        val førstegangsbehandling =
            behandlingRepository.insert(
                behandling(
                    fagsak,
                    steg = stegType,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
        val barn =
            barnRepository.insert(
                behandlingBarn(
                    UUID.randomUUID(),
                    førstegangsbehandling.id,
                    UUID.randomUUID(),
                    "01012212345",
                    "Junior",
                    LocalDate.now(),
                ),
            )

        val søknad = SøknadMedVedlegg(Testsøknad.søknadBarnetilsyn, emptyList())
        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandlingId = førstegangsbehandling.id,
                andelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelse(
                            fraOgMed = LocalDate.of(2022, 1, 1),
                            kildeBehandlingId = førstegangsbehandling.id,
                            beløp = 2000,
                            tilOgMed = LocalDate.of(2022, 4, 30),
                        ),
                    ),
            )
        val utgiftsperiode =
            UtgiftsperiodeDto(
                årMånedFra = YearMonth.of(2022, 1),
                årMånedTil = YearMonth.of(2022, 4),
                periode = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 4)),
                barn = listOf(barn.id),
                utgifter = 2500,
                periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID,
                sanksjonsårsak = null,
            )

        val vedtakDto =
            InnvilgelseBarnetilsyn(
                begrunnelse = "",
                perioder = listOf(utgiftsperiode),
                perioderKontantstøtte = listOf(),
                kontantstøtteBegrunnelse = null,
                tilleggsstønad =
                    TilleggsstønadDto(
                        perioder = listOf(),
                        begrunnelse = null,
                    ),
            )

        søknadService.lagreSøknadForBarnetilsyn(søknad.søknad, førstegangsbehandling.id, fagsak.id, "1234")
        tilkjentYtelseRepository.insert(tilkjentYtelse)
        vedtakService.lagreVedtak(vedtakDto, førstegangsbehandling.id, fagsak.stønadstype)
        grunnlagsdataService.opprettGrunnlagsdata(førstegangsbehandling.id)

        return Pair(fagsak, førstegangsbehandling)
    }

    private fun lagRevurdering(
        stegType: StegType = StegType.BESLUTTE_VEDTAK,
        fagsak: Fagsak,
        forrigeBehandlingId: UUID,
    ): Behandling {
        val revurdering =
            behandlingRepository.insert(
                behandling(
                    fagsak,
                    steg = stegType,
                    type = BehandlingType.REVURDERING,
                    status = BehandlingStatus.UTREDES,
                ),
            )
        val tilkjentYtelse =
            lagTilkjentYtelse(
                behandlingId = revurdering.id,
                andelerTilkjentYtelse =
                    listOf(
                        lagAndelTilkjentYtelse(
                            fraOgMed = LocalDate.of(2022, 1, 1),
                            beløp = 2000,
                            kildeBehandlingId = revurdering.id,
                            tilOgMed = LocalDate.of(2022, 2, 28),
                        ),
                        lagAndelTilkjentYtelse(
                            fraOgMed = LocalDate.of(2022, 3, 1),
                            beløp = 3000,
                            kildeBehandlingId = revurdering.id,
                            tilOgMed = LocalDate.of(2022, 6, 30),
                        ),
                    ),
            )

        val barn =
            barnRepository
                .findByBehandlingId(forrigeBehandlingId)
                .map { it.copy(behandlingId = revurdering.id, id = UUID.randomUUID()) }
        barnRepository.insertAll(barn)
        val utgiftsperiode =
            UtgiftsperiodeDto(
                årMånedFra = YearMonth.of(2022, 3),
                årMånedTil = YearMonth.of(2022, 6),
                periode = Månedsperiode(YearMonth.of(2022, 3), YearMonth.of(2022, 6)),
                barn = barn.map { it.id },
                utgifter = 3000,
                periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID,
                sanksjonsårsak = null,
            )

        val vedtakDto =
            InnvilgelseBarnetilsyn(
                begrunnelse = "",
                perioder = listOf(utgiftsperiode),
                perioderKontantstøtte = listOf(),
                kontantstøtteBegrunnelse = null,
                tilleggsstønad =
                    TilleggsstønadDto(
                        perioder = listOf(),
                        begrunnelse = null,
                    ),
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse)
        vedtakService.lagreVedtak(vedtakDto, revurdering.id, fagsak.stønadstype)
        return revurdering
    }

    private fun fullførVedtak(
        id: UUID,
        vedtakDto: VedtakDto,
    ): ResponseEntity<Ressurs<UUID>> =
        testRestTemplate.exchange(
            localhost("/api/vedtak/$id/lagre-vedtak"),
            HttpMethod.POST,
            HttpEntity(vedtakDto, headers),
        )

    private fun hentBeløpsperioderForBehandling(id: UUID): ResponseEntity<Ressurs<List<BeløpsperiodeBarnetilsynDto>>> =
        testRestTemplate.exchange(
            localhost("/api/beregning/barnetilsyn/$id"),
            HttpMethod.GET,
            HttpEntity<Ressurs<List<BeløpsperiodeBarnetilsynDto>>>(headers),
        )
}
