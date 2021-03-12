package no.nav.familie.ef.sak.regler

enum class ResultatRegel(override val id: String, val resultat: Resultat) : RegelId {
    OPPFYLT(id = "OPPFYLT", resultat = Resultat.OPPFYLT),
    IKKE_OPPFYLT(id = "IKKE_OPPFYLT", resultat = Resultat.IKKE_OPPFYLT)
}
