package no.nav.familie.ef.sak.regler.evalutation

import no.nav.familie.ef.sak.api.dto.DelvilkårsvurderingDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.api.dto.VurderingDto
import no.nav.familie.ef.sak.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.regler.HovedregelMetadata
import no.nav.familie.ef.sak.regler.RegelId
import no.nav.familie.ef.sak.regler.SvarId
import no.nav.familie.ef.sak.regler.Vilkårsregel
import no.nav.familie.ef.sak.regler.vilkår.SivilstandRegel
import no.nav.familie.ef.sak.repository.domain.*
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

class OppdaterVilkårTest {

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
        assertThat(resultat.delvilkår(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE).resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
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
    internal fun `sivilstand - trenger ikke å svare på hovedvilkår som ikke er aktuelle`() {
        val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().build().søknadOvergangsstønad)
        val regel = SivilstandRegel()
        val initDelvilkår = regel.initereDelvilkårsvurderingMedVilkårsresultat(HovedregelMetadata(søknad, Sivilstandstype.SKILT, ))
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
    internal fun `sivilstand - sender inn svar på en annen regel enn det som man skal svare på`() {
        val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().build().søknadOvergangsstønad)
        val regel = SivilstandRegel()
        val initDelvilkår = regel.initereDelvilkårsvurderingMedVilkårsresultat(HovedregelMetadata(søknad, Sivilstandstype.SKILT))

        val vilkårsvurdering = Vilkårsvurdering(behandlingId = UUID.randomUUID(),
                                                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                type = VilkårType.SIVILSTAND,
                                                delvilkårsvurdering = DelvilkårsvurderingWrapper(initDelvilkår))

        val oppdatering = listOf(DelvilkårsvurderingDto(Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                                                        listOf(VurderingDto(RegelId.SIVILSTAND_UNNTAK, SvarId.JA))))
        assertThat(catchThrowable { OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering, oppdatering) })
                .hasMessage("Delvilkårsvurderinger savner svar på hovedregler - hovedregler=[KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE] delvilkår=[SIVILSTAND_UNNTAK]")
    }

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