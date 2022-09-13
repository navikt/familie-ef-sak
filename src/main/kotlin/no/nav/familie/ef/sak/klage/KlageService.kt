package no.nav.familie.ef.sak.klage

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.klage.dto.OpprettKlageDto
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.klage.OpprettKlagebehandlingRequest
import no.nav.familie.kontrakter.felles.klage.Stønadstype
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class KlageService(
    private val behandlingService: BehandlingService,
    private val fagsakService: FagsakService,
    private val klageClient: KlageClient
) {

    fun opprettKlage(behandlingId: UUID, opprettKlageDto: OpprettKlageDto) {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val aktivIdent = fagsakService.hentAktivIdent(behandling.fagsakId)
        klageClient.opprettKlage(
            OpprettKlagebehandlingRequest(
                aktivIdent,
                Stønadstype.fraEfStønadstype(behandling.stønadstype),
                behandling.eksternId.toString(),
                behandling.eksternFagsakId.toString(),
                Fagsystem.EF,
                opprettKlageDto.mottattDato
            )
        )
    }
}
