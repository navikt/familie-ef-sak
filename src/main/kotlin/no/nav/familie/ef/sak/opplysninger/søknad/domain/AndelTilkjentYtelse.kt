package no.nav.familie.ef.sak.opplysninger.søknad.domain

import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDate
import java.util.UUID

data class AndelTilkjentYtelse(@Column("belop")
                               val beløp: Int,
                               @Column("stonad_fom")
                               val stønadFom: LocalDate, /// TODO  Gjør nullable
                               @Column("stonad_tom")
                               val stønadTom: LocalDate, /// TODO  Gjør nullable
                               val personIdent: String,
                               val inntekt: Int,
                               val inntektsreduksjon: Int,
                               val samordningsfradrag: Int,
                               val kildeBehandlingId: UUID)