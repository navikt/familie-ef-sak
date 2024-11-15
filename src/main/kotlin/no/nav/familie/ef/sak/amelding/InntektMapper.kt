package no.nav.familie.ef.sak.amelding

import no.nav.familie.ef.sak.amelding.ekstern.AMeldingInntekt
import no.nav.familie.ef.sak.amelding.ekstern.Aktør
import no.nav.familie.ef.sak.amelding.ekstern.AktørType
import no.nav.familie.ef.sak.amelding.ekstern.HentInntektListeResponse
import no.nav.familie.ef.sak.felles.kodeverk.CachedKodeverkService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.ereg.EregService
import no.nav.familie.kontrakter.felles.kodeverk.InntektKodeverkType
import org.springframework.stereotype.Component
import java.time.YearMonth
import no.nav.familie.ef.sak.amelding.ekstern.InntektType as EksternInntektType

@Component
class InntektMapper(
    private val kodeverkService: CachedKodeverkService,
    private val eregService: EregService,
) {
    fun mapInntekt(response: HentInntektListeResponse): AMeldingInntektDto =
        AMeldingInntektDto(
            inntektPerVirksomhet = mapOrganisasjoner(response),
            avvik = mapAvvik(response),
        )

    private fun mapOrganisasjoner(response: HentInntektListeResponse): List<InntektForVirksomhetDto> {
        val inntektPerMånedOgAktør = mapInntektresponseTilInntektPerVirksomhetOgPeriode(response)
        val organisasjoner = hentOrganisasjoner(inntektPerMånedOgAktør.keys)

        return inntektPerMånedOgAktør.map { entry ->
            InntektForVirksomhetDto(
                identifikator = entry.key.identifikator,
                navn = organisasjoner[entry.key.identifikator] ?: "Ukjent",
                inntektPerMåned =
                    entry.value.entries.associate { inntektEntry ->
                        inntektEntry.key to
                            InntektPerMånedDto(
                                totalbeløp = inntektEntry.value.sumOf { it.beløp },
                                inntekt = mapInntekt(inntektEntry.value),
                            )
                    },
            )
        }
    }

    private fun mapInntektresponseTilInntektPerVirksomhetOgPeriode(response: HentInntektListeResponse): MutableMap<Aktør, MutableMap<YearMonth, MutableList<AMeldingInntekt>>> {
        val map: MutableMap<Aktør, MutableMap<YearMonth, MutableList<AMeldingInntekt>>> = mutableMapOf()
        response.arbeidsinntektMåned?.forEach { arbeidsInntektMaaned ->
            arbeidsInntektMaaned.arbeidsInntektInformasjon?.inntektListe?.forEach { inntekt ->
                map
                    .getOrPut(inntekt.virksomhet) { mutableMapOf() }
                    .getOrPut(arbeidsInntektMaaned.årMåned) { mutableListOf() }
                    .add(inntekt)
            }
        }
        return map
    }

    private fun hentOrganisasjoner(aktører: Set<Aktør>): Map<String, String> {
        val organisasjonsnumre =
            aktører
                .filter { it.aktørType == AktørType.ORGANISASJON }
                .map { it.identifikator }
        return eregService
            .hentOrganisasjoner(organisasjonsnumre)
            .associate { it.organisasjonsnummer to it.navn }
    }

    private fun mapInntekt(list: List<AMeldingInntekt>) =
        list.map { inntekt ->
            InntektDto(
                beløp = inntekt.beløp,
                beskrivelse = inntekt.beskrivelse?.let { hentMapping(mapInntektTypeTilKodeverkType(inntekt.inntektType), it) },
                fordel = Fordel.fraVerdi(inntekt.fordel),
                type = mapInntektType(inntekt.inntektType),
                kategori =
                    inntekt.tilleggsinformasjon?.kategori?.let {
                        hentMapping(InntektKodeverkType.TILLEGSINFORMASJON_KATEGORI, it)
                    },
                opptjeningsland = inntekt.opptjeningsland,
                opptjeningsperiodeFom = inntekt.opptjeningsperiodeFom,
                opptjeningsperiodeTom = inntekt.opptjeningsperiodeTom,
            )
        }

    private fun hentMapping(
        type: InntektKodeverkType,
        verdi: String,
    ) = kodeverkService.hentInntekt()[type]?.get(verdi) ?: "$verdi (mangler verdi i kodeverk)"

    private fun mapInntektType(type: EksternInntektType): InntektType =
        when (type) {
            EksternInntektType.LOENNSINNTEKT -> InntektType.LØNNSINNTEKT
            EksternInntektType.NAERINGSINNTEKT -> InntektType.NÆRINGSINNTEKT
            EksternInntektType.PENSJON_ELLER_TRYGD -> InntektType.PENSJON_ELLER_TRYGD
            EksternInntektType.YTELSE_FRA_OFFENTLIGE -> InntektType.YTELSE_FRA_OFFENTLIGE
        }

    private fun mapInntektTypeTilKodeverkType(type: EksternInntektType): InntektKodeverkType =
        when (type) {
            EksternInntektType.LOENNSINNTEKT -> InntektKodeverkType.LOENNSINNTEKT
            EksternInntektType.NAERINGSINNTEKT -> InntektKodeverkType.NAERINGSINNTEKT
            EksternInntektType.PENSJON_ELLER_TRYGD -> InntektKodeverkType.PENSJON_ELLER_TRYGD
            EksternInntektType.YTELSE_FRA_OFFENTLIGE -> InntektKodeverkType.YTELSE_FRA_OFFENTLIGE
        }

    private fun mapAvvik(response: HentInntektListeResponse) =
        response.arbeidsinntektMåned
            ?.flatMap { it.avvikListe ?: emptyList() }
            ?.map { "${it.virksomhet.identifikator} (${it.avvikPeriode}) - ${it.tekst}" }
            ?: emptyList()
}
