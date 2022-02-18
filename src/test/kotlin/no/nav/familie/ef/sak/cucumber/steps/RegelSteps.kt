package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.No
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.lagDefaultTilkjentYtelseFraAndel
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import org.assertj.core.api.Assertions
import java.time.LocalDateTime

class RegelSteps : No {
    private var vedtak = listOf<Vedtak>()
    private var tilkjentYtelse = listOf<TilkjentYtelse>()
    private var forventetAndelHistorikk = listOf<AndelHistorikkDto>()

    init {
        Gitt("følgende vedtak") { dataTable: DataTable ->
            vedtak = VedtakDomeneParser.mapVedtak(dataTable)
        }
        Gitt("følgende andeler tilkjent ytelse") { dataTable: DataTable ->
            //vedtak = VedtakDomeneParser.mapVedtak(dataTable)
            tilkjentYtelse = lagDefaultTilkjentYtelseFraAndel(dataTable)

        }
        Når("lag andelhistorikk kjøres") {

            val behandlinger = vedtak.map { it.behandlingId }.distinct().mapIndexed { index, id ->
                behandling(id = id, opprettetTid = LocalDateTime.now().plusMinutes(index.toLong()))
            }
            forventetAndelHistorikk = AndelHistorikkBeregner.lagHistorikk(tilkjentYtelse, vedtak, behandlinger, null)
            println(forventetAndelHistorikk.size)
        }

        Så("forvent følgende historikk") { dataTable: DataTable ->
            val forventetHistorikkEndringer = VedtakDomeneParser.mapBehandlingForHistorikkEndring(dataTable)

            for (forventetBehandlingMedHistorikkEndring in forventetHistorikkEndringer) {
                val andelHistorikkDto = forventetAndelHistorikk.first { it.behandlingId == forventetBehandlingMedHistorikkEndring.first}
                if (forventetBehandlingMedHistorikkEndring.second == null) {
                    Assertions.assertThat(andelHistorikkDto.endring).isNull()
                } else {
                    Assertions.assertThat(forventetBehandlingMedHistorikkEndring.second?.behandlingId).isEqualTo(andelHistorikkDto.endring?.behandlingId)
                    Assertions.assertThat(forventetBehandlingMedHistorikkEndring.second?.type).isEqualTo(andelHistorikkDto.endring?.type)
                }
            }
        }
    }
}