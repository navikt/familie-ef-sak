package no.nav.familie.ef.sak.opplysninger.personopplysninger

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.behandlingsflyt.steg.BehandlerRolle
import no.nav.familie.ef.sak.fagsak.FagsakPersonService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Kjønn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.NavnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer.EndringerIPersonOpplysningerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import java.util.UUID

internal class PersonopplysningerControllerTest {
    private val personopplysningerService = mockk<PersonopplysningerService>()
    private val tilgangService = mockk<TilgangService>(relaxed = true)
    private val fagsakPersonService = mockk<FagsakPersonService>()

    private val personopplysningerController =
        PersonopplysningerController(
            personopplysningerService = personopplysningerService,
            endringerIPersonOpplysningerService = mockk<EndringerIPersonOpplysningerService>(relaxed = true),
            tilgangService = tilgangService,
            fagsakPersonService = fagsakPersonService,
        )

    private val fagsakPersonId = UUID.randomUUID()

    @BeforeEach
    internal fun setUp() {
        every { fagsakPersonService.hentAktivIdent(fagsakPersonId) } returns "12345678910"
    }

    @Test
    internal fun `skal returnere personopplysninger med fullmakt ukjent hvis bruker kun har veilederrolle`() {
        every { personopplysningerService.hentPersonopplysningerFraRegister(any()) } returns dto(fullmakt = null)
        every { tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER) } returns false

        val respons = personopplysningerController.personopplysningerFraFagsakPersonId(fagsakPersonId)

        assertThat(respons.data?.fullmakt).isNull()
    }

    @Test
    internal fun `skal kaste tydelig feil hvis fullmakt er ukjent og bruker har reell saksbehandler- eller beslutterrolle`() {
        every { personopplysningerService.hentPersonopplysningerFraRegister(any()) } returns
            dto(fullmakt = null, fullmaktIkkeTilgangÅrsak = "mangler geografisk tilgang i tilgangsmaskinen")
        every { tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER) } returns true

        val feil =
            assertThrows<Feil> {
                personopplysningerController.personopplysningerFraFagsakPersonId(fagsakPersonId)
            }
        assertThat(feil.httpStatus).isEqualTo(HttpStatus.FORBIDDEN)
        assertThat(feil.frontendFeilmelding).contains("mangler geografisk tilgang i tilgangsmaskinen")
    }

    @Test
    internal fun `skal ikke kaste feil hvis fullmakt er kjent selv om bruker har saksbehandlerrolle`() {
        every { personopplysningerService.hentPersonopplysningerFraRegister(any()) } returns dto(fullmakt = emptyList())
        every { tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER) } returns true

        val respons = personopplysningerController.personopplysningerFraFagsakPersonId(fagsakPersonId)

        assertThat(respons.data?.fullmakt).isEmpty()
    }

    private fun dto(
        fullmakt: List<no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.FullmaktDto>?,
        fullmaktIkkeTilgangÅrsak: String? = null,
    ) = PersonopplysningerDto(
        personIdent = "12345678910",
        navn = NavnDto("", "", "", ""),
        kjønn = Kjønn.MANN,
        adressebeskyttelse = null,
        folkeregisterpersonstatus = null,
        fødselsdato = null,
        dødsdato = null,
        statsborgerskap = emptyList(),
        sivilstand = emptyList(),
        adresse = emptyList(),
        fullmakt = fullmakt,
        fullmaktIkkeTilgangÅrsak = fullmaktIkkeTilgangÅrsak,
        egenAnsatt = false,
        barn = emptyList(),
        innflyttingTilNorge = emptyList(),
        utflyttingFraNorge = emptyList(),
        oppholdstillatelse = emptyList(),
        vergemål = emptyList(),
    )
}
