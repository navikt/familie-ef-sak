package no.nav.familie.ef.sak.behandling.domain

import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.queryForObject
import org.springframework.stereotype.Component

@Component
class EksternBehandlingIdGenerator(
    private val jdbcTemplate: JdbcTemplate,
) : BeforeConvertCallback<Behandling> {
    override fun onBeforeConvert(behandling: Behandling): Behandling {
        if (behandling.eksternId == 0L) {
            val id =
                jdbcTemplate.queryForObject<Long>("SELECT nextval('behandling_ekstern_id_seq')") ?: throw IllegalStateException("Sekvens behandling_ekstern_id_seq returnerte null, det skal ikke kunne skje.")
            return behandling.copy(eksternId = id)
        }
        return behandling
    }
}
