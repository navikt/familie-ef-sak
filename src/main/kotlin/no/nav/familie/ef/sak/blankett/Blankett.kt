package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.repository.domain.Fil
import org.springframework.data.annotation.Id
import java.util.UUID

data class Blankett(@Id
                    val behandlingId: UUID,
                    val pdf: Fil)