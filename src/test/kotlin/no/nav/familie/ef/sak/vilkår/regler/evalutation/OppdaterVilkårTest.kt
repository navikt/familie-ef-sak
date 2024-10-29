package no.nav.familie.ef.sak.vilkår.regler.evalutation

import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype.GIFT
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.SøknadsskjemaMapper
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.testutil.søknadBarnTilBehandlingBarn
import no.nav.familie.ef.sak.vilkår.Delvilkårsvurdering
import no.nav.familie.ef.sak.vilkår.DelvilkårsvurderingWrapper
import no.nav.familie.ef.sak.vilkår.Opphavsvilkår
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
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.opprettNyeVilkårsvurderinger
import no.nav.familie.ef.sak.vilkår.regler.evalutation.OppdaterVilkår.utledResultatForVilkårSomGjelderFlereBarn
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.util.FnrGenerator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

internal class OppdaterVilkårTest {
    val barn1 = UUID.randomUUID()
    val barn2 = UUID.randomUUID()

    @Test
    fun `Skal lage ALDER_PÅ_BARN-vurderinger for barn det er søkt om OG de andre barna som finnes - barnetilsyn`() {
        val behandlingId = UUID.randomUUID()
        val barn =
            BehandlingBarn(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = "01472152579",
                navn = null,
                fødselTermindato = null,
            )
        val barnUtenSøknad = barn.copy(id = UUID.randomUUID())
        val metadata =
            HovedregelMetadata(
                sivilstandSøknad = null,
                sivilstandstype = GIFT,
                erMigrering = false,
                barn = listOf(barn, barnUtenSøknad),
                søktOmBarnetilsyn = listOf(barn.id),
                vilkårgrunnlagDto = VilkårTestUtil.mockVilkårGrunnlagDto(),
                behandling = behandling(),
            )

        val nyeVilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
                behandlingId,
                metadata,
                StønadType.BARNETILSYN,
            )

