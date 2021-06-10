package no.nav.familie.ef.sak.no.nav.familie.ef.sak.økonomi

import no.nav.familie.ef.sak.repository.domain.AndelTilkjentYtelse
import java.time.LocalDate
import java.util.UUID

fun lagAndelTilkjentYtelse(beløp: Int,
                           fraOgMed: LocalDate,
                           tilOgMed: LocalDate,
                           personIdent: String = "123",
        //periodetype: Periodetype = Periodetype.MÅNED,
                           periodeId: Long? = null,
                           forrigePeriodeId: Long? = null,
                           kildeBehandlingId: UUID? = UUID.randomUUID(),
                           inntekt: Int = 0,
                           samordningsfradrag: Int = 0,
                           inntektsreduksjon: Int = 0) =
        AndelTilkjentYtelse(beløp = beløp,
                            stønadFom = fraOgMed,
                            stønadTom = tilOgMed,
                            personIdent = personIdent,
                //periodetype = periodetype,
                            inntekt = inntekt,
                            samordningsfradrag = samordningsfradrag,
                            inntektsreduksjon = inntektsreduksjon,
                            periodeId = periodeId,
                            forrigePeriodeId = forrigePeriodeId,
                            kildeBehandlingId = kildeBehandlingId)