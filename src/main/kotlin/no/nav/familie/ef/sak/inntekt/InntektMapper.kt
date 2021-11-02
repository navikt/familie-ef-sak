package no.nav.familie.ef.sak.inntekt

import no.nav.familie.ef.sak.felles.kodeverk.CachedKodeverkService
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkType
import org.springframework.stereotype.Component
import java.time.YearMonth

@Component
class InntektMapper(
        private val kodeverkService: CachedKodeverkService,
) {

    fun mapInntektTypeTilKodeverkType(response: HentInntektListeResponse): InntektResponseDto {
        val map: MutableMap<Aktoer, MutableMap<YearMonth, MutableList<Inntekt>>> = mutableMapOf()
        val avvik = mapAvvik(response)
        response.arbeidsInntektMaaned?.let { arbeidsInntektMaaned ->
            arbeidsInntektMaaned.forEach { inntektMaaned ->
                inntektMaaned.arbeidsInntektInformasjon?.inntektListe?.forEach { inntekt ->
                    map.getOrPut(inntekt.virksomhet) { mutableMapOf() }
                            .getOrPut(inntektMaaned.aarMaaned) { mutableListOf() }
                            .add(inntekt)
                }
            }
        }
        return InntektResponseDto(organisasjoner = mapOrganisasjoner(map),
                                  avvik = avvik)

    }

    private fun mapOrganisasjoner(map: Map<Aktoer, MutableMap<YearMonth, MutableList<Inntekt>>>) =
            map.map { entry ->
                OrganisasjonInntektDto(
                        orgNr = entry.key.identifikator,
                        orgNavn = entry.key.identifikator, // TODO
                        inntektPerMåned = entry.value.entries.associate { inntektEntry ->
                            inntektEntry.key to InntektPerMånedDto(totalbeløp = inntektEntry.value.sumOf { it.beloep },
                                                                   inntekt = mapInntekt(inntektEntry.value))
                        }
                )
            }

    private fun mapInntekt(list: List<Inntekt>) = list.map { inntekt ->
        InntektDto(
                beløp = inntekt.beloep,
                beskrivelse = inntekt.beskrivelse?.let { hentMapping(mapInntektTypeTilKodeverkType(inntekt.inntektType), it) },
                fordel = inntekt.fordel,
                type = inntekt.inntektType,
                kategori = inntekt.tilleggsinformasjon?.kategori?.let {
                    hentMapping(InntektKodeverkType.TILLEGSINFORMASJON_KATEGORI, it)
                },
                opptjeningsland = inntekt.opptjeningsland,
                opptjeningsperiodeFom = inntekt.opptjeningsperiodeFom,
                opptjeningsperiodeTom = inntekt.opptjeningsperiodeTom
        )
    }

    private fun hentMapping(type: InntektKodeverkType, verdi: String) =
            kodeverkService.hentInntekt()[type]?.get(verdi) ?: "$verdi (savner verdi i kodeverk)"

    private fun mapInntektTypeTilKodeverkType(type: InntektType): InntektKodeverkType {
        return when (type) {
            InntektType.LOENNSINNTEKT -> InntektKodeverkType.LOENNSINNTEKT
            InntektType.NAERINGSINNTEKT -> InntektKodeverkType.NAERINGSINNTEKT
            InntektType.PENSJON_ELLER_TRYGD -> InntektKodeverkType.PENSJON_ELLER_TRYGD
            InntektType.YTELSE_FRA_OFFENTLIGE -> InntektKodeverkType.YTELSE_FRA_OFFENTLIGE
        }
    }

    private fun mapAvvik(response: HentInntektListeResponse) =
            response.arbeidsInntektMaaned
                    ?.flatMap { it.avvikListe ?: emptyList() }
                    ?.map { "${it.virksomhet.identifikator} (${it.avvikPeriode}) - ${it.tekst}" }
            ?: emptyList()

}