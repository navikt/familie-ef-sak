package no.nav.familie.ef.sak.service

import io.mockk.every
import io.mockk.mockk
import no.nav.familie.ef.sak.domene.SøkerMedBarn
import no.nav.familie.ef.sak.integration.PdlClient
import no.nav.familie.ef.sak.integration.dto.pdl.ForelderBarnRelasjon
import no.nav.familie.ef.sak.integration.dto.pdl.Familierelasjonsrolle
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.integration.dto.pdl.PdlTestdata
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class PersonServiceTest {

    private val pdlClient: PdlClient = mockk()
    private val personService = PersonService(pdlClient)

    @Test
    fun `skal finne indentifikatorer for barn og dets foreldre gitt en forelders ident`() {

        val pdlBarnOla = PdlBarn(emptyList(),
                                 emptyList(),
                                 emptyList(),
                                 emptyList(),
                                 listOf(ForelderBarnRelasjon("1234", Familierelasjonsrolle.MOR, Familierelasjonsrolle.BARN),
                                        ForelderBarnRelasjon("5678", Familierelasjonsrolle.FAR, Familierelasjonsrolle.BARN)),
                                 emptyList(),
                                 emptyList())

        val pdlBarnKari = PdlBarn(emptyList(),
                                  emptyList(),
                                  emptyList(),
                                  emptyList(),
                                  listOf(ForelderBarnRelasjon("1234", Familierelasjonsrolle.MOR, Familierelasjonsrolle.BARN),
                                         ForelderBarnRelasjon("9999", Familierelasjonsrolle.MEDMOR, Familierelasjonsrolle.BARN),
                                         ForelderBarnRelasjon("7777", Familierelasjonsrolle.BARN, Familierelasjonsrolle.MOR)),
                                  emptyList(),
                                  emptyList())

        every {
            pdlClient.hentSøker(any())
        } returns PdlTestdata.pdlSøkerData.person!!

        every {
            pdlClient.hentBarn(any())
        } returns mapOf("1111" to pdlBarnOla, "2222" to pdlBarnKari)


        val søkerMedBarn = SøkerMedBarn("1234", PdlTestdata.pdlSøkerData.person!!, mapOf("1111" to pdlBarnOla))
        assertThat(personService.hentIdenterForBarnOgForeldre("1234")).containsAll(listOf("1111", "1234", "5678"))
    }
}

