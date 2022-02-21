package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.steps

import io.cucumber.datatable.DataTable
import io.cucumber.java8.No
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.BasisDomeneParser
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.lagDefaultTilkjentYtelseFraAndel
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomeneParser.lagDefaultTilkjentYtelseUtenAndel
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.VedtakDomenebegrep
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.AndelHistorikkBeregner
import no.nav.familie.ef.sak.vedtak.AndelHistorikkDto
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import org.assertj.core.api.Assertions
import java.time.LocalDate
import java.time.LocalDateTime

class RegelSteps : No {

    private var vedtak = listOf<Vedtak>()
    private var tilkjentYtelse = mutableListOf<TilkjentYtelse>()
    private var beregnetAndelHistorikkList = listOf<AndelHistorikkDto>()

    init {
        Gitt("følgende vedtak") { dataTable: DataTable ->
            vedtak = VedtakDomeneParser.mapVedtak(dataTable)
        }

        Gitt("følgende andeler tilkjent ytelse") { dataTable: DataTable ->
            //vedtak = VedtakDomeneParser.mapVedtak(dataTable)
            tilkjentYtelse = lagDefaultTilkjentYtelseFraAndel(dataTable)
        }

        Gitt("følgende tilkjent ytelse uten andel") { dataTable: DataTable ->
            tilkjentYtelse.addAll(lagDefaultTilkjentYtelseUtenAndel(dataTable))
        }

        Når("lag andelhistorikk kjøres") {
            val behandlinger = vedtak.map { it.behandlingId }.distinct().mapIndexed { index, id ->
                behandling(id = id, opprettetTid = LocalDateTime.now().plusMinutes(index.toLong()))
            }
            beregnetAndelHistorikkList = AndelHistorikkBeregner.lagHistorikk(tilkjentYtelse, vedtak, behandlinger, null)
        }

        Så("forvent følgende historikk") { dataTable: DataTable ->
            Assertions.assertThat(dataTable.asMaps().size).isEqualTo(beregnetAndelHistorikkList.size)

            dataTable.asMaps().map {
                val endringType = BasisDomeneParser.parseEndringType(it)
                val behandlingId =
                        VedtakDomeneParser.behandlingIdTilUUID[BasisDomeneParser.parseInt(VedtakDomenebegrep.BEHANDLING_ID, it)]!!
                val stønadFra = BasisDomeneParser.parseValgfriDato(VedtakDomenebegrep.FRA_OG_MED_DATO, it) ?: LocalDate.now()
                val stønadTil = BasisDomeneParser.parseValgfriDato(VedtakDomenebegrep.TIL_OG_MED_DATO, it) ?: LocalDate.now().plusYears(1)
                val endretIBehandlingId = VedtakDomeneParser.behandlingIdTilUUID[BasisDomeneParser.parseValgfriInt(VedtakDomenebegrep.ENDRET_I_BEHANDLING_ID, it)]
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
            }
        }
    }
}