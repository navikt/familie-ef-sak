package no.nav.familie.ef.sak.brev

import no.nav.familie.ef.sak.infrastruktur.exception.brukerfeilHvisIkke

object BrevmottakerUtil {
    fun validerUnikeBrevmottakere(brevmottakereDto: BrevmottakereDto) {
        val personmottakerIdenter = brevmottakereDto.personer.map { it.personIdent }
        brukerfeilHvisIkke(personmottakerIdenter.distinct().size == personmottakerIdenter.size) {
            "En person kan bare legges til en gang som brevmottaker"
        }

        val organisasjonsmottakerIdenter = brevmottakereDto.organisasjoner.map { it.organisasjonsnummer }
        brukerfeilHvisIkke(organisasjonsmottakerIdenter.distinct().size == organisasjonsmottakerIdenter.size) {
            "En organisasjon kan bare legges til en gang som brevmottaker"
        }
    }
}
