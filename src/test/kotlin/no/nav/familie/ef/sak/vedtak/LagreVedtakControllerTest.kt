package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.vedtak.domain.AktivitetstypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeMedBeløp
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class LagreVedtakControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var vedtakService: VedtakService

    @Autowired
    private lateinit var barnRepository: BarnRepository

    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal innvilge vedtak for barnetilsyn `() {
        val (behandling, barn) = opprettFagsakOgBehandlingMedBarn()
        val utgiftsperiode = lagUtgiftsperioder(barn)
        val vedtakDto =
            InnvilgelseBarnetilsyn(
                begrunnelse = "",
                perioder = listOf(utgiftsperiode),
                perioderKontantstøtte = listOf(),
                tilleggsstønad = tomTillegsstønad(),
            )

        val respons: ResponseEntity<Ressurs<UUID>> = fullførVedtak(behandling.id, vedtakDto)
        val vedtak =
            Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.INNVILGE,
                avslåBegrunnelse = null,
                barnetilsyn =
                    BarnetilsynWrapper(
                        perioder =
                            listOf(
                                Barnetilsynperiode(
                                    periode = utgiftsperiode.periode,
                                    utgifter = utgiftsperiode.utgifter,
                                    barn = utgiftsperiode.barn,
                                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                                    aktivitet = AktivitetstypeBarnetilsyn.I_ARBEID,
                                ),
                            ),
                        begrunnelse = "",
                    ),
                kontantstøtte = KontantstøtteWrapper(emptyList()),
                tilleggsstønad = TilleggsstønadWrapper(null, emptyList(), null),
                saksbehandlerIdent = "julenissen",
                opprettetAv = "julenissen",
            )

        val vedtakRespons: ResponseEntity<Ressurs<InnvilgelseBarnetilsyn?>> = hentVedtak(behandling.id)

        Assertions
            .assertThat(vedtakService.hentVedtak(respons.body?.data!!))
            .usingRecursiveComparison()
            .ignoringFields("opprettetTid")
            .isEqualTo(vedtak)
        Assertions.assertThat(vedtakRespons.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(vedtakRespons.body?.data).isNotNull
        Assertions.assertThat(vedtakRespons.body?.data?.resultatType).isEqualTo(ResultatType.INNVILGE)
        Assertions.assertThat(vedtakRespons.body?.data?._type).isEqualTo("InnvilgelseBarnetilsyn")
    }

    @Test
    internal fun `Skal innvilge uten utbetaling når kontantstøtte overstiger utgifter `() {
        val (behandling, barn) = opprettFagsakOgBehandlingMedBarn()
        val utgiftsperiode = lagUtgiftsperioder(barn)
        val kontantstøttePeriode = lagKontantstøttePeriode(3000)
        val vedtakDto =
            InnvilgelseBarnetilsyn(
                begrunnelse = "",
                perioder = listOf(utgiftsperiode),
                perioderKontantstøtte = listOf(kontantstøttePeriode),
                tilleggsstønad = tomTillegsstønad(),
                _type = "InnvilgelseBarnetilsynUtenUtbetaling",
            )

        val respons: ResponseEntity<Ressurs<UUID>> = fullførVedtak(behandling.id, vedtakDto)
        val vedtak =
            Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.INNVILGE_UTEN_UTBETALING,
                avslåBegrunnelse = null,
                barnetilsyn =
                    BarnetilsynWrapper(
                        perioder =
                            listOf(
                                Barnetilsynperiode(
                                    periode = utgiftsperiode.periode,
                                    utgifter = utgiftsperiode.utgifter,
                                    barn = utgiftsperiode.barn,
                                    periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                                    aktivitet = AktivitetstypeBarnetilsyn.I_ARBEID,
                                ),
                            ),
                        begrunnelse = "",
                    ),
                kontantstøtte =
                    KontantstøtteWrapper(
                        listOf(
                            PeriodeMedBeløp(
                                periode = kontantstøttePeriode.periode,
                                beløp = kontantstøttePeriode.beløp,
                            ),
                        ),
                    ),
                tilleggsstønad = TilleggsstønadWrapper(null, emptyList(), null),
                saksbehandlerIdent = "julenissen",
                opprettetAv = "julenissen",
            )

        val vedtakRespons: ResponseEntity<Ressurs<InnvilgelseBarnetilsyn?>> = hentVedtak(behandling.id)

        Assertions
            .assertThat(vedtakService.hentVedtak(respons.body?.data!!))
            .usingRecursiveComparison()
            .ignoringFields("opprettetTid")
            .isEqualTo(vedtak)
        Assertions.assertThat(vedtakRespons.statusCode).isEqualTo(HttpStatus.OK)
        Assertions.assertThat(vedtakRespons.body?.data).isNotNull
        Assertions.assertThat(vedtakRespons.body?.data?.resultatType).isEqualTo(ResultatType.INNVILGE_UTEN_UTBETALING)
        Assertions.assertThat(vedtakRespons.body?.data?._type).isEqualTo("InnvilgelseBarnetilsynUtenUtbetaling")
    }

    @Test
    internal fun `Skal feile hvis vi innvilger uten utbetaling når kontantstøtte ikke overstiger utgifter`() {
        val (behandling, barn) = opprettFagsakOgBehandlingMedBarn()
        val utgiftsperiode = lagUtgiftsperioder(barn)
        val kontantstøttePeriode = lagKontantstøttePeriode(0)
        val vedtakDto =
            InnvilgelseBarnetilsyn(
                begrunnelse = "",
                perioder = listOf(utgiftsperiode),
                perioderKontantstøtte = listOf(kontantstøttePeriode),
                tilleggsstønad = tomTillegsstønad(),
                _type = "InnvilgelseBarnetilsynUtenUtbetaling",
            )

        val respons: ResponseEntity<Ressurs<UUID>> = fullførVedtak(behandling.id, vedtakDto)

        Assertions.assertThat(respons.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        Assertions.assertThat(respons.body?.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
        Assertions.assertThat(respons.body?.data).isNull()
    }

    @Test
    internal fun `Skal feile hvis vi innvilger når kontantstøtte overstiger utgifter`() {
        val (behandling, barn) = opprettFagsakOgBehandlingMedBarn()
        val utgiftsperiode = lagUtgiftsperioder(barn)
        val kontantstøttePeriode = lagKontantstøttePeriode(3000)
        val vedtakDto =
            InnvilgelseBarnetilsyn(
                begrunnelse = "",
                perioder = listOf(utgiftsperiode),
                perioderKontantstøtte = listOf(kontantstøttePeriode),
                tilleggsstønad = tomTillegsstønad(),
                _type = "InnvilgelseBarnetilsyn",
            )

        val respons: ResponseEntity<Ressurs<UUID>> = fullførVedtak(behandling.id, vedtakDto)

        Assertions.assertThat(respons.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        Assertions.assertThat(respons.body?.status).isEqualTo(Ressurs.Status.FUNKSJONELL_FEIL)
        Assertions.assertThat(respons.body?.data).isNull()
    }

    private fun tomTillegsstønad() =
        TilleggsstønadDto(
            perioder = listOf(),
            begrunnelse = null,
        )

    private fun lagKontantstøttePeriode(beløp: Int): PeriodeMedBeløpDto =
        PeriodeMedBeløpDto(
            årMånedFra = YearMonth.of(2022, 1),
            årMånedTil = YearMonth.of(2022, 3),
            periode = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 3)),
            beløp = beløp,
        )

    private fun lagUtgiftsperioder(barn: BehandlingBarn): UtgiftsperiodeDto {
        val utgiftsperiode =
            UtgiftsperiodeDto(
                årMånedFra = YearMonth.of(2022, 1),
                årMånedTil = YearMonth.of(2022, 3),
                periode = Månedsperiode(YearMonth.of(2022, 1), YearMonth.of(2022, 3)),
                barn = listOf(barn.id),
                utgifter = 2500,
                periodetype = PeriodetypeBarnetilsyn.ORDINÆR,
                aktivitetstype = AktivitetstypeBarnetilsyn.I_ARBEID,
                sanksjonsårsak = null,
            )
        return utgiftsperiode
    }

    private fun opprettFagsakOgBehandlingMedBarn(): Pair<Behandling, BehandlingBarn> {
        val fagsak =
            testoppsettService.lagreFagsak(
                fagsak(
                    stønadstype = StønadType.BARNETILSYN,
                    identer = setOf(PersonIdent("")),
                ),
            )
        val behandling =
            behandlingRepository.insert(
                behandling(
                    fagsak,
                    steg = StegType.BEREGNE_YTELSE,
                    type = BehandlingType.FØRSTEGANGSBEHANDLING,
                    status = BehandlingStatus.UTREDES,
                ),
            )

        val barn =
            barnRepository.insert(
                behandlingBarn(
                    UUID.randomUUID(),
                    behandling.id,
                    UUID.randomUUID(),
                    "01012212345",
                    "Junior",
                    LocalDate.now(),
                ),
            )
        return Pair(behandling, barn)
    }

    private fun fullførVedtak(
        id: UUID,
        vedtakDto: VedtakDto,
    ): ResponseEntity<Ressurs<UUID>> =
        restTemplate.exchange(
            localhost("/api/vedtak/$id/lagre-vedtak"),
            HttpMethod.POST,
            HttpEntity(vedtakDto, headers),
        )

    private fun hentVedtak(id: UUID): ResponseEntity<Ressurs<InnvilgelseBarnetilsyn?>> =
        restTemplate.exchange(
            localhost("/api/vedtak/$id"),
            HttpMethod.GET,
            HttpEntity<Ressurs<InnvilgelseBarnetilsyn?>>(headers),
        )
}
