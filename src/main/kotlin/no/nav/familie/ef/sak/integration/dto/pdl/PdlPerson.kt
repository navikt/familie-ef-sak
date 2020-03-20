package no.nav.familie.ef.sak.integration.dto.pdl

import java.time.LocalDate
import java.time.LocalDateTime


data class Identliste(
        val identer: List<IdentInformasjon>
)

data class IdentInformasjon(
        val ident: String,
        val gruppe: IdentGruppe,
        val historisk: Boolean
)

enum class IdentGruppe {
    AKTORID,
    FOLKEREGISTERIDENT,
    NPID
}

data class PdlHentPersonResponse(val data: PdlPerson?,
                                 val errors: Array<PdlError>?) {

    fun harFeil(): Boolean {
        return errors != null && errors.isNotEmpty()
    }

    fun errorMessages(): String {
        return errors?.joinToString { it -> it.message } ?: ""
    }
}

data class PdlError (val message: String)


data class PdlPerson(val person: PdlPersonData?)


data class PdlPersonData(val adressebeskyttelse: List<Adressebeskyttelse>,
                         val bostedsadresse: List<Bostedsadresse>,
                         val deltBosted: List<DeltBosted>,
                         val doedsfall: List<Doedsfall>,
                         val falskIdentitet: FalskIdentitet?,
                         val familierelasjoner: List<Familierelasjon>,
                         val foedsel: List<Foedsel>,
                         val folkeregisteridentifikator: List<Folkeregisteridentifikator>,
                         val folkeregisterpersonstatus: List<Folkeregisterpersonstatus>,
                         val fullmakt: List<Fullmakt>,
                         val identitetsgrunnlag: List<Identitetsgrunnlag>,
                         val kjoenn: List<Kjoenn>,
                         val kontaktadresse: List<Kontaktadresse>,
                         val kontaktadresseIUtlandet: List<KontaktadresseIUtlandet>,
                         val kontaktinformasjonForDoedsbo: List<KontaktinformasjonForDoedsbo>,
                         val navn: List<Navn>,
                         val opphold: List<Opphold>,
                         val oppholdsadresse: List<Oppholdsadresse>,
                         val sikkerhetstiltak: List<Sikkerhetstiltak>,
                         val sivilstand: List<Sivilstand>,
                         val statsborgerskap: List<Statsborgerskap>,
                         val telefonnummer: List<Telefonnummer>,
                         val tilrettelagtKommunikasjon: List<TilrettelagtKommunikasjon>,
                         val utenlandskIdentifikasjonsnummer: List<UtenlandskIdentifikasjonsnummer>,
                         val innflyttingTilNorge: List<InnflyttingTilNorge>,
                         val utflyttingFraNorge: List<UtflyttingFraNorge>)

data class DeltBosted(val startdatoForKontrakt: LocalDateTime,
                      val sluttdatoForKontrakt: LocalDateTime?,
                      @Deprecated("reason: Flyttet til adresser rett under")
                      val coAdressenavn: String?,
                      val vegadresse: Vegadresse?,
                      val matrikkeladresse: Matrikkeladresse?,
                      val ukjentBosted: UkjentBosted?,
                      val folkeregistermetadata: Folkeregistermetadata,
                      val metadata: Metadata
)

data class Bostedsadresse(val angittFlyttedato: LocalDate?,
                          val coAdressenavn: String?,
                          val vegadresse: Vegadresse?,
                          val matrikkeladresse: Matrikkeladresse?,
                          val ukjentBosted: UkjentBosted?,
                          val folkeregistermetadata: Folkeregistermetadata,
                          val metadata: Metadata)

data class Oppholdsadresse(val oppholdsadressedato: LocalDate?,
                           val coAdressenavn: String?,
                           val utenlandskAdresse: InternasjonalAdresse?,
                           val vegadresse: Vegadresse?,
                           val matrikkeladresse: Matrikkeladresse?,
                           val oppholdAnnetSted: String?,
                           val folkeregistermetadata: Folkeregistermetadata?,
                           val metadata: Metadata)

data class Kontaktadresse(val gyldigFraOgMed: LocalDateTime?,
                          val coAdressenavn: String?,
                          val postboksadresse: Postboksadresse?,
                          val vegadresseForPost: Vegadresse?,
                          val postadresseIFrittFormat: PostadresseIFrittFormat?,
                          val folkeregistermetadata: Folkeregistermetadata?,
                          val metadata: Metadata)

