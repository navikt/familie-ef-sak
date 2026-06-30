package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Regelendring2026
import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvis
import no.nav.familie.ef.sak.oppgave.TilordnetRessursService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class Regelendring2026Service(
    private val regelendring2026Repository: Regelendring2026Repository,
    private val behandlingService: BehandlingService,
    private val tilordnetRessursService: TilordnetRessursService,
) {
    fun hent(behandlingId: UUID): Regelendring2026? = regelendring2026Repository.findByBehandlingId(behandlingId)

    fun oppdaterBegrunnelse(
        behandlingId: UUID,
        begrunnelse: String,
    ) {
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        brukerfeilHvis(saksbehandling.status.behandlingErLåstForVidereRedigering()) {
            "Behandling er låst for videre redigering"
        }
        brukerfeilHvis(!tilordnetRessursService.tilordnetRessursErInnloggetSaksbehandler(behandlingId)) {
            "Behandlingen eies av en annen saksbehandler"
        }

        val eksisterende = hent(behandlingId)
        if (eksisterende != null) {
            regelendring2026Repository.update(
                eksisterende.copy(begrunnelse = begrunnelse),
            )
        } else {
            regelendring2026Repository.insert(
                Regelendring2026(
                    behandlingId = behandlingId,
                    begrunnelse = begrunnelse,
                ),
            )
        }
    }
}
