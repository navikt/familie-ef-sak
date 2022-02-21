package no.nav.familie.ef.sak.opplysninger.søknad.domain

import java.time.LocalDateTime
import java.time.YearMonth

data class Søknadsverdier(
        val barn: Set<SøknadBarn>,
        val fødselsnummer: String,
        val medlemskap: Medlemskap,
        val sivilstandsplaner: Sivilstandsplaner?,
        val bosituasjon: Bosituasjon,
        val sivilstand: Sivilstand,
        val aktivitet: Aktivitet? = null, // Gjelder: OS og BT
        val situasjon: Situasjon? = null, // Gjelder: OS
        val datoMottatt: LocalDateTime,
        val søkerFra: YearMonth? = null,
)


fun SøknadsskjemaSkolepenger.tilSøknadsverdier() = Søknadsverdier(
        aktivitet = null,
        barn = this.barn,
        fødselsnummer = this.fødselsnummer,
        medlemskap = this.medlemskap,
        sivilstand = this.sivilstand,
        sivilstandsplaner = this.sivilstandsplaner,
        bosituasjon = this.bosituasjon,
        situasjon = null,
        datoMottatt = this.datoMottatt,

        )

fun SøknadsskjemaBarnetilsyn.tilSøknadsverdier() = Søknadsverdier(
        aktivitet = this.aktivitet,
        barn = this.barn,
        fødselsnummer = this.fødselsnummer,
        medlemskap = this.medlemskap,
        sivilstand = this.sivilstand,
        sivilstandsplaner = this.sivilstandsplaner,
        bosituasjon = this.bosituasjon,
        situasjon = null,
        datoMottatt = this.datoMottatt,
        søkerFra = this.søkerFra,

        )

fun SøknadsskjemaOvergangsstønad.tilSøknadsverdier() = Søknadsverdier(
        aktivitet = this.aktivitet,
        barn = this.barn,
        fødselsnummer = this.fødselsnummer,
        medlemskap = this.medlemskap,
        sivilstand = this.sivilstand,
        sivilstandsplaner = this.sivilstandsplaner,
        bosituasjon = this.bosituasjon,
        situasjon = this.situasjon,
        datoMottatt = this.datoMottatt,
        søkerFra = this.søkerFra

)
