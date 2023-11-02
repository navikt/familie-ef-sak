package no.nav.familie.ef.sak.vilkår.regler.vilkår

import java.time.LocalDate

object AlderPåBarnRegelUtil {
    fun harFullførtFjerdetrinn(fødselsdato: LocalDate, datoForBeregning: LocalDate = LocalDate.now()): Boolean {
        val alder = datoForBeregning.year - fødselsdato.year
        var skoletrinn = alder - 5 // Begynner på skolen i det året de fyller 6
        if (datoForBeregning.month.plus(1).value < 6) { // Legger til en sikkerhetsmargin på 1 mnd tilfelle de fyller år mens saken behandles
            skoletrinn--
        }
        return skoletrinn > 4
    }
}
