package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.domain.DelvilkårMetadata
import no.nav.familie.ef.sak.repository.domain.DelvilkårType.*
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class DelvilkårReglerTest {

    @Test
    internal fun `skal ha initiell verdi IKKE_VURDERT for delvilkår uten spesialregler`() {
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        assertThat(utledDelvilkårResultat(FEM_ÅRS_MEDLEMSKAP, søknad, delvilkårMetadata()))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        assertThat(utledDelvilkårResultat(DOKUMENTERT_FLYKTNINGSTATUS, søknad, delvilkårMetadata()))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        assertThat(utledDelvilkårResultat(BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                          søknad,
                                          delvilkårMetadata()))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)
    }

    @Test
    internal fun `skal dokumentere ekteskap hvis uformelt gift`() {
        val søknadUformeltgift = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad.copy(
                sivilstandsdetaljer = Søknadsfelt("",
                                                  Sivilstandsdetaljer(erUformeltGift = Søknadsfelt("", true)))
        ))

        val søknadIkkeUformeltgift = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad.copy(
                sivilstandsdetaljer = Søknadsfelt("",
                                                  Sivilstandsdetaljer(erUformeltGift = Søknadsfelt("", false)))
        ))

        assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                          søknadUformeltgift,
                                          delvilkårMetadata(Sivilstandstype.UOPPGITT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                          søknadUformeltgift,
                                          delvilkårMetadata(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


        assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                          søknadIkkeUformeltgift,
                                          delvilkårMetadata(Sivilstandstype.UOPPGITT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                          søknadIkkeUformeltgift,
                                          delvilkårMetadata(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                          søknadUformeltgift,
                                          delvilkårMetadata(Sivilstandstype.GIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

    }

    @Test
    internal fun `skal dokumentere separasjon eller skilsmisse hvis uformelt skilt eller separert`() {
        val søknadUformeltSkilt = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad.copy(
                sivilstandsdetaljer = Søknadsfelt("",
                                                  Sivilstandsdetaljer(erUformeltSeparertEllerSkilt = Søknadsfelt("", true)))
        ))

        val søknadIkkeUformeltSkilt = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad.copy(
                sivilstandsdetaljer = Søknadsfelt("",
                                                  Sivilstandsdetaljer(erUformeltSeparertEllerSkilt = Søknadsfelt("", false)))
        ))

        assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                          søknadUformeltSkilt,
                                          delvilkårMetadata(Sivilstandstype.UOPPGITT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                          søknadUformeltSkilt,
                                          delvilkårMetadata(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


        assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                          søknadIkkeUformeltSkilt,
                                          delvilkårMetadata(Sivilstandstype.UOPPGITT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                          søknadIkkeUformeltSkilt,
                                          delvilkårMetadata(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                          søknadIkkeUformeltSkilt,
                                          delvilkårMetadata(Sivilstandstype.GIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

    }

    @Test
    internal fun `skal vurdere om samlivsbrudd er likestilt med separasjon eller skilsmisse`() {
        val søknadSomHarSøktOmSkilsmisse = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad.copy(
                sivilstandsdetaljer = Søknadsfelt("",
                                                  Sivilstandsdetaljer(søktOmSkilsmisseSeparasjon = Søknadsfelt("", true)))
        ))

        val søknadSomIkkeHarSøktOmSkilsmisse = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad.copy(
                sivilstandsdetaljer = Søknadsfelt("",
                                                  Sivilstandsdetaljer(søktOmSkilsmisseSeparasjon = Søknadsfelt("", false)))
        ))

        assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                          søknadSomHarSøktOmSkilsmisse,
                                          delvilkårMetadata(Sivilstandstype.GIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                          søknadSomHarSøktOmSkilsmisse,
                                          delvilkårMetadata(Sivilstandstype.REGISTRERT_PARTNER)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


        assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                          søknadSomIkkeHarSøktOmSkilsmisse,
                                          delvilkårMetadata(Sivilstandstype.GIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                          søknadSomIkkeHarSøktOmSkilsmisse,
                                          delvilkårMetadata(Sivilstandstype.REGISTRERT_PARTNER)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                          søknadSomIkkeHarSøktOmSkilsmisse,
                                          delvilkårMetadata(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

    }

    @Test
    internal fun `skal vurdere om dato for samlivsbrudd samsvarer med fraflyttingsdato`() {
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                          søknad,
                                          delvilkårMetadata(Sivilstandstype.SEPARERT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                          søknad,
                                          delvilkårMetadata(Sivilstandstype.SEPARERT_PARTNER)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


        assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                          søknad,
                                          delvilkårMetadata(Sivilstandstype.GIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                          søknad,
                                          delvilkårMetadata(Sivilstandstype.REGISTRERT_PARTNER)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                          søknad,
                                          delvilkårMetadata(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

    }

    @Test
    internal fun `skal vurdere krav om sivilstand`() {
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        Sivilstandstype.values().forEach {
            when (it) {
                Sivilstandstype.ENKE_ELLER_ENKEMANN,
                Sivilstandstype.GJENLEVENDE_PARTNER -> assertThat(utledDelvilkårResultat(KRAV_SIVILSTAND,
                                                                                         søknad,
                                                                                         delvilkårMetadata(
                                                                                                 it)))
                        .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)
                else -> assertThat(utledDelvilkårResultat(KRAV_SIVILSTAND,
                                                          søknad,
                                                          delvilkårMetadata(it)))
                        .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


            }
        }
    }

    private fun delvilkårMetadata(sivilstandstype: Sivilstandstype = Sivilstandstype.GIFT): DelvilkårMetadata =
            DelvilkårMetadata(sivilstandstype = sivilstandstype)


}