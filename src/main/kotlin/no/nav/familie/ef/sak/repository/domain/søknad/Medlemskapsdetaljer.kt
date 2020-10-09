package no.nav.familie.ef.sak.repository.domain.søknad

data class Medlemskapsdetaljer(val oppholderDuDegINorge: Boolean,
                               val bosattNorgeSisteÅrene: Boolean,
                               val utenlandsopphold: List<Utenlandsopphold>? = null)
