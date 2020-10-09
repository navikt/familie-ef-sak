package no.nav.familie.ef.sak.repository.domain.søknad

data class Arbeidssøker(val registrertSomArbeidssøkerNav: Søknadsfelt<Boolean>,
                        val villigTilÅTaImotTilbudOmArbeid: Søknadsfelt<Boolean>,
                        val kanDuBegynneInnenEnUke: Søknadsfelt<Boolean>,
                        val kanDuSkaffeBarnepassInnenEnUke: Søknadsfelt<Boolean>?,
                        val hvorØnskerDuArbeid: Søknadsfelt<String>,
                        val ønskerDuMinst50ProsentStilling: Søknadsfelt<Boolean>,
                        val ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon: Søknadsfelt<Dokumentasjon>? = null)
