package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling.revurdering

import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.beregning.Inntektsperiode
import no.nav.familie.ef.sak.beregning.OmregningService
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.testutil.TestoppsettService
import no.nav.familie.ef.sak.testutil.mockTestMedGrunnbeløpFra2025
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkårsreglerForStønad
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.YearMonth
import java.util.UUID

@Service
class GOmregningTestUtil {
    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var vedtakRepository: VedtakRepository

    @Autowired
    private lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    private lateinit var iverksettClient: IverksettClient

    @Autowired
    lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    private lateinit var omregningService: OmregningService

    @Autowired
    lateinit var søknadService: SøknadService

    @Autowired
    lateinit var testoppsettService: TestoppsettService

    fun gOmregne(
        behandlingId: UUID,
        fagsakId: UUID,
        fom: YearMonth = YearMonth.of(2024, 6),
        beløp: Int = 23023,
    ) {
        val månedsperiode = Månedsperiode(fom, YearMonth.of(fom.year + 1, 12))
        val inntektsperiode = inntektsperiode(månedsperiode = månedsperiode, BigDecimal(beløp))
        lagSøknadOgVilkårOgVedtak(behandlingId, fagsakId, inntektsperiode)
        val tilkjentYtelse = lagreTilkjentYtelseForInntektsperiode(behandlingId, inntektsperiode)

        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(1)
        assertThat(inntektsperiode.totalinntekt().toInt()).isEqualTo(beløp * 12)

        mockTestMedGrunnbeløpFra2025 {
            omregningService.utførGOmregning(fagsakId)
        }
    }

    fun lagSøknadOgVilkårOgVedtak(
        behandlingId: UUID,
        fagsakId: UUID,
        inntekt: Inntektsperiode,
    ) {
        val (fagsak, behandling) = opprettFagsakOgAvsluttetBehandling(fagsakId, behandlingId)
        grunnlagsdataService.opprettGrunnlagsdata(behandlingId)
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")

        val vilkårsvurderinger = lagVilkårsvurderinger(behandlingId)
        vilkårsvurderingRepository.insertAll(vilkårsvurderinger)

        val vedtak = vedtak(InntektWrapper(listOf(inntekt)), behandlingId)
        vedtakRepository.insert(vedtak)
    }

    private fun lagreBarnPåBehandling(behandlingId: UUID) =
        barnRepository.insert(
            behandlingBarn(
                behandlingId = behandlingId,
                personIdent = "01012067050",
                navn = "Kid Kiddesen",
            ),
        )

    private fun lagreTilkjentYtelseForInntektsperiode(
        behandlingId: UUID,
        inntektsperiode: Inntektsperiode,
    ): TilkjentYtelse {
        val tilkjentYtelse =
            tilkjentYtelse(
                behandlingId = behandlingId,
                inntektsperiode = inntektsperiode,
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse)
        return tilkjentYtelse
    }

    fun finnAndelEtterNyGDato(iverksettDto: IverksettOvergangsstønadDto) =
        iverksettDto.vedtak.tilkjentYtelse?.andelerTilkjentYtelse?.firstOrNull {
            it.periode.inneholder(
                Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed.plusMonths(1),
            )
        }

    fun finnInntektsperiodeEtterNyGDato(
        behandlingId: UUID,
        grunnbeløpsår: Int,
    ): Inntektsperiode {
        val behandlingNy = behandlingRepository.findByIdOrThrow(behandlingId)
        val vedtakNy = vedtakRepository.findByIdOrThrow(behandlingNy.id)
        return vedtakNy.inntekter?.inntekter!!.first { it.periode.inneholder(YearMonth.of(grunnbeløpsår, 6)) }
    }

    private fun opprettFagsakOgAvsluttetBehandling(
        fagsakId: UUID,
        behandlingId: UUID,
    ): Pair<Fagsak, Behandling> {
        val fagsak = testoppsettService.lagreFagsak(fagsak(id = fagsakId, identer = setOf(PersonIdent("321"))))
        val behandling =
            behandlingRepository.insert(
                behandling(
                    id = behandlingId,
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
        return Pair(fagsak, behandling)
    }

    fun lagVilkårsvurderinger(
        behandlingId: UUID,
        barn: BehandlingBarn = lagreBarnPåBehandling(behandlingId),
    ): List<Vilkårsvurdering> {
        val vilkårsvurderinger =
            vilkårsreglerForStønad(StønadType.OVERGANGSSTØNAD).map { vilkårsregel ->
                val delvilkårsvurdering =
                    vilkårsregel.initiereDelvilkårsvurdering(
                        HovedregelMetadata(
                            sivilstandSøknad = null,
                            sivilstandstype = Sivilstandstype.UGIFT,
                            erMigrering = false,
                            barn = listOf(barn),
                            søktOmBarnetilsyn = emptyList(),
                            langAvstandTilSøker = listOf(),
                            vilkårgrunnlagDto = VilkårTestUtil.mockVilkårGrunnlagDto(),
                            behandling = behandling(),
                        ),
                    )
                Vilkårsvurdering(
                    behandlingId = behandlingId,
                    resultat = Vilkårsresultat.OPPFYLT,
                    type = vilkårsregel.vilkårType,
                    barnId = if (vilkårsregel.vilkårType == VilkårType.ALENEOMSORG) barn.id else null,
                    delvilkårsvurdering =
                        DelvilkårsvurderingWrapper(
                            delvilkårsvurdering.map {
                                it.copy(
                                    resultat = Vilkårsresultat.OPPFYLT,
                                    vurderinger =
                                        it.vurderinger.map { vurdering ->
                                            vurdering.copy(begrunnelse = "Godkjent")
                                        },
                                )
                            },
                        ),
                    opphavsvilkår = null,
                )
            }
        return vilkårsvurderinger
    }
}
