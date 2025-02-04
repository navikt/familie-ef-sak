package no.nav.familie.ef.sak.cucumber.steps

import com.fasterxml.jackson.module.kotlin.readValue
import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.oppgaveforopprettelse.OppgaverForOpprettelseService
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.BeregningUtils
import no.nav.familie.ef.sak.beregning.Grunnbeløp
import no.nav.familie.ef.sak.beregning.OmregningService
import no.nav.familie.ef.sak.beregning.ValiderOmregningService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.beregning.skolepenger.BeregningSkolepengerService
import no.nav.familie.ef.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.barnIder
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.behandlingIdFraUUID
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser
import no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.mapBarn
import no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomenebegrep
import no.nav.familie.ef.sak.cucumber.domeneparser.parseAktivitetType
import no.nav.familie.ef.sak.cucumber.domeneparser.parseEndringType
import no.nav.familie.ef.sak.cucumber.domeneparser.parseFraOgMed
import no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parsePeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriIntRange
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriÅrMånedEllerDato
import no.nav.familie.ef.sak.cucumber.domeneparser.parseVedtaksperiodeType
import no.nav.familie.ef.sak.cucumber.domeneparser.parseÅrMåned
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.DatoFormat.YEAR_MONTH_FORMAT_NORSK
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.config.ObjectMapperProvider
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.iverksett.IverksettingDtoMapper
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.SaksbehandlingDomeneParser
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.sisteDagenIMånedenEllerDefault
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.testutil.mockTestMedGrunnbeløpFra
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.PeriodetypeBarnetilsyn
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseBarnetilsyn
import no.nav.familie.ef.sak.vedtak.dto.InnvilgelseOvergangsstønad
import no.nav.familie.ef.sak.vedtak.dto.PeriodeMedBeløpDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.HistorikkKonfigurasjon
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.ef.iverksett.IverksettOvergangsstønadDto
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.prosessering.internal.TaskService
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.math.RoundingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.UUID

class StepDefinitions {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private var gittVedtak = listOf<Vedtak>()
    private var saksbehandlinger = mapOf<UUID, Pair<Behandling, Saksbehandling>>()
    private var inntekter = mapOf<UUID, InntektWrapper>()
    private var beregnetAndelHistorikkList = listOf<AndelHistorikkDto>()
    private var beregnYtelseException: Exception? = null

    private var grunnbeløp: Grunnbeløp? = null

    private val behandlingService = mockk<BehandlingService>()
    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val andelsHistorikkService = mockk<AndelsHistorikkService>(relaxed = true)
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val featureToggleService = mockFeatureToggleService()
    private val beregningService = BeregningService()
    private val beregningBarnetilsynService = BeregningBarnetilsynService(featureToggleService)
    private val beregningSkolepengerService =
        BeregningSkolepengerService(
            behandlingService = behandlingService,
            vedtakService = vedtakService,
        )
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val barnRepository = mockk<BarnRepository>()
    private val barnService = spyk(BarnService(barnRepository, mockk(), behandlingService))
    private val fagsakService = mockFagsakService()
    private val validerOmregningService = mockk<ValiderOmregningService>(relaxed = true)
    private val oppgaverForOpprettelseService = mockk<OppgaverForOpprettelseService>(relaxed = true)

    private val beregnYtelseSteg =
        BeregnYtelseSteg(
            tilkjentYtelseService,
            andelsHistorikkService,
            beregningService,
            beregningBarnetilsynService,
            beregningSkolepengerService,
            simuleringService,
            vedtakService,
            tilbakekrevingService,
            barnService,
            fagsakService,
            validerOmregningService,
            oppgaverForOpprettelseService,
            featureToggleService,
        )

    private val vedtakHistorikkService =
        VedtakHistorikkService(fagsakService, andelsHistorikkService, barnService, behandlingService, vedtakService)

    private lateinit var stønadstype: StønadType
    private val behandlingIdsToAktivitetArbeid = mutableMapOf<UUID, SvarId?>()
    private lateinit var tilkjentYtelser: MutableMap<UUID, TilkjentYtelse>
    private val tilkjentYtelseSlot = slot<TilkjentYtelse>()
    private lateinit var lagredeVedtak: MutableList<Vedtak>

