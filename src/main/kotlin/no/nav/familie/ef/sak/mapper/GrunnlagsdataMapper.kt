package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.api.dto.Sivilstandstype
import no.nav.familie.ef.sak.domene.AnnenForelderMedIdent
import no.nav.familie.ef.sak.domene.BarnMedIdent
import no.nav.familie.ef.sak.domene.FullmaktMedNavn
import no.nav.familie.ef.sak.domene.SivilstandMedNavn
import no.nav.familie.ef.sak.domene.Søker
import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlPersonKort
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.integration.dto.pdl.visningsnavn

object GrunnlagsdataMapper {

    fun mapBarn(pdlBarn: Map<String, PdlBarn>) =
            pdlBarn.map {
                mapBarn(it)
            }

    fun mapBarn(it: Map.Entry<String, PdlBarn>) =
            BarnMedIdent(fødsel = it.value.fødsel,
                         adressebeskyttelse = it.value.adressebeskyttelse,
                         navn = it.value.navn,
                         bostedsadresse = it.value.bostedsadresse,
                         dødsfall = it.value.dødsfall,
                         deltBosted = it.value.deltBosted,
                         forelderBarnRelasjon = it.value.forelderBarnRelasjon,
                         personIdent = it.key)

    fun mapAnnenForelder(barneForeldre: Map<String, PdlAnnenForelder>) =
            barneForeldre.map {
                AnnenForelderMedIdent(
                        adressebeskyttelse = it.value.adressebeskyttelse,
                        personIdent = it.key,
                        fødsel = it.value.fødsel,
                        bostedsadresse = it.value.bostedsadresse,
                        dødsfall = it.value.dødsfall,
                        innflyttingTilNorge = it.value.innflyttingTilNorge,
                        navn = it.value.navn,
                        opphold = it.value.opphold,
                        oppholdsadresse = it.value.oppholdsadresse,
                        statsborgerskap = it.value.statsborgerskap,
                        utflyttingFraNorge = it.value.utflyttingFraNorge
                )
            }

    fun mapSøker(pdlSøker: PdlSøker, andrePersoner: Map<String, PdlPersonKort>) = Søker(
            sivilstand = mapSivivilstand(pdlSøker, andrePersoner),
            adressebeskyttelse = pdlSøker.adressebeskyttelse.first(),
            bostedsadresse = pdlSøker.bostedsadresse,
            dødsfall = pdlSøker.dødsfall.firstOrNull(),
            forelderBarnRelasjon = pdlSøker.forelderBarnRelasjon,
            fullmakt = mapFullmakt(pdlSøker, andrePersoner),
            fødsel = pdlSøker.fødsel.first(),
            folkeregisterpersonstatus = pdlSøker.folkeregisterpersonstatus,
            innflyttingTilNorge = pdlSøker.innflyttingTilNorge,
            kjønn = pdlSøker.kjønn.first(),
            kontaktadresse = pdlSøker.kontaktadresse,
            navn = pdlSøker.navn,
            opphold = pdlSøker.opphold,
            oppholdsadresse = pdlSøker.oppholdsadresse,
            statsborgerskap = pdlSøker.statsborgerskap,
            telefonnummer = pdlSøker.telefonnummer,
            tilrettelagtKommunikasjon = pdlSøker.tilrettelagtKommunikasjon,
            utflyttingFraNorge = pdlSøker.utflyttingFraNorge,
            vergemaalEllerFremtidsfullmakt = pdlSøker.vergemaalEllerFremtidsfullmakt // TODO trenger vi denne? Eller trenger vi kun fullmakt? Spør funksjonell
    )

    private fun mapSivivilstand(pdlSøker: PdlSøker, andrePersoner: Map<String, PdlPersonKort>): List<SivilstandMedNavn> {

        return pdlSøker.sivilstand.map {
            SivilstandMedNavn(type = Sivilstandstype.valueOf(it.type.name),
                              gyldigFraOgMed = it.gyldigFraOgMed,
                              relatertVedSivilstand = it.relatertVedSivilstand,
                              bekreftelsesdato = it.bekreftelsesdato,
                              metadata = it.metadata,
                              navn = andrePersoner[it.relatertVedSivilstand]?.navn?.gjeldende()?.visningsnavn())
        }
    }

    private fun mapFullmakt(pdlSøker: PdlSøker, andrePersoner: Map<String, PdlPersonKort>): List<FullmaktMedNavn> {
        return pdlSøker.fullmakt.map {
            FullmaktMedNavn(gyldigFraOgMed = it.gyldigFraOgMed,
                            gyldigTilOgMed = it.gyldigTilOgMed,
                            motpartsPersonident = it.motpartsPersonident,
                            navn = andrePersoner[it.motpartsPersonident]?.navn?.gjeldende()?.visningsnavn())
        }
    }
}