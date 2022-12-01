package no.nav.familie.ef.sak.brev.dto

import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevType

enum class FrittståendeBrevKategori(val frittståendeBrevType: FrittståendeBrevType) {
    INFORMASJONSBREV(frittståendeBrevType = FrittståendeBrevType.INFORMASJONSBREV),
    INNHENTING_AV_OPPLYSNINGER(frittståendeBrevType = (FrittståendeBrevType.INNHENTING_AV_OPPLYSNINGER)),
    VARSEL_OM_AKTIVITETSPLIKT(frittståendeBrevType = FrittståendeBrevType.VARSEL_OM_AKTIVITETSPLIKT),
    VARSEL_OM_SANKSJON(frittståendeBrevType = FrittståendeBrevType.VARSEL_OM_SANKSJON),
    INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE(frittståendeBrevType = FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_HOVEDPERIODE),
    INNHENTING_AV_KARAKTERUTSKRIFT_UTVIDET_PERIODE(frittståendeBrevType = FrittståendeBrevType.INNHENTING_AV_KARAKTERUTSKRIFT_UTVIDET_PERIODE),
    BREV_OM_SVARTID_KLAGE(frittståendeBrevType = FrittståendeBrevType.BREV_OM_SVARTID_KLAGE)
}