    val søknadService = mockk<SøknadService>(relaxed = true)
    val grunnlagsdataService = mockk<GrunnlagsdataService>(relaxed = true)
    val vurderingService = mockk<VurderingService>(relaxed = true)
    val barnServiceMock = mockk<BarnService>(relaxed = true)
    val iverksettingDtoMapper = mockk<IverksettingDtoMapper>()
    val iverksettClient = mockk<IverksettClient>(relaxed = true)
    val taskService = mockk<TaskService>(relaxed = true)
    private val omregningService =
        OmregningService(
            behandlingService,
            vedtakHistorikkService,
            taskService = taskService,
            iverksettClient = iverksettClient,
            ytelseService = tilkjentYtelseService,
            grunnlagsdataService = grunnlagsdataService,
            vurderingService = vurderingService,
            beregnYtelseSteg,
            iverksettingDtoMapper = iverksettingDtoMapper,
            søknadService = søknadService,
            barnService = barnServiceMock,
        )

    init {
        every { behandlingService.hentSaksbehandling(any<UUID>()) } answers {
            val behandlingId = firstArg<UUID>()
            val behandlingIdInt = behandlingIdTilUUID.entries.find { it.value == behandlingId }?.key
            val pair =
                saksbehandlinger[behandlingId] ?: error("Finner ikke behandling=$behandlingId ($behandlingIdInt)")
            pair.second
        }
        every { behandlingService.hentBehandling(any()) } answers { behandling() }
        every { vedtakService.hentVedtak(any()) } answers { lagredeVedtak.single { it.behandlingId == firstArg() } }
        justRun { barnService.validerBarnFinnesPåBehandling(any(), any()) }
        mockBarnRepository()
    }

    private fun mockBarnRepository() {
        every { barnRepository.findAllById(any()) } answers {
            barnIder.entries
                .flatMap { behandlingBarn ->
                    behandlingBarn.value.entries.map {
                        BehandlingBarn(it.value, behandlingBarn.key, personIdent = it.key)
                    }
                }.distinct()
        }
        every { barnRepository.findByBehandlingId(any()) } answers {
            barnIder
                .getOrDefault(firstArg(), emptyMap())
                .map { BehandlingBarn(it.value, firstArg(), personIdent = it.key) }
        }
    }

    @Gitt("følgende behandlinger for {}")
    fun følgende_behandlinger(
        stønadTypeArg: String,
        dataTable: DataTable,
    ) {
        stønadstype = StønadType.valueOf(stønadTypeArg.uppercase())
        saksbehandlinger = SaksbehandlingDomeneParser.mapSaksbehandlinger(dataTable, stønadstype)
    }

    @Gitt("følgende vedtak")
    fun følgende_vedtak(dataTable: DataTable) {
        følgende_vedtak(StønadType.OVERGANGSSTØNAD.name, dataTable)
    }

    @Gitt("følgende vedtak for {}")
    fun følgende_vedtak(
        stønadTypeArg: String,
        dataTable: DataTable,
    ) {
        val stønadstype = StønadType.valueOf(stønadTypeArg.uppercase())
        validerOgSettStønadstype(stønadstype)
        gittVedtak =
            when (stønadstype) {
                StønadType.OVERGANGSSTØNAD -> VedtakDomeneParser.mapVedtakOvergangsstønad(dataTable)
                StønadType.BARNETILSYN -> {
                    behandlingIdsToAktivitetArbeid.putAll(VedtakDomeneParser.mapAktivitetForBarnetilsyn(dataTable))
                    VedtakDomeneParser.mapVedtakForBarnetilsyn(dataTable)
                }

                StønadType.SKOLEPENGER -> VedtakDomeneParser.mapVedtakForSkolepenger(dataTable)
            }
    }

    @Gitt("behandling {int} opphører alle perioder for skolepenger")
    fun opphør_alle_skolepenger(behandlingIdInt: Int) {
        val behandlingId = behandlingIdTilUUID[behandlingIdInt] ?: error("Finner ikke id for $behandlingIdInt")
        validerOgSettStønadstype(StønadType.SKOLEPENGER)
        if (gittVedtak.any { it.behandlingId == behandlingId }) {
            error("Kan ikke opphøre med behandlingId som allerede har et vedtak")
        }
        gittVedtak = gittVedtak + VedtakDomeneParser.opphørSkolepengerUtenPerioder(behandlingId)
    }

    @Gitt("følgende inntekter")
    fun følgende_inntekter(dataTable: DataTable) {
        feilHvis(stønadstype != StønadType.OVERGANGSSTØNAD) {
            "Kan kun sette inntekter på overgangsstønad"
        }
        inntekter = VedtakDomeneParser.mapInntekter(dataTable)
    }

