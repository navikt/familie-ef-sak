package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Sivilstand
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.vilkår.Vilkårsresultat
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import no.nav.familie.ef.sak.vilkår.regler.vilkår.SivilstandRegel
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SivilstandRegelTest {

    val hovedregelMetadataMock = mockk<HovedregelMetadata>()

    @BeforeEach
    fun setup() {
        every { hovedregelMetadataMock.behandling } returns behandling()
    }

    @Test
    fun `Automatisk vurder sivilstand vilkår hvor søker er skilt og svart nei på uformelt gift eller skilt i søknad`() {
        every { hovedregelMetadataMock.sivilstandstype } returns Sivilstandstype.SKILT
        every { hovedregelMetadataMock.sivilstandSøknad } returns Sivilstand(erUformeltGift = false, erUformeltSeparertEllerSkilt = false)

        val listDelvilkårsvurdering = SivilstandRegel().initiereDelvilkårsvurdering(hovedregelMetadataMock, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.AUTOMATISK_OPPFYLT)
    }

    @Test
    fun `Ikke automatisk vurder sivilstand vilkår hvor søker har svart ja på spørsmål i søknad om er uformelt gift`() {
        every { hovedregelMetadataMock.sivilstandstype } returns Sivilstandstype.UGIFT
        every { hovedregelMetadataMock.sivilstandSøknad } returns Sivilstand(erUformeltGift = true, erUformeltSeparertEllerSkilt = false)

        val listDelvilkårsvurdering = SivilstandRegel().initiereDelvilkårsvurdering(hovedregelMetadataMock, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `Skal ikke automatisk vurdere sivilstand vilkår hvor søker er gift`() {
        every { hovedregelMetadataMock.sivilstandstype } returns Sivilstandstype.GIFT
        every { hovedregelMetadataMock.sivilstandSøknad } returns Sivilstand(erUformeltGift = false, erUformeltSeparertEllerSkilt = false)

        val listDelvilkårsvurdering = SivilstandRegel().initiereDelvilkårsvurdering(hovedregelMetadataMock, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }

    @Test
    fun `skal ikke automatisk vurdere sivilstand vilkår hvis ikke sendt inn søknad digitalt`() {
        every { hovedregelMetadataMock.behandling } returns behandling(årsak = BehandlingÅrsak.PAPIRSØKNAD)
        every { hovedregelMetadataMock.sivilstandstype } returns Sivilstandstype.GIFT
        every { hovedregelMetadataMock.sivilstandSøknad } returns Sivilstand(erUformeltGift = true, erUformeltSeparertEllerSkilt = false)

        val listDelvilkårsvurdering = SivilstandRegel().initiereDelvilkårsvurdering(hovedregelMetadataMock, Vilkårsresultat.IKKE_TATT_STILLING_TIL)

        Assertions.assertThat(listDelvilkårsvurdering.first().resultat).isEqualTo(Vilkårsresultat.IKKE_TATT_STILLING_TIL)
    }
}
