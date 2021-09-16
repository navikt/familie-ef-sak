package no.nav.familie.ef.sak.vedtak.domain

import org.springframework.data.annotation.Id
import java.time.LocalDate
import java.util.UUID

data class MellomlagretBrev(@Id
                            val behandlingId: UUID,
                            val brevverdier: String,
                            val brevmal: String,
                            val sanityVersjon: String,
                            val opprettetTid: LocalDate)
