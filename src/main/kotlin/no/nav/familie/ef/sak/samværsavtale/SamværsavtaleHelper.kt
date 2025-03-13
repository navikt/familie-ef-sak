package no.nav.familie.ef.sak.samværsavtale

import no.nav.familie.ef.sak.brev.dto.Avsnitt
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsandel
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsdag
import no.nav.familie.ef.sak.samværsavtale.domain.Samværsuke
import no.nav.familie.ef.sak.samværsavtale.dto.sumSamværDag
import no.nav.familie.ef.sak.samværsavtale.dto.tilSamværsandelerPerDag
import no.nav.familie.ef.sak.samværsavtale.dto.tilVisningstekst

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
                if (samværsandeler.isEmpty()) {
                    "$ukedag: -"
                } else {
                    "$ukedag (${samværsandeler.sumSamværDag()}) - ${samværsandeler.tilVisningstekst()}"
                }
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
}