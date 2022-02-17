package no.nav.familie.ef.sak.opplysninger.søknad.domain

data class Søknadsverdier(
        val barn: Set<SøknadBarn>,
        val fødselsnummer: String,
        val medlemskap: Medlemskap,
        val sivilstandsplaner: Sivilstandsplaner?,
        val bosituasjon: Bosituasjon,
        val sivilstand: Sivilstand,
        val aktivitet: Aktivitet? = null, // Gjelder: OS og BT
        val situasjon: Situasjon? = null, // Gjelder: OS
)


fun SøknadsskjemaSkolepenger.tilSøknadsverdier() = Søknadsverdier(
        aktivitet = null,
        barn = this.barn,
        fødselsnummer = this.fødselsnummer,
        medlemskap = this.medlemskap,
        sivilstand = this.sivilstand,
        sivilstandsplaner = this.sivilstandsplaner,
        bosituasjon = this.bosituasjon,
        situasjon = null

)

fun SøknadsskjemaBarnetilsyn.tilSøknadsverdier() = Søknadsverdier(
        aktivitet = this.aktivitet,
        barn = this.barn,
        fødselsnummer = this.fødselsnummer,
        medlemskap = this.medlemskap,
        sivilstand = this.sivilstand,
        sivilstandsplaner = this.sivilstandsplaner,
        bosituasjon = this.bosituasjon,
        situasjon = null

)

fun SøknadsskjemaOvergangsstønad.tilSøknadsverdier() = Søknadsverdier(
        aktivitet = this.aktivitet,
        barn = this.barn,
        fødselsnummer = this.fødselsnummer,
        medlemskap = this.medlemskap,
        sivilstand = this.sivilstand,
        sivilstandsplaner = this.sivilstandsplaner,
        bosituasjon = this.bosituasjon,
        situasjon = this.situasjon

)
