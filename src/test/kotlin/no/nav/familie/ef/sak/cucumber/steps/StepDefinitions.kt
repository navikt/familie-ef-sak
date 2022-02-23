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
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import org.assertj.core.api.Assertions
import java.time.LocalDate
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

        dataTable.asMaps().map {
            val endringType = parseEndringType(it)
            val behandlingId =
                    VedtakDomeneParser.behandlingIdTilUUID[parseInt(VedtakDomenebegrep.BEHANDLING_ID, it)]!!
            val stønadFra = parseValgfriDato(VedtakDomenebegrep.FRA_OG_MED_DATO, it) ?: LocalDate.now()
            val stønadTil = parseValgfriDato(VedtakDomenebegrep.TIL_OG_MED_DATO, it) ?: LocalDate.now().plusYears(1)
            val endretIBehandlingId = VedtakDomeneParser.behandlingIdTilUUID[parseValgfriInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, it)]
            val beregnetAndelHistorikk = beregnetAndelHistorikkList.firstOrNull {
                it.behandlingId == behandlingId &&
                it.andel.stønadFra == stønadFra &&
                it.andel.stønadTil == stønadTil
            }
            Assertions.assertThat(beregnetAndelHistorikk).isNotNull
            if (endringType == null) {
                Assertions.assertThat(beregnetAndelHistorikk?.endring).isNull()
            } else {
                Assertions.assertThat(beregnetAndelHistorikk!!.endring!!.type).isEqualTo(endringType)
                Assertions.assertThat(beregnetAndelHistorikk.endring?.behandlingId).isEqualTo(endretIBehandlingId)
            }
            Assertions.assertThat(beregnetAndelHistorikk?.andel?.inntekt).isEqualTo(forventetHistorikkEndringer.first { it.id == behandlingId }.inntekt)
            Assertions.assertThat(beregnetAndelHistorikk?.andel?.beløp).isEqualTo(forventetHistorikkEndringer.first { it.id == behandlingId }.beløp)
            Assertions.assertThat(beregnetAndelHistorikk?.aktivitet).isEqualTo(forventetHistorikkEndringer.first { it.id == behandlingId }.aktivitetType)
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