        assertThat(nyeVilkårsvurderinger.filter { it.type === VilkårType.ALDER_PÅ_BARN }).hasSize(2)
        val barnIdMedAlderPåBarnVilkår =
            nyeVilkårsvurderinger.filter { it.type === VilkårType.ALDER_PÅ_BARN }.map { it.barnId }
        assertThat(barnIdMedAlderPåBarnVilkår).containsAll(listOf(barn.id, barnUtenSøknad.id))
    }

    @Test
    fun `ALDER_PÅ_BARN-vurderinger skal automatisk vurderes ved opprettelse - barnetilsyn`() {
        val behandlingId = UUID.randomUUID()
        val barn =
            BehandlingBarn(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = FnrGenerator.generer(LocalDate.now().minusYears(5)),
                navn = null,
                fødselTermindato = null,
            )
        val barnUtenSøknad =
            barn.copy(
                id = UUID.randomUUID(),
                personIdent = FnrGenerator.generer(LocalDate.now().minusYears(4)),
            )
        val metadata =
            HovedregelMetadata(
                sivilstandSøknad = null,
                sivilstandstype = GIFT,
                erMigrering = false,
                barn = listOf(barn, barnUtenSøknad),
                søktOmBarnetilsyn = listOf(barn.id),
                vilkårgrunnlagDto = VilkårTestUtil.mockVilkårGrunnlagDto(),
                behandling = behandling(),
            )

        val nyeVilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
                behandlingId,
                metadata,
                StønadType.BARNETILSYN,
            )

        val alderPåBarnVilkår = nyeVilkårsvurderinger.filter { it.type === VilkårType.ALDER_PÅ_BARN }
        assertThat(alderPåBarnVilkår).hasSize(2)
        for (vilkårsvurdering in alderPåBarnVilkår) {
            val delvilkårsvurderinger = vilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger
            assertThat(delvilkårsvurderinger.size).isEqualTo(1)
            assertThat(delvilkårsvurderinger.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
        }
    }

    @Test
    fun `Skal lage ALENEOMSORG-vurderinger for barn det er søkt om OG barn det ikke er søkt om - barnetilsyn`() {
        val behandlingId = UUID.randomUUID()
        val barn =
            BehandlingBarn(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = "03441983106",
                navn = null,
                fødselTermindato = null,
            )
        val barnUtenSøknad = barn.copy(id = UUID.randomUUID())
        val metadata =
            HovedregelMetadata(
                sivilstandSøknad = null,
                sivilstandstype = GIFT,
                erMigrering = false,
                barn = listOf(barn, barnUtenSøknad),
                søktOmBarnetilsyn = listOf(barn.id),
                vilkårgrunnlagDto = VilkårTestUtil.mockVilkårGrunnlagDto(),
                behandling = behandling(),
            )

        val nyeVilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
                behandlingId,
                metadata,
                StønadType.BARNETILSYN,
            )

        assertThat(nyeVilkårsvurderinger.filter { it.type === VilkårType.ALENEOMSORG }).hasSize(2)
        val barnIdMedVilkårAleneomsorg =
            nyeVilkårsvurderinger.filter { it.type === VilkårType.ALENEOMSORG }.map { it.barnId }
        assertThat(barnIdMedVilkårAleneomsorg).containsAll(listOf(barn.id, barnUtenSøknad.id))
    }

    @Test
    fun `Skal lage null ALDER_PÅ_BARN vurderinger når type er overgangsstønad`() {
        val behandlingId = UUID.randomUUID()
        val barn =
            BehandlingBarn(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = null,
                navn = null,
                fødselTermindato = null,
            )
        val metadata =
            HovedregelMetadata(
                sivilstandSøknad = null,
                sivilstandstype = GIFT,
                erMigrering = false,
                barn = listOf(barn, barn.copy(id = UUID.randomUUID())),
                søktOmBarnetilsyn = emptyList(),
                vilkårgrunnlagDto = VilkårTestUtil.mockVilkårGrunnlagDto(),
                behandling = behandling(),
            )

        val nyeVilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
                behandlingId,
                metadata,
                StønadType.OVERGANGSSTØNAD,
            )

        assertThat(nyeVilkårsvurderinger.filter { it.type === VilkårType.ALDER_PÅ_BARN }).hasSize(0)
    }

    @Test
    fun `Skal lage ALENEOMSORG-vurderinger for alle barn når type er overgangsstønad`() {
        val behandlingId = UUID.randomUUID()
        val barn =
            BehandlingBarn(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                søknadBarnId = null,
                personIdent = null,
                navn = null,
                fødselTermindato = null,
            )
        val metadata =
            HovedregelMetadata(
                sivilstandSøknad = null,
                sivilstandstype = GIFT,
                erMigrering = false,
                barn = listOf(barn, barn.copy(id = UUID.randomUUID())),
                søktOmBarnetilsyn = emptyList(),
                vilkårgrunnlagDto = VilkårTestUtil.mockVilkårGrunnlagDto(),
                behandling = behandling(),
            )

        val nyeVilkårsvurderinger =
            opprettNyeVilkårsvurderinger(
                behandlingId,
                metadata,
                StønadType.OVERGANGSSTØNAD,
            )

        assertThat(nyeVilkårsvurderinger.filter { it.type === VilkårType.ALENEOMSORG }).hasSize(2)
    }

    @Test
    fun `har kun svart på første spørsmål som er en sluttnode - allt ok`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat =
            validerOgOppdater(
                vilkårsvurdering,
                regel,
                VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA),
            )

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `har svart på två spørsmål hvor det siste er en sluttnode - allt ok`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat =
            validerOgOppdater(
                vilkårsvurdering,
                regel,
                VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "a"),
                VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE, SvarId.JA),
            )

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `Skal feile dersom det mangler obligatorisk begrunnelse på et av delvilkårene`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat =
            assertThrows<Feil> {
                validerOgOppdater(
                    vilkårsvurdering,
                    regel,
                    VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI),
                    VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE, SvarId.JA),
                )
            }

        assertThat(resultat.message).isNotNull
    }

    @Test
    fun `har svart på två spørsmål hvor det som gir IKKE_OPPFYLT`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat =
            validerOgOppdater(
                vilkårsvurdering,
                regel,
                VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "a"),
                VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE, SvarId.NEI),
            )

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.førsteDelvilkår().resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    internal fun `skal fjerne opphavsvilkår når man oppdaterer et vilkår`() {
        val regel = VilkårsregelEnHovedregel()
        val vilkårsvurdering =
            opprettVurdering(regel)
                .copy(opphavsvilkår = Opphavsvilkår(UUID.randomUUID(), LocalDateTime.now()))
        val resultat =
            validerOgOppdater(
                vilkårsvurdering,
                regel,
                VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, "a"),
                VurderingDto(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE, SvarId.NEI),
            )

        assertThat(resultat.opphavsvilkår).isNull()
    }

    @Test
    fun `två rotRegler - en IKKE_OPPFYLT og en OPPFYLT`() {
        val regel = VilkårsregelToHovedregler()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat =
            validerOgOppdater(
                vilkårsvurdering,
                regel,
                delvilkårsvurderingDto(VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.JA)),
                delvilkårsvurderingDto(
                    VurderingDto(
                        RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                        SvarId.NEI,
                    ),
                ),
            )

        assertThat(resultat.resultat).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(resultat.delvilkår(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE).resultat).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(resultat.delvilkår(RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE).resultat)
            .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
    }

    @Test
    fun `skal feile dersom man lagrer et delvilkår uten fullstendige opplysninger - ett med OPPFYLT og et annet med IKKE_TATT_STILLING_TIL`() {
        val regel = VilkårsregelToHovedregler()
        val vilkårsvurdering = opprettVurdering(regel)
        val resultat =
            assertThrows<Feil> {
                validerOgOppdater(
                    vilkårsvurdering,
                    regel,
                    delvilkårsvurderingDto(
                        VurderingDto(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, SvarId.NEI, ""),
                    ),
                    delvilkårsvurderingDto(
                        VurderingDto(
                            RegelId.KRAV_SIVILSTAND_PÅKREVD_BEGRUNNELSE,
                            SvarId.JA,
                        ),
                    ),
                )
            }

        assertThat(resultat.message).isNotNull
    }

    @Test
    fun `sivilstand - trenger ikke å svare på hovedvilkår som ikke er aktuelle`() {
        val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().build().søknadOvergangsstønad)
        val regel = SivilstandRegel()
        val barn = søknadBarnTilBehandlingBarn(søknad.barn)
        val initDelvilkår =
            regel.initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    søknad.sivilstand,
                    Sivilstandstype.SKILT,
                    barn = barn,
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = mockk(),
                    behandling = behandling(),
                ),
            )
        val aktuelleDelvilkår = initDelvilkår.filter { it.resultat == Vilkårsresultat.IKKE_TATT_STILLING_TIL }
        assertThat(initDelvilkår).hasSize(5)
        assertThat(initDelvilkår.filter { it.resultat == Vilkårsresultat.IKKE_AKTUELL }).hasSize(4)
        assertThat(aktuelleDelvilkår).hasSize(1)

        val vilkårsvurdering =
            Vilkårsvurdering(
                behandlingId = UUID.randomUUID(),
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                type = VilkårType.SIVILSTAND,
                delvilkårsvurdering = DelvilkårsvurderingWrapper(initDelvilkår),
                opphavsvilkår = null,
            )

        val oppdatering =
            listOf(
                DelvilkårsvurderingDto(
                    Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                    listOf(VurderingDto(aktuelleDelvilkår.first().hovedregel, SvarId.JA)),
                ),
            )
        val lagNyOppdatertVilkårsvurdering =
            OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering, oppdatering)
        assertThat(lagNyOppdatertVilkårsvurdering.delvilkårsvurdering.delvilkårsvurderinger).hasSize(regel.hovedregler.size)
    }

    @Test
    fun `sivilstand - sender inn svar på en annen regel enn det som man skal svare på`() {
        val søknad = SøknadsskjemaMapper.tilDomene(TestsøknadBuilder.Builder().build().søknadOvergangsstønad)
        val regel = SivilstandRegel()
        val barn = søknadBarnTilBehandlingBarn(søknad.barn)
        val initDelvilkår =
            regel.initiereDelvilkårsvurdering(
                HovedregelMetadata(
                    søknad.sivilstand,
                    Sivilstandstype.SKILT,
                    barn = barn,
                    søktOmBarnetilsyn = emptyList(),
                    vilkårgrunnlagDto = mockk(),
                    behandling = behandling(),
                ),
            )

        val vilkårsvurdering =
            Vilkårsvurdering(
                behandlingId = UUID.randomUUID(),
                resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                type = VilkårType.SIVILSTAND,
                delvilkårsvurdering = DelvilkårsvurderingWrapper(initDelvilkår),
                opphavsvilkår = null,
            )

        val oppdatering =
            listOf(
                DelvilkårsvurderingDto(
                    Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                    listOf(VurderingDto(RegelId.SIVILSTAND_UNNTAK, SvarId.JA)),
                ),
            )
        assertThat(catchThrowable { OppdaterVilkår.lagNyOppdatertVilkårsvurdering(vilkårsvurdering, oppdatering) })
            .hasMessage(
                "Delvilkårsvurderinger savner svar på hovedregler - " +
                    "hovedregler=[KRAV_SIVILSTAND_UTEN_PÅKREVD_BEGRUNNELSE] delvilkår=[SIVILSTAND_UNNTAK]",
            )
    }

    @Test
    fun `utledResultatForAleneomsorg - gir OPPFYLT hvis en er OPPFYLT`() {
        assertThat(utledResultatForVilkårSomGjelderFlereBarn(listOf(aleneomsorg(Vilkårsresultat.OPPFYLT))))
            .isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    aleneomsorg(Vilkårsresultat.OPPFYLT),
                    aleneomsorg(Vilkårsresultat.IKKE_OPPFYLT),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    aleneomsorg(Vilkårsresultat.OPPFYLT),
                    aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.OPPFYLT)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    aleneomsorg(Vilkårsresultat.OPPFYLT),
                    aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.OPPFYLT)
    }

    @Test
    fun `utledResultatForAleneomsorg - gir IKKE_OPPFYLT hvis en er oppfylt og resten er IKKE_OPPFYLT eller SKAL_IKKE_VURDERES`() {
        assertThat(utledResultatForVilkårSomGjelderFlereBarn(listOf(aleneomsorg(Vilkårsresultat.IKKE_OPPFYLT))))
            .isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    aleneomsorg(Vilkårsresultat.IKKE_OPPFYLT),
                    aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.IKKE_OPPFYLT)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    aleneomsorg(Vilkårsresultat.IKKE_OPPFYLT),
                    aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `utledResultatForAleneomsorg - gir SKAL_IKKE_VURDERES hvis kun SKAL_IKKE_VURDERES`() {
        assertThat(utledResultatForVilkårSomGjelderFlereBarn(listOf(aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES))))
            .isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
    }

    @Test
    fun `Aleneomsorg - gir IKKE_TATT_STILLING_TIL om det finnes IKKE_TATT_STILLING_TIL og SKAL_IKKE_VURDERES`() {
        assertThat(utledResultatForVilkårSomGjelderFlereBarn(listOf(aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL, barn1))))
            .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL, barn1),
                    aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES, barn2),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `AlderPåBarn - gir IKKE_TATT_STILLING_TIL om det finnes IKKE_TATT_STILLING_TIL og SKAL_IKKE_VURDERES`() {
        assertThat(utledResultatForVilkårSomGjelderFlereBarn(listOf(alderPåBarn(Vilkårsresultat.IKKE_TATT_STILLING_TIL, barn1))))
            .isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    alderPåBarn(Vilkårsresultat.IKKE_TATT_STILLING_TIL, barn1),
                    alderPåBarn(Vilkårsresultat.SKAL_IKKE_VURDERES, barn2),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Aleneomsorg uten tilknyttet barn - gir SKAL_IKKE_VURDERES om det kun finnes IKKE_TATT_STILLING_TIL`() {
        assertThat(utledResultatForVilkårSomGjelderFlereBarn(listOf(aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL))))
            .isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL),
                    aleneomsorg(Vilkårsresultat.SKAL_IKKE_VURDERES),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `AlderPåBarn uten tilknyttet barn - gir SKAL_IKKE_VURDERES om det kun finnes IKKE_TATT_STILLING_TIL`() {
        assertThat(utledResultatForVilkårSomGjelderFlereBarn(listOf(alderPåBarn(Vilkårsresultat.IKKE_TATT_STILLING_TIL))))
            .isEqualTo(Vilkårsresultat.SKAL_IKKE_VURDERES)
        assertThat(
            utledResultatForVilkårSomGjelderFlereBarn(
                listOf(
                    alderPåBarn(Vilkårsresultat.IKKE_TATT_STILLING_TIL),
                    alderPåBarn(Vilkårsresultat.SKAL_IKKE_VURDERES),
                ),
            ),
        ).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `utledResultatForAleneomsorg - skal kaste feil hvis vilkåren inneholder noe annet enn aleneomsorg`() {
        val vilkårForSivilstand = aleneomsorg(Vilkårsresultat.IKKE_TATT_STILLING_TIL).copy(type = VilkårType.SIVILSTAND)
        assertThat(catchThrowable { utledResultatForVilkårSomGjelderFlereBarn(listOf(vilkårForSivilstand)) })
            .hasMessage("Denne metoden kan kun kalles med vilkår som kan ha flere barn")
    }

    @Test
    fun `erAlleVilkårVurdert - alle vilkåren er OPPFYLT`() {
        assertThat(erAlleVilkårTattStillingTil(listOf(Vilkårsresultat.OPPFYLT)))
            .isTrue
    }

    @Test
    fun `erAlleVilkårVurdert - skal kunne ha en kombinasjon av OPPFYLT og SKAL_IKKE_VURDERES`() {
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.OPPFYLT,
                    Vilkårsresultat.SKAL_IKKE_VURDERES,
                ),
            ),
        ).isTrue
    }

    @Test
    fun `erAlleVilkårVurdert - ett vilkår er IKKE_OPPFYLT og resten er gyldig`() {
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.IKKE_OPPFYLT,
                    Vilkårsresultat.OPPFYLT,
                ),
            ),
        ).isTrue
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.IKKE_OPPFYLT,
                    Vilkårsresultat.SKAL_IKKE_VURDERES,
                ),
            ),
        ).isTrue
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.IKKE_OPPFYLT,
                    Vilkårsresultat.IKKE_OPPFYLT,
                ),
            ),
        ).isTrue
    }

    @Test
    fun `erAlleVilkårVurdert - IKKE_TATT_STILLING_TIL skal gi false`() {
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.IKKE_OPPFYLT,
                    Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                ),
            ),
        ).isFalse
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.OPPFYLT,
                    Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                ),
            ),
        ).isFalse
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.SKAL_IKKE_VURDERES,
                    Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                ),
            ),
        ).isFalse
    }

    @Test
    fun `erAlleVilkårVurdert - SKAL_IKKE_VURDERES må være i en kombinasjon med IKKE_OPPFYLT`() {
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.SKAL_IKKE_VURDERES,
                    Vilkårsresultat.IKKE_OPPFYLT,
                ),
            ),
        ).isTrue

        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.SKAL_IKKE_VURDERES,
                    Vilkårsresultat.SKAL_IKKE_VURDERES,
                ),
            ),
        ).withFailMessage("Alle vilkår skal kunne være satt til SKAL_IKKE_VURDERES")
            .isTrue
        assertThat(
            erAlleVilkårTattStillingTil(
                listOf(
                    Vilkårsresultat.SKAL_IKKE_VURDERES,
                    Vilkårsresultat.OPPFYLT,
                ),
            ),
        ).withFailMessage("Alle vilkår skal kunne være satt til enten SKAL_IKKE_VURDERES eller OPPFYLT")
            .isTrue
    }

    @Test
    fun `Skal returnere behandlingKategori EØS for vilkårsvurderinger med EØS-unntak`() {
        val eøsMedlemskapUnntak =
            listOf(
                SvarId.MEDLEM_MER_ENN_5_ÅR_EØS,
                SvarId.MEDLEM_MER_ENN_5_ÅR_EØS_ANNEN_FORELDER_TRYGDEDEKKET_I_NORGE,
            )
        eøsMedlemskapUnntak.forEach {
            val nasjonalMedlemskapOgOpphold = opprettVurderingForEøsEllerNasjonal(it, null)
            assertThat(OppdaterVilkår.utledBehandlingKategori(nasjonalMedlemskapOgOpphold)).isEqualTo(BehandlingKategori.EØS)
        }

        val eøsOppholdUnntak = opprettVurderingForEøsEllerNasjonal(null, SvarId.OPPHOLDER_SEG_I_ANNET_EØS_LAND)
        assertThat(OppdaterVilkår.utledBehandlingKategori(eøsOppholdUnntak)).isEqualTo(BehandlingKategori.EØS)
    }

    @Test
    fun `Skal returnere behandlingKategori Nasjonal for vilkårsvurderinger uten EØS-unntak`() {
        val nasjonalMedlemskapOgOpphold = opprettVurderingForEøsEllerNasjonal(null, null)
        val nasjonaleMedlemskapUnntak =
            listOf(
                SvarId.MEDLEM_MER_ENN_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
                SvarId.MEDLEM_MER_ENN_7_ÅR_AVBRUDD_MER_ENN_10ÅR,
                SvarId.I_LANDET_FOR_GJENFORENING_ELLER_GIFTE_SEG,
                SvarId.ANDRE_FORELDER_MEDLEM_SISTE_5_ÅR,
                SvarId.ANDRE_FORELDER_MEDLEM_MINST_5_ÅR_AVBRUDD_MINDRE_ENN_10_ÅR,
                SvarId.ANDRE_FORELDER_MEDLEM_MINST_7_ÅR_AVBRUDD_MER_ENN_10_ÅR,
                SvarId.TOTALVURDERING_OPPFYLLER_FORSKRIFT,
            )
        val nasjonaleOppholdUnntak =
            listOf(
                SvarId.ARBEID_NORSK_ARBEIDSGIVER,
                SvarId.UTENLANDSOPPHOLD_MINDRE_ENN_6_UKER,
            )

        assertThat(OppdaterVilkår.utledBehandlingKategori(nasjonalMedlemskapOgOpphold)).isEqualTo(BehandlingKategori.NASJONAL)
        nasjonaleMedlemskapUnntak.forEach {
            val nasjonalMedlemskapOgOpphold = opprettVurderingForEøsEllerNasjonal(it, null)
            assertThat(OppdaterVilkår.utledBehandlingKategori(nasjonalMedlemskapOgOpphold)).isEqualTo(BehandlingKategori.NASJONAL)
        }
        nasjonaleOppholdUnntak.forEach {
            val nasjonalMedlemskapOgOpphold = opprettVurderingForEøsEllerNasjonal(null, it)
            assertThat(OppdaterVilkår.utledBehandlingKategori(nasjonalMedlemskapOgOpphold)).isEqualTo(BehandlingKategori.NASJONAL)
        }
    }

    private fun opprettVurderingForEøsEllerNasjonal(
        medlemskapUnntak: SvarId?,
        oppholdUnntak: SvarId?,
    ): List<Vilkårsvurdering> {
        val delvilkårForutgåendeMedlemskap =
            listOf(
                Delvilkårsvurdering(
                    Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                    listOf(
                        Vurdering(RegelId.SØKER_MEDLEM_I_FOLKETRYGDEN, medlemskapUnntak?.let { SvarId.NEI } ?: SvarId.JA),
                        medlemskapUnntak?.let { Vurdering(RegelId.MEDLEMSKAP_UNNTAK, it, begrunnelse = "Polen") },
                    ).filterNotNull(),
                ),
            )

        val delvilkårOpphold =
            listOf(
                Delvilkårsvurdering(
                    Vilkårsresultat.IKKE_TATT_STILLING_TIL,
                    listOf(
                        Vurdering(RegelId.BOR_OG_OPPHOLDER_SEG_I_NORGE, oppholdUnntak?.let { SvarId.NEI } ?: SvarId.JA),
                        oppholdUnntak?.let { Vurdering(RegelId.OPPHOLD_UNNTAK, it, begrunnelse = "Polen") },
                    ).filterNotNull(),
                ),
            )

        val forutgåendeMedlemskap =
            Vilkårsvurdering(
                behandlingId = UUID.randomUUID(),
                type = VilkårType.FORUTGÅENDE_MEDLEMSKAP,
                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårForutgåendeMedlemskap),
                opphavsvilkår = null,
            )

        val opphold =
            Vilkårsvurdering(
                behandlingId = UUID.randomUUID(),
                type = VilkårType.LOVLIG_OPPHOLD,
                delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårOpphold),
                opphavsvilkår = null,
            )

        return listOf(forutgåendeMedlemskap, opphold)
    }

    private fun aleneomsorg(
        vilkårsresultat: Vilkårsresultat,
        barnId: UUID? = null,
    ) = Vilkårsvurdering(
        behandlingId = UUID.randomUUID(),
        resultat = vilkårsresultat,
        type = VilkårType.ALENEOMSORG,
        delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()),
        opphavsvilkår = null,
        barnId = barnId,
    )

    private fun alderPåBarn(
        vilkårsresultat: Vilkårsresultat,
        barnId: UUID? = null,
    ) = Vilkårsvurdering(
        behandlingId = UUID.randomUUID(),
        resultat = vilkårsresultat,
        type = VilkårType.ALDER_PÅ_BARN,
        delvilkårsvurdering = DelvilkårsvurderingWrapper(emptyList()),
        opphavsvilkår = null,
        barnId = barnId,
    )

    private fun Vilkårsvurdering.delvilkår(regelId: RegelId) =
        this.delvilkårsvurdering.delvilkårsvurderinger.singleOrNull { it.hovedregel == regelId }
            ?: error("Finner ikke regelId=$regelId blant ${this.delvilkårsvurdering.delvilkårsvurderinger.map { it.hovedregel }}")

    private fun Vilkårsvurdering.førsteDelvilkår() = this.delvilkårsvurdering.delvilkårsvurderinger.first()

    private fun delvilkårsvurderingDto(vararg vurderinger: VurderingDto) = DelvilkårsvurderingDto(resultat = Vilkårsresultat.IKKE_AKTUELL, vurderinger = vurderinger.toList())

    private fun validerOgOppdater(
        vilkårsvurdering: Vilkårsvurdering,
        regel: Vilkårsregel,
        vararg vurderinger: VurderingDto,
    ): Vilkårsvurdering = validerOgOppdater(vilkårsvurdering, regel, delvilkårsvurderingDto(*vurderinger))

    private fun validerOgOppdater(
        vilkårsvurdering: Vilkårsvurdering,
        regel: Vilkårsregel,
        vararg delvilkårsvurderingDto: DelvilkårsvurderingDto,
    ): Vilkårsvurdering =
        OppdaterVilkår.lagNyOppdatertVilkårsvurdering(
            vilkårsvurdering = vilkårsvurdering,
            vilkårsregler = mapOf(regel.vilkårType to regel),
            oppdatering = delvilkårsvurderingDto.toList(),
        )

    private fun opprettVurdering(regel: Vilkårsregel): Vilkårsvurdering {
        val delvilkårsvurderinger =
            regel.hovedregler.map {
                Delvilkårsvurdering(resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL, vurderinger = listOf(Vurdering(it)))
            }
        return Vilkårsvurdering(
            behandlingId = UUID.randomUUID(),
            resultat = Vilkårsresultat.IKKE_TATT_STILLING_TIL,
            type = VilkårType.ALENEOMSORG,
            delvilkårsvurdering = DelvilkårsvurderingWrapper(delvilkårsvurderinger),
            opphavsvilkår = null,
        )
    }
}
