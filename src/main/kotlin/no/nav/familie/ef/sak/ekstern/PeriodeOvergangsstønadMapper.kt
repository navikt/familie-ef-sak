package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.ef.Datakilde
import no.nav.familie.kontrakter.felles.ef.EksternPeriode

fun AndelTilkjentYtelse.tilEksternPeriodeOvergangsstønad() =
    EksternPeriode(
        personIdent = this.personIdent,
        fomDato = this.stønadFom,
        tomDato = this.stønadTom,
        datakilde = Datakilde.EF,
    )
