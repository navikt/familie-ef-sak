package no.nav.familie.ef.sak.domene

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.ef.sak.api.dto.AdresseDto
import no.nav.familie.ef.sak.api.dto.BarnMedSamværRegistergrunnlagDto
import no.nav.familie.ef.sak.api.dto.InnflyttingDto
import no.nav.familie.ef.sak.api.dto.MedlUnntakDto
import no.nav.familie.ef.sak.api.dto.OppholdstillatelseDto
import no.nav.familie.ef.sak.api.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.api.dto.StatsborgerskapDto
import no.nav.familie.ef.sak.api.dto.UtflyttingDto
import no.nav.familie.ef.sak.integration.dto.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.integration.dto.pdl.Bostedsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Dødsfall
import no.nav.familie.ef.sak.integration.dto.pdl.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.integration.dto.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.integration.dto.pdl.Fullmakt
import no.nav.familie.ef.sak.integration.dto.pdl.Fødsel
import no.nav.familie.ef.sak.integration.dto.pdl.InnflyttingTilNorge
import no.nav.familie.ef.sak.integration.dto.pdl.Kjønn
import no.nav.familie.ef.sak.integration.dto.pdl.Kontaktadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Navn
import no.nav.familie.ef.sak.integration.dto.pdl.Opphold
import no.nav.familie.ef.sak.integration.dto.pdl.Oppholdsadresse
import no.nav.familie.ef.sak.integration.dto.pdl.Sivilstand
import no.nav.familie.ef.sak.integration.dto.pdl.Statsborgerskap
import no.nav.familie.ef.sak.integration.dto.pdl.Telefonnummer
import no.nav.familie.ef.sak.integration.dto.pdl.TilrettelagtKommunikasjon
import no.nav.familie.ef.sak.integration.dto.pdl.UtflyttingFraNorge
import no.nav.familie.ef.sak.integration.dto.pdl.VergemaalEllerFremtidsfullmakt
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo

data class Grunnlagsdata (
        val adressebeskyttelse: Adressebeskyttelse,
        val bostedsadresse: List<Bostedsadresse>,
        val dødsfall: Dødsfall,
        val forelderBarnRelasjon: List<ForelderBarnRelasjon>,
        val fødsel: Fødsel,
        val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
        val fullmakt: List<Fullmakt>,
        val kjønn: Kjønn,
        val kontaktadresse: List<Kontaktadresse>,
        val navn: List<Navn>,
        val opphold: List<Opphold>,
        val oppholdsadresse: List<Oppholdsadresse>,
        val sivilstand: List<Sivilstand>,
        val statsborgerskap: List<Statsborgerskap>,
        val telefonnummer: List<Telefonnummer>,
        val tilrettelagtKommunikasjon: List<TilrettelagtKommunikasjon>,
        val innflyttingTilNorge: List<InnflyttingTilNorge>,
        val utflyttingFraNorge: List<UtflyttingFraNorge>,
        val vergemaalEllerFremtidsfullmakt: List<VergemaalEllerFremtidsfullmakt>,
        val barnMedSamværRegistergrunnlagDto: List<BarnMedSamværRegistergrunnlagDto>,
        val medlUnntak: Medlemskapsinfo
)
