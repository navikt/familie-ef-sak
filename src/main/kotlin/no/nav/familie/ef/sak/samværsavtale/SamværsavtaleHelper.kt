package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.brev.dto.Avsnitt
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsuke
import no.nav.familie.ef.sak.samværsavtale.dto.tilSamværsandelerPerDag

object SamværsavtaleHelper {
    fun lagAvsnittFritekstbrev(
        ukenummer: Int,
        uke: Samværsuke,
    ) = Avsnitt(
        deloverskrift = "Uke $ukenummer",
        innhold = lagAvsnittInnhold(uke),
    )

    private fun lagAvsnittInnhold(uke: Samværsuke) =
        uke
            .tilSamværsandelerPerDag()
            .mapIndexed { dagIndex, samværsandeler ->
                val ukedag = indexTilDagMap.getValue(dagIndex)
                "$ukedag ${utledSumSamværsandelTekst(samværsandeler)}${tilVisningstekst(samværsandeler)}"
            }.joinToString(separator = "\n")

    private val indexTilDagMap =
        mapOf(
            0 to "Mandag",
            1 to "Tirsdag",
            2 to "Onsdag",
            3 to "Torsdag",
            4 to "Fredag",
            5 to "Lørdag",
            6 to "Søndag",
        )

    private fun tilVisningstekst(samværsandeler: List<Samværsandel>): String =
        when {
            samværsandeler.isEmpty() -> ""
            samværsandeler.size == 1 -> samværsandeler.first().visningsnavn
            samværsandeler.size == 4 -> "hele dagen"
            else -> samværsandeler.dropLast(1).joinToString { it.visningsnavn } + " og " + samværsandeler.last().visningsnavn
        }

    private fun utledSumSamværsandelTekst(samværsandeler: List<Samværsandel>): String {
        val sum = samværsandeler.sumOf { it.verdi }

        return when {
            sum == 0 -> "(0)"
            sum % 8 == 0 -> "(${sum / 8}) - "
            else -> "($sum/8) - "
        }
    }
}
