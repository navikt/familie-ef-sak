package no.nav.familie.ef.sak.ekstern

import no.nav.familie.ef.sak.tilkjentytelse.domain.AndelTilkjentYtelse
import no.nav.familie.kontrakter.felles.ef.PeriodeOvergangsstønad


fun AndelTilkjentYtelse.tilEksternPeriodeOvergangsstønad() =
        PeriodeOvergangsstønad(personIdent = this.personIdent,
                               fomDato = this.stønadFom,
                               tomDato = this.stønadTom,
                               datakilde = PeriodeOvergangsstønad.Datakilde.EF)
