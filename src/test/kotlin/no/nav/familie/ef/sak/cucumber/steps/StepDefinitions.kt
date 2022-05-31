package no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.beregning.skolepenger.BeregningSkolepengerService
import no.nav.familie.ef.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser
import no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomenebegrep
import no.nav.familie.ef.sak.cucumber.domeneparser.parseAktivitetType
import no.nav.familie.ef.sak.cucumber.domeneparser.parseEndringType
import no.nav.familie.ef.sak.cucumber.domeneparser.parseFraOgMed
import no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parseTilOgMed
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriIntRange
import no.nav.familie.ef.sak.cucumber.domeneparser.parseVedtaksperiodeType
import no.nav.familie.ef.sak.cucumber.domeneparser.parseÅrMåned
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.SaksbehandlingDomeneParser
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.historikk.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.historikk.VedtakHistorikkService
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class StepDefinitions {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private var gittVedtak = listOf<Vedtak>()
    private var saksbehandlinger = mapOf<UUID, Pair<Behandling, Saksbehandling>>()
    private var inntekter = mapOf<UUID, InntektWrapper>()
    private var beregnetAndelHistorikkList = listOf<AndelHistorikkDto>()

    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val beregningService = BeregningService()
    private val beregningBarnetilsynService = BeregningBarnetilsynService()
    private val beregningSkolepengerService = BeregningSkolepengerService()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val barnService = mockk<BarnService>(relaxed = true)
    private val fagsakService = mockFagsakService()

    private val beregnYtelseSteg = BeregnYtelseSteg(
        tilkjentYtelseService,
        beregningService,
        beregningBarnetilsynService,
        beregningSkolepengerService,
        simuleringService,
        vedtakService,
        tilbakekrevingService,
        barnService,
        fagsakService,
        mockk()
    )

    private val vedtakHistorikkService = VedtakHistorikkService(fagsakService, tilkjentYtelseService)

    private lateinit var stønadstype: StønadType
    private val behandlingIdsToAktivitetArbeid = mutableMapOf<UUID, SvarId?>()
    private lateinit var tilkjentYtelser: MutableMap<UUID, TilkjentYtelse>
    private lateinit var lagredeVedtak: MutableList<Vedtak>

    @Gitt("følgende behandlinger for {}")
    fun følgende_behandlinger(stønadTypeArg: String, dataTable: DataTable) {
        stønadstype = StønadType.valueOf(stønadTypeArg.uppercase())
        saksbehandlinger = SaksbehandlingDomeneParser.mapSaksbehandlinger(dataTable, stønadstype)
    }

    @Gitt("følgende vedtak")
    fun følgende_vedtak(dataTable: DataTable) {
        følgende_vedtak(StønadType.OVERGANGSSTØNAD.name, dataTable)
    }

    @Gitt("følgende vedtak for {}")
    fun følgende_vedtak(stønadTypeArg: String, dataTable: DataTable) {
        val stønadstype = StønadType.valueOf(stønadTypeArg.uppercase())
        validerOgSettStønadstype(stønadstype)
        gittVedtak = when (stønadstype) {
            StønadType.OVERGANGSSTØNAD -> VedtakDomeneParser.mapVedtakOvergangsstønad(dataTable)
            StønadType.BARNETILSYN -> {
                behandlingIdsToAktivitetArbeid.putAll(VedtakDomeneParser.mapAktivitetForBarnetilsyn(dataTable))
                VedtakDomeneParser.mapVedtakForBarnetilsyn(dataTable)
            }
            StønadType.SKOLEPENGER -> VedtakDomeneParser.mapVedtakForSkolepenger(dataTable)
        }
    }

    @Gitt("følgende inntekter")
    fun følgende_inntekter(dataTable: DataTable) {
        feilHvis(stønadstype != StønadType.OVERGANGSSTØNAD) {
            "Kan kun sette inntekter på overgangsstønad"
        }
        inntekter = VedtakDomeneParser.mapInntekter(dataTable)
    }

    @Gitt("følgende kontantstøtte")
    fun følgende_kontantstøtte(dataTable: DataTable) {
        feilHvis(stønadstype != StønadType.BARNETILSYN) {
            "Kan kun sette kontantstøtte på barnetilsyn"
        }
        gittVedtak = VedtakDomeneParser.mapOgSettPeriodeMedBeløp(gittVedtak, dataTable) { vedtak, perioder ->
            vedtak.copy(kontantstøtte = KontantstøtteWrapper(perioder))
        }
    }

    @Gitt("følgende tilleggsstønad")
    fun følgende_tilleggsstønad(dataTable: DataTable) {
        feilHvis(stønadstype != StønadType.BARNETILSYN) {
            "Kan kun sette tilleggsstønad på barnetilsyn"
        }
        gittVedtak = VedtakDomeneParser.mapOgSettPeriodeMedBeløp(gittVedtak, dataTable) { vedtak, perioder ->
            vedtak.copy(tilleggsstønad = TilleggsstønadWrapper(true, perioder, null))
        }
    }

    @Når("beregner ytelse")
    fun `beregner ytelse`() {
        initialiserTilkjentYtelseOgVedtakMock()

        val behandlinger = mapBehandlinger()

        if (stønadstype == StønadType.OVERGANGSSTØNAD) {
            // Skriver over inntekt hvis inntekter er definiert
            gittVedtak = gittVedtak.map {
                it.copy(inntekter = inntekter[it.behandlingId] ?: it.inntekter)
            }
        }

        gittVedtak.map {
            beregnYtelseSteg.utførSteg(behandlinger[it.behandlingId]!!.second, it.tilVedtakDto())
        }
        // kan ikke beregne historikk ennå
        if (stønadstype == StønadType.SKOLEPENGER) return
        beregnetAndelHistorikkList = AndelHistorikkBeregner.lagHistorikk(
            tilkjentYtelser.values.toList(),
            lagredeVedtak,
            behandlinger.values.map { it.first }.toList(),
            null,
            behandlingIdsToAktivitetArbeid
        )
    }

    @Så("forvent følgende vedtaksperioder fra dato: {}")
    fun `forvent følgende vedtaksperiodene fra dato`(årMåned: String, dataTable: DataTable) {
        val vedtak = vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(UUID.randomUUID(), parseÅrMåned(årMåned))
        val perioder = vedtak.perioder
        dataTable.asMaps().mapIndexed { index, rad ->
            val periode = perioder[index]

            val fraOgMed = parseÅrMåned(Domenebegrep.FRA_OG_MED_DATO, rad)
            val tilOgMed = parseÅrMåned(Domenebegrep.TIL_OG_MED_DATO, rad)
            assertThat(periode.årMånedFra).isEqualTo(fraOgMed)
            assertThat(periode.årMånedTil).isEqualTo(tilOgMed)

            assertThat(periode.aktivitet).isEqualTo(parseAktivitetType(rad))
            assertThat(periode.periodeType).isEqualTo(parseVedtaksperiodeType(rad))
        }
        assertThat(dataTable.asMaps()).hasSize(perioder.size)
    }

    @Så("forvent følgende inntektsperioder fra dato: {}")
    fun `forvent følgende inntektsperioder fra dato`(årMåned: String, dataTable: DataTable) {
        val vedtak = vedtakHistorikkService.hentVedtakForOvergangsstønadFraDato(UUID.randomUUID(), parseÅrMåned(årMåned))
        val perioder = vedtak.inntekter
        dataTable.asMaps().mapIndexed { index, rad ->
            val periode = perioder[index]

            val fraOgMed = parseÅrMåned(Domenebegrep.FRA_OG_MED_DATO, rad)
            assertThat(periode.årMånedFra).isEqualTo(fraOgMed)

            assertThat(periode.forventetInntekt?.toInt()).isEqualTo(parseInt(VedtakDomenebegrep.INNTEKT, rad))
            assertThat(periode.samordningsfradrag?.toInt()).isEqualTo(parseInt(VedtakDomenebegrep.SAMORDNINGSFRADRAG, rad))
        }
        assertThat(dataTable.asMaps()).hasSize(perioder.size)
    }

    @Så("forvent følgende andeler lagret for behandling med id: {int}")
    fun `forvent følgende andeler lagret`(behandling: Int, dataTable: DataTable) {
        if (stønadstype == StønadType.SKOLEPENGER) return // TODO denne må slettes når vi fikset beregning av periodene
        dataTable.asMaps().mapIndexed { index, rad ->
            val behandlingId = behandlingIdTilUUID[behandling]
            val kildeBehandlingId =
                behandlingIdTilUUID[parseInt(VedtakDomenebegrep.KILDE_BEHANDLING_ID, rad)]
            val gjeldendeTilkjentYtelse: TilkjentYtelse =
                tilkjentYtelser[behandlingId] ?: error("Fant ikke tilkjent ytelse med id $behandlingId")

            val fraOgMed = parseFraOgMed(rad)
            val tilOgMed = parseTilOgMed(rad)
            val beløpMellom = parseValgfriIntRange(VedtakDomenebegrep.BELØP_MELLOM, rad)
            val beløp = parseValgfriInt(VedtakDomenebegrep.BELØP, rad)

            val gjelendeAndel = gjeldendeTilkjentYtelse.andelerTilkjentYtelse.find { it.stønadFom == fraOgMed }
                ?: error("Fant ingen andel med startdato $fraOgMed")

            try {

                assertThat(fraOgMed).isEqualTo(gjelendeAndel.stønadFom)
                assertThat(tilOgMed).isEqualTo(gjelendeAndel.stønadTom)
                beløpMellom?.let {
                    assertThat(gjelendeAndel.beløp)
                        .isGreaterThanOrEqualTo(it.first)
                        .isLessThanOrEqualTo(it.second)
                }
                beløp?.let { assertThat(gjelendeAndel.beløp).isEqualTo(it) }
                assertThat(kildeBehandlingId).isEqualTo(gjelendeAndel.kildeBehandlingId)
            } catch (e: Throwable) {
                logger.info("Expected: {}", rad)
                logger.info("Actual: {}", gjelendeAndel)
                throw Throwable("Feilet rad $index", e)
            }
        }
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
                acc + behandling(
                    id = id,
                    opprettetTid = LocalDateTime.now().plusMinutes(index.toLong()),
                    type = BehandlingType.REVURDERING,
                    forrigeBehandlingId = acc.lastOrNull()?.id
                )
            }
            .map { it to saksbehandling(fagsak, it) }
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
            tilkjentYtelser.put(tilkjentYtelse.behandlingId, tilkjentYtelse)
            tilkjentYtelse
        }
        every {
            tilkjentYtelseService.hentForBehandling(any())
        } answers {
            tilkjentYtelser.getValue(firstArg())
        }
        every { tilkjentYtelseService.hentHistorikk(any(), any()) } answers {
            beregnetAndelHistorikkList
        }
        return tilkjentYtelser
    }

    private fun mockFagsakService(): FagsakService {
        val mock = mockk<FagsakService>(relaxed = true)
        every { mock.hentFagsak(any()) } answers { fagsak(stønadstype = stønadstype) }
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
                throw Throwable("Feilet rad $index", e)
            }
        }
    }

    private fun assertBeregnetAndel(
        it: MutableMap<String, String>,
        index: Int,
        forventetHistorikkEndringer: List<VedtakDomeneParser.ForventetHistorikk>,
        andelHistorikkDto: AndelHistorikkDto
    ) {
        val endringType = parseEndringType(it)
        val endretIBehandlingId =
            behandlingIdTilUUID[parseValgfriInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, it)]
        val beregnetAndelHistorikk = andelHistorikkDto
        val forventetHistorikkEndring = forventetHistorikkEndringer[index]

        assertThat(beregnetAndelHistorikk).isNotNull
        assertThat(beregnetAndelHistorikk.andel.stønadFra).isEqualTo(forventetHistorikkEndring.stønadFra)
        assertThat(beregnetAndelHistorikk.andel.stønadTil).isEqualTo(forventetHistorikkEndring.stønadTil)
        assertThat(beregnetAndelHistorikk.behandlingId).isEqualTo(forventetHistorikkEndring.behandlingId)
        if (endringType == null) {
            assertThat(beregnetAndelHistorikk.endring).isNull()
        } else {
            assertThat(beregnetAndelHistorikk.endring!!.type).isEqualTo(endringType)
            assertThat(beregnetAndelHistorikk.endring?.behandlingId).isEqualTo(endretIBehandlingId)
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
        assertThat(beregnetAndelHistorikk.aktivitet).isEqualTo(forventetHistorikkEndring.aktivitetType)

        if (beregnetAndelHistorikk.endring != null || forventetHistorikkEndring.historikkEndring != null) {
            assertThat(beregnetAndelHistorikk.endring?.type).isEqualTo(forventetHistorikkEndring.historikkEndring?.type)
            assertThat(beregnetAndelHistorikk.endring?.behandlingId)
                .isEqualTo(forventetHistorikkEndring.historikkEndring?.behandlingId)
        }
    }
}
