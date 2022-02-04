package no.nav.familie.ef.sak.no.nav.familie.ef.sak.behandling

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.barn.BehandlingBarn
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.NyeBarnService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataDomene
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.SøkerMedBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Fødsel
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlSøker
import no.nav.familie.kontrakter.felles.PersonIdent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class NyeBarnServiceTest {

    val behandlingService = mockk<BehandlingService>()
    val fagsakService = mockk<FagsakService>()
    val personService = mockk<PersonService>()
    val barnService = mockk<BarnService>()
    val pdlSøker = mockk<PdlSøker>(relaxed = true)
    val nyeBarnService = NyeBarnService(behandlingService, fagsakService, personService, barnService)

    val grunnlagsdataMedMetadata = mockk<GrunnlagsdataMedMetadata>()
    val fagsak = mockk<Fagsak>()
    val behandling = mockk<Behandling>()
    val grunnlagsdataDomene = mockk<GrunnlagsdataDomene>()

    val fnrForEksisterendeBarn = "19011870794"
    val fødselsdatoEksisterendeBarn = LocalDate.of(2018, 1, 19)
    val fnrForNyttBarn = "15012279679"
    val fødselsdatoNyttBarn = LocalDate.of(2022, 1, 15)
    val fnrForVoksentBarn = "22100188701"
    val fødselsdatoVoksentBarn = LocalDate.of(2001, 10, 22)

    @BeforeEach
    fun init() {
        every { behandlingService.finnSisteIverksatteBehandling(any()) } returns behandling
        every { fagsakService.finnFagsak(any(), any()) } returns fagsak
        every { fagsak.id } returns UUID.randomUUID()
        every { behandling.id } returns UUID.randomUUID()
        every { grunnlagsdataMedMetadata.grunnlagsdata } returns grunnlagsdataDomene
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandling med et nytt barn i PDL siden behandling, forvent ett nytt barn`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForNyttBarn to pdlBarn(fødsel(fødselsdato = fødselsdatoNyttBarn)))
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(1)
        assertThat(barn.first()).isEqualTo(fnrForNyttBarn)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandling med ett og samme barn i PDL siden behandling, forvent ingen treff`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel(fødselsdato = fødselsdatoEksisterendeBarn)))
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandling med ett ekstra voksent barn i PDL, forvent ingen treff`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForVoksentBarn to pdlBarn(fødsel(fødselsdato = fødselsdatoVoksentBarn)))
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn))

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    @Test
    fun `finnNyeBarnSidenGjeldendeBehandling med ett ekstra terminbarn i PDL, forvent ingen treff`() {
        val pdlBarn = mapOf(fnrForEksisterendeBarn to pdlBarn(fødsel(fødselsdato = fødselsdatoEksisterendeBarn)),
                            fnrForNyttBarn to pdlBarn(fødsel(fødselsdato = fødselsdatoNyttBarn)))
        every { personService.hentPersonMedBarn(any()) } returns søkerMedBarn(pdlBarn)
        every { barnService.finnBarnPåBehandling(any()) } returns listOf(behandlingBarn(fnrForEksisterendeBarn),
                                                                         behandlingBarn(fødselTermindato = fødselsdatoNyttBarn))

        val barn = nyeBarnService.finnNyeBarnSidenGjeldendeBehandlingForPersonIdent(PersonIdent("fnr til søker"))
        assertThat(barn.size).isEqualTo(0)
    }

    private fun behandlingBarn(fnr: String? = null,
                               fødselTermindato: LocalDate? = null,
                               søknadBarnId: UUID? = null) = BehandlingBarn(
            behandlingId = UUID.randomUUID(),
            søknadBarnId = søknadBarnId,
            personIdent = fnr,
            fødselTermindato = fødselTermindato,
            navn = null
    )

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

    private fun søkerMedBarn(pdlBarn: Map<String, PdlBarn>): SøkerMedBarn = SøkerMedBarn("søker", pdlSøker, pdlBarn)

}