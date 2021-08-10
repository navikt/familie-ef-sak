package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class MellomlagretBrev(@Id
                            val behandlingId: UUID,
                            val brevverdier: String,
                            val brevmal: String,
                            val saksbehandlerId: String,
                            val versjon: String)
