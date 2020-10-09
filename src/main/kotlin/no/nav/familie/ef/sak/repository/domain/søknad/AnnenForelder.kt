package no.nav.familie.ef.sak.repository.domain.søknad

data class AnnenForelder(val ikkeOppgittAnnenForelderBegrunnelse: String? = null,
                         val bosattNorge: Boolean? = null,
                         val land: String? = null,
                         val person: PersonMinimum? = null)

