package no.nav.familie.ef.sak.næringsinntektskontroll

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime
import java.util.UUID

@Table("naeringsinntekt_kontroll")
data class NæringsinntektKontrollDomain(
    @Id
    val id: UUID = UUID.randomUUID(),
    val oppgaveId: Long,
    val fagsakId: UUID,
    @Column("kjoretidspunkt")
    val kjøretidspunkt: LocalDateTime = LocalDateTime.now(),
    val utfall: NæringsinntektKontrollUtfall,
)

enum class NæringsinntektKontrollUtfall {
    KONTROLLERES_IKKE,
    OPPFYLLER_IKKE_AKTIVITETSPLIKT,
    MINIMUM_TI_PROSENT_ENDRING_I_INNTEKT,
    UENDRET_INNTEKT,
}
