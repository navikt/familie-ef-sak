package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad

fun AndelTilkjentYtelse.tilEksternPeriodeOvergangsstønad() =
    PeriodeOvergangsstønad(
        personIdent = this.personIdent,
        fomDato = this.periode.fom,
        tomDato = this.periode.tom,
        datakilde = PeriodeOvergangsstønad.Datakilde.EF
    )
