package no.nav.familie.ef.sak.vilkår.regler.vilkår

import java.time.LocalDate
import java.time.Month

object AlderPåBarnRegelUtil {

    /**
     * Et barn født i 2013 har ikke avsluttet 3'e trinn før juni det året man fyller 10
     *
     * 2013 født
     * 2014 fyller 1 år
     * ...
     * 2019 fyller 6 år - starter 1 trinn
     * 2020 fyller 7 år - fullfører 1 trinn
     * ...
     * 2023 fyller 10 år - fullfører 4 trinn i juni
     */
    fun harFullførtFjerdetrinn(fødselsdato: LocalDate, datoForBeregning: LocalDate = LocalDate.now()): Boolean {
        return if (datoForBeregning.month >= Month.JUNE) {
            datoForBeregning.year - fødselsdato.year > 9
        } else {
            datoForBeregning.year - fødselsdato.year > 10
        }
    }
}
