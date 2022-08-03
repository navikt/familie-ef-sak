package no.nav.familie.ef.sak.beregning

import com.fasterxml.jackson.module.kotlin.readValue
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vedtaksperiode
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkårsreglerForStønad
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.domene.TaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDateTime
import java.util.UUID

internal class OmregningServiceTest : OppslagSpringRunnerTest() {

    @Autowired
    lateinit var fagsakService: FagsakService

    @Autowired
    lateinit var omregningService: OmregningService

    @Autowired
    lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    lateinit var vedtakRepository: VedtakRepository

    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var iverksettClient: IverksettClient

    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var søknadService: SøknadService

    val personService = mockk<PersonService>()
    val år = nyesteGrunnbeløpGyldigFraOgMed.year

    @BeforeEach
    fun setup() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(listOf(PdlIdent("321", false)))
        clearMocks(iverksettClient, answers = false)
    }

    /**
     * Denne brekker hver gang det kommer nytt G-beløp.
     * Beløp må oppdateres i omregnes i expectedIverksettDto.json.
     */
    @Test
    fun `utførGOmregning kaller iverksettUtenBrev med korrekt iverksettDto `() {
        val fagsakId = UUID.fromString("3549f9e2-ddd1-467d-82be-bfdb6c7f07e1")
        val behandlingId = UUID.fromString("39c7dc82-adc1-43db-a6f9-64b8e4352ff6")
        val fagsak = testoppsettService.lagreFagsak(fagsak(id = fagsakId, identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                id = behandlingId,
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år))
        vedtakRepository.insert(vedtak(behandling.id, år = år))
        val barn = barnRepository.insert(
            behandlingBarn(
                behandlingId = behandling.id,
                personIdent = "01012067050",
                navn = "Kid Kiddesen"
            )
        )
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")

        val vilkårsvurderinger = lagVilkårsvurderinger(barn, behandlingId)
        vilkårsvurderingRepository.insertAll(vilkårsvurderinger)

        omregningService.utførGOmregning(fagsakId)
        val nyBehandling = behandlingRepository.findByFagsakId(fagsakId).single { it.årsak == BehandlingÅrsak.G_OMREGNING }

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNotNull
        val iverksettDtoSlot = slot<IverksettOvergangsstønadDto>()
        verify { iverksettClient.iverksettUtenBrev(capture(iverksettDtoSlot)) }

        val iverksettDto = iverksettDtoSlot.captured
        val expectedIverksettDto = iverksettMedOppdaterteIder(fagsak, behandling, iverksettDto.vedtak.vedtakstidspunkt)
        assertThat(iverksettDto).usingRecursiveComparison()
            .ignoringFields("behandling.vilkårsvurderinger")
            .isEqualTo(expectedIverksettDto)
        assertThat(iverksettDto.behandling.vilkårsvurderinger)
            .hasSameElementsAs(expectedIverksettDto.behandling.vilkårsvurderinger)
        assertThat(søknadService.hentSøknadsgrunnlag(nyBehandling.id)).isNotNull
        assertThat(barnRepository.findByBehandlingId(nyBehandling.id).single().personIdent).isEqualTo(barn.personIdent)
        assertThat(
            vilkårsvurderingRepository.findByBehandlingId(nyBehandling.id).single { it.type == VilkårType.ALENEOMSORG }.barnId
        ).isNotNull
    }

    @Test
    fun `utførGOmregning med samordningsfradrag returner og etterlater seg ingen spor i databasen i live run`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år, samordningsfradrag = 10))
        val inntektsperiode = inntektsperiode(år = år, samordningsfradrag = 100.toBigDecimal())
        vedtakRepository.insert(vedtak(behandling.id, år = år, inntekter = InntektWrapper(listOf(inntektsperiode))))

        omregningService.utførGOmregning(fagsak.id)

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `utførGOmregning med sanksjon returner og etterlater seg ingen spor i databasen i live run`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling = behandlingRepository.insert(
            behandling(
                fagsak = fagsak,
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.FERDIGSTILT
            )
        )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år, samordningsfradrag = 10))
        val inntektsperiode = inntektsperiode(år = år, samordningsfradrag = 100.toBigDecimal())
        val vedtaksperiode = vedtaksperiode(år = år, vedtaksperiodeType = VedtaksperiodeType.SANKSJON)
        vedtakRepository.insert(
            vedtak(
                behandlingId = behandling.id,
                år = år,
                inntekter = InntektWrapper(listOf(inntektsperiode)),
                perioder = PeriodeWrapper(listOf(vedtaksperiode))
            )
        )

        omregningService.utførGOmregning(fagsak.id)

        assertThat(taskRepository.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    private fun lagVilkårsvurderinger(
        barn: BehandlingBarn,
        behandlingId: UUID
    ): List<Vilkårsvurdering> {
        val vilkårsvurderinger = vilkårsreglerForStønad(StønadType.OVERGANGSSTØNAD).map { vilkårsregel ->
            val delvilkårsvurdering = vilkårsregel.initereDelvilkårsvurdering(
                HovedregelMetadata(
                    sivilstandSøknad = null,
                    sivilstandstype = Sivilstandstype.UGIFT,
                    erMigrering = false,
                    barn = listOf(barn),
                    søktOmBarnetilsyn = emptyList()
                )
            )
            Vilkårsvurdering(
                behandlingId = behandlingId,
                resultat = Vilkårsresultat.OPPFYLT,
                type = vilkårsregel.vilkårType,
                barnId = if (vilkårsregel.vilkårType == VilkårType.ALENEOMSORG) barn.id else null,
                delvilkårsvurdering = DelvilkårsvurderingWrapper(
                    delvilkårsvurdering.map {
                        it.copy(
                            resultat = Vilkårsresultat.OPPFYLT,
                            vurderinger = it.vurderinger.map { vurdering ->
                                vurdering.copy(begrunnelse = "Godkjent")
                            }
                        )
                    }
                )
            )
        }
        return vilkårsvurderinger
    }

    fun iverksettMedOppdaterteIder(
        fagsak: Fagsak,
        behandling: Behandling,
        vedtakstidspunkt: LocalDateTime
    ): IverksettOvergangsstønadDto {

        val personidenter = fagsak.personIdenter.map { it.ident }.toSet()
        val forrigeBehandling = fagsakService.finnFagsak(personidenter, StønadType.OVERGANGSSTØNAD)?.let {
            behandlingRepository.findByFagsakId(it.id).maxByOrNull { it.sporbar.opprettetTid }
        } ?: error("Finner ikke tidligere iverksatt behandling")

        val expectedIverksettDto: IverksettOvergangsstønadDto =
            ObjectMapperProvider.objectMapper.readValue(readFile("expectedIverksettDto.json"))

        val andelerTilkjentYtelse = expectedIverksettDto.vedtak.tilkjentYtelse?.andelerTilkjentYtelse?.map {
            if (it.periode.fomDato >= nyesteGrunnbeløpGyldigFraOgMed) {
                it.copy(kildeBehandlingId = forrigeBehandling.id)
            } else {
                it.copy(kildeBehandlingId = behandling.id)
            }
        } ?: emptyList()
        val tilkjentYtelseDto = expectedIverksettDto.vedtak.tilkjentYtelse?.copy(andelerTilkjentYtelse = andelerTilkjentYtelse)
        val vedtak = expectedIverksettDto.vedtak.copy(tilkjentYtelse = tilkjentYtelseDto, vedtakstidspunkt = vedtakstidspunkt)
        val behandlingsdetaljerDto = expectedIverksettDto.behandling.copy(
            behandlingId = forrigeBehandling.id,
            eksternId = forrigeBehandling.eksternId.id
        )
        return expectedIverksettDto.copy(
            vedtak = vedtak,
            behandling = behandlingsdetaljerDto,
            fagsak = expectedIverksettDto.fagsak.copy(eksternId = fagsak.eksternId.id)
        )
    }

    private fun readFile(filnavn: String): String {
        return this::class.java.getResource("/omregning/$filnavn")!!.readText()
    }
}
