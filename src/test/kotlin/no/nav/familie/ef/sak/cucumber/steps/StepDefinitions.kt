package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.lagDefaultTilkjentYtelseFraAndel
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.lagDefaultTilkjentYtelseUtenAndel
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomenebegrep
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.parseEndringType
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import org.assertj.core.api.Assertions
import java.time.LocalDateTime

class StepDefinitions {

    private var vedtak = listOf<Vedtak>()
    private var tilkjentYtelse = mutableListOf<TilkjentYtelse>()
    private var beregnetAndelHistorikkList = listOf<AndelHistorikkDto>()


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

        val behandlinger = vedtak.map { it.behandlingId }.distinct().mapIndexed { index, id ->
            behandling(id = id, opprettetTid = LocalDateTime.now().plusMinutes(index.toLong()))
        }
        beregnetAndelHistorikkList = AndelHistorikkBeregner.lagHistorikk(tilkjentYtelse, vedtak, behandlinger, null)
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