    @Gitt("G i 2023 er 120_000")
    fun grunnbeløpI2023er120_000() {
        grunnbeløp =
            Grunnbeløp(
                periode = Månedsperiode(YearMonth.parse("2023-05"), YearMonth.from(LocalDate.MAX)),
                grunnbeløp = 120_000.toBigDecimal(),
                perMnd = 10000.toBigDecimal(),
                gjennomsnittPerÅr = 117_160.toBigDecimal(),
            )
    }

    @Gitt("følgende kontantstøtte")
    fun følgende_kontantstøtte(dataTable: DataTable) {
        feilHvis(stønadstype != StønadType.BARNETILSYN) {
            "Kan kun sette kontantstøtte på barnetilsyn"
        }
        gittVedtak =
            VedtakDomeneParser.mapOgSettPeriodeMedBeløp(gittVedtak, dataTable) { vedtak, perioder ->
                vedtak.copy(kontantstøtte = KontantstøtteWrapper(perioder, null))
            }
    }

    @Gitt("følgende tilleggsstønad")
    fun følgende_tilleggsstønad(dataTable: DataTable) {
        feilHvis(stønadstype != StønadType.BARNETILSYN) {
            "Kan kun sette tilleggsstønad på barnetilsyn"
        }
        gittVedtak =
            VedtakDomeneParser.mapOgSettPeriodeMedBeløp(gittVedtak, dataTable) { vedtak, perioder ->
                vedtak.copy(tilleggsstønad = TilleggsstønadWrapper(perioder, null))
            }
    }

    @Når("beregner ytelse kaster feil med innehold {}")
    fun `beregner ytelse kaster feil pga validering`(feilmelding: String) {
        Assertions
            .assertThatThrownBy { `beregner ytelse`() }
            .hasMessageContaining(feilmelding)
    }

    @Når("beregner ytelse med G for år {} med beløp {}")
    fun `beregn ytelse med gitt grunnbeløp`(
        år: Int,
        beløp: Int,
    ) {
        settGrunnbeløp(år, beløp)
        mockTestMedGrunnbeløpFra(grunnbeløp!!) {
            `beregner ytelse`()
        }
    }

    @Når("utfør g-omregning for år {} med beløp {}")
    fun `utfør g-omregning`(
        år: Int,
        beløp: Int,
    ) {
        settGrunnbeløp(år, beløp)
        val fagsakId = saksbehandlinger.firstNotNullOf { saksb -> saksb.value.second }.fagsakId

        val forrigeBehandling = saksbehandlinger.firstNotNullOf { saksb -> saksb.value.first }
        mockGOmregning(forrigeBehandling, fagsakId)

        mockTestMedGrunnbeløpFra(grunnbeløp!!) {
            every { tilkjentYtelseService.opprettTilkjentYtelse(capture(tilkjentYtelseSlot)) } answers { firstArg() }
            omregningService.utførGOmregning(fagsakId = fagsakId)
        }
    }

    private fun settGrunnbeløp(
        år: Int,
        beløp: Int,
    ) {
        val mai = YearMonth.of(år, 5)
        grunnbeløp =
            Grunnbeløp(
                periode = Månedsperiode(mai, YearMonth.from(LocalDate.MAX)),
                grunnbeløp = beløp.toBigDecimal(),
                perMnd = beløp.toBigDecimal().divide(BeregningUtils.ANTALL_MÅNEDER_ÅR, RoundingMode.HALF_UP),
                gjennomsnittPerÅr = 0.toBigDecimal(),
            )
    }

    private fun mockGOmregning(
        forrigeBehandling: Behandling,
        fagsakId: UUID,
    ) {
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns forrigeBehandling
        every { behandlingService.finnesÅpenBehandling(any()) } returns false
        every { behandlingService.finnesBehandlingSomIkkeErFerdigstiltEllerSattPåVent(any()) } returns false

        every { tilkjentYtelseService.hentForBehandling(any()) } returns tilkjentYtelser.values.first()
        val behandling =
            behandling(
                id = UUID.randomUUID(),
                opprettetTid = LocalDateTime.now(),
                type = BehandlingType.REVURDERING,
                forrigeBehandlingId = forrigeBehandling.id,
                vedtakstidspunkt = LocalDateTime.MIN,
                årsak = BehandlingÅrsak.G_OMREGNING,
            )
        every {
            behandlingService.opprettBehandling(
                BehandlingType.REVURDERING,
                fagsakId,
                BehandlingStatus.OPPRETTET,
                StegType.VILKÅR,
                BehandlingÅrsak.G_OMREGNING,
                null,
                false,
            )
        } returns behandling

        every { behandlingService.hentSaksbehandling(behandling.id) } returns
            saksbehandling(
                fagsak(id = fagsakId),
                behandling,
            )

        every { behandlingService.oppdaterResultatPåBehandling(any(), any()) } returns behandling
        every {
            behandlingService.oppdaterStegPåBehandling(
                any(),
                any(),
            )
        } returns behandling.copy(steg = StegType.VENTE_PÅ_STATUS_FRA_IVERKSETT)
        every {
            behandlingService.oppdaterStatusPåBehandling(
                any(),
                any(),
            )
        } returns behandling.copy(status = BehandlingStatus.IVERKSETTER_VEDTAK)

        val expectedIverksettDto: IverksettOvergangsstønadDto =
            ObjectMapperProvider.objectMapper.readValue(readFile("expectedIverksettDto.json"))

        every { iverksettingDtoMapper.tilDtoMaskineltBehandlet(any()) } returns expectedIverksettDto
    }

