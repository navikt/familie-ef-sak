package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår

import io.mockk.mockk
import no.nav.familie.ef.sak.opplysninger.søknad.domain.DokumentasjonFraSøknadDto
import no.nav.familie.ef.sak.vilkår.dto.AdresseopplysningerDto
import no.nav.familie.ef.sak.vilkår.dto.AktivitetDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BosituasjonDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapDto
import no.nav.familie.ef.sak.vilkår.dto.SagtOppEllerRedusertStillingDto
import no.nav.familie.ef.sak.vilkår.dto.SivilstandInngangsvilkårDto
import no.nav.familie.ef.sak.vilkår.dto.SivilstandsplanerDto
import no.nav.familie.ef.sak.vilkår.dto.TidligereVedtaksperioderDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårGrunnlagDto
import java.time.LocalDateTime

object VilkårTestUtil {

    fun mockVilkårGrunnlagDto(
        tidligereVedtaksperioder: TidligereVedtaksperioderDto = mockk(relaxed = true),
        medlemskap: MedlemskapDto = mockk(relaxed = true),
        sivilstand: SivilstandInngangsvilkårDto = mockk(relaxed = true),
        bosituasjon: BosituasjonDto? = mockk(relaxed = true),
        barnMedSamvær: List<BarnMedSamværDto> = mockk(relaxed = true),
        sivilstandsplaner: SivilstandsplanerDto = mockk(relaxed = true),
        aktivitet: AktivitetDto? = mockk(relaxed = true),
        sagtOppEllerRedusertStilling: SagtOppEllerRedusertStillingDto? = mockk(relaxed = true),
        lagtTilEtterFerdigstilling: Boolean = false,
        registeropplysningerOpprettetTid: LocalDateTime = mockk(relaxed = true),
        adresseopplysninger: AdresseopplysningerDto = mockk(relaxed = true),
        dokumentasjon: DokumentasjonFraSøknadDto? = mockk(relaxed = true)
    ) =
        VilkårGrunnlagDto(
            tidligereVedtaksperioder = tidligereVedtaksperioder,
            medlemskap = medlemskap,
            sivilstand = sivilstand,
            bosituasjon = bosituasjon,
            barnMedSamvær = barnMedSamvær,
            sivilstandsplaner = sivilstandsplaner,
            aktivitet = aktivitet,
            sagtOppEllerRedusertStilling = sagtOppEllerRedusertStilling,
            lagtTilEtterFerdigstilling = lagtTilEtterFerdigstilling,
            registeropplysningerOpprettetTid = registeropplysningerOpprettetTid,
            adresseopplysninger = adresseopplysninger,
            dokumentasjon = dokumentasjon
        )
}
