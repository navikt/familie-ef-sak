package no.nav.familie.ef.sak.regler

enum class BegrunnelseType {
    UTEN,
    PÅKREVD,
    VALGFRI
}

enum class Resultat {
    OPPFYLT,
    IKKE_OPPFYLT
}

fun jaNeiMapping(hvisJa: RegelNode = SluttRegel.OPPFYLT,
                 hvisNei: RegelNode = SluttRegel.IKKE_OPPFYLT): Map<SvarId, RegelNode> =
        mapOf(SvarId.JA to hvisJa,
              SvarId.NEI to hvisNei)