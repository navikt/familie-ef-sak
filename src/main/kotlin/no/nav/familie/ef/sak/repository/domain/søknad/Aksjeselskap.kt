package no.nav.familie.ef.sak.repository.domain.søknad

class Aksjeselskap(val navn: String,
                   val arbeidsmengde: Int? = null)

/**
 * Arbeidsmengde skal ikke fylles ut av Barnetilsyn
 */
