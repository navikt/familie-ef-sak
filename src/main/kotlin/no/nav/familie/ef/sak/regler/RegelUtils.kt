package no.nav.familie.ef.sak.regler

fun regelIds(vararg regelSteg: RegelSteg) = regelSteg.map { it.regelId }.toSet()