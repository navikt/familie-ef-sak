package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.felles.dto.PersonIdentDto
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.infrastruktur.exception.PdlNotFoundException
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakPersonControllerTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var fagsakPersonController: FagsakPersonController

    @Test
    internal fun `skal finne fagsaker til person`() {
        val person = testoppsettService.opprettPerson("1")
        val overgangsstønad = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = StønadType.OVERGANGSSTØNAD))
        val barnetilsyn = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = StønadType.BARNETILSYN))
        val skolepenger = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = StønadType.SKOLEPENGER))

        val fagsakPersonDto = testWithBrukerContext { fagsakPersonController.hentFagsakPerson(person.id).getDataOrThrow() }

        assertThat(fagsakPersonDto.overgangsstønad?.id).isEqualTo(overgangsstønad.id)
        assertThat(fagsakPersonDto.barnetilsyn?.id).isEqualTo(barnetilsyn.id)
        assertThat(fagsakPersonDto.skolepenger?.id).isEqualTo(skolepenger.id)
    }

    @Test
    internal fun `skal ikke finne finne fagsaker til person når det ikke finnes noen`() {
        val person = testoppsettService.opprettPerson("1")

        val fagsakPersonDto = testWithBrukerContext { fagsakPersonController.hentFagsakPerson(person.id).getDataOrThrow() }

        assertThat(fagsakPersonDto.overgangsstønad).isNull()
        assertThat(fagsakPersonDto.barnetilsyn).isNull()
        assertThat(fagsakPersonDto.skolepenger).isNull()
    }

    @Test
    internal fun `skal opprette fagsakperson når man søker etter gyldig personident fagsaker til person`() {
        val fagsakPersonId =
            testWithBrukerContext { fagsakPersonController.hentFagsakPersonIdForPerson(PersonIdentDto("01010172272")).getDataOrThrow() }

        assertThat(fagsakPersonId).isNotNull
    }

    @Test
    internal fun `skal ikke opprette fagsakperson når man søker etter personident som ikke finnes i pdl`() {
        assertThrows<PdlNotFoundException> {
            testWithBrukerContext {
                fagsakPersonController
                    .hentFagsakPersonIdForPerson(PersonIdentDto("19117313797"))
                    .getDataOrThrow()
            }
        }
    }
}
