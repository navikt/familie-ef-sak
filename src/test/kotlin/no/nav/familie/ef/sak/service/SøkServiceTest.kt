package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.SøkService
import no.nav.familie.ef.sak.fagsak.dto.PersonPåAdresse
import no.nav.familie.ef.sak.fagsak.dto.SøkeresultatPerson
import no.nav.familie.ef.sak.infrastruktur.config.KodeverkServiceMock
import no.nav.familie.ef.sak.infrastruktur.exception.ApiFeil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.Toggle
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vilkår.VilkårTestUtil.mockVilkårGrunnlagDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlSaksbehandlerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FalskIdentitetDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Sivilstandstype
import no.nav.familie.ef.sak.opplysninger.personopplysninger.mapper.AdresseMapper
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Bostedsadresse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.FolkeregisteridentifikatorFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.KjønnType
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Metadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.NAVN_MANGLER
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Navn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdenter
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonFraSøk
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkResultat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PersonSøkTreff
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Vegadresse
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.lagNavn
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.pdlSøker
import no.nav.familie.ef.sak.testutil.PdlTestdataHelper.ukjentBostedsadresse
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.regler.HovedregelMetadata
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Kjønn as PdlKjønn

internal class SøkServiceTest {
    private val pdlSaksbehandlerClient = mockk<PdlSaksbehandlerClient>()
    private val personService = mockk<PersonService>()
    private val fagsakService = mockk<FagsakService>()
    private val adresseMapper: AdresseMapper = AdresseMapper(KodeverkServiceMock().kodeverkService())
    private val behandlingService = mockk<BehandlingService>()
    private val fagsakPersonService = mockk<FagsakPersonService>()
    private val vurderingService = mockk<VurderingService>()
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val featureToggleService = mockk<FeatureToggleService>()
    private val søkService =
        SøkService(
            fagsakPersonService,
            behandlingService,
            personService,
            pdlSaksbehandlerClient,
            adresseMapper,
            fagsakService,
            vurderingService,
            personopplysningerService,
            featureToggleService,
        )

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentAktivIdent(any()) } returns "ident"
        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns true
        every { personopplysningerService.hentFalskIdentitet(any()) } returns null
        every { vurderingService.hentGrunnlagOgMetadata(any()) } returns
            Pair(
                mockVilkårGrunnlagDto(),
                HovedregelMetadata(
                    null,
                    Sivilstandstype.UGIFT,
                    false,
                    emptyList(),
                    emptyList(),
                    emptyList(),
                    mockk(),
                    mockk(),
                ),
            )
    }

    @Test
    fun `skal finne personIdent, navn og adresse gitt bostedsadresse`() {
        val vegadresse =
            Vegadresse(
                "23",
                "A",
                "",
                "Adressenavn",
                "",
                "",
                "0000",
                null,
                1L,
            )

        val bostedsadresseFraPdl =
            listOf(
                Bostedsadresse(
                    null,
                    LocalDate.now(),
                    LocalDate.now(),
                    null,
                    null,
                    vegadresse,
                    null,
                    null,
                    Metadata(historisk = false),
                ),
            )

        val navnFraPdl = listOf(Navn("Fornavn", "Mellomnavn", "Etternavn", Metadata(false)))

        val personFraPdl =
            PersonSøkResultat(
                hits =
                    listOf(
                        PersonSøkTreff(
                            person =
                                PdlPersonFraSøk(
                                    listOf(FolkeregisteridentifikatorFraSøk("123456789")),
                                    bostedsadresseFraPdl,
                                    navnFraPdl,
                                ),
                        ),
                    ),
                totalHits = 1,
                pageNumber = 1,
                totalPages = 1,
            )

        every {
            pdlSaksbehandlerClient.søkPersonerMedSammeAdresse(any())
        } returns personFraPdl
        every {
            personService.hentSøker(any())
        } returns pdlSøker(bostedsadresse = bostedsadresseFraPdl)

        val forventetResultat =
            PersonPåAdresse(
                personIdent = "123456789",
                visningsadresse = "Adressenavn 23 A, 0000 Oslo",
                "Fornavn Mellomnavn Etternavn",
                fødselsdato = null,
                erSøker = null,
                erBarn = null,
            )

        val person = SøkeresultatPerson(listOf(forventetResultat))
        assertThat(søkService.søkEtterPersonerMedSammeAdressePåBehandling(UUID.randomUUID())).isEqualTo(person)
    }

    @Test
    internal fun `skal vise vanlige personopplysninger når toggle er av`() {
        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false
        every { fagsakService.finnFagsakEllerOpprettHvisPersonFinnesIInfotrygd(any(), any()) } returns emptyList()
        every { fagsakPersonService.finnPerson(any()) } returns null
        every { personService.hentSøker("ident") } returns pdlSøker(navn = listOf(lagNavn()), kjønn = PdlKjønn(KjønnType.KVINNE))

        val søkeresultat = søkService.søkPerson(PdlIdenter(listOf(PdlIdent("ident", false))))

        assertThat(søkeresultat.visningsnavn).isEqualTo("Fornavn mellomnavn Etternavn")
        assertThat(søkeresultat.kjønn).isEqualTo(Kjønn.KVINNE)
        assertThat(søkeresultat.falskIdentitet).isNull()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    internal fun `skal bare tillate manglende navn ved søk uten fagsak når toggle er på`(togglePå: Boolean) {
        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns togglePå
        every { personService.hentPersonKortBolk(listOf("ident")) } returns mapOf("ident" to PdlPersonKort(emptyList(), emptyList(), emptyList()))

        if (togglePå) {
            assertThat(søkService.søkPersonUtenFagsak("ident").navn).isEqualTo(NAVN_MANGLER)
        } else {
            assertThatThrownBy { søkService.søkPersonUtenFagsak("ident") }
                .isInstanceOf(NoSuchElementException::class.java)
        }
    }

    @Test
    internal fun `skal beholde tidligere oppførsel for manglende navn når toggle er av`() {
        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false
        every { fagsakService.finnFagsakEllerOpprettHvisPersonFinnesIInfotrygd(any(), any()) } returns emptyList()
        every { fagsakPersonService.finnPerson(any()) } returns null
        every { personService.hentSøker("ident") } returns pdlSøker(navn = emptyList(), kjønn = PdlKjønn(KjønnType.KVINNE))

        assertThatThrownBy { søkService.søkPerson(PdlIdenter(listOf(PdlIdent("ident", false)))) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    internal fun `skal beholde tidligere oppførsel for manglende kjønn når toggle er av`() {
        every { featureToggleService.isEnabled(Toggle.FALSK_IDENTITET) } returns false
        every { fagsakService.finnFagsakEllerOpprettHvisPersonFinnesIInfotrygd(any(), any()) } returns emptyList()
        every { fagsakPersonService.finnPerson(any()) } returns null
        every { personService.hentSøker("ident") } returns pdlSøker(navn = listOf(lagNavn()), kjønn = null)

        assertThatThrownBy { søkService.søkPerson(PdlIdenter(listOf(PdlIdent("ident", false)))) }
            .isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    internal fun `skal ikke feile når navn og kjønn er annullert, f eks ved falsk identitet`() {
        every { fagsakService.finnFagsakEllerOpprettHvisPersonFinnesIInfotrygd(any(), any()) } returns emptyList()
        every { fagsakPersonService.finnPerson(any()) } returns null
        every { personService.hentSøker("ident") } returns pdlSøker(navn = emptyList(), kjønn = null)

        val søkeresultat = søkService.søkPerson(PdlIdenter(listOf(PdlIdent("ident", false))))

        assertThat(søkeresultat.visningsnavn).isEqualTo(NAVN_MANGLER)
        assertThat(søkeresultat.kjønn).isEqualTo(Kjønn.UKJENT)
    }

    @Test
    internal fun `skal returnere falsk identitet i søkeresultatet`() {
        every { fagsakService.finnFagsakEllerOpprettHvisPersonFinnesIInfotrygd(any(), any()) } returns emptyList()
        every { fagsakPersonService.finnPerson(any()) } returns null
        every { personService.hentSøker("ident") } returns pdlSøker(navn = emptyList(), kjønn = null)
        every { personopplysningerService.hentFalskIdentitet("ident") } returns FalskIdentitetDto(rettIdentitetErUkjent = true)

        val søkeresultat = søkService.søkPerson(PdlIdenter(listOf(PdlIdent("ident", false))))

        assertThat(søkeresultat.falskIdentitet).isEqualTo(FalskIdentitetDto(rettIdentitetErUkjent = true))
    }

    @Test
    internal fun `skal kaste feil hvis personen har ukjent adresse`() {
        every {
            personService.hentSøker(any())
        } returns pdlSøker(bostedsadresse = listOf(ukjentBostedsadresse()))

        assertThatThrownBy { søkService.søkEtterPersonerMedSammeAdressePåBehandling(UUID.randomUUID()) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("ukjent bostedsadresse")
    }
}
