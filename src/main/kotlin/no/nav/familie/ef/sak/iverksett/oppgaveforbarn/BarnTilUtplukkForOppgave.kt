package no.nav.familie.ef.sak.iverksett.oppgaveforbarn

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.UUID

data class BarnTilUtplukkForOppgave(
    @Id
    val behandlingId: UUID,
    @Column("fodselsnummer_soker")
    val fødselsnummerSøker: String,
    @Column("fodselsnummer_barn")
    val fødselsnummerBarn: String?,
    val termindatoBarn: LocalDate?,
    val fraMigrering: Boolean,
)
