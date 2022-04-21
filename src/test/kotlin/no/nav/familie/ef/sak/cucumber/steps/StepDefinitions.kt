package no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.beregning.barnetilsyn.BeregningBarnetilsynService
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser
import no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomenebegrep
import no.nav.familie.ef.sak.cucumber.domeneparser.parseDato
import no.nav.familie.ef.sak.cucumber.domeneparser.parseEndringType
import no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriIntRange
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.SaksbehandlingDomeneParser
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.InntektWrapper
import no.nav.familie.ef.sak.vedtak.domain.KontantstøtteWrapper
import no.nav.familie.ef.sak.vedtak.domain.TilleggsstønadWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.UUID

class StepDefinitions {

    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    private var gittVedtak = listOf<Vedtak>()
    private var saksbehandlinger = listOf<Saksbehandling>()
    private var inntekter = mapOf<UUID, InntektWrapper>()
    private var beregnetAndelHistorikkList = listOf<AndelHistorikkDto>()

    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val beregningService = BeregningService()
    private val beregningBarnetilsynService = BeregningBarnetilsynService()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val barnService = mockk<BarnService>(relaxed = true)
    private val fagsakService = mockk<FagsakService>(relaxed = true)

    private val beregnYtelseSteg = BeregnYtelseSteg(tilkjentYtelseService,
                                                    beregningService,
                                                    beregningBarnetilsynService,
                                                    simuleringService,
                                                    vedtakService,
                                                    tilbakekrevingService,
                                                    barnService,
                                                    fagsakService)

    private val slot = slot<TilkjentYtelse>()
    private var stønadstype: StønadType = StønadType.OVERGANGSSTØNAD
    private val behandlingIdsToAktivitetArbeid = mutableMapOf<UUID, SvarId?>()
    private lateinit var tilkjentYtelser: MutableMap<UUID, TilkjentYtelse>
    private lateinit var lagredeVedtak: MutableList<Vedtak>

    @Gitt("følgende vedtak")
    fun følgende_vedtak(dataTable: DataTable) {
        gittVedtak = VedtakDomeneParser.mapVedtak(dataTable)
    }

    @Gitt("følgende vedtak for barnetilsyn")
    fun følgende_vedtak_barnetilsyn(dataTable: DataTable) {
        stønadstype = StønadType.BARNETILSYN

        behandlingIdsToAktivitetArbeid.putAll(VedtakDomeneParser.mapAktivitetForBarnetilsyn(dataTable))
        gittVedtak = VedtakDomeneParser.mapVedtakForBarnetilsyn(dataTable)
    }

    @Gitt("følgende saksbehandlinger")
    fun følgende_saksbehandlinger(dataTable: DataTable) {
        stønadstype = StønadType.BARNETILSYN

        behandlingIdsToAktivitetArbeid.putAll(VedtakDomeneParser.mapAktivitetForBarnetilsyn(dataTable))
        gittVedtak = VedtakDomeneParser.mapVedtakForBarnetilsyn(dataTable)
        saksbehandlinger = SaksbehandlingDomeneParser.mapSaksbehandlinger(dataTable)
    }


    @Gitt("følgende inntekter")
    fun følgende_inntekter(dataTable: DataTable) {
        inntekter = VedtakDomeneParser.mapInntekter(dataTable)
    }

    @Gitt("følgende kontantstøtte")
    fun følgende_kontantstøtte(dataTable: DataTable) {
        gittVedtak = VedtakDomeneParser.mapOgSettPeriodeMedBeløp(gittVedtak, dataTable) { vedtak, perioder ->
            vedtak.copy(kontantstøtte = KontantstøtteWrapper(perioder))
        }
    }

    @Gitt("følgende tilleggsstønad")
    fun følgende_tilleggsstønad(dataTable: DataTable) {
        gittVedtak = VedtakDomeneParser.mapOgSettPeriodeMedBeløp(gittVedtak, dataTable) { vedtak, perioder ->
            vedtak.copy(tilleggsstønad = TilleggsstønadWrapper(true, perioder, null))
        }
    }

