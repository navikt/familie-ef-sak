package no.nav.familie.ef.sak.samværsavtale.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table("samvaersavtale")
data class Samværsavtale(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val behandlingBarnId: UUID,
    val uker: SamværsukeWrapper,
)

data class SamværsukeWrapper(
    val uker: List<Samværsuke>,
)

data class Samværsuke(
    val mandag: Samværsdag,
    val tirsdag: Samværsdag,
    val onsdag: Samværsdag,
    val torsdag: Samværsdag,
    val fredag: Samværsdag,
    val lørdag: Samværsdag,
    val søndag: Samværsdag,
)

data class Samværsdag(
    val andeler: List<Samværsandel>,
)

enum class Samværsandel(
    val verdi: Int,
    val visningsnavn: String,
) {
    KVELD_NATT(4, "kveld/natt"),
    MORGEN(1, "morgen"),
    BARNEHAGE_SKOLE(2, "barnehage/skole"),
    ETTERMIDDAG(1, "ettermiddag"),
}
