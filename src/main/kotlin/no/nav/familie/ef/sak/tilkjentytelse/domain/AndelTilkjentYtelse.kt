package no.nav.familie.ef.sak.tilkjentytelse.domain

import no.nav.familie.kontrakter.felles.Datoperiode
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class AndelTilkjentYtelse(
    @Column("belop")
    val beløp: Int,
    @Embedded(prefix = "stonad_", onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val periode: Datoperiode,
    val personIdent: String,
    val inntekt: Int,
    val inntektsreduksjon: Int,
    val samordningsfradrag: Int,
    val kildeBehandlingId: UUID
)