    @Når("lag andelhistorikk kjøres")
    fun `lag andelhistorikk kjøres`() {
        initialiserTilkjentYtelseOgVedtakMock()

        val behandlinger = gittVedtak.map { it.behandlingId }.distinct().foldIndexed(listOf<Behandling>()) { index, acc, id ->
            acc + behandling(id = id,
                             opprettetTid = LocalDateTime.now().plusMinutes(index.toLong()),
                             type = BehandlingType.REVURDERING,
                             forrigeBehandlingId = acc.lastOrNull()?.id)
        }.associateBy { it.id }

        //Skriver over inntekt hvis inntekter er definiert
        val vedtakMedInntekt = gittVedtak.map {
            it.copy(inntekter = inntekter[it.behandlingId] ?: it.inntekter)
        }

        vedtakMedInntekt.forEach {
            val behandling = behandlinger.getValue(it.behandlingId)
            val saksbehandling = saksbehandling(fagsak(id = behandling.fagsakId, stønadstype = stønadstype), behandling)
            beregnYtelseSteg.utførSteg(saksbehandling, it.tilVedtakDto())
        }
        beregnetAndelHistorikkList = AndelHistorikkBeregner.lagHistorikk(tilkjentYtelser.values.toList(),
                                                                         lagredeVedtak,
                                                                         behandlinger.values.toList(),
                                                                         null,
                                                                         behandlingIdsToAktivitetArbeid)
    }

    @Når("vedtak vedtas")
    fun `når vedtak vedtas`() {

        initialiserTilkjentYtelseOgVedtakMock()

        gittVedtak.map {
            val saksbehandling = saksbehandlinger.find { saksbehandling -> saksbehandling.id == it.behandlingId }
                                 ?: error("Fant ikke saksbehandling for vedtak")
            beregnYtelseSteg.utførSteg(saksbehandling, it.tilVedtakDto())
        }
    }

    @Så("forvent følgende andeler lagret for behandling med id: {int}")
    fun `forvent følgende andeler lagret`(behandling: Int, dataTable: DataTable) {
        dataTable.asMaps().mapIndexed { index, it ->
            val behandlingId = behandlingIdTilUUID[behandling]
            val kildeBehandlingId =
                    behandlingIdTilUUID[parseInt(VedtakDomenebegrep.KILDE_BEHANDLING_ID, it)]
            val gjeldendeTilkjentYtelse: TilkjentYtelse =
                    tilkjentYtelser[behandlingId] ?: error("Fant ikke tilkjent ytelse med id $behandlingId")

            val fraOgMed = parseDato(VedtakDomenebegrep.FRA_OG_MED_DATO, it)
            val tilOgMed = parseDato(VedtakDomenebegrep.TIL_OG_MED_DATO, it)
            val beløpMellom = parseValgfriIntRange(VedtakDomenebegrep.BELØP_MELLOM, it)

            val gjelendeAndel = gjeldendeTilkjentYtelse.andelerTilkjentYtelse.find { it.stønadFom == fraOgMed }
                                ?: error("Fant ingen andel med startdato $fraOgMed")

            try {

                Assertions.assertThat(fraOgMed).isEqualTo(gjelendeAndel.stønadFom)
                Assertions.assertThat(tilOgMed).isEqualTo(gjelendeAndel.stønadTom)
                beløpMellom?.let {
                    Assertions.assertThat(gjelendeAndel.beløp)
                            .isGreaterThanOrEqualTo(it.first)
                            .isLessThanOrEqualTo(it.second)
                }
                Assertions.assertThat(kildeBehandlingId).isEqualTo(gjelendeAndel.kildeBehandlingId)
            } catch (e: Throwable) {
                logger.info("Expected: {}", it)
                logger.info("Actual: {}", gjelendeAndel)
                throw Throwable("Feilet rad $index", e)
            }
        }
    }

    private fun initialiserTilkjentYtelseOgVedtakMock() {
        tilkjentYtelser = mockTilkjentYtelse()
        lagredeVedtak = mockLagreVedtak()
    }

