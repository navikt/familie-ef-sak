package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.felles.util.BrukerContextUtil.testWithBrukerContext
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.kontrakter.felles.getDataOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

internal class FagsakPersonControllerTest : OppslagSpringRunnerTest() {

    @Autowired private lateinit var fagsakPersonController: FagsakPersonController

    @Test
    internal fun `skal finne fagsaker til person`() {
        val person = testoppsettService.opprettPerson("1")
        val overgangsstønad = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.OVERGANGSSTØNAD))
        val barnetilsyn = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.BARNETILSYN))
        val skolepenger = testoppsettService.lagreFagsak(fagsak(person = person, stønadstype = Stønadstype.SKOLEPENGER))

        val fagsakPersonDto = testWithBrukerContext { fagsakPersonController.hentFagsakPersonUtvidet(person.id).getDataOrThrow() }

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
}