package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import java.util.*

data class Brev(@Id
                val id: UUID = UUID.randomUUID(),
                val behandling: UUID,
                val pdf: ByteArray)
