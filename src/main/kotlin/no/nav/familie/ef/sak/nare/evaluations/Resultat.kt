package no.nav.familie.ef.sak.nare.evaluations

enum class Resultat {
    JA {

        override infix fun og(other: Resultat): Resultat = other
        override infix fun eller(other: Resultat): Resultat = JA
    },

    NEI {

        override infix fun og(other: Resultat): Resultat = NEI
        override infix fun eller(other: Resultat): Resultat = other
    },

    KANSKJE {

        override infix fun og(other: Resultat): Resultat = if (other == JA) KANSKJE else other
        override infix fun eller(other: Resultat): Resultat = if (other == NEI) KANSKJE else other
    };

    abstract infix fun og(other: Resultat): Resultat
    abstract infix fun eller(other: Resultat): Resultat
}
