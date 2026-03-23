package no.nav.familie.ef.sak.beregning

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil
import no.nav.familie.ef.sak.infrastruktur.config.JsonMapperProvider
import no.nav.familie.ef.sak.infrastruktur.config.RolleConfig
import no.nav.familie.ef.sak.infrastruktur.config.readValue
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling.revurdering.GOmregningTestUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.behandlingBarn
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.inntektsperiode
import no.nav.familie.ef.sak.repository.lagInntekt
import no.nav.familie.ef.sak.repository.tilkjentYtelse
import no.nav.familie.ef.sak.repository.vedtak
import no.nav.familie.ef.sak.repository.vedtaksperiode
import no.nav.familie.ef.sak.testutil.mockTestMedGrunnbeløpFra2022
import no.nav.familie.ef.sak.testutil.mockTestMedGrunnbeløpFra2023
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodeWrapper
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.VilkårsvurderingRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
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
    lateinit var taskService: TaskService

    @Autowired
    lateinit var iverksettClient: IverksettClient

    @Autowired
    lateinit var vilkårsvurderingRepository: VilkårsvurderingRepository

    @Autowired
    lateinit var barnRepository: BarnRepository

    @Autowired
    lateinit var søknadService: SøknadService

    @Autowired
    lateinit var grunnlagsdataService: GrunnlagsdataService

    @Autowired
    private lateinit var rolleConfig: RolleConfig

    @Autowired
    private lateinit var omregningServiceTestUtil: GOmregningTestUtil

    val personService = mockk<PersonService>()
    val år = Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed.year

    @BeforeEach
    fun setup() {
        every { personService.hentPersonIdenter(any()) } returns PdlIdenter(listOf(PdlIdent("321", false)))
        clearMocks(iverksettClient, answers = false)
        BrukerContextUtil.mockBrukerContext("Saksbehandler", groups = listOf(rolleConfig.saksbehandlerRolle))
    }

    @AfterEach
    fun tearDown() {
        BrukerContextUtil.clearBrukerContext()
    }

    val fagsakId = UUID.fromString("3549f9e2-ddd1-467d-82be-bfdb6c7f07e1")
    val behandlingId = UUID.fromString("39c7dc82-adc1-43db-a6f9-64b8e4352ff6")

    @Test
    fun `Verifiser riktig beløp og inntektsjustering`() {
        val inntektsperiode = inntektsperiode(2022, dagsats = BigDecimal(201), månedsinntekt = BigDecimal(2002), inntekt = BigDecimal(200003)) // 201 * 260 + 2002 * 12 + 200003
        lagSøknadOgVilkårOgVedtak(behandlingId, fagsakId, inntektsperiode, stønadsår = 2022)
        val tilkjentYtelse = lagreTilkjentYtelse(behandlingId, stønadsår = 2022)
        val iverksettDtoSlot = slot<IverksettOvergangsstønadDto>()
        // Gitt assert: - skal splittes til to med ny g
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(1)
        assertThat(inntektsperiode.totalinntekt().toInt()).isEqualTo(276287)

        mockTestMedGrunnbeløpFra2022 {
            omregningService.utførGOmregning(fagsakId)

            verify { iverksettClient.iverksettUtenBrev(capture(iverksettDtoSlot)) }
            val iverksettDto = iverksettDtoSlot.captured

            assertThat(
                iverksettDto.vedtak.tilkjentYtelse
                    ?.andelerTilkjentYtelse
                    ?.size,
            ).isEqualTo(2) // skal være splittet
            // Sjekk andel etter ny g omregningsdato
            val andelTilkjentYtelseOmregnet = omregningServiceTestUtil.finnAndelEtterNyGDato(iverksettDto)!!
            assertThat(andelTilkjentYtelseOmregnet.inntekt).isEqualTo(289100) // justert med F
            assertThat(andelTilkjentYtelseOmregnet.beløp).isEqualTo(12151)
            // Sjekk inntektsperiode etter ny G omregning
            val inntektsperiodeEtterGomregning = omregningServiceTestUtil.finnInntektsperiodeEtterNyGDato(iverksettDto.behandling.behandlingId, 2022)

            assertThat(inntektsperiodeEtterGomregning.dagsats?.toInt()).isEqualTo(0)
            assertThat(inntektsperiodeEtterGomregning.månedsinntekt?.toInt()).isEqualTo(0)
            assertThat(inntektsperiodeEtterGomregning.inntekt.toInt()).isEqualTo(289100)
            assertThat(inntektsperiodeEtterGomregning.totalinntekt().toInt()).isEqualTo(289100)
        }
    }

    @Test
    fun `Verifiser stans av gomregning dersom behandlingen er på vent`() {
        val inntektPeriode = lagInntekt(201, 2002, 200003, 2022)
        lagSøknadOgVilkårOgVedtak(behandlingId, fagsakId, inntektPeriode, stønadsår = 2022)

        val behandlingSattPåVent =
            behandling(
                id = UUID.randomUUID(),
                fagsak = fagsakService.hentFagsak(fagsakId),
                resultat = BehandlingResultat.INNVILGET,
                status = BehandlingStatus.SATT_PÅ_VENT,
                type = BehandlingType.REVURDERING,
            )
        behandlingRepository.insert(behandlingSattPåVent)
        grunnlagsdataService.opprettGrunnlagsdata(behandlingSattPåVent.id)

        val tilkjentYtelse = lagreTilkjentYtelse(behandlingId, stønadsår = 2022)
        // Gitt assert: - skal splittes til to med ny g
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(1)
        assertThat(inntektPeriode.totalinntekt().toInt()).isEqualTo(276287)

        mockTestMedGrunnbeløpFra2022 {
            val feil =
                assertThrows<Feil> {
                    omregningService.utførGOmregning(fagsakId)
                }

            assertThat(feil.message).contains("det finnes åpen behandling på fagsak")
        }
    }

    @Test
    fun `Verifiser riktig beløp og inntektsjustering for 2023`() {
        val inntektPeriode = lagInntekt(0, 0, 210_000, 2023)
        lagSøknadOgVilkårOgVedtak(behandlingId, fagsakId, inntektPeriode, stønadsår = 2023)
        val tilkjentYtelse = lagreTilkjentYtelse(behandlingId, stønadsår = 2023)
        val iverksettDtoSlot = slot<IverksettOvergangsstønadDto>()
        // Gitt assert: - skal splittes til to med ny g
        assertThat(tilkjentYtelse.andelerTilkjentYtelse).hasSize(1)
        assertThat(inntektPeriode.totalinntekt().toInt()).isEqualTo(210_000)

        mockTestMedGrunnbeløpFra2023 {
            omregningService.utførGOmregning(fagsakId)

            verify { iverksettClient.iverksettUtenBrev(capture(iverksettDtoSlot)) }
            val iverksettDto = iverksettDtoSlot.captured

            assertThat(
                iverksettDto.vedtak.tilkjentYtelse
                    ?.andelerTilkjentYtelse
                    ?.size,
            ).isEqualTo(2) // skal være splittet
            // Sjekk andel etter ny g omregningsdato
            val andelTilkjentYtelseOmregnet = omregningServiceTestUtil.finnAndelEtterNyGDato(iverksettDto)!!
            assertThat(andelTilkjentYtelseOmregnet.inntekt).isEqualTo(223400)
            assertThat(andelTilkjentYtelseOmregnet.beløp).isEqualTo(16088)
            // Sjekk inntektsperiode etter ny G omregning
            val inntektsperiodeEtterGomregning = omregningServiceTestUtil.finnInntektsperiodeEtterNyGDato(iverksettDto.behandling.behandlingId, 2023)
            assertThat(inntektsperiodeEtterGomregning.dagsats?.toInt()).isEqualTo(0)
            assertThat(inntektsperiodeEtterGomregning.månedsinntekt?.toInt()).isEqualTo(0)
            assertThat(inntektsperiodeEtterGomregning.inntekt.toInt()).isEqualTo(223400)
            assertThat(inntektsperiodeEtterGomregning.totalinntekt().toInt()).isEqualTo(223400)
        }
    }

    @Test
    fun `utførGOmregning kaller iverksettUtenBrev med korrekt iverksettDto `() {
        val fagsakId = UUID.fromString("3549f9e2-ddd1-467d-82be-bfdb6c7f07e1")
        val behandlingId = UUID.fromString("39c7dc82-adc1-43db-a6f9-64b8e4352ff6")
        val fagsak = testoppsettService.lagreFagsak(fagsak(id = fagsakId, identer = setOf(PersonIdent("321"))))
        val behandling =
            behandlingRepository.insert(
                behandling(
                    id = behandlingId,
                    eksternId = 11,
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", 2022))
        val inntekter =
            listOf(inntektsperiode(2022, inntekt = BigDecimal(277_100), samordningsfradrag = BigDecimal.ZERO))
        vedtakRepository.insert(vedtak(behandling.id, år = 2022, inntekter = InntektWrapper(inntekter)))
        val barn =
            barnRepository.insert(
                behandlingBarn(
                    behandlingId = behandling.id,
                    personIdent = "01012067050",
                    navn = "Kid Kiddesen",
                ),
            )
        søknadService.lagreSøknadForOvergangsstønad(
            Testsøknad.søknadOvergangsstønad,
            behandling.id,
            fagsak.id,
            "1L",
        )

        val vilkårsvurderinger = omregningServiceTestUtil.lagVilkårsvurderinger(behandlingId, barn)
        vilkårsvurderingRepository.insertAll(vilkårsvurderinger)

        mockTestMedGrunnbeløpFra2022 {
            omregningService.utførGOmregning(fagsakId)
            val nyBehandling =
                behandlingRepository.findByFagsakId(fagsakId).single { it.årsak == BehandlingÅrsak.G_OMREGNING }

            assertThat(taskService.findAll().find { it.type == "pollerStatusFraIverksett" }).isNotNull
            val iverksettDtoSlot = slot<IverksettOvergangsstønadDto>()
            verify { iverksettClient.iverksettUtenBrev(capture(iverksettDtoSlot)) }

            val iverksettDto = iverksettDtoSlot.captured
            val expectedIverksettDto =
                iverksettMedOppdaterteIder(fagsak, behandling, iverksettDto.vedtak.vedtakstidspunkt)
            assertThat(iverksettDto)
                .usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(expectedIverksettDto)
            assertThat(søknadService.hentSøknadsgrunnlag(nyBehandling.id)).isNotNull
            assertThat(
                barnRepository.findByBehandlingId(nyBehandling.id).single().personIdent,
            ).isEqualTo(barn.personIdent)
            assertThat(
                vilkårsvurderingRepository
                    .findByBehandlingId(nyBehandling.id)
                    .single { it.type == VilkårType.ALENEOMSORG }
                    .barnId,
            ).isNotNull
        }
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

    @Test
    fun `utførGOmregning med samordningsfradrag returner og etterlater seg ingen spor i databasen i live run`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling =
            behandlingRepository.insert(
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år, samordningsfradrag = 10))
        val inntektsperiode = inntektsperiode(år = år, samordningsfradrag = 100.toBigDecimal())
        vedtakRepository.insert(vedtak(behandling.id, år = år, inntekter = InntektWrapper(listOf(inntektsperiode))))

        omregningService.utførGOmregning(fagsak.id)

        assertThat(taskService.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `utførGOmregning med sanksjon returner og etterlater seg ingen spor i databasen i live run`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent("321"))))
        val behandling =
            behandlingRepository.insert(
                behandling(
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse(behandling.id, "321", år, samordningsfradrag = 10))
        val inntektsperiode = inntektsperiode(år = år, samordningsfradrag = 100.toBigDecimal())
        val vedtaksperiode =
            vedtaksperiode(
                startDato = LocalDate.of(år, 1, 1),
                sluttDato = LocalDate.of(år, 1, 31),
                vedtaksperiodeType = VedtaksperiodeType.SANKSJON,
            )
        vedtakRepository.insert(
            vedtak(
                behandlingId = behandling.id,
                år = år,
                inntekter = InntektWrapper(listOf(inntektsperiode)),
                perioder = PeriodeWrapper(listOf(vedtaksperiode)),
            ),
        )

        omregningService.utførGOmregning(fagsak.id)

        assertThat(taskService.findAll().find { it.type == "pollerStatusFraIverksett" }).isNull()
        assertThat(behandlingRepository.findByFagsakId(fagsak.id).size).isEqualTo(1)
        verify(exactly = 0) { iverksettClient.simuler(any()) }
        verify(exactly = 0) { iverksettClient.iverksettUtenBrev(any()) }
    }

    @Test
    fun `utførGOmregning med 0 beløp skal oppdatere grunnbeløpsmåned i tilkjent ytelse`() {
        /*
        2021-08-01 -> 2021-11-30 med beløp 0, inntekt 700 000 og inntektsreduksjon 5543
        2021-12-01 -> 2024-07-31 med beløp 0, inntekt 700 000 og inntektsreduksjon 5543
        tilkjent ytelse grunnbeløpsmåned: 2021-05 skal bli 2022-05
         */
        val fagsak = insertVedtakMed0BeløpSomSkalGOmregnes()

        mockTestMedGrunnbeløpFra2022 { omregningService.utførGOmregning(fagsak.id) }

        val iverksettDtoSlot = slot<IverksettOvergangsstønadDto>()
        verify { iverksettClient.iverksettUtenBrev(capture(iverksettDtoSlot)) }
        val iverksettDto = iverksettDtoSlot.captured
        assertThat(
            iverksettDto.vedtak.tilkjentYtelse
                ?.andelerTilkjentYtelse
                ?.all { it.beløp == 0 },
        ).isTrue
        val oppdatertTilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(iverksettDto.behandling.behandlingId)
        assertThat(oppdatertTilkjentYtelse?.grunnbeløpsmåned).isEqualTo(YearMonth.of(2022, 5))
    }

    private fun insertVedtakMed0BeløpSomSkalGOmregnes(): Fagsak {
        val fagsak = testoppsettService.lagreFagsak(fagsak(id = UUID.randomUUID(), identer = setOf(PersonIdent("123"))))
        val behandling =
            behandlingRepository.insert(
                behandling(
                    id = UUID.randomUUID(),
                    fagsak = fagsak,
                    resultat = BehandlingResultat.INNVILGET,
                    status = BehandlingStatus.FERDIGSTILT,
                ),
            )

        val andelTilkjentYtelse =
            lagAndelTilkjentYtelse(
                beløp = 0,
                fraOgMed = LocalDate.of(2021, 8, 1),
                tilOgMed = LocalDate.of(2021, 11, 30),
                personIdent = "123",
                inntekt = 700000,
                inntektsreduksjon = 5543,
                kildeBehandlingId = behandling.id,
            )

        val andelTilkjentYtelse2 =
            lagAndelTilkjentYtelse(
                beløp = 0,
                fraOgMed = LocalDate.of(2021, 12, 1),
                tilOgMed = LocalDate.of(2024, 7, 31),
                personIdent = "123",
                inntekt = 700000,
                inntektsreduksjon = 5543,
                kildeBehandlingId = behandling.id,
            )

        val tilkjentYtelse =
            lagTilkjentYtelse(
                andelerTilkjentYtelse = listOf(andelTilkjentYtelse, andelTilkjentYtelse2),
                startdato = LocalDate.of(2021, 8, 1),
                grunnbeløpsmåned = YearMonth.of(2021, 5),
                behandlingId = behandling.id,
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse)

        val perioder =
            listOf(
                vedtaksperiode(
                    startDato = LocalDate.of(2021, 8, 1),
                    sluttDato = LocalDate.of(2021, 11, 30),
                    vedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
                    aktivitetstype = AktivitetType.BARN_UNDER_ETT_ÅR,
                ),
                vedtaksperiode(
                    startDato = LocalDate.of(2021, 12, 1),
                    sluttDato = LocalDate.of(2024, 7, 31),
                    vedtaksperiodeType = VedtaksperiodeType.HOVEDPERIODE,
                    aktivitetstype = AktivitetType.FORSØRGER_ER_SYK,
                ),
            )
        val inntekt =
            inntektsperiode(
                startDato = LocalDate.of(2021, 8, 1),
                sluttDato = LocalDate.MAX,
                inntekt = BigDecimal.valueOf(700000),
                samordningsfradrag = BigDecimal.ZERO,
            )

        vedtakRepository.insert(
            vedtak(
                behandling.id,
                år = år,
                inntekter = InntektWrapper(listOf(inntekt)),
                perioder = PeriodeWrapper(perioder),
            ),
        )
        val barn =
            barnRepository.insert(
                behandlingBarn(
                    behandlingId = behandling.id,
                    personIdent = "01012067050",
                    navn = "Kid Kiddesen",
                ),
            )
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")

        val vilkårsvurderinger = omregningServiceTestUtil.lagVilkårsvurderinger(behandling.id, barn)
        vilkårsvurderingRepository.insertAll(vilkårsvurderinger)
        return fagsak
    }

    fun iverksettMedOppdaterteIder(
        fagsak: Fagsak,
        behandling: Behandling,
        vedtakstidspunkt: LocalDateTime,
    ): IverksettOvergangsstønadDto {
        val personidenter = fagsak.personIdenter.map { it.ident }.toSet()
        val forrigeBehandling =
            fagsakService.finnFagsak(personidenter, StønadType.OVERGANGSSTØNAD)?.let {
                behandlingRepository.findByFagsakId(it.id).maxByOrNull { it.sporbar.opprettetTid }
            } ?: error("Finner ikke tidligere iverksatt behandling")

        val expectedIverksettDto: IverksettOvergangsstønadDto =
            JsonMapperProvider.jsonMapper.readValue(readFile("expectedIverksettDto.json"))

        val andelerTilkjentYtelse =
            expectedIverksettDto.vedtak.tilkjentYtelse?.andelerTilkjentYtelse?.map {
                if (it.periode.fomDato >= Grunnbeløpsperioder.nyesteGrunnbeløpGyldigFraOgMed.atDay(1)) {
                    it.copy(kildeBehandlingId = forrigeBehandling.id)
                } else {
                    it.copy(kildeBehandlingId = behandling.id)
                }
            } ?: emptyList()
        val tilkjentYtelseDto =
            expectedIverksettDto.vedtak.tilkjentYtelse?.copy(andelerTilkjentYtelse = andelerTilkjentYtelse)
        val vedtak =
            expectedIverksettDto.vedtak.copy(tilkjentYtelse = tilkjentYtelseDto, vedtakstidspunkt = vedtakstidspunkt)
        val behandlingsdetaljerDto =
            expectedIverksettDto.behandling.copy(
                behandlingId = forrigeBehandling.id,
                eksternId = forrigeBehandling.eksternId,
            )
        return expectedIverksettDto.copy(
            vedtak = vedtak,
            behandling = behandlingsdetaljerDto,
            fagsak = expectedIverksettDto.fagsak.copy(eksternId = fagsak.eksternId),
        )
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/omregning/$filnavn")!!.readText()

    private fun lagreTilkjentYtelse(
        behandlingId: UUID,
        stønadsår: Int,
    ): TilkjentYtelse {
        val tilkjentYtelse =
            tilkjentYtelse(
                behandlingId = behandlingId,
                personIdent = "321",
                stønadsår = stønadsår,
                inntekt = 1,
                beløp = 1,
            )
        tilkjentYtelseRepository.insert(tilkjentYtelse)
        return tilkjentYtelse
    }

    private fun lagSøknadOgVilkårOgVedtak(
        behandlingId: UUID,
        fagsakId: UUID,
        inntekt: Inntektsperiode,
        stønadsår: Int,
    ) {
        val (fagsak, behandling) = opprettFagsakOgAvsluttetBehandling(fagsakId, behandlingId)
        grunnlagsdataService.opprettGrunnlagsdata(behandlingId)
        søknadService.lagreSøknadForOvergangsstønad(Testsøknad.søknadOvergangsstønad, behandling.id, fagsak.id, "1L")

        val vilkårsvurderinger = omregningServiceTestUtil.lagVilkårsvurderinger(behandlingId)
        vilkårsvurderingRepository.insertAll(vilkårsvurderinger)

        val vedtak = vedtak(behandling.id, år = stønadsår, inntekter = InntektWrapper(listOf(inntekt)))
        vedtakRepository.insert(vedtak)
    }
}
