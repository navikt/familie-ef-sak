package no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.økonomi.domain.TilkjentYtelse
import org.springframework.data.repository.CrudRepository

interface TilkjentYtelseRepository : CrudRepository<TilkjentYtelse,Long>
