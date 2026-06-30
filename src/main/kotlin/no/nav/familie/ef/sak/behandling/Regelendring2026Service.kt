package no.nav.familie.ef.sak.behandling

import no.nav.familie.ef.sak.behandling.domain.Regelendring2026
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class Regelendring2026Service(
    private val regelendring2026Repository: Regelendring2026Repository,
) {
    fun hent(behandlingId: UUID): Regelendring2026? = regelendring2026Repository.findByBehandlingId(behandlingId)

    fun oppdaterBegrunnelse(
        behandlingId: UUID,
        begrunnelse: String,
    ) {
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
