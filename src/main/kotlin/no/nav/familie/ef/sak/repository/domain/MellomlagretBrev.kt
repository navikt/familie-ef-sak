package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import java.util.UUID

data class MellomlagretBrev(@Id
                            val behandlingId: UUID,
                            val brevverdier: String,
                            val brevmal: String,
                            val versjon: String)
