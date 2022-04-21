package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.cucumber.domeneparser.Domenenøkkel
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.familie.ef.sak.cucumber.domeneparser.parseBehandlingstype
import no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.SaksbehandlingDomeneBegrep.BEHANDLING_ID
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.felles.ef.StønadType

object SaksbehandlingDomeneParser {


    fun mapSaksbehandlinger(dataTable: DataTable): List<Saksbehandling> = dataTable.asMaps()
            .groupBy { it.getValue(BEHANDLING_ID.nøkkel) }
            .map { (_, rader) ->
                val rad = rader.first()

                saksbehandling(
                        fagsak = fagsak(stønadstype = StønadType.BARNETILSYN),
                        id = behandlingIdTilUUID[parseInt(BEHANDLING_ID, rad)]!!,
                        forrigeBehandlingId = behandlingIdTilUUID[parseValgfriInt(SaksbehandlingDomeneBegrep.FORRIGE_BEHANDLING,
                                                                                   rad)],
                        type = parseBehandlingstype(rad) ?: BehandlingType.FØRSTEGANGSBEHANDLING,
                )
            }
}


enum class SaksbehandlingDomeneBegrep(val nøkkel: String) : Domenenøkkel {
    BEHANDLING_ID("BehandlingId"),
    BEHANDLINGSTYPE("Behandlingstype"),
    FORRIGE_BEHANDLING("Forrige behandling")

    ;

    override fun nøkkel(): String {
        return nøkkel
    }
}
