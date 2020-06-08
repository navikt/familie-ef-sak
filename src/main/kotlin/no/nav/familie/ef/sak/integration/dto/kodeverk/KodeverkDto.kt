package no.nav.familie.integrasjoner.kodeverk.domene

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class KodeverkDto(@JsonProperty("betydninger") val betydninger: Map<String, List<BetydningDto>>)

data class BetydningDto(@JsonProperty("gyldigFra") val gyldigFra: LocalDate,
                        @JsonProperty("gyldigTil") val gyldigTil: LocalDate,
                        @JsonProperty("beskrivelser") val beskrivelser: Map<String, BeskrivelseDto>)

data class BeskrivelseDto(@JsonProperty("term") val term: String,
                          @JsonProperty("tekst") val tekst: String)