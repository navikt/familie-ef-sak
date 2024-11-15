package no.nav.familie.ef.sak.vilkår.regler

/**
 * [GJELDENDE] Spørsmålet skal fortsatt stilles og besvares siden dette kreves av gjeldende regelverk.
 * [HISTORISK] Spørsmålet tilhører et foreldet regelverk og skal ikke stilles lenger. Allerede
 *             besvarte spørsmål som tidligere har vært gjeldende men som nå er historiske skal
 *             fortsatt vises i lesemodus i saksbehandlingen.
 */
enum class RegelVersjon {
    GJELDENDE,
    HISTORISK,
}
