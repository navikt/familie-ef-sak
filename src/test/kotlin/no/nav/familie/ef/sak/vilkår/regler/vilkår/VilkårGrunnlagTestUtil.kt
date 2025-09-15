package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.regler.vilkår

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.InnflyttingDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.UtflyttingDto
import no.nav.familie.ef.sak.vilkår.dto.AnnenForelderDto
import no.nav.familie.ef.sak.vilkår.dto.AvstandTilSøkerDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnMedSamværSøknadsgrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.BarnepassDto
import no.nav.familie.ef.sak.vilkår.dto.LangAvstandTilSøker
import no.nav.familie.ef.sak.vilkår.dto.MedlUnntakDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapRegistergrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.MedlemskapSøknadsgrunnlagDto
import no.nav.familie.ef.sak.vilkår.dto.PersonaliaDto
import no.nav.familie.ef.sak.vilkår.dto.StatsborgerskapDto
import java.time.LocalDate
import java.util.UUID

fun medlemskapDto(
    land: String = "norge",
    folkeregisterpersonstatus: Folkeregisterpersonstatus = Folkeregisterpersonstatus.BOSATT,
    innflytting: List<InnflyttingDto> = emptyList(),
    utflytting: List<UtflyttingDto> = emptyList(),
    oppholderDuDegINorge: Boolean = true,
    søknadsgrunnlag: MedlemskapSøknadsgrunnlagDto =
        MedlemskapSøknadsgrunnlagDto(
            bosattNorgeSisteÅrene = true,
            oppholderDuDegINorge = oppholderDuDegINorge,
            utenlandsopphold = emptyList(),
        ),
) = MedlemskapDto(
    søknadsgrunnlag = søknadsgrunnlag,
    registergrunnlag =
        MedlemskapRegistergrunnlagDto(
            nåværendeStatsborgerskap = listOf(),
            statsborgerskap = listOf(StatsborgerskapDto(land = land, null, null)),
            oppholdstatus = emptyList(),
            bostedsadresse = emptyList(),
            innflytting = innflytting,
            utflytting = utflytting,
            folkeregisterpersonstatus = folkeregisterpersonstatus,
            medlUnntak = MedlUnntakDto(emptyList()),
        ),
)

fun personaliaDto(fødeland: String = "NOR") =
    PersonaliaDto(
        navn = NavnDto(fornavn = "Ola", null, etternavn = "etternavn", visningsnavn = "visningsnavn"),
        personIdent = "111111111",
        bostedsadresse = null,
        fødeland = fødeland,
    )

fun barnMedSamværListe(
    antall: Int = 2,
    harSammeAdresse: Boolean = true,
) = (1..antall).map {
    barnMedSamværDto(harSammeAdresse = harSammeAdresse)
}

fun barnMedSamværDto(
    barnId: UUID = UUID.randomUUID(),
    harSammeAdresse: Boolean,
) = BarnMedSamværDto(
    barnId,
    søknadsgrunnlag = tomtSøknadsgrunnlag(),
    registergrunnlag =
        BarnMedSamværRegistergrunnlagDto(
            UUID.randomUUID(),
            "navn",
            "fnr",
            harSammeAdresse = harSammeAdresse,
            emptyList(),
            false,
            AnnenForelderDto(
                "navn",
                "fnr2",
                LocalDate.now().minusYears(23),
                true,
                "Norge",
                "Vei 1B",
                null,
                null,
                AvstandTilSøkerDto(null, LangAvstandTilSøker.UKJENT),
                erKopiertFraAnnetBarn = null,
            ),
            null,
            null,
            Folkeregisterpersonstatus.BOSATT,
            null,
        ),
    barnepass =
        BarnepassDto(
            barnId,
            skalHaBarnepass = true,
            barnepassordninger = listOf(),
            årsakBarnepass = null,
        ),
)

fun terminbarnSøknadsgrunnlag() = tomtSøknadsgrunnlag().copy(fødselTermindato = LocalDate.now().plusMonths(2))

fun tomtSøknadsgrunnlag() =
    BarnMedSamværSøknadsgrunnlagDto(
        UUID.randomUUID(),
        fødselTermindato = null,
        navn = null,
        harSammeAdresse = null,
        skalBoBorHosSøker = null,
        forelder = null,
        ikkeOppgittAnnenForelderBegrunnelse = null,
        spørsmålAvtaleOmDeltBosted = null,
        skalAnnenForelderHaSamvær = null,
        harDereSkriftligAvtaleOmSamvær = null,
        hvordanPraktiseresSamværet = null,
        borAnnenForelderISammeHus = null,
        borAnnenForelderISammeHusBeskrivelse = null,
        harDereTidligereBoddSammen = null,
        nårFlyttetDereFraHverandre = null,
        hvorMyeErDuSammenMedAnnenForelder = null,
        beskrivSamværUtenBarn = null,
    )
