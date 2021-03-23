package no.nav.familie.ef.sak.regler

fun regelIder(vararg regelSteg: RegelSteg): Set<RegelId> {
    val regelIder = regelSteg.map { it.regelId }.toSet()
    if (regelIder.size != regelSteg.size) {
        error("Kan ikke ha 2 rotregler med samme regelId")
    }
    return regelIder
}