data class KontaktadresseIUtlandet(val gyldigFraOgMed: LocalDateTime?,
                                   val coAdressenavn: String?,
                                   val utenlandskAdresse: InternasjonalAdresse?,
                                   val utenlandskAdresseIFrittFormat: InternasjonalAdresseIFrittFormat?,
                                   val folkeregistermetadata: Folkeregistermetadata?,
                                   val metadata: Metadata)

data class Vegadresse(val matrikkelId: Int?,
                      val husnummer: String?,
                      val husbokstav: String?,
                      val bruksenhetsnummer: String?,
                      val adressenavn: String?,
                      val kommunenummer: String?,
                      val tilleggsnavn: String?,
                      val postnummer: String?,
                      val koordinater: Koordinater?)

data class Matrikkeladresse(val matrikkelId: Int?,
                            val bruksenhetsnummer: String?,
                            val tilleggsnavn: String?,
                            val postnummer: String?,
                            val kommunenummer: String?,
                            val koordinater: Koordinater?)

data class UkjentBosted(val bostedskommune: String?)

data class InternasjonalAdresse(val adressenavn: String?,
                                val bygningsinformasjon: String?,
                                val postboks: String?,
                                val postkode: String?,
                                val byEllerStedsnavn: String?,
                                val distriktEllerRegion: String?,
                                val landkode: String)

data class InternasjonalAdresseIFrittFormat(val adresselinje1: String?,
                                            val adresselinje2: String?,
                                            val adresselinje3: String?,
                                            val postkode: String?,
                                            val byEllerStedsnavn: String?,
                                            val landkode: String)

data class Postboksadresse(val postbokseier: String?,
                           val postboks: String,
                           val postnummer: String?)

data class PostadresseIFrittFormat(val adresselinje1: String?,
                                   val adresselinje2: String?,
                                   val adresselinje3: String?,
                                   val postnummer: String?)

data class Koordinater(val x: Float?,
                       val y: Float?,
                       val z: Float?,
                       val kvalitet: Int?)

data class FalskIdentitet(val erFalsk: Boolean,
                          val rettIdentitetVedIdentifikasjonsnummer: String?,
                          val rettIdentitetErUkjent: Boolean?,
                          val rettIdentitetVedOpplysninger: FalskIdentitetIdentifiserendeInformasjon?,
                          val metadata: Metadata)

data class FalskIdentitetIdentifiserendeInformasjon(val personnavn: Personnavn,
                                                    val foedselsdato: LocalDate?,
                                                    val statsborgerskap: List<String>,
                                                    val kjoenn: Kjoenntype?)

data class KontaktinformasjonForDoedsbo(val skifteform: KontaktinformasjonForDoedsboSkifteform,
                                        val attestutstedelsesdato: LocalDate,
                                        val personSomKontakt: KontaktinformasjonForDoedsboPersonSomKontakt?,
                                        val advokatSomKontakt: KontaktinformasjonForDoedsboAdvokatSomKontakt?,
                                        val organisasjonSomKontakt: KontaktinformasjonForDoedsboOrganisasjonSomKontakt?,
                                        val adresse: KontaktinformasjonForDoedsboAdresse,
                                        val folkeregistermetadata: Folkeregistermetadata,
                                        val metadata: Metadata)

enum class KontaktinformasjonForDoedsboSkifteform {
    OFFENTLIG,
    ANNET
}

data class KontaktinformasjonForDoedsboPersonSomKontakt(val foedselsdato: LocalDate?,
                                                        val personnavn: Personnavn?,
                                                        val identifikasjonsnummer: String?)

data class KontaktinformasjonForDoedsboAdvokatSomKontakt(val personnavn: Personnavn,
                                                         val organisasjonsnavn: String?,
                                                         val organisasjonsnummer: String?)

data class KontaktinformasjonForDoedsboOrganisasjonSomKontakt(val kontaktperson: Personnavn?,
                                                              val organisasjonsnavn: String,
                                                              val organisasjonsnummer: String?)

