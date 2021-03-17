package no.nav.familie.ef.sak.regler

import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat

enum class BegrunnelseType {
    UTEN,
    PÅKREVD,
    VALGFRI
}

enum class Resultat(val vilkårsresultat: Vilkårsresultat) {
    OPPFYLT(Vilkårsresultat.OPPFYLT),
    IKKE_OPPFYLT(Vilkårsresultat.IKKE_OPPFYLT)
}

fun jaNeiMapping(hvisJa: RegelNode = SluttRegel.OPPFYLT,
                 hvisNei: RegelNode = SluttRegel.IKKE_OPPFYLT): Map<SvarId, RegelNode> =
        mapOf(SvarId.JA to hvisJa,
              SvarId.NEI to hvisNei)