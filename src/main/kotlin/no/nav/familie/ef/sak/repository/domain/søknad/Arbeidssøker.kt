package no.nav.familie.ef.sak.repository.domain.søknad

data class Arbeidssøker(val registrertSomArbeidssøkerNav: Boolean,
                        val villigTilÅTaImotTilbudOmArbeid: Boolean,
                        val kanDuBegynneInnenEnUke: Boolean,
                        val kanDuSkaffeBarnepassInnenEnUke: Boolean?,
                        val hvorØnskerDuArbeid: String,
                        val ønskerDuMinst50ProsentStilling: Boolean,
                        val ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon: Dokumentasjon? = null)
