package no.nav.familie.ef.sak.domene

import no.nav.familie.ef.sak.api.dto.SivilstandRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.integration.dto.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.DeltBosted
import no.nav.familie.ef.sak.integration.dto.pdl.Dødsfall
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.integration.dto.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.integration.dto.pdl.Fullmakt
import no.nav.familie.ef.sak.integration.dto.pdl.Fødsel
import no.nav.familie.ef.sak.integration.dto.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.integration.dto.pdl.Kjønn
import no.nav.familie.ef.sak.integration.dto.pdl.Kontaktadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Metadata
import no.nav.familie.ef.sak.integration.dto.pdl.Navn
import no.nav.familie.ef.sak.integration.dto.pdl.Opphold
import no.nav.familie.ef.sak.integration.dto.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.Statsborgerskap
import no.nav.familie.ef.sak.integration.dto.pdl.Telefonnummer
import no.nav.familie.ef.sak.integration.dto.pdl.TilrettelagtKommunikasjon
import no.nav.familie.ef.sak.integration.dto.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.integration.dto.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import java.time.LocalDate

data class Grunnlagsdata(val søker: Søker,
                         val annenForelder: List<AnnenForelderMedIdent>,
                         val medlUnntak: Medlemskapsinfo,
                         val barn: List<Barn>
)

data class Søker(val adressebeskyttelse: Adressebeskyttelse, //Er en liste i PDLSøker
                 val bostedsadresse: List<Bostedsadresse>,
                 val dødsfall: Dødsfall, //Er en liste i PDLSøker
                 val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
                 val fødsel: Fødsel, //Er en liste i PDLSøker
                 val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
                 val fullmakt: List<Fullmakt>,
                 val kjønn: Kjønn,
                 val kontaktadresse: List<Kontaktadresse>,
                 val navn: List<Navn>,
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
        val navn: List<Navn>,
        val opphold: List<Opphold>,
        val oppholdsadresse: List<Oppholdsadresse>,
        val statsborgerskap: List<Statsborgerskap>,
        val innflyttingTilNorge: List<InnflyttingTilNorge>,
        val utflyttingFraNorge: List<UtflyttingFraNorge>,
        val personIdent: String
)

data class Barn(
        val adressebeskyttelse: List<Adressebeskyttelse>,
        val bostedsadresse: List<Bostedsadresse>,
        val deltBosted: List<DeltBosted>,
        val dødsfall: List<Dødsfall>,
        val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
        val fødsel: List<Fødsel>,
        val navn: List<Navn>,
        val personIdent: String,
)

data class SivilstandMedNavn(val type: Sivilstandstype,
                             val gyldigFraOgMed: LocalDate?,
                             val relatertVedSivilstand: String?,
                             val bekreftelsesdato: String?,
                             val navn: String?,
                             val metadata: Metadata)


fun AnnenForelderMedIdent.tilPdlAnnenForelder(): PdlAnnenForelder = PdlAnnenForelder(
        adressebeskyttelse = this.adressebeskyttelse,
        bostedsadresse = this.bostedsadresse,
        dødsfall = this.dødsfall,
        fødsel = this.fødsel,
        navn = this.navn,
        opphold = this.opphold,
        oppholdsadresse = this.oppholdsadresse,
        statsborgerskap = this.statsborgerskap,
        innflyttingTilNorge = this.innflyttingTilNorge,
        utflyttingFraNorge = this.utflyttingFraNorge
)

fun Barn.tilPdlBarn(): PdlBarn = PdlBarn(
        adressebeskyttelse = this.adressebeskyttelse,
        bostedsadresse = this.bostedsadresse,
        deltBosted = this.deltBosted,
        dødsfall = this.dødsfall,
        forelderBarnRelasjon = this.forelderBarnRelasjon,
        fødsel = this.fødsel,
        navn = this.navn
)

fun SivilstandMedNavn.tilSivilstandRegistergrunnlagDto(): SivilstandRegistergrunnlagDto = SivilstandRegistergrunnlagDto(this.type,
                                                                                                                        gyldigFraOgMed = this.gyldigFraOgMed,
                                                                                                                        navn = this.navn)