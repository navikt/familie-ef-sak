package no.nav.familie.ef.sak.inntekt

import java.time.LocalDate
import java.time.YearMonth

data class InntektResponseDto(
        val organisasjoner: List<OrganisasjonInntektDto>,
        val avvik: List<String>
)

data class OrganisasjonInntektDto(
        val orgNr: String,
        val orgNavn: String,
        val inntektPerMåned: Map<YearMonth, InntektPerMånedDto>
)

data class InntektPerMånedDto(
        val totalbeløp: Int,
        val inntekt: List<InntektDto>
)

/**
 * beskrivelse hentes fra kodeverk
 *  Lønnsinntekt -> Loennsbeskrivelse
 *  Næringsinntekt -> Naeringsinntektsbeskrivelse
 *  Pensjon eller trygd -> PensjonEllerTrygdeBeskrivelse
 *  Ytelse fra offentlig -> YtelseFraOffentligeBeskrivelse
 */
data class InntektDto(
        val beløp: Int,
        val beskrivelse: String?, // kodeverk
        val fordel: String?, // kontantytelse / Naturalytelse / Utgiftsgodtgjørelse
        val type: InntektType,
        val kategori: String?, // Kodeverk - EDAGTilleggsinfoKategorier
        val opptjeningsland: String? = null,
        val opptjeningsperiodeFom: LocalDate? = null,
        val opptjeningsperiodeTom: LocalDate? = null,
)