    private fun readFile(filnavn: String): String = this::class.java.getResource("/omregning/$filnavn")!!.readText()

    @Når("beregner ytelse")
    fun `beregner ytelse`() {
        initialiserTilkjentYtelseOgVedtakMock()

        saksbehandlinger = mapBehandlinger()

        if (stønadstype == StønadType.OVERGANGSSTØNAD) {
            // Skriver over inntekt hvis inntekter er definert
            gittVedtak =
                gittVedtak.map {
                    it.copy(inntekter = inntekter[it.behandlingId] ?: it.inntekter)
                }
        }

        gittVedtak.map {
            try {
                beregnYtelseSteg.utførSteg(saksbehandlinger[it.behandlingId]!!.second, it.tilVedtakDto())
            } catch (e: Exception) {
                logger.error("Feilet for behandling ${behandlingIdFraUUID(it.behandlingId)}")
                throw e
            }
            // kan ikke beregne historikk ennå
            if (stønadstype != StønadType.SKOLEPENGER) {
                beregnetAndelHistorikkList =
                    AndelHistorikkBeregner.lagHistorikk(
                        stønadstype,
                        tilkjentYtelser.values.toList(),
                        lagredeVedtak,
                        saksbehandlinger.values.map { it.first }.toList(),
                        null,
                        behandlingIdsToAktivitetArbeid,
                        HistorikkKonfigurasjon(brukIkkeVedtatteSatser = false),
                    )
            }
        }
    }

    @Når("beregner ytelse kaster feil")
    fun `beregner ytelse kaster feil`() {
        try {
            `beregner ytelse`()
        } catch (e: Exception) {
            beregnYtelseException = e
        }
        if (beregnYtelseException == null) {
            error("Forventet at beregn ytelse kaster feil")
        }
    }

    @Så("forvent følgende feil: {}")
    fun `forvent følgende feil`(feilmeldingTekst: String) {
        assertThat(beregnYtelseException).isNotNull
        assertThat(beregnYtelseException).hasMessageContaining(feilmeldingTekst)
    }

    @Så("forvent følgende vedtaksperioder fra dato: {}")
    fun `forvent følgende vedtaksperiodene fra dato`(
        årMåned: String,
        dataTable: DataTable,
    ) {
        val behandlingId = UUID.randomUUID()
        opprettBarnForNyBehandling(behandlingId)
        val vedtak = vedtakHistorikkService.hentVedtakFraDato(behandlingId, parseÅrMåned(årMåned))
        when (vedtak) {
            is InnvilgelseOvergangsstønad -> {
                val perioder = vedtak.perioder
                dataTable.asMaps().mapIndexed { index, rad ->
                    val periode = perioder[index]

                    assertThat(periode.periode.fom).isEqualTo(parseÅrMåned(Domenebegrep.FRA_OG_MED_DATO, rad))
                    assertThat(periode.periode.tom).isEqualTo(parseÅrMåned(Domenebegrep.TIL_OG_MED_DATO, rad))

                    assertThat(periode.aktivitet).isEqualTo(parseAktivitetType(rad))
                    assertThat(periode.periodeType).isEqualTo(parseVedtaksperiodeType(rad))
                }
                assertThat(dataTable.asMaps()).hasSize(perioder.size)
            }

            is InnvilgelseBarnetilsyn -> {
                val perioder = vedtak.perioder
                dataTable.asMaps().mapIndexed { index, rad ->
                    val periode = perioder[index]

                    assertThat(periode.periode.fom).isEqualTo(parseÅrMåned(Domenebegrep.FRA_OG_MED_DATO, rad))
                    assertThat(periode.periode.tom).isEqualTo(parseÅrMåned(Domenebegrep.TIL_OG_MED_DATO, rad))

                    assertThat(periode.barn).containsExactlyElementsOf(mapBarn(behandlingId, rad))
                    assertThat(periode.utgifter).isEqualTo(parseInt(VedtakDomenebegrep.UTGIFTER, rad))
                    val forventetPeriodetype = parsePeriodetypeBarnetilsyn(rad) ?: PeriodetypeBarnetilsyn.ORDINÆR
                    assertThat(periode.periodetype).isEqualTo(forventetPeriodetype)
                }
                assertThat(dataTable.asMaps()).hasSize(perioder.size)
            }

            else -> error("Støtter ikke ${vedtak.javaClass.simpleName}")
        }
    }

