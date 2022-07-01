package no.nav.familie.ef.sak.opplysninger.personopplysninger.domene

import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.DeltBosted
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Dødsfall
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kontaktadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Opphold
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Statsborgerskap
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Telefonnummer
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.TilrettelagtKommunikasjon
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
    val lagtTilEtterFerdigstilling: Boolean,
    val opprettetTidspunkt: LocalDateTime
)

data class GrunnlagsdataDomene(
    val søker: Søker,
    val annenForelder: List<AnnenForelderMedIdent>,
    val medlUnntak: Medlemskapsinfo,
    val barn: List<BarnMedIdent>,
    val tidligereVedtaksperioder: TidligereVedtaksperioder?
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
    val telefonnummer: List<Telefonnummer>,
    val tilrettelagtKommunikasjon: List<TilrettelagtKommunikasjon>,
    val innflyttingTilNorge: List<InnflyttingTilNorge>,
    val utflyttingFraNorge: List<UtflyttingFraNorge>,
    val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>
)

data class AnnenForelderMedIdent(
    val adressebeskyttelse: List<Adressebeskyttelse>,
    val bostedsadresse: List<Bostedsadresse>,
    val dødsfall: List<Dødsfall>,
    val fødsel: List<Fødsel>,
    val navn: Navn,
    val personIdent: String,
    val tidligereVedtaksperioder: TidligereVedtaksperioder?
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
)

data class ForelderBarnRelasjon(
    val relatertPersonsIdent: String,
    val relatertPersonsRolle: Familierelasjonsrolle,
    val minRolleForPerson: Familierelasjonsrolle?
)

data class SivilstandMedNavn(
    val type: Sivilstandstype,
    val gyldigFraOgMed: LocalDate?,
    val relatertVedSivilstand: String?,
    val bekreftelsesdato: LocalDate?,
    val dødsfall: Dødsfall?, // Er en liste i PDLSøker
    val navn: String?,
    val metadata: Metadata
)

data class FullmaktMedNavn(
    val gyldigFraOgMed: LocalDate,
    val gyldigTilOgMed: LocalDate,
    val motpartsPersonident: String,
    val navn: String?
)

data class TidligereVedtaksperioder(
    val infotrygd: TidligereInnvilgetVedtak,
    val sak: TidligereInnvilgetVedtak? = null
)

data class TidligereInnvilgetVedtak(
    val harTidligereOvergangsstønad: Boolean = false,
    val harTidligereBarnetilsyn: Boolean = false,
    val harTidligereSkolepenger: Boolean = false
)
