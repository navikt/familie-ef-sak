package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.cucumber.domeneparser.Domenenøkkel
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder.behandlingIdTilUUID
import no.nav.familie.ef.sak.cucumber.domeneparser.parseBehandlingstype
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriDato
import no.nav.familie.ef.sak.cucumber.domeneparser.parseValgfriInt
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser.DataTableUtil.forHverBehandling
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDateTime
import java.util.UUID

object SaksbehandlingDomeneParser {
    fun mapSaksbehandlinger(
        dataTable: DataTable,
        stønadstype: StønadType,
    ): Map<UUID, Pair<Behandling, Saksbehandling>> {
        val fagsak = fagsak(stønadstype = stønadstype)
        var forrigeBehandlingId: UUID? = null
        return dataTable
            .forHverBehandling { behandlingId, rader ->
                val rad = rader.first()
                val forrigeBehandlingIdFraRad =
                    behandlingIdTilUUID[parseValgfriInt(SaksbehandlingDomeneBegrep.FORRIGE_BEHANDLING, rad)]
                        ?: forrigeBehandlingId
                forrigeBehandlingId = behandlingId
                val behandling =
                    behandling(
                        fagsak = fagsak,
                        id = behandlingId,
                        forrigeBehandlingId = forrigeBehandlingIdFraRad,
                        type = parseBehandlingstype(rad) ?: BehandlingType.FØRSTEGANGSBEHANDLING,
                        vedtakstidspunkt =
                            parseValgfriDato(SaksbehandlingDomeneBegrep.VEDTAKSDATO, rad)?.atStartOfDay()
                                ?: LocalDateTime.now(),
                    )
                behandling.id to Pair(behandling, saksbehandling(fagsak, behandling))
            }.toMap()
    }
}

enum class SaksbehandlingDomeneBegrep(
    val nøkkel: String,
) : Domenenøkkel {
    BEHANDLINGSTYPE("Behandlingstype"),
    FORRIGE_BEHANDLING("Forrige behandling"),
    VEDTAKSDATO("Vedtaksdato"),
    ;

    override fun nøkkel(): String = nøkkel
}
