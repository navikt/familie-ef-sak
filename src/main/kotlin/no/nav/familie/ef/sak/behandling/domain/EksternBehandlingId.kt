package no.nav.familie.ef.sak.behandling.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("behandling_ekstern")
data class EksternBehandlingId(@Id
                               val id: Long = 0)