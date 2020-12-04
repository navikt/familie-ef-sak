package no.nav.familie.ef.sak.vurdering

import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.domain.DelvilkårType
import no.nav.familie.ef.sak.repository.domain.DelvilkårType.*
import no.nav.familie.ef.sak.repository.domain.Vilkårsresultat
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.ef.søknad.Testsøknad
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class DelvilkårReglerTest {

    @Test
    internal fun `skal ha initiell verdi IKKE_VURDERT for delvilkår uten spesialregler`() {
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        Assertions.assertThat(utledDelvilkårResultat(DelvilkårType.TRE_ÅRS_MEDLEMSKAP, søknad, inngangsvilkårTestdata()))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        Assertions.assertThat(utledDelvilkårResultat(DelvilkårType.DOKUMENTERT_FLYKTNINGSTATUS, søknad, inngangsvilkårTestdata()))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        Assertions.assertThat(utledDelvilkårResultat(DelvilkårType.BOR_OG_OPPHOLDER_SEG_I_NORGE,
                                                     søknad,
                                                     inngangsvilkårTestdata()))
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

        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                                     søknadUformeltgift,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UOPPGITT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                                     søknadUformeltgift,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                                     søknadIkkeUformeltgift,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UOPPGITT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                                     søknadIkkeUformeltgift,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_EKTESKAP,
                                                     søknadUformeltgift,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.GIFT)))
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

        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                                     søknadUformeltSkilt,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UOPPGITT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                                     søknadUformeltSkilt,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                                     søknadIkkeUformeltSkilt,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UOPPGITT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                                     søknadIkkeUformeltSkilt,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        Assertions.assertThat(utledDelvilkårResultat(DOKUMENTERT_SEPARASJON_ELLER_SKILSMISSE,
                                                     søknadIkkeUformeltSkilt,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.GIFT)))
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

        Assertions.assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                                     søknadSomHarSøktOmSkilsmisse,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.GIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        Assertions.assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                                     søknadSomHarSøktOmSkilsmisse,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.REGISTRERT_PARTNER)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


        Assertions.assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                                     søknadSomIkkeHarSøktOmSkilsmisse,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.GIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        Assertions.assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                                     søknadSomIkkeHarSøktOmSkilsmisse,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.REGISTRERT_PARTNER)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        Assertions.assertThat(utledDelvilkårResultat(SAMLIVSBRUDD_LIKESTILT_MED_SEPARASJON,
                                                     søknadSomIkkeHarSøktOmSkilsmisse,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

    }

    @Test
    internal fun `skal vurdere om dato for samlivsbrudd samsvarer med fraflyttingsdato`() {
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        Assertions.assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                                     søknad,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.SEPARERT)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)

        Assertions.assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                                     søknad,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.SEPARERT_PARTNER)))
                .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


        Assertions.assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                                     søknad,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.GIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        Assertions.assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                                     søknad,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.REGISTRERT_PARTNER)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

        Assertions.assertThat(utledDelvilkårResultat(SAMSVAR_DATO_SEPARASJON_OG_FRAFLYTTING,
                                                     søknad,
                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(Sivilstandstype.UGIFT)))
                .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)

    }

    @Test
    internal fun `skal vurdere krav om sivilstand`() {
        val søknad = SøknadsskjemaMapper.tilDomene(Testsøknad.søknadOvergangsstønad)

        Sivilstandstype.values().forEach {
            when (it) {
                Sivilstandstype.ENKE_ELLER_ENKEMANN,
                Sivilstandstype.GJENLEVENDE_PARTNER -> Assertions.assertThat(utledDelvilkårResultat(KRAV_SIVILSTAND,
                                                                                                    søknad,
                                                                                                    inngangsvilkårGrunnlagDtoMedSivilstandstype(
                                                                                                            it)))
                        .isEqualTo(Vilkårsresultat.IKKE_AKTUELL)
                else -> Assertions.assertThat(utledDelvilkårResultat(KRAV_SIVILSTAND,
                                                                     søknad,
                                                                     inngangsvilkårGrunnlagDtoMedSivilstandstype(it)))
                        .isEqualTo(Vilkårsresultat.IKKE_VURDERT)


            }
        }
    }

    private fun inngangsvilkårGrunnlagDtoMedSivilstandstype(sivilstandstype: Sivilstandstype) =
            inngangsvilkårTestdata().copy(sivilstand = sivilstandInngangsvilkårDtoTestdata()
                    .copy(registergrunnlag = SivilstandRegistergrunnlagDto(sivilstandstype, null)))

    private fun inngangsvilkårTestdata(): InngangsvilkårGrunnlagDto {
        return InngangsvilkårGrunnlagDto(
                medlemskap = medlemskapDtoTestdata(),
                sivilstand = sivilstandInngangsvilkårDtoTestdata()
        )
    }

    private fun sivilstandInngangsvilkårDtoTestdata(): SivilstandInngangsvilkårDto {
        return SivilstandInngangsvilkårDto(
                SivilstandSøknadsgrunnlagDto(null, null, null, null, null, null, null, null, null),
                SivilstandRegistergrunnlagDto(Sivilstandstype.GIFT, null))
    }

    private fun medlemskapDtoTestdata(): MedlemskapDto {
        return MedlemskapDto(
                MedlemskapSøknadsgrunnlagDto(true, true, emptyList()),
                MedlemskapRegistergrunnlagDto(emptyList(),
                                              emptyList(),
                                              emptyList(),
                                              emptyList(),
                                              emptyList(),
                                              emptyList(),
                                              null)
        )
    }
}