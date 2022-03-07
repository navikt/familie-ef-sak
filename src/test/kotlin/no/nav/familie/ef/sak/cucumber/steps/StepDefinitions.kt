package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.behandlingsflyt.steg.BeregnYtelseSteg
import no.nav.familie.ef.sak.beregning.BeregningService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.mockFeatureToggleService
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.lagDefaultTilkjentYtelseFraAndel
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.lagDefaultTilkjentYtelseUtenAndel
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomenebegrep
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.parseEndringType
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import org.assertj.core.api.Assertions
import java.time.LocalDateTime
import java.util.UUID

class StepDefinitions {

    private var vedtak = listOf<Vedtak>()
    private var tilkjentYtelse = mutableListOf<TilkjentYtelse>()
    private var beregnetAndelHistorikkList = listOf<AndelHistorikkDto>()

    private val tilkjentYtelseService = mockk<TilkjentYtelseService>(relaxed = true)
    private val beregningService = BeregningService()
    private val vedtakService = mockk<VedtakService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>(relaxed = true)
    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val fagsakService = mockk<FagsakService>(relaxed = true)
    private val featureToggleService = mockFeatureToggleService()

    private val beregnYtelseSteg = BeregnYtelseSteg(tilkjentYtelseService,
                                        beregningService,
                                        simuleringService,
                                        vedtakService,
                                        tilbakekrevingService,
                                        fagsakService,
                                        featureToggleService)

    private val slot = slot<TilkjentYtelse>()

    @Gitt("følgende vedtak")
    fun følgende_vedtak(dataTable: DataTable) {
        vedtak = VedtakDomeneParser.mapVedtak(dataTable)
    }

    @Gitt("følgende andeler tilkjent ytelse")
    fun `følgende andeler tilkjent ytelse`(dataTable: DataTable) {
        tilkjentYtelse = lagDefaultTilkjentYtelseFraAndel(dataTable)
    }

    @Gitt("følgende tilkjent ytelse uten andel")
    fun `følgende tilkjent ytelse uten andel`(dataTable: DataTable) {
        tilkjentYtelse.addAll(lagDefaultTilkjentYtelseUtenAndel(dataTable))
    }

    @Når("lag andelhistorikk kjøres")
    fun `lag andelhistorikk kjøres`() {
        val tilkjentYtelser = mockTilkjentYtelse()
        val lagredeVedtak = mockLagreVedtak()

        val behandlinger = vedtak.map { it.behandlingId }.distinct().mapIndexed { index, id ->
            val behandling = behandling(id = id, opprettetTid = LocalDateTime.now().plusMinutes(index.toLong()))
             behandling.id to behandling
        }.toMap()

        vedtak.forEach {
            beregnYtelseSteg.utførSteg(behandlinger.getValue(it.behandlingId), it.tilVedtakDto())
        }
        beregnetAndelHistorikkList = AndelHistorikkBeregner.lagHistorikk(tilkjentYtelser.values.toList(), lagredeVedtak, behandlinger.values.toList(), null)
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
        val forventetHistorikkEndringer = VedtakDomeneParser.mapBehandlingForHistorikkEndring(dataTable)

        dataTable.asMaps().mapIndexed { index, it ->
            val endringType = parseEndringType(it)
            val endretIBehandlingId = VedtakDomeneParser.behandlingIdTilUUID[parseValgfriInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, it)]
            val beregnetAndelHistorikk = beregnetAndelHistorikkList[index]
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
            Assertions.assertThat(beregnetAndelHistorikk.andel.inntekt).isEqualTo(forventetHistorikkEndring.inntekt)
            Assertions.assertThat(beregnetAndelHistorikk.andel.beløp).isEqualTo(forventetHistorikkEndring.beløp)
            Assertions.assertThat(beregnetAndelHistorikk.aktivitet).isEqualTo(forventetHistorikkEndring.aktivitetType)
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

}