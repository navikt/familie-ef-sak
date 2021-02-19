package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.service.steg.StegType
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.*

data class Behandling(@Id
                      val id: UUID = UUID.randomUUID(),
                      val fagsakId: UUID,
                      @MappedCollection(idColumn = "behandling_id")
                      val eksternId: EksternBehandlingId = EksternBehandlingId(),
                      val versjon: Int = 0,
                      val aktiv: Boolean = true,

                      val type: BehandlingType,
                      var status: BehandlingStatus,
                      var steg: StegType,
                      @MappedCollection(idColumn = "behandling_id")
                      var journalposter: Set<Behandlingsjournalpost> = setOf(),

                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                      val sporbar: Sporbar = Sporbar())

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    BLANKETT("Blankett"),
    REVURDERING("Revurdering"),
    KLAGE("Klage"),
    TEKNISK_OPPHØR("Teknisk opphør")
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    FERDIGSTILT;

    fun behandlingErLåstForVidereRedigering(): Boolean =
            setOf(FATTER_VEDTAK, IVERKSETTER_VEDTAK, FERDIGSTILT).contains(this)
}
