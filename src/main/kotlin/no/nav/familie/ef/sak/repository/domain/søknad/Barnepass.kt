package no.nav.familie.ef.sak.repository.domain.søknad

data class Barnepass(val årsakBarnepass: String? = null,
                     val barnepassordninger: List<BarnepassOrdning>)

data class BarnepassOrdning(val hvaSlagsBarnepassOrdning: String,
                            val navn: String,
                            @Deprecated("Bruk datoperiode") val periode: MånedÅrPeriode? = null,
                            val datoperiode: Datoperiode? = null,
                            val belop: Double)
