package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.brev.dto.Avsnitt
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsavtale
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsuke
import no.nav.familie.ef.sak.samværsavtale.dto.BeregnetSamvær
import no.nav.familie.ef.sak.samværsavtale.dto.tilDto
import no.nav.familie.ef.sak.samværsavtale.dto.tilSamværsandelerPerDag
import java.math.BigDecimal
import java.math.RoundingMode

object SamværsavtaleHelper {
    val maksimalSamværsandelPerDag = Samværsandel.values().toList().sumOf { it.verdi }

    fun lagBeregnetSamvær(
        samværsavtale: Samværsavtale,
    ) = BeregnetSamvær(
        behandlingBarnId = samværsavtale.behandlingBarnId,
        uker =
            samværsavtale.uker.uker.mapIndexed { ukeIndex, samværsuke ->
                lagAvsnitt(ukeIndex + 1, samværsuke)
            },
        oppsummering = utledOppsummering(samværsavtale),
    )

    fun lagAvsnitt(
        ukenummer: Int,
        uke: Samværsuke,
    ) = Avsnitt(
        deloverskrift = "Uke $ukenummer",
        innhold = lagInnhold(uke),
    )

    private fun lagInnhold(uke: Samværsuke) =
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
            sum % maksimalSamværsandelPerDag == 0 -> "(${sum / maksimalSamværsandelPerDag}) - "
            else -> "($sum/$maksimalSamværsandelPerDag) - "
        }
    }

    private fun utledOppsummering(samværsavtale: Samværsavtale): String {
        val summertSamvær = samværsavtale.tilDto().summerTilSamværsandelerVerdiPerDag().sum()
        val maksimalSamværsandel = samværsavtale.uker.uker.size * 7 * maksimalSamværsandelPerDag

        val antallHeleDagerMedSamvær = summertSamvær / maksimalSamværsandelPerDag
        val rest = summertSamvær % maksimalSamværsandelPerDag
        val restSuffix = if (rest == 0) "" else "/$maksimalSamværsandelPerDag"

        val samværsandel = BigDecimal(summertSamvær).divide(BigDecimal(maksimalSamværsandel), 3, RoundingMode.HALF_UP)

        val visningstekstAntallDager = "$antallHeleDagerMedSamvær dager og $rest$restSuffix deler"
        val visningstekstProsentandel = "${samværsandel * BigDecimal(100).stripTrailingZeros()}%"

        return "Samvær: $visningstekstAntallDager av totalt ${samværsavtale.uker.uker.size} uker = $visningstekstProsentandel"
    }
}
