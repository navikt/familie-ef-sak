package no.nav.familie.ef.sak.repository.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.Table
import java.util.*

@Table("behandling")
data class Behandling(@Id
                      val id: UUID   = UUID.randomUUID(),
                      @Column("fagsak_id")
                      val fagsakId: UUID,

                      val versjon: Int = 0,
                      val aktiv: Boolean = true,

                      val type: BehandlingType,
                      val opprinnelse: BehandlingOpprinnelse,
                      val status: BehandlingStatus,
                      val steg: BehandlingSteg,

                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                      val sporbar: Sporbar = Sporbar())

enum class BehandlingOpprinnelse {
    MANUELL,
    AUTOMATISK_VED_JOURNALFØRING
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    REVURDERING("Revurdering"),
    KLAGE("Klage"),
    TEKNISK_OPPHØR("Teknisk opphør")
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    FERDIGSTILT,
}

enum class BehandlingSteg {
    KOMMER_SENDERE
}
