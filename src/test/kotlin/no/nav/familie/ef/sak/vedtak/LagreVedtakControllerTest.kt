package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Barnetilsynperiode
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.TilleggsstønadDto
import no.nav.familie.ef.sak.vedtak.dto.UtgiftsperiodeDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.kontrakter.felles.Ressurs
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.client.exchange
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class LagreVedtakControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var behandlingRepository: BehandlingRepository
    @Autowired private lateinit var vedtakService: VedtakService
    @Autowired private lateinit var vilkårsvurderingService: VurderingService
    @Autowired private lateinit var søknadService: SøknadService
    @Autowired private lateinit var grunnlagsdataService: GrunnlagsdataService
    @Autowired private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository
    @Autowired private lateinit var barnRepository: BarnRepository


    @BeforeEach
    fun setUp() {
        headers.setBearerAuth(lokalTestToken)
    }

    @Test
    internal fun `Skal innvilge vedtak for barnetilsyn `() {
        val fagsak =
                testoppsettService.lagreFagsak(fagsak(stønadstype = StønadType.BARNETILSYN, identer = setOf(PersonIdent(""))))
        val behandling = behandlingRepository.insert(behandling(fagsak,
                                                                steg = StegType.BEREGNE_YTELSE,
                                                                type = BehandlingType.FØRSTEGANGSBEHANDLING,
                                                                status = BehandlingStatus.UTREDES))

        val barn = barnRepository.insert(behandlingBarn(UUID.randomUUID(),
                                                        behandling.id,
                                                        UUID.randomUUID(),
                                                        "01012212345",
                                                        "Junior",
                                                        LocalDate.now()))
        val utgiftsperiode = UtgiftsperiodeDto(årMånedFra = YearMonth.of(2022, 1),
                                               årMånedTil = YearMonth.of(2022, 3),
                                               barn = listOf(barn.id),
                                               utgifter = 2500)
        val vedtakDto = InnvilgelseBarnetilsyn(begrunnelse = "",
                                               perioder = listOf(utgiftsperiode),
                                               perioderKontantstøtte = listOf(),
                                               tilleggsstønad = TilleggsstønadDto(harTilleggsstønad = false,
                                                                                  perioder = listOf(),
                                                                                  begrunnelse = null))

        val respons: ResponseEntity<Ressurs<UUID>> = fullførVedtak(behandling.id, vedtakDto)
        val vedtak = Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.INNVILGE,
                avslåBegrunnelse = null,
                barnetilsyn = BarnetilsynWrapper(listOf(Barnetilsynperiode(datoFra = utgiftsperiode.årMånedFra.atDay(1),
                                                                           datoTil = utgiftsperiode.årMånedTil.atEndOfMonth(),
                                                                           utgifter = utgiftsperiode.utgifter,
                                                                           barn = utgiftsperiode.barn)),
                                                 begrunnelse = ""),
                kontantstøtte = KontantstøtteWrapper(emptyList()),
                tilleggsstønad = TilleggsstønadWrapper(false, emptyList(), null),
        )

        Assertions.assertThat(vedtakService.hentVedtak(respons.body.data!!)).isEqualTo(vedtak)
    }

    private fun fullførVedtak(id: UUID, vedtakDto: VedtakDto): ResponseEntity<Ressurs<UUID>> {
        return restTemplate.exchange(localhost("/api/vedtak/$id/lagre-vedtak"),
                                     HttpMethod.POST,
                                     HttpEntity(vedtakDto, headers))
    }
}