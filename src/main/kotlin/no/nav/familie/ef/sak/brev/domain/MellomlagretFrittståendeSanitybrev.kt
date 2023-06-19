package no.nav.familie.ef.sak.brev.domain

import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("mellomlagret_frittstaende_sanitybrev")
data class MellomlagretFrittståendeSanitybrev(
    @Id
    val id: UUID = UUID.randomUUID(),
    val fagsakId: UUID,
    val brevverdier: String,
    val brevmal: String,
    val opprettetTid: LocalDateTime = LocalDateTime.now(),
    val saksbehandlerIdent: String = SikkerhetContext.hentSaksbehandler(),
)
