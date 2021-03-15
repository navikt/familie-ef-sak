package no.nav.familie.ef.sak.regler

interface RegelId {

    val id: String
}

interface RegelIdMedBeskrivelse : RegelId {

    val beskrivelse: String
}

/*
enum class RegelType {
    REGEL_STEG,
    RESULTAT
}*/

enum class Begrunnelse {
    //UTEN,
    PÅKREVD,
    VALGFRI
}

enum class Resultat {
    OPPFYLT,
    IKKE_OPPFYLT
}

enum class JaNei {
    JA,
    NEI
}

interface RegelFlyt {

    //val type: RegelType
}

//TODO burde mappe til JA/NEI?
interface Årsak {

    val mapping: JaNei
}