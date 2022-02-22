package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java.no.Gitt
import io.cucumber.java.no.Når
import io.cucumber.java.no.Så
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.lagDefaultTilkjentYtelseFraAndel
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import org.assertj.core.api.Assertions
import java.time.LocalDateTime

class StepDefinitions {

    private var vedtak = listOf<Vedtak>()
    private var tilkjentYtelse = listOf<TilkjentYtelse>()
    private var forventetAndelHistorikk = listOf<AndelHistorikkDto>()


    @Gitt("følgende vedtak")
    fun følgende_vedtak(dataTable: DataTable) {
        vedtak = VedtakDomeneParser.mapVedtak(dataTable)
    }

    @Gitt("følgende andeler tilkjent ytelse")
    fun `følgende andeler tilkjent ytelse`(dataTable: DataTable) {
        tilkjentYtelse = lagDefaultTilkjentYtelseFraAndel(dataTable)

    }

    @Når("lag andelhistorikk kjøres")
    fun `lag andelhistorikk kjøres`() {

        val behandlinger = vedtak.map { it.behandlingId }.distinct().mapIndexed { index, id ->
            behandling(id = id, opprettetTid = LocalDateTime.now().plusMinutes(index.toLong()))
        }
        forventetAndelHistorikk = AndelHistorikkBeregner.lagHistorikk(tilkjentYtelse, vedtak, behandlinger, null)
    }

    @Så("forvent følgende historikk")
    fun forvent_følgende_historik(dataTable: DataTable) {
        val forventetHistorikkEndringer = VedtakDomeneParser.mapBehandlingForHistorikkEndring(dataTable)

        for (forventetBehandlingMedHistorikkEndring in forventetHistorikkEndringer) {
            val andelHistorikkDto =
                    forventetAndelHistorikk.first { it.behandlingId == forventetBehandlingMedHistorikkEndring.id }

            if (forventetBehandlingMedHistorikkEndring.historikkEndring == null) {
                Assertions.assertThat(andelHistorikkDto.endring).isNull()
                Assertions.assertThat(andelHistorikkDto.andel.inntekt).isEqualTo(forventetBehandlingMedHistorikkEndring.inntekt)
            } else {
                Assertions.assertThat(forventetBehandlingMedHistorikkEndring.historikkEndring.behandlingId)
                        .isEqualTo(andelHistorikkDto.endring?.behandlingId)
                Assertions.assertThat(forventetBehandlingMedHistorikkEndring.historikkEndring.type)
                        .isEqualTo(andelHistorikkDto.endring?.type)
                Assertions.assertThat(forventetBehandlingMedHistorikkEndring.inntekt).isEqualTo(andelHistorikkDto.andel.inntekt)
            }
        }
    }

}