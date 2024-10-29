package no.nav.familie.ef.sak.vilkår.regler

import no.nav.familie.ef.sak.infrastruktur.exception.Feil

/**
 * Ett [RegelSteg] er en regel med ett spørsmål med flere svar som mapper til en [SvarRegel]
 * @param regelId er id for spørsmålet
 * @param svarMapping er en mapping mellom svarId og regel for det svaret
 *
 * Eks
 * SPØRSMÅL_1 har 2 ulike svar,
 *  SVAR_1 som peker mot att vilkåret er OPPFYLT
 *  SVAR_2 som peker til ett nytt spørsmål
 * {
 *  [regelId]: "SPØRSMÅL_1",
 *  [svarMapping]: {
 *      "SVAR_1": SluttSvarRegel.OPPFYLT,
 *      "SVAR_2": {
 *          "regelId": "SPØRSMÅL_2",
 *          "begrunnelseType": "UTEN"
 *      }
 *  }
 * }
 */
data class RegelSteg(
    val regelId: RegelId,
    val svarMapping: Map<SvarId, SvarRegel>,
) {
    fun svarMapping(svarId: SvarId): SvarRegel = svarMapping[svarId] ?: throw Feil("Finner ikke svarId=$svarId for regelId=$regelId")
}

/**
 * Util for å sette opp svarmapping med [SvarId.JA] og [SvarId.NEI] som svarsalternativ
 */
fun jaNeiSvarRegel(
    hvisJa: SvarRegel = SluttSvarRegel.OPPFYLT,
    hvisNei: SvarRegel = SluttSvarRegel.IKKE_OPPFYLT,
): Map<SvarId, SvarRegel> = mapOf(SvarId.JA to hvisJa, SvarId.NEI to hvisNei)
