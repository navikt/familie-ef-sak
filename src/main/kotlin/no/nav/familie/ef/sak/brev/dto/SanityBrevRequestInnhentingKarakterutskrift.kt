package no.nav.familie.ef.sak.brev.dto

data class SanityBrevRequestInnhentingKarakterutskrift(val flettefelter: Flettefelter)

data class Flettefelter(val navn: List<String>, val fodselsnummer: List<String>)
