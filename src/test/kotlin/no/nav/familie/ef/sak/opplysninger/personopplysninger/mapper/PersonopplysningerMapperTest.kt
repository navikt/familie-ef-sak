package no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.arbeidsfordeling.Arbeidsfordelingsenhet
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype.GIFT
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype.SEPARERT
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype.SKILT
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.repository.sivilstand
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

internal class PersonopplysningerMapperTest {

    val kodeverkService = KodeverkServiceMock().kodeverkService()
    val adresseMapper = AdresseMapper(kodeverkService = kodeverkService)
    val statsborgerskapMapper = StatsborgerskapMapper(kodeverkService = kodeverkService)
    val innflyttingUtflyttingMapper = InnflyttingUtflyttingMapper(kodeverkService = kodeverkService)
    val arbeidsfordelingService = mockk<ArbeidsfordelingService>()
    val personopplysningerMapper = PersonopplysningerMapper(
        adresseMapper = adresseMapper,
        statsborgerskapMapper = statsborgerskapMapper,
        innflyttingUtflyttingMapper = innflyttingUtflyttingMapper,
        arbeidsfordelingService = arbeidsfordelingService
    )

    @BeforeEach
    internal fun setUp() {
        every { arbeidsfordelingService.hentNavEnhet(any()) } returns Arbeidsfordelingsenhet("4489", "NAY")
    }

    @Test
    internal fun `skal mappe personopplysninger og sortere sivilstand på gjeldende - dernest dato`() {
        val giftFørsteGang = sivilstand(
            type = GIFT,
            gyldigFraOgMed = LocalDate.now().minusDays(100),
            metadata = PdlTestdataHelper.metadataHistorisk
        )
        val separert = sivilstand(
            type = SEPARERT,
            gyldigFraOgMed = LocalDate.now(),
            metadata = PdlTestdataHelper.metadataHistorisk
        )
        val gifteMålOpphørt = sivilstand(
            type = GIFT,
            gyldigFraOgMed = LocalDate.now().minusDays(100),
            metadata = PdlTestdataHelper.metadataGjeldende
        )

        val sivilstandMedSeparasjonSomErOpphørt = listOf(
            giftFørsteGang,
            separert,
            gifteMålOpphørt
        )

        val søker = opprettGrunnlagsdata().søker
        val grunnlagsdata = opprettGrunnlagsdata().copy(
            søker = søker.copy(
                sivilstand = sivilstandMedSeparasjonSomErOpphørt,
                fødsel = listOf(
                    PdlTestdataHelper.fødsel(LocalDate.now())
                )
            )
        )
        val personOpplysninger = personopplysningerMapper.tilPersonopplysninger(
            grunnlagsdataMedMetadata = GrunnlagsdataMedMetadata(grunnlagsdata, false, LocalDateTime.now()),
            egenAnsatt = false,
            søkerIdenter = PdlIdenter(
                listOf(PdlIdent("11223344551", false))
            )
        )

        Assertions.assertThat(personOpplysninger.sivilstand[0].erGjeldende).isEqualTo(true)
        Assertions.assertThat(personOpplysninger.sivilstand[0].type).isEqualTo(gifteMålOpphørt.type)
        Assertions.assertThat(personOpplysninger.sivilstand[0].gyldigFraOgMed).isEqualTo(gifteMålOpphørt.gyldigFraOgMed)

        Assertions.assertThat(personOpplysninger.sivilstand[1].erGjeldende).isEqualTo(false)
        Assertions.assertThat(personOpplysninger.sivilstand[1].type).isEqualTo(separert.type)
        Assertions.assertThat(personOpplysninger.sivilstand[1].gyldigFraOgMed).isEqualTo(separert.gyldigFraOgMed)

        Assertions.assertThat(personOpplysninger.sivilstand[2].erGjeldende).isEqualTo(false)
        Assertions.assertThat(personOpplysninger.sivilstand[2].type).isEqualTo(giftFørsteGang.type)
        Assertions.assertThat(personOpplysninger.sivilstand[2].gyldigFraOgMed).isEqualTo(giftFørsteGang.gyldigFraOgMed)
    }

    @Test
    internal fun `skal mappe personopplysninger og sortere sivilstand`() {
        val giftFørsteGang = sivilstand(
            type = GIFT,
            gyldigFraOgMed = LocalDate.now().minusDays(100),
            metadata = PdlTestdataHelper.metadataHistorisk
        )
        val separert = sivilstand(
            type = SEPARERT,
            gyldigFraOgMed = LocalDate.now().minusDays(85),
            metadata = PdlTestdataHelper.metadataHistorisk
        )
        val skilsmisse = sivilstand(
            type = SKILT,
            gyldigFraOgMed = LocalDate.now().minusDays(40),
            metadata = PdlTestdataHelper.metadataHistorisk
        )

        val giftPåNy = sivilstand(
            type = GIFT,
            gyldigFraOgMed = LocalDate.now().minusDays(20),
            metadata = PdlTestdataHelper.metadataGjeldende
        )

        val sivilstandMedSeparasjonSomErOpphørt = listOf(
            separert,
            giftPåNy,
            skilsmisse,
            giftFørsteGang
        )

        val søker = opprettGrunnlagsdata().søker
        val grunnlagsdata = opprettGrunnlagsdata().copy(
            søker = søker.copy(
                sivilstand = sivilstandMedSeparasjonSomErOpphørt,
                fødsel = listOf(
                    PdlTestdataHelper.fødsel(LocalDate.now())
                )
            )
        )
        val personOpplysninger = personopplysningerMapper.tilPersonopplysninger(
            grunnlagsdataMedMetadata = GrunnlagsdataMedMetadata(grunnlagsdata, false, LocalDateTime.now()),
            egenAnsatt = false,
            søkerIdenter = PdlIdenter(
                listOf(PdlIdent("11223344551", false))
            )
        )

        Assertions.assertThat(personOpplysninger.sivilstand[0].erGjeldende).isEqualTo(true)
        Assertions.assertThat(personOpplysninger.sivilstand[0].gyldigFraOgMed).isEqualTo(giftPåNy.gyldigFraOgMed)
        Assertions.assertThat(personOpplysninger.sivilstand[0].type).isEqualTo(giftPåNy.type)

        Assertions.assertThat(personOpplysninger.sivilstand[1].erGjeldende).isEqualTo(false)
        Assertions.assertThat(personOpplysninger.sivilstand[1].type).isEqualTo(skilsmisse.type)
        Assertions.assertThat(personOpplysninger.sivilstand[1].gyldigFraOgMed).isEqualTo(skilsmisse.gyldigFraOgMed)

        Assertions.assertThat(personOpplysninger.sivilstand[2].erGjeldende).isEqualTo(false)
        Assertions.assertThat(personOpplysninger.sivilstand[2].type).isEqualTo(separert.type)
        Assertions.assertThat(personOpplysninger.sivilstand[2].gyldigFraOgMed).isEqualTo(separert.gyldigFraOgMed)

        Assertions.assertThat(personOpplysninger.sivilstand[3].erGjeldende).isEqualTo(false)
        Assertions.assertThat(personOpplysninger.sivilstand[3].type).isEqualTo(giftFørsteGang.type)
        Assertions.assertThat(personOpplysninger.sivilstand[3].gyldigFraOgMed).isEqualTo(giftFørsteGang.gyldigFraOgMed)
    }
}