data class KontaktinformasjonForDoedsboAdresse(val adresselinje1: String,
                                               val adresselinje2: String?,
                                               val poststedsnavn: String,
                                               val postnummer: String,
                                               val landkode: String?)

data class UtenlandskIdentifikasjonsnummer(val identifikasjonsnummer: String,
                                           val utstederland: String,
                                           val opphoert: Boolean,
                                           val metadata: Metadata)

data class Adressebeskyttelse(val gradering: AdressebeskyttelseGradering,
                              val folkeregistermetadata: Folkeregistermetadata,
                              val metadata: Metadata)

enum class AdressebeskyttelseGradering {
    STRENGT_FORTROLIG,
    FORTROLIG,
    UGRADERT
}

data class Foedsel(val foedselsaar: Int?,
                   val foedselsdato: LocalDate?,
                   val foedeland: String?,
                   val foedested: String?,
                   val foedekommune: String?,
                   val metadata: Metadata)

data class Kjoenn(val kjoenn: Kjoenntype?,
                  val folkeregistermetadata: Folkeregistermetadata?,
                  val metadata: Metadata)

data class Doedsfall(val doedsdato: LocalDate?,
                     val metadata: Metadata)

data class Familierelasjon(val relatertPersonsIdent: String,
                           val relatertPersonsRolle: Familierelasjonsrolle,
                           val minRolleForPerson: Familierelasjonsrolle?,
                           val folkeregistermetadata: Folkeregistermetadata?,
                           val metadata: Metadata)

enum class Familierelasjonsrolle {
    BARN,
    MOR,
    FAR,
    MEDMOR
}

data class Folkeregisterpersonstatus(val status: String,
                                     val forenkletStatus: String,
                                     val folkeregistermetadata: Folkeregistermetadata,
                                     val metadata: Metadata)

data class Navn(val fornavn: String,
                val mellomnavn: String?,
                val etternavn: String,
                val forkortetNavn: String?,
                val originaltNavn: OriginaltNavn?,
                val folkeregistermetadata: Folkeregistermetadata?,
                val metadata: Metadata)

data class OriginaltNavn(val fornavn: String?,
                         val mellomnavn: String?,
                         val etternavn: String?)

data class Personnavn(val fornavn: String,
                      val mellomnavn: String?,
                      val etternavn: String)

enum class Kjoenntype {
    MANN,
    KVINNE,
    UKJENT
}

data class Identitetsgrunnlag(val status: Identitetsgrunnlagsstatus,
                              val folkeregistermetadata: Folkeregistermetadata,
                              val metadata: Metadata)

enum class Identitetsgrunnlagsstatus {
    IKKE_KONTROLLERT,
    KONTROLLERT,
    INGEN_STATUS
}

data class Folkeregistermetadata(val ajourholdstidspunkt: LocalDateTime?,
                                 val gyldighetstidspunkt: LocalDateTime?,
                                 val opphoerstidspunkt: LocalDateTime?,
                                 val kilde: String?,
                                 val aarsak: String?,
                                 val sekvens: Int?)

data class Telefonnummer(val landskode: String,
                         val nummer: String,
                         val prioritet: Int,
                         val metadata: Metadata)

data class TilrettelagtKommunikasjon(val talespraaktolk: Tolk?,
                                     val tegnspraaktolk: Tolk?,
                                     val metadata: Metadata)

data class Tolk(val spraak: String?)

enum class FullmaktsRolle {
    FULLMAKTSGIVER,
    FULLMEKTIG
}

data class Fullmakt(val motpartsPersonident: String,
                    val motpartsRolle: FullmaktsRolle,
                    val omraader: List<String>,
                    val gyldigFraOgMed: LocalDate,
                    val gyldigTilOgMed: LocalDate,
                    val metadata: Metadata)

data class Folkeregisteridentifikator(val identifikasjonsnummer: String,
                                      val status: String,
                                      val type: String,
                                      val folkeregistermetadata: Folkeregistermetadata,
                                      val metadata: Metadata)

data class SikkerhetstiltakKontaktperson(val personident: String,
                                         val enhet: String)

data class Sikkerhetstiltak(val tiltakstype: String,
                            val beskrivelse: String,
                            val kontaktperson: SikkerhetstiltakKontaktperson?,
                            val gyldigFraOgMed: LocalDate,
                            val gyldigTilOgMed: LocalDate?,
                            val metadata: Metadata)

