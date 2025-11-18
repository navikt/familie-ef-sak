package no.nav.familie.ef.sak.vilkår.regler

import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AktivitetArbeidRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AktivitetRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AlderPåBarnRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.AleneomsorgRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.DokumentasjonTilsynsutgifterRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.ForutgåendeMedlemskapRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.InntektRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.MorEllerFarRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.NyttBarnSammePartnerRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.OppholdINorgeRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SagtOppEllerRedusertRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SamlivRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.TidligareVedtaksperioderRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.skolepenger.DokumentasjonAvUtdanningRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.skolepenger.RettTilOvergangsstønadRegel
import no.nav.familie.ef.sak.vilkår.regler.vilkår.skolepenger.UtdanningErHensiktsmessigRegel
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.ef.StønadType.SKOLEPENGER

/**
 * Singleton for å holde på alle regler
 */
class Vilkårsregler private constructor(
    val vilkårsregler: Map<VilkårType, Vilkårsregel>,
) {
    companion object {
        val ALLE_VILKÅRSREGLER = Vilkårsregler(alleVilkårsregler.associateBy { it.vilkårType })
    }
}

private val alleVilkårsregler = StønadType.values().map { vilkårsreglerForStønad(it) }.flatten()

fun vilkårsreglerForStønad(stønadstype: StønadType): List<Vilkårsregel> =
    when (stønadstype) {
        OVERGANGSSTØNAD -> {
            listOf(
                ForutgåendeMedlemskapRegel(),
                OppholdINorgeRegel(),
                MorEllerFarRegel(),
                SivilstandRegel(),
                SamlivRegel(),
                AleneomsorgRegel(),
                NyttBarnSammePartnerRegel(),
                AktivitetRegel(),
                SagtOppEllerRedusertRegel(),
                TidligareVedtaksperioderRegel(),
            )
        }

        BARNETILSYN -> {
            listOf(
                ForutgåendeMedlemskapRegel(),
                OppholdINorgeRegel(),
                MorEllerFarRegel(),
                SivilstandRegel(),
                SamlivRegel(),
                AleneomsorgRegel(),
                NyttBarnSammePartnerRegel(),
                AktivitetArbeidRegel(),
                InntektRegel(),
                AlderPåBarnRegel(),
                DokumentasjonTilsynsutgifterRegel(),
            )
        }

        SKOLEPENGER -> {
            listOf(
                ForutgåendeMedlemskapRegel(),
                OppholdINorgeRegel(),
                MorEllerFarRegel(),
                SivilstandRegel(),
                SamlivRegel(),
                AleneomsorgRegel(),
                NyttBarnSammePartnerRegel(),
                RettTilOvergangsstønadRegel(),
                DokumentasjonAvUtdanningRegel(),
                UtdanningErHensiktsmessigRegel(),
            )
        }
    }

fun hentVilkårsregel(vilkårType: VilkårType): Vilkårsregel =
    Vilkårsregler.ALLE_VILKÅRSREGLER.vilkårsregler[vilkårType]
        ?: error("Finner ikke vilkårsregler for vilkårType=$vilkårType")
