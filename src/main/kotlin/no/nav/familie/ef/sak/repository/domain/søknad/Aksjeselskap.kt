package no.nav.familie.ef.sak.repository.domain.søknad

class Aksjeselskap(val navn: Søknadsfelt<String>,
                   val arbeidsmengde: Søknadsfelt<Int>? = null)

/**
 * Arbeidsmengde skal ikke fylles ut av Barnetilsyn
 */
