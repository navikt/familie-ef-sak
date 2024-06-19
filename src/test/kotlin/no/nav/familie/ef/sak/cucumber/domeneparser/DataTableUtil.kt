package no.nav.familie.ef.sak.no.nav.familie.ef.sak.cucumber.domeneparser

import io.cucumber.datatable.DataTable
import no.nav.familie.ef.sak.cucumber.domeneparser.Domenebegrep
import no.nav.familie.ef.sak.cucumber.domeneparser.IdTIlUUIDHolder
import no.nav.familie.ef.sak.cucumber.domeneparser.parseInt
import java.util.UUID

object DataTableUtil {
    fun <T> DataTable.forHverBehandling(mapper: (behandlingId: UUID, rader: List<Map<String, String>>) -> T) =
        this
            .asMaps()
            .groupBy {
                IdTIlUUIDHolder.behandlingIdTilUUID[parseInt(Domenebegrep.BEHANDLING_ID, it)]!!
            }.map { (behandlingId, rader) -> mapper(behandlingId, rader) }
}