    private fun opprettBarnForNyBehandling(behandlingId: UUID) = barnIder.values.flatMap { it.keys }.forEach { IdTIlUUIDHolder.hentEllerOpprettBarn(behandlingId, it) }

    @Så("forvent følgende inntektsperioder fra dato: {}")
    fun `forvent følgende inntektsperioder fra dato`(
        årMåned: String,
        dataTable: DataTable,
    ) {
        val vedtak =
            vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(UUID.randomUUID(), parseÅrMåned(årMåned))
        val perioder = vedtak.inntekter
        dataTable.asMaps().mapIndexed { index, rad ->
            val periode = perioder[index]

            val fraOgMed = parseÅrMåned(Domenebegrep.FRA_OG_MED_DATO, rad)
            assertThat(periode.årMånedFra).isEqualTo(fraOgMed)

            parseValgfriInt(VedtakDomenebegrep.DAGSATS, rad)?.let {
                assertThat(periode.dagsats?.toInt() ?: 0).isEqualTo(it)
            }
            parseValgfriInt(VedtakDomenebegrep.MÅNEDSINNTEKT, rad)?.let {
                assertThat(periode.månedsinntekt?.toInt() ?: 0).isEqualTo(it)
            }
            assertThat(periode.forventetInntekt?.toInt()).isEqualTo(parseInt(VedtakDomenebegrep.INNTEKT, rad))
            assertThat(periode.samordningsfradrag?.toInt())
                .isEqualTo(parseInt(VedtakDomenebegrep.SAMORDNINGSFRADRAG, rad))
        }
        assertThat(dataTable.asMaps()).hasSize(perioder.size)
    }

    @Så("forvent følgende perioder for kontantstøtte fra dato: {}")
    fun `forvent følgende perioder for kontantstøtte fra dato`(
        årMåned: String,
        dataTable: DataTable,
    ) {
        forventFølgendeBeløpsperioder(årMåned, dataTable) { it.perioderKontantstøtte }
    }

    @Så("forvent følgende perioder for tilleggsstønad fra dato: {}")
    fun `forvent følgende perioder for tilleggsstønad fra dato`(
        årMåned: String,
        dataTable: DataTable,
    ) {
        forventFølgendeBeløpsperioder(årMåned, dataTable) { it.tilleggsstønad.perioder }
    }

    private fun forventFølgendeBeløpsperioder(
        årMåned: String,
        dataTable: DataTable,
        perioder: (InnvilgelseBarnetilsyn) -> List<PeriodeMedBeløpDto>,
    ) {
        val behandlingId = UUID.randomUUID()
        opprettBarnForNyBehandling(behandlingId)
        val vedtak = vedtakHistorikkService.hentVedtakFraDato(behandlingId, parseÅrMåned(årMåned))
        feilHvisIkke(vedtak is InnvilgelseBarnetilsyn) {
            "Må være vedtak av type barnetilsyn"
        }
        val perioder = perioder((vedtak as InnvilgelseBarnetilsyn))
        dataTable.asMaps().mapIndexed { index, rad ->
            val periode = perioder[index]

            assertThat(periode.periode.fom).isEqualTo(parseÅrMåned(Domenebegrep.FRA_OG_MED_DATO, rad))
            assertThat(periode.periode.tom).isEqualTo(parseÅrMåned(Domenebegrep.TIL_OG_MED_DATO, rad))

            assertThat(periode.beløp).isEqualTo(parseInt(VedtakDomenebegrep.BELØP, rad))
        }
        assertThat(dataTable.asMaps()).hasSize(perioder.size)
    }

