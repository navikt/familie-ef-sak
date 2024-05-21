package no.nav.familie.ef.sak.vilkår

import no.nav.familie.ef.sak.vilkår.VilkårType.AKTIVITET
import no.nav.familie.ef.sak.vilkår.VilkårType.AKTIVITET_ARBEID
import no.nav.familie.ef.sak.vilkår.VilkårType.ALDER_PÅ_BARN
import no.nav.familie.ef.sak.vilkår.VilkårType.ALENEOMSORG
import no.nav.familie.ef.sak.vilkår.VilkårType.DOKUMENTASJON_AV_UTDANNING
import no.nav.familie.ef.sak.vilkår.VilkårType.DOKUMENTASJON_TILSYNSUTGIFTER
import no.nav.familie.ef.sak.vilkår.VilkårType.ER_UTDANNING_HENSIKTSMESSIG
import no.nav.familie.ef.sak.vilkår.VilkårType.FORUTGÅENDE_MEDLEMSKAP
import no.nav.familie.ef.sak.vilkår.VilkårType.INNTEKT
import no.nav.familie.ef.sak.vilkår.VilkårType.LOVLIG_OPPHOLD
import no.nav.familie.ef.sak.vilkår.VilkårType.MOR_ELLER_FAR
import no.nav.familie.ef.sak.vilkår.VilkårType.NYTT_BARN_SAMME_PARTNER
import no.nav.familie.ef.sak.vilkår.VilkårType.RETT_TIL_OVERGANGSSTØNAD
import no.nav.familie.ef.sak.vilkår.VilkårType.SAGT_OPP_ELLER_REDUSERT
import no.nav.familie.ef.sak.vilkår.VilkårType.SAMLIV
import no.nav.familie.ef.sak.vilkår.VilkårType.SIVILSTAND
import no.nav.familie.ef.sak.vilkår.VilkårType.TIDLIGERE_VEDTAKSPERIODER
import no.nav.familie.kontrakter.felles.ef.StønadType.BARNETILSYN
import no.nav.familie.kontrakter.felles.ef.StønadType.OVERGANGSSTØNAD
import no.nav.familie.kontrakter.felles.ef.StønadType.SKOLEPENGER
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class VilkårTypeTest {
    private val vilkårForOvergangsstønad =
        listOf(
            FORUTGÅENDE_MEDLEMSKAP,
            LOVLIG_OPPHOLD,
            MOR_ELLER_FAR,
            SIVILSTAND,
            SAMLIV,
            ALENEOMSORG,
            NYTT_BARN_SAMME_PARTNER,
            SAGT_OPP_ELLER_REDUSERT,
            AKTIVITET,
            TIDLIGERE_VEDTAKSPERIODER,
        )

    private val vilkårForBarnetilsyn =
        listOf(
            FORUTGÅENDE_MEDLEMSKAP,
            LOVLIG_OPPHOLD,
            MOR_ELLER_FAR,
            SIVILSTAND,
            SAMLIV,
            ALENEOMSORG,
            NYTT_BARN_SAMME_PARTNER,
            AKTIVITET_ARBEID,
            INNTEKT,
            ALDER_PÅ_BARN,
            DOKUMENTASJON_TILSYNSUTGIFTER,
        )

    private val vilkårForSkolepenger =
        listOf(
            FORUTGÅENDE_MEDLEMSKAP,
            LOVLIG_OPPHOLD,
            MOR_ELLER_FAR,
            SIVILSTAND,
            SAMLIV,
            ALENEOMSORG,
            NYTT_BARN_SAMME_PARTNER,
            RETT_TIL_OVERGANGSSTØNAD,
            DOKUMENTASJON_AV_UTDANNING,
            ER_UTDANNING_HENSIKTSMESSIG,
        )

    @Test
    internal fun `skal hente ut vilkår for overgangsstønad`() {
        val filtrerteVilkårstyper = VilkårType.hentVilkårForStønad(OVERGANGSSTØNAD)
        assertThat(filtrerteVilkårstyper).containsExactlyInAnyOrderElementsOf(vilkårForOvergangsstønad)
    }

    @Test
    internal fun `skal hente ut vilkår for barnetilsyn`() {
        val filtrerteVilkårstyper = VilkårType.hentVilkårForStønad(BARNETILSYN)
        assertThat(filtrerteVilkårstyper).containsExactlyInAnyOrderElementsOf(vilkårForBarnetilsyn)
    }

    @Test
    internal fun `skal hente ut vilkår for skolepenger`() {
        val filtrerteVilkårstyper = VilkårType.hentVilkårForStønad(SKOLEPENGER)
        assertThat(filtrerteVilkårstyper).containsExactlyInAnyOrderElementsOf(vilkårForSkolepenger)
    }
}
