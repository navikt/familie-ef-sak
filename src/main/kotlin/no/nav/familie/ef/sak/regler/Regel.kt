package no.nav.familie.ef.sak.regler

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

    val regelNode: RegelNode
}

enum class DefaultSvar : Svar {
    JA,
    NEI
}

fun jaNeiMapping(hvisJa: RegelNode = SluttRegel.OPPFYLT,
                 hvisNei: RegelNode = SluttRegel.IKKE_OPPFYLT): Map<Svar, RegelNode> =
        mapOf(DefaultSvar.JA to hvisJa,
              DefaultSvar.NEI to hvisNei)