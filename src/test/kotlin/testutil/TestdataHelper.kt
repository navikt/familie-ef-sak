package no.nav.familie.ef.sak.testutil

import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.kontrakter.ef.søknad.Adresseopplysninger
import no.nav.familie.kontrakter.ef.søknad.Aktivitet
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.Bosituasjon
import no.nav.familie.kontrakter.ef.søknad.Innsendingsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Personalia
import no.nav.familie.kontrakter.ef.søknad.Situasjon
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Sivilstandsplaner
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.SøknadOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import java.util.UUID

fun søknad(
    personalia: Søknadsfelt<Personalia> = mockk(),
    innsendingsdetaljer: Søknadsfelt<Innsendingsdetaljer> = mockk(),
    adresseopplysninger: Søknadsfelt<Adresseopplysninger> = mockk(),
    sivilstandsdetaljer: Søknadsfelt<Sivilstandsdetaljer> = mockk(),
    medlemskapsdetaljer: Søknadsfelt<Medlemskapsdetaljer> = mockk(),
    bosituasjon: Søknadsfelt<Bosituasjon> = mockk(),
    sivilstandsplaner: Søknadsfelt<Sivilstandsplaner>? = mockk(),
    barn: Søknadsfelt<List<Barn>> = mockk(),
    aktivitet: Søknadsfelt<Aktivitet> = mockk(),
    situasjon: Søknadsfelt<Situasjon> = mockk(),
    stønadsstart: Søknadsfelt<Stønadsstart> = mockk(),
) = SøknadOvergangsstønad(
    innsendingsdetaljer = innsendingsdetaljer,
    personalia = personalia,
    adresseopplysninger = adresseopplysninger,
    sivilstandsdetaljer = sivilstandsdetaljer,
    medlemskapsdetaljer = medlemskapsdetaljer,
    bosituasjon = bosituasjon,
    sivilstandsplaner = sivilstandsplaner,
    barn = barn,
    aktivitet = aktivitet,
    situasjon = situasjon,
    stønadsstart = stønadsstart,
)

fun søknadBarnTilBehandlingBarn(
    barn: Set<SøknadBarn>,
    behandlingId: UUID = UUID.randomUUID(),
): List<BehandlingBarn> =
    barn.map {
        it.tilBehandlingBarn(behandlingId)
    }

fun SøknadBarn.tilBehandlingBarn(behandlingId: UUID) =
    BehandlingBarn(
        behandlingId = behandlingId,
        søknadBarnId = this.id,
        personIdent = this.fødselsnummer,
        navn = this.navn,
        fødselTermindato = this.fødselTermindato,
    )

fun BarnMedIdent.tilBehandlingBarn(behandlingId: UUID) =
    BehandlingBarn(
        behandlingId = behandlingId,
        søknadBarnId = null,
        personIdent = this.personIdent,
        navn = this.navn.visningsnavn(),
        fødselTermindato = null,
    )