    private fun mockLagreVedtak(): MutableList<Vedtak> {
        val lagredeVedtak = mutableListOf<Vedtak>()
        every {
            vedtakService.lagreVedtak(any(), any())
        } answers {
            lagredeVedtak.add(firstArg<VedtakDto>().tilVedtak(secondArg()))
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
        return tilkjentYtelser
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

        /*
        for (forventetBehandlingMedHistorikkEndring in forventetHistorikkEndringer) {
            val andelHistorikkDto =
                    beregnetAndelHistorikkList.first { it.behandlingId == forventetBehandlingMedHistorikkEndring.id }

            if (forventetBehandlingMedHistorikkEndring.historikkEndring == null) {
                Assertions.assertThat(andelHistorikkDto.endring).isNull()
                if (forventetBehandlingMedHistorikkEndring.inntekt > 0) {
                    Assertions.assertThat(andelHistorikkDto.andel.inntekt).isEqualTo(forventetBehandlingMedHistorikkEndring.inntekt)
                }
            } else {
                Assertions.assertThat(forventetBehandlingMedHistorikkEndring.historikkEndring.behandlingId)
                        .isEqualTo(andelHistorikkDto.endring?.behandlingId)
                Assertions.assertThat(forventetBehandlingMedHistorikkEndring.historikkEndring.type)
                        .isEqualTo(andelHistorikkDto.endring?.type)
                Assertions.assertThat(forventetBehandlingMedHistorikkEndring.inntekt).isEqualTo(andelHistorikkDto.andel.inntekt)
            }
        }
         */
    }

    private fun assertBeregnetAndel(it: MutableMap<String, String>,
                                    index: Int,
                                    forventetHistorikkEndringer: List<VedtakDomeneParser.ForventetHistorikk>,
                                    andelHistorikkDto: AndelHistorikkDto
    ) {
        val endringType = parseEndringType(it)
        val endretIBehandlingId =
                behandlingIdTilUUID[parseValgfriInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, it)]
        val beregnetAndelHistorikk = andelHistorikkDto
        val forventetHistorikkEndring = forventetHistorikkEndringer[index]

        Assertions.assertThat(beregnetAndelHistorikk).isNotNull
        Assertions.assertThat(beregnetAndelHistorikk.andel.stønadFra).isEqualTo(forventetHistorikkEndring.stønadFra)
        Assertions.assertThat(beregnetAndelHistorikk.andel.stønadTil).isEqualTo(forventetHistorikkEndring.stønadTil)
        Assertions.assertThat(beregnetAndelHistorikk.behandlingId).isEqualTo(forventetHistorikkEndring.behandlingId)
        if (endringType == null) {
            Assertions.assertThat(beregnetAndelHistorikk.endring).isNull()
        } else {
            Assertions.assertThat(beregnetAndelHistorikk.endring!!.type).isEqualTo(endringType)
            Assertions.assertThat(beregnetAndelHistorikk.endring?.behandlingId).isEqualTo(endretIBehandlingId)
        }
        forventetHistorikkEndring.inntekt?.let {
            Assertions.assertThat(beregnetAndelHistorikk.andel.inntekt).isEqualTo(it)
        }
        forventetHistorikkEndring.beløp?.let {
            Assertions.assertThat(beregnetAndelHistorikk.andel.beløp).isEqualTo(it)
        }
        forventetHistorikkEndring.tilleggsstønad?.let {
            Assertions.assertThat(beregnetAndelHistorikk.andel.tilleggsstønad).isEqualTo(it)
        }
        forventetHistorikkEndring.kontantstøtte?.let {
            Assertions.assertThat(beregnetAndelHistorikk.andel.kontantstøtte).isEqualTo(it)
        }
        forventetHistorikkEndring.antallBarn?.let {
            Assertions.assertThat(beregnetAndelHistorikk.andel.antallBarn).isEqualTo(it)
        }
        forventetHistorikkEndring.utgifter?.let {
            Assertions.assertThat(beregnetAndelHistorikk.andel.utgifter.toInt()).isEqualTo(it)
        }
        forventetHistorikkEndring.arbeidAktivitet?.let {
            Assertions.assertThat(beregnetAndelHistorikk.aktivitetArbeid?.name).isEqualTo(it.name)
        }
        Assertions.assertThat(beregnetAndelHistorikk.aktivitet).isEqualTo(forventetHistorikkEndring.aktivitetType)
    }

}