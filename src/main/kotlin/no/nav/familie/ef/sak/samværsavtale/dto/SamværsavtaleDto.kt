package no.nav.familie.ef.sak.samværsavtale.dto

import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsuke
import no.nav.familie.ef.sak.samværsavtale.domain.SamværsukeWrapper
import java.util.UUID

data class SamværsavtaleDto(
    val behandlingId: UUID,
    val behandlingBarnId: UUID,
    val uker: List<Samværsuke>,
) {
    fun mapTilSamværsandelerPerDag() = this.uker.flatMap { uke -> uke.tilSamværsandelerPerDag() }

    fun summerTilSamværsandelerVerdiPerDag() = mapTilSamværsandelerPerDag().map { dag -> dag.sumOf { it.verdi } }
}

fun Samværsavtale.tilDto() =
    SamværsavtaleDto(
        behandlingId = this.behandlingId,
        behandlingBarnId = this.behandlingBarnId,
        uker = this.uker.uker,
    )

fun SamværsavtaleDto.tilDomene() =
    Samværsavtale(
        behandlingId = this.behandlingId,
        behandlingBarnId = this.behandlingBarnId,
        uker = SamværsukeWrapper(uker = this.uker),
    )

fun List<Samværsavtale>.tilDto() = this.map { it.tilDto() }

fun Samværsuke.tilSamværsandelerPerDag() =
    listOf(
        this.mandag.andeler,
        this.tirsdag.andeler,
        this.onsdag.andeler,
        this.torsdag.andeler,
        this.fredag.andeler,
        this.lørdag.andeler,
        this.søndag.andeler,
    )

fun List<Samværsandel>.tilVisningstekst() = this.joinToString { it.visningsnavn }

fun List<Samværsandel>.sumSamværDag(): String {
    val sum = this.sumOf { it.verdi }

    return when {
        sum % 8 == 0 -> (sum / 8).toString()
        else -> "$sum/8"
    }
}