data class Statsborgerskap(val land: String,
                           val gyldigFraOgMed: LocalDate?,
                           val gyldigTilOgMed: LocalDate?,
                           val folkeregistermetadata: Folkeregistermetadata?,
                           val metadata: Metadata)

data class Opphold(val type: Oppholdstillatelse,
                   val oppholdFra: LocalDate?,
                   val oppholdTil: LocalDate?,
                   val folkeregistermetadata: Folkeregistermetadata,
                   val metadata: Metadata)

enum class Oppholdstillatelse {
    MIDLERTIDIG,
    PERMANENT,
    OPPLYSNING_MANGLER
}

data class Sivilstand(val type: Sivilstandstype,
                      val gyldigFraOgMed: LocalDate?,
                      val myndighet: String?,
                      val kommune: String?,
                      val sted: String?,
                      val utland: String?,
                      val relatertVedSivilstand: String?,
                      val bekreftelsesdato: String?,
                      val folkeregistermetadata: Folkeregistermetadata?,
                      val metadata: Metadata)

enum class Sivilstandstype {
    UOPPGITT,
    UGIFT,
    GIFT,
    ENKE_ELLER_ENKEMANN,
    SKILT,
    SEPARERT,
    REGISTRERT_PARTNER,
    SEPARERT_PARTNER,
    SKILT_PARTNER,
    GJENLEVENDE_PARTNER
}

data class InnflyttingTilNorge(val fraflyttingsland: String?,
                               val fraflyttingsstedIUtlandet: String?,
                               val folkeregistermetadata: Folkeregistermetadata?,
                               val metadata: Metadata)

data class UtflyttingFraNorge(val tilflyttingsland: String?,
                              val tilflyttingsstedIUtlandet: String?,
                              val folkeregistermetadata: Folkeregistermetadata?,
                              val metadata: Metadata)

data class Metadata(
//         I PDL så får alle forekomster av en opplysning en ID som representerer dens unike forekomst.
//         F.eks, så vil en Opprett ha ID X, korriger ID Y (der hvor den spesifiserer at den korrigerer X).
// Dersom en opplysning ikke er lagret i PDL, så vil denne verdien ikke være utfylt.
        val opplysningsId: String?,
// Master refererer til hvem som eier opplysningen, f.eks så har PDL en kopi av Folkeregisteret, da vil master være FREG og eventuelle endringer på dette må gå via Folkeregisteret (API mot dem eller andre rutiner).
        val master: String,
// En liste over alle endringer som har blitt utført over tid.
// Vær obs på at denne kan endre seg og man burde takle at det finnes flere korrigeringer i listen, så dersom man ønsker å kun vise den siste, så må man selv filtrere ut dette.
// Det kan også ved svært få tilfeller skje at opprett blir fjernet. F.eks ved splitt tilfeller av identer. Dette skal skje i svært få tilfeller. Dersom man ønsker å presentere opprettet tidspunktet, så blir det tidspunktet på den første endringen.
        val endringer: List<Endring>
)

// no.nav.familie.ef.sak.integration.dto.pdl.Endring som har blitt utført på opplysningen. F.eks: Opprett -> Korriger -> Korriger
data class Endring(
//         Hvilke tyoe endring som har blitt utført.
        val type: Endringstype,
// Tidspunktet for registrering.
        val registrert: LocalDateTime,
// Hvem endringen har blitt utført av, ofte saksbehandler (f.eks Z990200), men kan også være system (f.eks srvXXXX). Denne blir satt til "Folkeregisteret" for det vi får fra dem.
        val registrertAv: String,
// Hvilke system endringen har kommet fra (f.eks srvXXX). Denne blir satt til "FREG" for det vi får fra Folkeregisteret.
        val systemkilde: String,
// Opphavet til informasjonen. I NAV blir dette satt i forbindelse med registrering (f.eks: Sykehuskassan).
// Fra Folkeregisteret får vi opphaven til dems opplysning, altså NAV, UDI, Politiet, Skatteetaten o.l.. Fra Folkeregisteret kan det også være tekniske navn som: DSF_MIGRERING, m.m..
        val kilde: String
)

enum class Endringstype {
    OPPRETT,
    KORRIGER,
    OPPHOER
}

