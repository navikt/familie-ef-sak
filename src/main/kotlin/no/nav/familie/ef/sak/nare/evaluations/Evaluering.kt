package no.nav.familie.ef.sak.nare.evaluations

data class Evaluering(val resultat: Resultat,
                      val begrunnelse: String,
                      val beskrivelse: String = "",
                      val identifikator: String = "",
                      val operator: Operator = Operator.INGEN,
                      var children: List<Evaluering> = emptyList()) {

    infix fun og(other: Evaluering) =
            Evaluering(resultat = resultat og other.resultat,
                       begrunnelse = "($begrunnelse OG ${other.begrunnelse})",
                       operator = Operator.OG,
                       children = this.specOrChildren() + other.specOrChildren())

    infix fun eller(other: Evaluering) =
            Evaluering(resultat = resultat eller other.resultat,
                       begrunnelse = if (resultat == Resultat.JA) begrunnelse else "($begrunnelse OG ${other.begrunnelse})",
                       operator = Operator.ELLER,
                       children = this.specOrChildren() + other.specOrChildren())

    private fun specOrChildren(): List<Evaluering> =
            if (identifikator.isBlank() && children.isNotEmpty()) children else listOf(this)

    companion object {

        fun ja(begrunnelse: String) = Evaluering(Resultat.JA, begrunnelse)

        fun nei(begrunnelse: String) = Evaluering(Resultat.NEI, begrunnelse)

        fun kanskje(begrunnelse: String) = Evaluering(Resultat.KANSKJE, begrunnelse)

        fun evaluer(identifikator: String, beskrivelse: String, eval: Evaluering) = eval.copy(identifikator = identifikator,
                                                                                              beskrivelse = beskrivelse)
    }

}

enum class Operator {
    OG,
    ELLER,
    INGEN
}


