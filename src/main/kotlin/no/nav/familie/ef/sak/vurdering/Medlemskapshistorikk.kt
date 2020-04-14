package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPerson
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import java.time.LocalDate

class Medlemskapshistorikk(pdlPerson: PdlPerson, medlemskapsinfo: Medlemskapsinfo) {

    private val bosattAdresser = pdlPerson.bostedsadresse
            .filter { it.angittFlyttedato != null || it.folkeregistermetadata.gyldighetstidspunkt != null }
            .sortedBy { it.angittFlyttedato ?: it.folkeregistermetadata.gyldighetstidspunkt!!.toLocalDate() }

    private val bosattPerioder = mapTilBosattperioder(bosattAdresser)

    val medlemskapsperioder = overstyrMedUnntak(medlemskapsinfo)

    private fun overstyrMedUnntak(medlemskapsinfo: Medlemskapsinfo): List<Periode> {
        val unntaksperioder = mapTilUnntaksperioder(medlemskapsinfo)

        if (unntaksperioder.isEmpty()) {
            return bosattPerioder
        }

        val bosattperioderMedPlassTilUnntak = bosattPerioder.map { bosattPeriode ->
            gjørPlassTilUnntak(bosattPeriode, unntaksperioder)
        }.flatten()

        return (bosattperioderMedPlassTilUnntak + unntaksperioder).sortedBy { it.fradato }
    }

    /**
     * Gå igjennom alle unntaksperioder som berører en bosattperiode og kort inn og stykk opp bosattperiode
     * slik at det blir plass till unntaksperiodene
     */
    private fun gjørPlassTilUnntak(bosattPeriode: Periode,
                                   unntaksperioder: List<Periode>): List<Periode> {
        var periodesegmenter = listOf(bosattPeriode)
        for (unntaksperiode in unntaksperioder) {
            periodesegmenter = periodesegmenter.map { periodesegment ->
                when {
                    periodesegment.gyldig == unntaksperiode.gyldig -> {
                        listOf(periodesegment)
                    }
                    periodesegment.omsluttesAv(unntaksperiode) -> {
                        emptyList()
                    }
                    periodesegment.inneholder(unntaksperiode) -> {
                        listOf(periodesegment.copy(tildato = unntaksperiode.fradato.minusDays(1)),
                               periodesegment.copy(fradato = unntaksperiode.tildato.plusDays(1)))

                    }
                    periodesegment.inneholder(unntaksperiode.fradato) -> {
                        listOf(periodesegment.copy(tildato = unntaksperiode.fradato.minusDays(1)))
                    }
                    periodesegment.inneholder(unntaksperiode.tildato) -> {
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

    private fun mapTilUnntaksperioder(medlemskapsinfo: Medlemskapsinfo): List<Periode> {
        return medlemskapsinfo.gyldigePerioder.map { Periode(it.fom, it.tom, true) } +
               medlemskapsinfo.uavklartePerioder.map { Periode(it.fom, it.tom, null) } +
               medlemskapsinfo.avvistePerioder.map { Periode(it.fom, it.tom, false) }.sortedBy { it.fradato }
    }

    private fun mapTilBosattperioder(bosattAdresser: List<Bostedsadresse>): List<Periode> {
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

    private fun tildato(it: Bostedsadresse) = it.folkeregistermetadata.opphoerstidspunkt?.toLocalDate() ?: LocalDate.MAX

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
}