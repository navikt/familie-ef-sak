package no.nav.familie.ef.sak.regler

fun regelIder(vararg regelSteg: RegelSteg) = regelSteg.map { it.regelId }.toSet()