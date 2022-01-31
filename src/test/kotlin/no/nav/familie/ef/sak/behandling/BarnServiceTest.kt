package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BarnService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class BarnServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val søknadService = mockk<SøknadService>()
    val personService = mockk<PersonService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val barnService = BarnService(behandlingService, fagsakService, søknadService, personService, grunnlagsdataService)

    val grunnlagsdataMedMetadata = mockk<GrunnlagsdataMedMetadata>()
    val fagsak = mockk<Fagsak>()
    val behandling = mockk<Behandling>()
    val grunnlagsdataDomene = mockk<GrunnlagsdataDomene>()
    val barnMedIdent = mockk<BarnMedIdent>()
    val søknadsskjemaOvergangsstønad = mockk<SøknadsskjemaOvergangsstønad>()
    val søkerMedBarn = mockk<SøkerMedBarn>()

    @BeforeEach
    fun init() {
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns grunnlagsdataMedMetadata
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { fagsak.id } returns UUID.randomUUID()
        every { behandling.id } returns UUID.randomUUID()
        every { grunnlagsdataMedMetadata.grunnlagsdata } returns grunnlagsdataDomene
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandling med et nytt barn i PDL siden behandling, forvent ett nytt barn`() {
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent)
        every { barnMedIdent.personIdent } returns "fnr for barn"
        every { søknadService.hentOvergangsstønad(any()) } returns null
        every { søkerMedBarn.barn } returns mapOf("fnr for barn" to pdlBarn(fødsel(fødselsdato = LocalDate.now())),
                                                  "fnr for nytt barn" to pdlBarn(fødsel(fødselsdato = LocalDate.now())))
        val barn = barnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(1)
        assertThat(barn.first()).isEqualTo("fnr for nytt barn")
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandling med ett og samme barn i PDL siden behandling, forvent ingen treff`() {
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent)
        every { barnMedIdent.personIdent } returns "fnr for barn"
        every { søknadService.hentOvergangsstønad(any()) } returns null
        every { søkerMedBarn.barn } returns mapOf("fnr for barn" to pdlBarn(fødsel(fødselsdato = LocalDate.now())))
        val barn = barnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandling med ett ekstra voksent barn i PDL, forvent ingen treff`() {
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent)
        every { barnMedIdent.personIdent } returns "fnr for barn"
        every { søknadService.hentOvergangsstønad(any()) } returns null
        every { søkerMedBarn.barn } returns mapOf("fnr for barn" to pdlBarn(fødsel(fødselsdato = LocalDate.now())),
                                                  "fnr for voksent barn" to pdlBarn(fødsel(fødselsdato = LocalDate.now()
                                                          .minusYears(18))))
        val barn = barnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandling med ett ekstra terminbarn i PDL, forvent ingen treff`() {
        val terminbarn = søknadsbarn(terminDato = LocalDate.now(), erBarnetFødt = false)
        every { grunnlagsdataDomene.barn } returns listOf(barnMedIdent)
        every { barnMedIdent.personIdent } returns "fnr for barn"
        every { søknadService.hentOvergangsstønad(any()) } returns søknadsskjemaOvergangsstønad
        every { søknadService.hentOvergangsstønad(any())?.barn } returns setOf(terminbarn)
        every { søkerMedBarn.barn } returns mapOf("fnr for barn" to pdlBarn(fødsel(fødselsdato = LocalDate.now())),
                                                  "fnr for terminbarn" to pdlBarn(fødsel(fødselsdato = LocalDate.now())))
        val barn = barnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    private fun fødsel(fødselsdato: LocalDate? = null, fødselsår: Int? = null): Fødsel {
        return Fødsel(fødselsår = fødselsår,
                      fødselsdato = fødselsdato,
                      fødeland = null,
                      fødested = null,
                      fødekommune = null,
                      metadata = Metadata(false))
    }

    private fun pdlBarn(fødsel: Fødsel): PdlBarn {
        return PdlBarn(adressebeskyttelse = emptyList(),
                       bostedsadresse = emptyList(),
                       deltBosted = emptyList(),
                       dødsfall = emptyList(),
                       forelderBarnRelasjon = emptyList(),
                       fødsel = listOf(fødsel),
                       navn = listOf(Navn(fornavn = "",
                                          mellomnavn = null,
                                          etternavn = "",
                                          metadata = Metadata(false))))
    }

    private fun søknadsbarn(terminDato: LocalDate? = null, fnr: String? = null, erBarnetFødt: Boolean = true) =
            SøknadBarn(fødselsnummer = fnr,
                       fødselTermindato = terminDato,
                       erBarnetFødt = erBarnetFødt,
                       harSkalHaSammeAdresse = true,
                       ikkeRegistrertPåSøkersAdresseBeskrivelse = "",
                       lagtTilManuelt = false)

}