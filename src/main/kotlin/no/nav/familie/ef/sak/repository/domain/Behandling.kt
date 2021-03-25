package no.nav.familie.ef.sak.repository.domain

import no.nav.familie.ef.sak.service.steg.StegType
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.UUID

data class Behandling(@Id
                      val id: UUID = UUID.randomUUID(),
                      val fagsakId: UUID,
                      @MappedCollection(idColumn = "behandling_id")
                      val eksternId: EksternBehandlingId = EksternBehandlingId(),
                      @Version
                      val versjon: Int = 0,
                      val aktiv: Boolean = true,

                      val type: BehandlingType,
                      val status: BehandlingStatus,
                      val steg: StegType,
                      @MappedCollection(idColumn = "behandling_id")
                      var journalposter: Set<Behandlingsjournalpost> = setOf(),

                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                      val sporbar: Sporbar = Sporbar(),
                      val resultat: BehandlingResultat) {

    fun kanAnnulleres(): Boolean = !status.behandlingErLåstForVidereRedigering() && type == BehandlingType.BLANKETT
}

enum class BehandlingType(val visningsnavn: String) {
    FØRSTEGANGSBEHANDLING("Førstegangsbehandling"),
    BLANKETT("Blankett"),
    REVURDERING("Revurdering"),
    KLAGE("Klage"),
    TEKNISK_OPPHØR("Teknisk opphør")
}

enum class BehandlingResultat(val displayName: String) {
    INNVILGET(displayName = "Innvilget"),
    IKKE_SATT(displayName = "Ikke satt"),
    ANNULLERT(displayName = "Annullert"),
}

enum class BehandlingStatus {
    OPPRETTET,
    UTREDES,
    FATTER_VEDTAK,
    IVERKSETTER_VEDTAK,
    FERDIGSTILT,
    ;

    fun behandlingErLåstForVidereRedigering(): Boolean =
            setOf(FATTER_VEDTAK, IVERKSETTER_VEDTAK, FERDIGSTILT).contains(this)
}
