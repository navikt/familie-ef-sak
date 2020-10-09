package no.nav.familie.ef.sak.repository.domain.søknad

data class TidligereUtdanning(val linjeKursGrad: Søknadsfelt<String>,
                              val nårVarSkalDuVæreElevStudent: Søknadsfelt<MånedÅrPeriode>)

data class GjeldendeUtdanning(val linjeKursGrad: Søknadsfelt<String>,
                              val nårVarSkalDuVæreElevStudent: Søknadsfelt<Datoperiode>)
