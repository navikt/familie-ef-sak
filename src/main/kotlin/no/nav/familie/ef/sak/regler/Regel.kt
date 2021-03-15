package no.nav.familie.ef.sak.regler

interface RegelId {

    val id: String
}

interface RegelIdMedBeskrivelse : RegelId {

    val beskrivelse: String
}

enum class SluttNod(override val id: String) : RegelId {
    SLUTTNOD("")
}

enum class Begrunnelse {
    UTEN,
    PÅKREVD,
    VALGFRI
}

enum class Resultat {
    OPPFYLT,
    IKKE_OPPFYLT
}

interface Svar
interface SvarMedSvarsalternativ : Svar {

    val regelNod: RegelNod
}

enum class DefaultSvar : Svar {
    JA,
    NEI
}

fun defaultSvarMapping(hvisJa: RegelNod = SluttRegel.OPPFYLT,
                       hvisNei: RegelNod = SluttRegel.IKKE_OPPFYLT): Map<Svar, RegelNod> =
        mapOf(DefaultSvar.JA to hvisJa,
              DefaultSvar.NEI to hvisNei)