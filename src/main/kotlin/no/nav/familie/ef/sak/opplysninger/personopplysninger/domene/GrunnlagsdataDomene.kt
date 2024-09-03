package no.nav.familie.ef.sak.opplysninger.personopplysninger.domene

import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikkBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataPeriodeHistorikkOvergangsstønad
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Dødsfall
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.FolkeregisteridentifikatorStatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Opphold
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Endringer i denne filen burde godkjennes av 2 personer då denne er lagret som json i databasen og breaking changes kan være
 * kritiske
 */

data class GrunnlagsdataMedMetadata(
    val grunnlagsdata: GrunnlagsdataDomene,
    val opprettetTidspunkt: LocalDateTime,
) {
    fun endringerMellom(tidligereGrunnlagsdata: GrunnlagsdataMedMetadata): List<GrunnlagsdataEndring> =
        GrunnlagsdataEndring.values().filter {
            when (it) {
                GrunnlagsdataEndring.BARN -> erBarnForskjelligMed(tidligereGrunnlagsdata)
                GrunnlagsdataEndring.SIVILSTAND -> erSivilstandOppdatertForskjelligMed(tidligereGrunnlagsdata)
                GrunnlagsdataEndring.ADRESSE_SØKER -> erAdresseForSøkerForskjelligMed(tidligereGrunnlagsdata)
                GrunnlagsdataEndring.ADRESSE_ANNEN_FORELDER -> erAdresserForAnnenForelderForskjelligMed(tidligereGrunnlagsdata)
            }
        }

    private fun erAdresseForSøkerForskjelligMed(tidligereGrunnlagsdata: GrunnlagsdataMedMetadata): Boolean = tidligereGrunnlagsdata.grunnlagsdata.søker.bostedsadresse != this.grunnlagsdata.søker.bostedsadresse

    private fun erAdresserForAnnenForelderForskjelligMed(tidligereGrunnlagsdata: GrunnlagsdataMedMetadata): Boolean {
        val harAnnenForelderEndretAdresse =
            tidligereGrunnlagsdata.grunnlagsdata.annenForelder.any { tidligereAnnenForelder ->
                val annenForelder = this.grunnlagsdata.annenForelder.find { it.personIdent == tidligereAnnenForelder.personIdent }
                tidligereAnnenForelder.bostedsadresse != annenForelder?.bostedsadresse
            }
        return harAnnenForelderEndretAdresse
    }

    private fun erSivilstandOppdatertForskjelligMed(tidligereGrunnlagsdata: GrunnlagsdataMedMetadata): Boolean = this.grunnlagsdata.søker.sivilstand != tidligereGrunnlagsdata.grunnlagsdata.søker.sivilstand

    private fun erBarnForskjelligMed(tidligereGrunnlagsdata: GrunnlagsdataMedMetadata): Boolean = this.grunnlagsdata.barn != tidligereGrunnlagsdata.grunnlagsdata.barn
}

data class GrunnlagsdataDomene(
    val søker: Søker,
    val annenForelder: List<AnnenForelderMedIdent>,
    val medlUnntak: Medlemskapsinfo,
    val barn: List<BarnMedIdent>,
    val tidligereVedtaksperioder: TidligereVedtaksperioder?,
    val harAvsluttetArbeidsforhold: Boolean?,
    val harKontantstøttePerioder: Boolean?,
) {
    fun tilPersonopplysninger() = Personopplysninger(søker = this.søker, annenForelder = this.annenForelder, barn = this.barn)
}

data class Personopplysninger(
    val søker: Søker,
    val annenForelder: List<AnnenForelderMedIdent>,
    val barn: List<BarnMedIdent>,
)

data class Søker(
    val adressebeskyttelse: Adressebeskyttelse?, // Er en liste i PDLSøker
    val bostedsadresse: List<Bostedsadresse>,
    val dødsfall: Dødsfall?, // Er en liste i PDLSøker
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
    val fødsel: List<Fødsel>, // Er en liste i PDLSøker
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
    val fullmakt: List<FullmaktMedNavn>,
    val kjønn: KjønnType,
    val kontaktadresse: List<Kontaktadresse>,
    val navn: Navn,
    val opphold: List<Opphold>,
    val oppholdsadresse: List<Oppholdsadresse>,
    val sivilstand: List<SivilstandMedNavn>,
    val statsborgerskap: List<Statsborgerskap>,
    val innflyttingTilNorge: List<InnflyttingTilNorge>,
    val utflyttingFraNorge: List<UtflyttingFraNorge>,
    val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>,
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>?,
)

data class AnnenForelderMedIdent(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val bostedsadresse: List<Bostedsadresse>,
    val dødsfall: List<Dødsfall>,
    val fødsel: List<Fødsel>,
    val navn: Navn,
    val personIdent: String,
    val folkeregisteridentifikator: List<Folkeregisteridentifikator>?,
    val tidligereVedtaksperioder: TidligereVedtaksperioder?,
)

data class BarnMedIdent(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val bostedsadresse: List<Bostedsadresse>,
    val deltBosted: List<DeltBosted>,
    val dødsfall: List<Dødsfall>,
    val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
    val fødsel: List<Fødsel>,
    val navn: Navn,
    val personIdent: String,
    val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>?,
)

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String,
    val relatertPersonsRolle: Familierelasjonsrolle,
    val minRolleForPerson: Familierelasjonsrolle?,
)

data class Folkeregisteridentifikator(
    val personIdent: String,
    val status: FolkeregisteridentifikatorStatus,
    val historisk: Boolean,
)

data class SivilstandMedNavn(
    val type: Sivilstandstype,
    val gyldigFraOgMed: LocalDate?,
    val relatertVedSivilstand: String?,
    val bekreftelsesdato: LocalDate?,
    val dødsfall: Dødsfall?, // Er en liste i PDLSøker
    val navn: String?,
    val metadata: Metadata,
)

data class FullmaktMedNavn(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate?,
    val motpartsPersonident: String,
    val navn: String?,
    val områder: List<String>?,
)

/**
 * @param historiskPensjon kalles også Infotrygd PE PP, infotrygdperioder før desember 2008
 */
data class TidligereVedtaksperioder(
    val infotrygd: TidligereInnvilgetVedtak,
    val sak: TidligereInnvilgetVedtak? = null,
    val historiskPensjon: Boolean? = null,
)

data class TidligereInnvilgetVedtak(
    val harTidligereOvergangsstønad: Boolean = false,
    val harTidligereBarnetilsyn: Boolean = false,
    val harTidligereSkolepenger: Boolean = false,
    val periodeHistorikkOvergangsstønad: List<GrunnlagsdataPeriodeHistorikkOvergangsstønad> = emptyList(),
    val periodeHistorikkBarnetilsyn: List<GrunnlagsdataPeriodeHistorikkBarnetilsyn> = emptyList(),
)

enum class GrunnlagsdataEndring {
    BARN,
    ADRESSE_ANNEN_FORELDER,
    ADRESSE_SØKER,
    SIVILSTAND,
}
