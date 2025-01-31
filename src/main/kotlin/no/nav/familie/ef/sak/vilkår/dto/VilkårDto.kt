package no.nav.familie.ef.sak.vilkår.dto

import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.KontantstøttePeriode
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AdresseDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.søknad.domain.DokumentasjonFraSøknadDto
import java.time.LocalDateTime

data class VilkårDto(
    val vurderinger: List<VilkårsvurderingDto>,
    val grunnlag: VilkårGrunnlagDto,
)

data class VilkårGrunnlagDto(
    val personalia: PersonaliaDto,
    val tidligereVedtaksperioder: TidligereVedtaksperioderDto,
    val medlemskap: MedlemskapDto,
    val sivilstand: SivilstandInngangsvilkårDto,
    val bosituasjon: BosituasjonDto?,
    val barnMedSamvær: List<BarnMedSamværDto>,
    val sivilstandsplaner: SivilstandsplanerDto,
    val aktivitet: AktivitetDto?,
    val sagtOppEllerRedusertStilling: SagtOppEllerRedusertStillingDto?, // Gjelder OS
    val registeropplysningerOpprettetTid: LocalDateTime,
    val adresseopplysninger: AdresseopplysningerDto?,
    val dokumentasjon: DokumentasjonFraSøknadDto?,
    val harAvsluttetArbeidsforhold: Boolean?,
    val harKontantstøttePerioder: Boolean?,
    val kontantstøttePerioder: List<KontantstøttePeriode>,
)

data class PersonaliaDto(
    val navn: NavnDto,
    val personIdent: String,
    val bostedsadresse: AdresseDto?,
    val fødeland: String?,
)

data class AdresseopplysningerDto(
    val søkerBorPåRegistrertAdresse: Boolean,
    val adresse: String?,
    val harMeldtAdresseendring: Boolean?,
)
