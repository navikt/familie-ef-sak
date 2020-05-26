package no.nav.familie.ef.sak.vurdering.medlemskap

import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPerson
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import java.time.LocalDate
import java.time.Period

class Medlemskapshistorikk(pdlPerson: PdlPerson, medlemskapsinfo: Medlemskapsinfo) {

    val medlemskapsperioder = byggMedlemskapsperioder(pdlPerson, medlemskapsinfo)

    private fun byggMedlemskapsperioder(pdlPerson: PdlPerson,
                                        medlemskapsinfo: Medlemskapsinfo): List<Periode> {

        val bosattPerioder = mapTilBosattperioder(pdlPerson.bostedsadresse)
        val unntaksperioder =
                medlemskapsinfo.gyldigePerioder.map { Periode(it.fom, it.tom, true) } +
                medlemskapsinfo.uavklartePerioder.map { Periode(it.fom, it.tom, null) } +
                medlemskapsinfo.avvistePerioder.map { Periode(it.fom, it.tom, false) }.sortedBy { it.fradato }

        if (unntaksperioder.isEmpty()) {
            return bosattPerioder
        }

        val bosattperioderMedPlassTilUnntak = bosattPerioder.map { bosattPeriode ->
            gjørPlassTilUnntak(bosattPeriode, unntaksperioder)
        }.flatten()
        val bosattperioderMedUnntak = (bosattperioderMedPlassTilUnntak + unntaksperioder).sortedBy { it.fradato }


        return fusjonerLikeKonsekutivePerioder(bosattperioderMedUnntak)
    }

    /**
     * Slår sammen alle påfølgende perioder med samme status slik at den resulterende listen alltid vil ha
     * forskjellig status mellom påfølgende perioder.
     */
    private fun fusjonerLikeKonsekutivePerioder(bosattperioderMedUnntak: List<Periode>): List<Periode> {

        var periodeTilFusjonering: Periode? = null
        val fusjonertePerioder = ArrayList<Periode>()
        for (medlemskapsperiode in bosattperioderMedUnntak) {

            periodeTilFusjonering = when {
                periodeTilFusjonering == null -> { // Opprett ny periode
                    medlemskapsperiode
                }
                periodeTilFusjonering.gyldig == medlemskapsperiode.gyldig -> {
                    // Like statuser, utvid periode
                    periodeTilFusjonering.copy(tildato = medlemskapsperiode.tildato)
                }
                else -> { // ulik status. Ferdig fusjonert periode legges til liste
                    fusjonertePerioder.add(periodeTilFusjonering)
                    medlemskapsperiode
                }
            }
        }
        if (periodeTilFusjonering != null) {
            fusjonertePerioder.add(periodeTilFusjonering)
        }
        return fusjonertePerioder.toList() // konverter til umuterbar liste.
    }

    /**
     * Gå igjennom alle unntaksperioder som berører en bosattperiode og kort inn og stykk opp bosattperiode
     * slik at det blir plass til unntaksperiodene
     */
    private fun gjørPlassTilUnntak(bosattPeriode: Periode,
                                   unntaksperioder: List<Periode>): List<Periode> {
        var periodesegmenter = listOf(bosattPeriode)
        for (unntaksperiode in unntaksperioder) {
            periodesegmenter = periodesegmenter.map { periodesegment ->
                when {
                    periodesegment.omsluttesAv(unntaksperiode) -> {
                        // Unntaket dekker hele bosattperioden, så vi forkaster den.
                        emptyList()
                    }
                    periodesegment.inneholder(unntaksperiode) -> {
                        // Unntaket befinner seg helhetlig inne i en bosattperiode, så vi deler den i to.
                        listOf(periodesegment.copy(tildato = unntaksperiode.fradato.minusDays(1)),
                               periodesegment.copy(fradato = unntaksperiode.tildato.plusDays(1)))

                    }
                    periodesegment.inneholder(unntaksperiode.fradato) -> {
                        // Unntaket dekker slutten av bosattperioden, så vi avkorter med en tidligere tildato.
                        listOf(periodesegment.copy(tildato = unntaksperiode.fradato.minusDays(1)))
                    }
                    periodesegment.inneholder(unntaksperiode.tildato) -> {
                        // Unntaket dekker slutten av bosattperioden, så vi avkorter med en senere fradato.
                        listOf(periodesegment.copy(fradato = unntaksperiode.tildato.plusDays(1)))
                    }
                    else -> {
                        listOf(periodesegment)
                    }
                }
            }.flatten()
        }
        return periodesegmenter
    }

    private fun mapTilBosattperioder(bostedsadresser: List<Bostedsadresse>): List<Periode> {
        val bosattAdresser = bostedsadresser
                .filter { it.angittFlyttedato != null || it.folkeregistermetadata.gyldighetstidspunkt != null }
                .sortedBy { it.angittFlyttedato ?: it.folkeregistermetadata.gyldighetstidspunkt!!.toLocalDate() }


        var periode: Periode? = null
        var forrigePeriode: Periode? = null
        val bosattperioder = ArrayList<Periode>()
        for (bostedsadresse in bosattAdresser) {

            if (periode == null) { // Opprett ny periode
                periode = Periode(fradato(bostedsadresse), tildato(bostedsadresse), true)
                if (forrigePeriode == null) {
                    // legg til innledende opphold i bosattperioder
                    bosattperioder.add(Periode(LocalDate.MIN, periode.fradato.minusDays(1), false))
                } else if ((forrigePeriode.tildato.plusDays(1)) != periode.fradato) {
                    // legg til opphold i bosattperioder
                    bosattperioder.add(Periode(forrigePeriode.tildato.plusDays(1),
                                               periode.fradato.minusDays(1),
                                               false))
                }
            } else {
                // sammenhengende perioder. Oppdater med ny tildato
                periode = periode.copy(tildato = tildato(bostedsadresse))
            }

            if (periode.tildato != LocalDate.MAX) {
                bosattperioder.add(periode)
                forrigePeriode = periode
                periode = null
            }
        }
        if (periode == null) {
            // Avsluttende opphold
            bosattperioder.add(Periode(forrigePeriode?.tildato?.plusDays(1) ?: LocalDate.MIN,
                                       LocalDate.MAX,
                                       false))
        } else {
            bosattperioder.add(periode)
        }

        return bosattperioder.toList()
    }

    private fun tildato(it: Bostedsadresse) = it.folkeregistermetadata.opphørstidspunkt?.toLocalDate() ?: LocalDate.MAX

    private fun fradato(it: Bostedsadresse) = it.angittFlyttedato ?: it.folkeregistermetadata.gyldighetstidspunkt!!.toLocalDate()

}

data class Periode(val fradato: LocalDate, val tildato: LocalDate, val gyldig: Boolean?) {

    fun inneholder(date: LocalDate): Boolean {
        return (!date.isBefore(fradato) && !date.isAfter(tildato))
    }

    fun inneholder(periode: Periode): Boolean {
        return (periode.fradato.isAfter(this.fradato) && periode.tildato.isBefore(this.tildato))
    }

    fun omsluttesAv(periode: Periode): Boolean {
        return (!periode.fradato.isAfter(this.fradato) && !periode.tildato.isBefore(this.tildato))
    }

    val lengde: Period = Period.between(fradato, tildato)
}