    @Så("forvent følgende andeler for g-omregnet tilkjent ytelse")
    fun `forvent følgende andeler for g-omregning`(dataTable: DataTable) {
        assertThat(tilkjentYtelseSlot.captured.andelerTilkjentYtelse.size).isEqualTo(dataTable.asMaps().size)
        dataTable.asMaps().mapIndexed { index, rad ->
            val fraOgMed = parseFraOgMed(rad)
            val tilOgMed =
                parseValgfriÅrMånedEllerDato(Domenebegrep.TIL_OG_MED_DATO, rad).sisteDagenIMånedenEllerDefault(fraOgMed)
            val beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad)
            val inntekt = parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad)
            // val indeksjustertInntekt = parseValgfriInt(VedtakDomenebegrep.INDEKSJUSTERT_INNTEKT, rad)
            assertThat(tilkjentYtelseSlot.captured.andelerTilkjentYtelse[index].stønadFom).isEqualTo(fraOgMed)
            assertThat(tilkjentYtelseSlot.captured.andelerTilkjentYtelse[index].stønadTom).isEqualTo(tilOgMed)
            assertThat(tilkjentYtelseSlot.captured.andelerTilkjentYtelse[index].beløp).isEqualTo(beløp)
            assertThat(tilkjentYtelseSlot.captured.andelerTilkjentYtelse[index].inntekt).isEqualTo(inntekt)
        }
    }

    @Så("forvent følgende andeler lagret for behandling med id: {int}")
    fun `forvent følgende andeler lagret`(
        behandling: Int,
        dataTable: DataTable,
    ) {
        val behandlingId = behandlingIdTilUUID[behandling]
        val gjeldendeTilkjentYtelse: TilkjentYtelse =
            tilkjentYtelser[behandlingId] ?: error("Fant ikke tilkjent ytelse med id $behandlingId")
        val gjeldendeAndelerTilkjentYtelse = gjeldendeTilkjentYtelse.andelerTilkjentYtelse
        dataTable.asMaps().mapIndexed { index, rad ->
            val kildeBehandlingId =
                behandlingIdTilUUID[parseInt(VedtakDomenebegrep.KILDE_BEHANDLING_ID, rad)]

            val fraOgMed = parseFraOgMed(rad)
            val tilOgMed =
                parseValgfriÅrMånedEllerDato(Domenebegrep.TIL_OG_MED_DATO, rad).sisteDagenIMånedenEllerDefault(fraOgMed)
            val beløpMellom = parseValgfriIntRange(VedtakDomenebegrep.BELØP_MELLOM, rad)
            val beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad)
            val inntekt = parseValgfriInt(VedtakDomenebegrep.INNTEKT, rad)

            val gjeldendeAndel = gjeldendeAndelerTilkjentYtelse[index]

            try {
                assertThat(fraOgMed).isEqualTo(gjeldendeAndel.stønadFom)
                assertThat(tilOgMed).isEqualTo(gjeldendeAndel.stønadTom)
                beløpMellom?.let {
                    assertThat(gjeldendeAndel.beløp)
                        .isGreaterThanOrEqualTo(it.first)
                        .isLessThanOrEqualTo(it.second)
                }
                beløp?.let { assertThat(gjeldendeAndel.beløp).isEqualTo(it) }
                assertThat(kildeBehandlingId).isEqualTo(gjeldendeAndel.kildeBehandlingId)
                inntekt?.let { assertThat(gjeldendeAndel.inntekt).isEqualTo(it) }
            } catch (e: Throwable) {
                logger.info("Expected: {}", rad)
                logger.info("Actual: {}", gjeldendeAndel)
                throw Throwable("Feilet rad $index", e)
            }
        }
        feilHvis(gjeldendeAndelerTilkjentYtelse.size > dataTable.asMaps().size) {
            val andelerTilkjentYtelse = gjeldendeAndelerTilkjentYtelse
            val manglandeAndeler = andelerTilkjentYtelse.subList(dataTable.asMaps().size, andelerTilkjentYtelse.size)
            "Mangler periodene: $manglandeAndeler"
        }
        assertThat(dataTable.asMaps().size).isEqualTo(gjeldendeAndelerTilkjentYtelse.size)
    }

    private fun validerOgSettStønadstype(stønadType: StønadType) {
        feilHvis(this::stønadstype.isInitialized && this.stønadstype != stønadType) {
            "Kan ikke bruke 2 ulike stønadstyper, ${this.stønadstype} er allerede satt, prøver å sette $stønadType"
        }
        stønadstype = stønadType
    }

    private fun mapBehandlinger(): Map<UUID, Pair<Behandling, Saksbehandling>> {
        if (saksbehandlinger.isNotEmpty()) return saksbehandlinger
        val fagsak = fagsak(stønadstype = stønadstype)

        return gittVedtak
            .map { it.behandlingId }
            .distinct()
            .foldIndexed<UUID, List<Behandling>>(listOf()) { index, acc, id ->
                acc +
                    behandling(
                        id = id,
                        opprettetTid = LocalDateTime.now().plusMinutes(index.toLong()),
                        type = if (index == 0) BehandlingType.FØRSTEGANGSBEHANDLING else BehandlingType.REVURDERING,
                        forrigeBehandlingId = acc.lastOrNull()?.id,
                        vedtakstidspunkt = LocalDateTime.MIN,
                    )
            }.map { it to saksbehandling(fagsak, it) }
            .associateBy { it.first.id }
    }

    private fun initialiserTilkjentYtelseOgVedtakMock() {
        tilkjentYtelser = mockTilkjentYtelse()
        lagredeVedtak = mockLagreVedtak()
    }

    private fun mockLagreVedtak(): MutableList<Vedtak> {
        val lagredeVedtak = mutableListOf<Vedtak>()
        every {
            vedtakService.lagreVedtak(any(), any(), any())
        } answers {
            lagredeVedtak.add(firstArg<VedtakDto>().tilVedtak(secondArg(), thirdArg()))
            secondArg()
        }
        return lagredeVedtak
    }

    private fun mockTilkjentYtelse(): MutableMap<UUID, TilkjentYtelse> {
        val tilkjentYtelser = mutableMapOf<UUID, TilkjentYtelse>()
        every {
            tilkjentYtelseService.opprettTilkjentYtelse(any())
        } answers {
            val tilkjentYtelse = firstArg<TilkjentYtelse>()
            tilkjentYtelser[tilkjentYtelse.behandlingId] = tilkjentYtelse
            tilkjentYtelse
        }
        every {
            tilkjentYtelseService.hentForBehandling(any())
        } answers {
            tilkjentYtelser.getValue(firstArg())
        }
        every { andelsHistorikkService.hentHistorikk(any(), any()) } answers {
            beregnetAndelHistorikkList
        }
        return tilkjentYtelser
    }

    private fun mockFagsakService(): FagsakService {
        val mock = mockk<FagsakService>(relaxed = true)
        every { mock.hentFagsak(any()) } answers { fagsak(stønadstype = stønadstype) }
        every { mock.hentFagsakForBehandling(any()) } answers { fagsak(stønadstype = stønadstype) }
        return mock
    }

    @Så("forvent følgende historikk")
    fun forvent_følgende_historik(dataTable: DataTable) {
        val forventetHistorikkEndringer = VedtakDomeneParser.mapBehandlingForHistorikkEndring(dataTable, stønadstype)

        dataTable.asMaps().forEachIndexed { index, it ->
            val andelHistorikkDto = beregnetAndelHistorikkList[index]
            try {
                assertBeregnetAndel(it, index, forventetHistorikkEndringer, andelHistorikkDto)
            } catch (e: Throwable) {
                logger.info("Expected: {}", it)
                logger.info("Actual: {}", andelHistorikkDto)
                loggForventet()

                throw Throwable("Feilet rad $index", e)
            }
        }
        try {
            assertThat(dataTable.asMaps()).hasSize(beregnetAndelHistorikkList.size)
        } catch (e: Throwable) {
            loggForventet()
            throw e
        }
    }

    private fun loggForventet() {
        beregnetAndelHistorikkList.forEach { andel ->
            logger.info(
                listOf(
                    behandlingIdTilUUID.entries.find { it.value == andel.behandlingId }!!.key,
                    andel.andel.periode.fom
                        .format(YEAR_MONTH_FORMAT_NORSK),
                    andel.andel.periode.tom
                        .format(YEAR_MONTH_FORMAT_NORSK),
                    andel.endring?.type ?: "",
                    andel.endring?.behandlingId?.let { bid -> behandlingIdTilUUID.entries.find { it.value == bid }!!.key }
                        ?: "",
                    "opphør=${andel.erOpphør}",
                ).joinToString("|", prefix = "|", postfix = "|"),
            )
        }
    }

    private fun assertBeregnetAndel(
        it: MutableMap<String, String>,
        index: Int,
        forventetHistorikkEndringer: List<VedtakDomeneParser.ForventetHistorikk>,
        andelHistorikkDto: AndelHistorikkDto,
    ) {
        val endringType = parseEndringType(it)
        val endretIBehandlingId = parseValgfriInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, it)
        val beregnetAndelHistorikk = andelHistorikkDto
        val forventetHistorikkEndring = forventetHistorikkEndringer[index]

        assertThat(beregnetAndelHistorikk).isNotNull
        assertThat(beregnetAndelHistorikk.andel.stønadFra).isEqualTo(forventetHistorikkEndring.stønadFra)
        assertThat(beregnetAndelHistorikk.andel.stønadTil).isEqualTo(forventetHistorikkEndring.stønadTil)
        assertThat(behandlingIdFraUUID(beregnetAndelHistorikk.behandlingId))
            .isEqualTo(behandlingIdFraUUID(forventetHistorikkEndring.behandlingId))
        if (endringType == null) {
            assertThat(beregnetAndelHistorikk.endring).isNull()
        } else {
            assertThat(beregnetAndelHistorikk.endring!!.type).isEqualTo(endringType)
            assertThat(beregnetAndelHistorikk.endring?.let { behandlingIdFraUUID(it.behandlingId) })
                .isEqualTo(endretIBehandlingId)
        }
        forventetHistorikkEndring.inntekt?.let {
            assertThat(beregnetAndelHistorikk.andel.inntekt).isEqualTo(it)
        }
        forventetHistorikkEndring.beløp?.let {
            assertThat(beregnetAndelHistorikk.andel.beløp).isEqualTo(it)
        }
        forventetHistorikkEndring.tilleggsstønad?.let {
            assertThat(beregnetAndelHistorikk.andel.tilleggsstønad).isEqualTo(it)
        }
        forventetHistorikkEndring.kontantstøtte?.let {
            assertThat(beregnetAndelHistorikk.andel.kontantstøtte).isEqualTo(it)
        }
        forventetHistorikkEndring.antallBarn?.let {
            assertThat(beregnetAndelHistorikk.andel.antallBarn).isEqualTo(it)
        }
        forventetHistorikkEndring.utgifter?.let {
            assertThat(beregnetAndelHistorikk.andel.utgifter.toInt()).isEqualTo(it)
        }
        forventetHistorikkEndring.arbeidAktivitet?.let {
            assertThat(beregnetAndelHistorikk.aktivitetArbeid?.name).isEqualTo(it.name)
        }
        forventetHistorikkEndring.erSanksjon?.let {
            assertThat(beregnetAndelHistorikk.erSanksjon).isEqualTo(it)
            if (beregnetAndelHistorikk.erSanksjon) {
                assertThat(beregnetAndelHistorikk.sanksjonsårsak).isNotNull
            }
            assertThat(beregnetAndelHistorikk.sanksjonsårsak).isEqualTo(forventetHistorikkEndring.sanksjonsårsak)
        }
        forventetHistorikkEndring.periodeType?.let {
            assertThat(beregnetAndelHistorikk.periodeType).isEqualTo(it)
        }
        assertThat(beregnetAndelHistorikk.periodetypeBarnetilsyn).isEqualTo(forventetHistorikkEndring.periodeTypeBarnetilsyn)
        assertThat(beregnetAndelHistorikk.aktivitet).isEqualTo(forventetHistorikkEndring.aktivitetType)
        assertThat(beregnetAndelHistorikk.aktivitetBarnetilsyn).isEqualTo(forventetHistorikkEndring.aktivitetTypeBarnetilsyn)

        forventetHistorikkEndring.vedtaksdato?.let {
            assertThat(beregnetAndelHistorikk.vedtakstidspunkt.toLocalDate()).isEqualTo(it)
        }

        assertThat(beregnetAndelHistorikk.erOpphør).isEqualTo(forventetHistorikkEndring.erOpphør)

        if (beregnetAndelHistorikk.endring != null || forventetHistorikkEndring.historikkEndring != null) {
            assertThat(beregnetAndelHistorikk.endring?.type).isEqualTo(forventetHistorikkEndring.historikkEndring?.type)
            assertThat(beregnetAndelHistorikk.endring?.behandlingId)
                .isEqualTo(forventetHistorikkEndring.historikkEndring?.behandlingId)

            forventetHistorikkEndring.historikkEndring
                ?.vedtakstidspunkt
                ?.takeIf { it != LocalDateTime.MIN } // denne er satt til MIN som default då den er required
                ?.let { assertThat(beregnetAndelHistorikk.endring?.vedtakstidspunkt).isEqualTo(it) }
        }
    }
}
