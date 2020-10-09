package no.nav.familie.ef.sak.repository.domain.søknad

data class Medlemskapsdetaljer(val oppholderDuDegINorge: Søknadsfelt<Boolean>,
                               val bosattNorgeSisteÅrene: Søknadsfelt<Boolean>,
                               val utenlandsopphold: Søknadsfelt<List<Utenlandsopphold>>? = null)
