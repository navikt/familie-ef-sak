package no.nav.familie.ef.sak.repository.domain.søknad

data class TidligereUtdanning(val linjeKursGrad: String,
                              val nårVarSkalDuVæreElevStudent: MånedÅrPeriode)

data class GjeldendeUtdanning(val linjeKursGrad: String,
                              val nårVarSkalDuVæreElevStudent: Datoperiode)
