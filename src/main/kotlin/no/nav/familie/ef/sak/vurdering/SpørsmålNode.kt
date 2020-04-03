package no.nav.familie.ef.sak.vurdering


data class SpørsmålNode<T>(
        val beskrivelse: String,
        val identifikator: String = "",
        val gjørEvaluering: T.() -> Evaluering,
        val hvisJa: T.() -> Evaluering,
        val hvisNei: T.() -> Evaluering,

        val children: List<SpørsmålNode<T>> = emptyList()) {

    fun evaluer(t: T): Evaluering {
        val evaluering = t.gjørEvaluering()
        return if (evaluering.resultat == Resultat.NEI) {
            evaluering og t.hvisNei()
        } else {
            evaluering og t.hvisJa()
        }
    }
}
