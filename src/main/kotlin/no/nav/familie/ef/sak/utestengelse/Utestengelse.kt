package no.nav.familie.ef.sak.utestengelse

import no.nav.familie.ef.sak.felles.domain.Sporbar
import no.nav.familie.kontrakter.felles.Månedsperiode
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.UUID

/**
 * spring-data-jdbc støtter ikke composite key, men id og versjon er primary key i databasen
 */
data class Utestengelse(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Version
    val versjon: Int = 1,
    val fagsakPersonId: UUID,
    val fom: LocalDate,
    val tom: LocalDate,
    val slettet: Boolean = false,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar()
)

fun Utestengelse.tilDto() = UtestengelseDto(
    id = id,
    periode = Månedsperiode(fom, tom),
    slettet = slettet,
    opprettetAv = sporbar.opprettetAv,
    opprettetTid = sporbar.opprettetTid,
    endretAv = sporbar.endret.endretAv,
    endretTid = sporbar.endret.endretTid
)