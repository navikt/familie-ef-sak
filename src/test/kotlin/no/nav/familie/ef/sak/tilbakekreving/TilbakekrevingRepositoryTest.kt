package no.nav.familie.ef.sak.no.nav.familie.ef.sak.tilbakekreving

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.fagsak.FagsakPersonRepository
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakPerson
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingRepository
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

class TilbakekrevingRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var tilbakekrevingRepository: TilbakekrevingRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakPersonRepository: FagsakPersonRepository

    val forrigeÅr = LocalDate.now().minusYears(1)
    val personIdent = "01010199999"

    @Test
    internal fun `finn tidligere tilbakekrevinger for bruker - skal finne for både OS og BT`() {
        val fagsakPerson = fagsakPerson(identer = setOf(PersonIdent(personIdent)))
        fagsakPersonRepository.insert(fagsakPerson)
        val fagsakOS = fagsak(person = fagsakPerson)
        testoppsettService.lagreFagsak(fagsakOS)
        val fagsakBT = fagsak(stønadstype = StønadType.BARNETILSYN, person = fagsakPerson)
        testoppsettService.lagreFagsak(fagsakBT)
        val behandlingOS = behandlingRepository.insert(behandling(fagsakOS, BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET, vedtakstidspunkt = LocalDateTime.now()))
        val behandlingBT = behandlingRepository.insert(behandling(fagsakBT, BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET, vedtakstidspunkt = LocalDateTime.now().minusMonths(11)))

        tilbakekrevingRepository.insert(Tilbakekreving(behandlingOS.id, Tilbakekrevingsvalg.OPPRETT_AUTOMATISK, "varseltekst", "begrunnelse"))
        tilbakekrevingRepository.insert(Tilbakekreving(behandlingBT.id, Tilbakekrevingsvalg.OPPRETT_AUTOMATISK, "varseltekst", "begrunnelse"))

        val antallAutomatiskBehandledeTilbakekrevinger = tilbakekrevingRepository.finnAntallTilbakekrevingerValgtEtterGittDatoForPersonIdent(fagsakOS.hentAktivIdent(), forrigeÅr)
        assertThat(antallAutomatiskBehandledeTilbakekrevinger).isEqualTo(2)
    }

    @Test
    internal fun `finn tidligere tilbakekrevinger for bruker - ingen treff`() {
        val fagsakPerson = fagsakPerson(identer = setOf(PersonIdent(personIdent)))
        val fagsakPersonUtenforSøk = fagsakPerson(identer = setOf(PersonIdent("02020288888")))

        val fagsakOS = fagsak(person = fagsakPerson)
        val fagsakBT = fagsak(stønadstype = StønadType.BARNETILSYN, person = fagsakPerson)
        val fagsakForPersonUtenforSøk = fagsak(person = fagsakPersonUtenforSøk)

        fagsakPersonRepository.insert(fagsakPerson)
        fagsakPersonRepository.insert(fagsakPersonUtenforSøk)

        testoppsettService.lagreFagsak(fagsakOS)
        testoppsettService.lagreFagsak(fagsakBT)
        testoppsettService.lagreFagsak(fagsakForPersonUtenforSøk)

        val behandlingOS = behandlingRepository.insert(behandling(fagsakOS, BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET, vedtakstidspunkt = LocalDateTime.now()))
        val behandlingBT = behandlingRepository.insert(behandling(fagsakBT, BehandlingStatus.UTREDES, resultat = BehandlingResultat.INNVILGET)) // Må være Ferdigstilt for treff
        val behandlingOS2 = behandlingRepository.insert(behandling(fagsakOS, BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.HENLAGT)) // Kan ikke være henlagt eller avslått for treff
        val behandlingOS3 = behandlingRepository.insert(behandling(fagsakForPersonUtenforSøk, BehandlingStatus.FERDIGSTILT, resultat = BehandlingResultat.INNVILGET)) // Annet fnr skal ikke gi treff

        tilbakekrevingRepository.insert(Tilbakekreving(behandlingOS.id, Tilbakekrevingsvalg.OPPRETT_MED_VARSEL, "varseltekst", "begrunnelse")) // Må være opprett Automatisk for treff
        tilbakekrevingRepository.insert(Tilbakekreving(behandlingBT.id, Tilbakekrevingsvalg.OPPRETT_AUTOMATISK, "varseltekst", "begrunnelse"))
        tilbakekrevingRepository.insert(Tilbakekreving(behandlingOS2.id, Tilbakekrevingsvalg.OPPRETT_AUTOMATISK, "varseltekst", "begrunnelse"))
        tilbakekrevingRepository.insert(Tilbakekreving(behandlingOS3.id, Tilbakekrevingsvalg.OPPRETT_AUTOMATISK, "varseltekst", "begrunnelse"))

        val antallAutomatiskBehandledeTilbakekrevinger = tilbakekrevingRepository.finnAntallTilbakekrevingerValgtEtterGittDatoForPersonIdent(fagsakOS.hentAktivIdent(), forrigeÅr)
        assertThat(antallAutomatiskBehandledeTilbakekrevinger).isEqualTo(0)
    }
}
