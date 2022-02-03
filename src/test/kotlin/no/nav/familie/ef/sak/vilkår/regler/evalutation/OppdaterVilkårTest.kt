package no.nav.familie.ef.sak.vilkår.regler.evalutation

import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.testutil.søknadsBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.VilkårType
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.Vilkårsvurdering
import no.nav.familie.ef.sak.vilkår.Vurdering
import no.nav.familie.ef.sak.vilkår.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.vilkår.dto.VurderingDto
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.RegelId
import no.nav.familie.ef.sak.vilkår.regler.SvarId
import no.nav.familie.ef.sak.vilkår.regler.Vilkårsregel
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.erAlleVilkårTattStillingTil
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.utledResultatForAleneomsorg
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

internal class OppdaterVilkårTest {

    @Test
    fun `har kun svart på første spørsmål som er en sluttnode - allt ok`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA))

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `har svart på två spørsmål hvor det siste er en sluttnode - allt ok`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "a"),
                                         VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE, SvarId.JA))

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    /**
     * TODO håndterer ikke [Vilkårsresultat.IKKE_TATT_STILLING_TIL] ennå
     */
    @Test
    @Disabled
    fun `har svart på två spørsmål hvor det siste er en sluttnode men mangler begrunnelse på første`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI),
                                         VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE, SvarId.JA))

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `har svart på två spørsmål hvor det som gir IKKE_OPPFYLT`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "a"),
                                         VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE, SvarId.NEI))

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `två rotRegler - en IKKE_OPPFYLT og en OPPFYLT`() {
        val regel = VilkårsregelToHovedregler()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         delvilkårsvurderingDto(VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA)),
                                         delvilkårsvurderingDto(VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                                                                             SvarId.NEI)))

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.delvilkår(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE).resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.delvilkår(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE).resultat)
                .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    /**
     * TODO håndterer ikke [Vilkårsresultat.IKKE_TATT_STILLING_TIL] ennå
     */
    @Test
    @Disabled
    fun `två rotRegler - en IKKE_TATT_STILLING_TIL og en OPPFYLT`() {
        val regel = VilkårsregelToHovedregler()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat = validerOgOppdater(vilkårsvurdering, regel,
                                         delvilkårsvurderingDto(
                                                 VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "")),
                                         delvilkårsvurderingDto(VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                                                                             SvarId.JA)))

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(resultat.delvilkår(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE).resultat)
                .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(resultat.delvilkår(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE).resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }


    @Test
    fun `sivilstand - trenger ikke å svare på hovedvilkår som ikke er aktuelle`() {
        val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().build().søknadOvergangsstønad)
        val regel = SivilstandRegel()
        val barn = søknadsBarnTilBehandlingBarn(søknad.barn)
        val initDelvilkår = regel.initereDelvilkårsvurdering(HovedregelMetadata(søknad.sivilstand, Sivilstandstype.SKILT, barn = barn))
        val aktuelleDelvilkår = initDelvilkår.filter { it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL }
        assertThat(initDelvilkår).hasSize(5)
        assertThat(initDelvilkår.filter { it.resultat == Vilkårsresultat.IKKE_AKTUELL }).hasSize(4)
        assertThat(aktuelleDelvilkår).hasSize(1)

        val vilkårsvurdering = Vilkårsvurdering(behandlingId = UUID.randomUUID(),
                                                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                type = VilkårType.SIVILSTAND,
                                                delvilkårsvurdering = DelvilkårsvurderingWrapper(initDelvilkår))

        val oppdatering = listOf(DelvilkårsvurderingDto(Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                        listOf(VurderingDto(aktuelleDelvilkår.first().hovedregel, SvarId.JA))))
        val lagNyOppdatertVilkårsvurdering = OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering, oppdatering)
        assertThat(lagNyOppdatertVilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger).hasSize(regel.hovedregler.size)
    }

    @Test
    fun `sivilstand - sender inn svar på en annen regel enn det som man skal svare på`() {
        val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().build().søknadOvergangsstønad)
        val regel = SivilstandRegel()
        val barn = søknadsBarnTilBehandlingBarn(søknad.barn)
        val initDelvilkår = regel.initereDelvilkårsvurdering(HovedregelMetadata(søknad.sivilstand, Sivilstandstype.SKILT, barn = barn))

        val vilkårsvurdering = Vilkårsvurdering(behandlingId = UUID.randomUUID(),
                                                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                type = VilkårType.SIVILSTAND,
                                                delvilkårsvurdering = DelvilkårsvurderingWrapper(initDelvilkår))

        val oppdatering = listOf(DelvilkårsvurderingDto(Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                        listOf(VurderingDto(RegelId.SIVILSTAND_UNNTAK, SvarId.JA))))
        assertThat(catchThrowable { OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering, oppdatering) })
                .hasMessage("Delvilkårsvurderinger savner svar på hovedregler - " +
                            "hovedregler=[KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE] delvilkår=[SIVILSTAND_UNNTAK]")
    }

    @Test
    fun `utledResultatForAleneomsorg - gir OPPFYLT hvis en er OPPFYLT`() {
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.OPPFYLT))))
                .isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.OPPFYLT),
                                                      aleneomsorg(Vilkårsresultat.IKKE_OPPFYLT))))
                .isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.OPPFYLT),
                                                      aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL))))
                .isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.OPPFYLT),
                                                      aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES))))
                .isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `utledResultatForAleneomsorg - gir IKKE_OPPFYLT hvis en er oppfylt og resten er IKKE_OPPFYLT eller SKAL_IKKE_VURDERES`() {
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.IKKE_OPPFYLT))))
                .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.IKKE_OPPFYLT),
                                                      aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES))))
                .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.IKKE_OPPFYLT),
                                                      aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL))))
                .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `utledResultatForAleneomsorg - gir SKAL_IKKE_VURDERES hvis kun SKAL_IKKE_VURDERES`() {
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES))))
                .isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
    }

    @Test
    fun `utledResultatForAleneomsorg - gir IKKE_TATT_STILLING_TIL om det finnes IKKE_TATT_STILLING_TIL og SKAL_IKKE_VURDERES`() {
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL))))
                .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(utledResultatForAleneomsorg(listOf(aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL),
                                                      aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES))))
                .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `utledResultatForAleneomsorg - skal kaste feil hvis vilkåren inneholder noe annet enn aleneomsorg`() {
        val vilkårForSivilstand = aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL).copy(type = VilkårType.SIVILSTAND)
        assertThat(catchThrowable { utledResultatForAleneomsorg(listOf(vilkårForSivilstand)) })
                .hasMessage("Denne metoden kan kun kalles med vilkår for Aleneomsorg")
    }

    @Test
    fun `erAlleVilkårVurdert - alle vilkåren er OPPFYLT`() {
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.OPPFYLT)))
                .isTrue
    }

    @Test
    fun `erAlleVilkårVurdert - skal kunne ha en kombinasjon av OPPFYLT og SKAL_IKKE_VURDERES`() {
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.OPPFYLT,
                                                      Vilkårsresultat.SKAL_IKKE_VURDERES)))
                .isTrue
    }

    @Test
    fun `erAlleVilkårVurdert - ett vilkår er IKKE_OPPFYLT og resten er gyldig`() {
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.IKKE_OPPFYLT,
                                                      Vilkårsresultat.OPPFYLT)))
                .isTrue
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.IKKE_OPPFYLT,
                                                      Vilkårsresultat.SKAL_IKKE_VURDERES)))
                .isTrue
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.IKKE_OPPFYLT,
                                                      Vilkårsresultat.IKKE_OPPFYLT)))
                .isTrue
    }

    @Test
    fun `erAlleVilkårVurdert - IKKE_TATT_STILLING_TIL skal gi false`() {
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.IKKE_OPPFYLT,
                                                      Vilkårsresultat.IKKE_TATT_STILLING_TIL)))
                .isFalse
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.OPPFYLT,
                                                      Vilkårsresultat.IKKE_TATT_STILLING_TIL)))
                .isFalse
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.SKAL_IKKE_VURDERES,
                                                      Vilkårsresultat.IKKE_TATT_STILLING_TIL)))
                .isFalse
    }

    @Test
    fun `erAlleVilkårVurdert - SKAL_IKKE_VURDERES må være i en kombinasjon med IKKE_OPPFYLT`() {
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.SKAL_IKKE_VURDERES,
                                                      Vilkårsresultat.IKKE_OPPFYLT)))
                .isTrue

        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.SKAL_IKKE_VURDERES,
                                                      Vilkårsresultat.SKAL_IKKE_VURDERES)))
                .withFailMessage("Alle vilkår skal kunne være satt til SKAL_IKKE_VURDERES")
                .isTrue
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.SKAL_IKKE_VURDERES,
                                                      Vilkårsresultat.OPPFYLT)))
                .withFailMessage("Alle vilkår skal kunne være satt til enten SKAL_IKKE_VURDERES eller OPPFYLT")
                .isTrue
    }

    private fun aleneomsorg(vilkårsresultat: Vilkårsresultat) =
            Vilkårsvurdering(behandlingId = UUID.randomUUID(),
                             resultat = vilkårsresultat,
                             type = VilkårType.ALENEOMSORG,
                             delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()))

    private fun Vilkårsvurdering.delvilkår(regelId: RegelId) =
            this.delvilkårsvurdering.delvilkårsvurderinger.singleOrNull { it.hovedregel == regelId }
            ?: error("Finner ikke regelId=$regelId blant ${this.delvilkårsvurdering.delvilkårsvurderinger.map { it.hovedregel }}")

    private fun Vilkårsvurdering.førsteDelvilkår() = this.delvilkårsvurdering.delvilkårsvurderinger.first()

    private fun delvilkårsvurderingDto(vararg vurderinger: VurderingDto) =
            DelvilkårsvurderingDto(resultat = Vilkårsresultat.IKKE_AKTUELL, vurderinger = vurderinger.toList())

    private fun validerOgOppdater(vilkårsvurdering: Vilkårsvurdering,
                                  regel: Vilkårsregel,
                                  vararg vurderinger: VurderingDto): Vilkårsvurdering {
        return validerOgOppdater(vilkårsvurdering, regel, delvilkårsvurderingDto(*vurderinger))
    }

    private fun validerOgOppdater(vilkårsvurdering: Vilkårsvurdering,
                                  regel: Vilkårsregel,
                                  vararg delvilkårsvurderingDto: DelvilkårsvurderingDto): Vilkårsvurdering {
        return OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering = vilkårsvurdering,
                                                             vilkårsregler = mapOf(regel.vilkårType to regel),
                                                             oppdatering = delvilkårsvurderingDto.toList())
    }

    private fun opprettVurdering(regel: Vilkårsregel): Vilkårsvurdering {
        val delvilkårsvurderinger = regel.hovedregler.map {
            Delvilkårsvurdering(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL, vurderinger = listOf(Vurdering(it)))
        }
        return Vilkårsvurdering(behandlingId = UUID.randomUUID(),
                                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                type = VilkårType.ALENEOMSORG,
                                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurderinger))
    }

}