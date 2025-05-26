package no.nav.familie.ef.sak.næringsinntektskontroll

import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.slot
import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.beregning.Grunnbeløpsperioder
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.økonomi.lagAndelTilkjentYtelse
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.FrittståendeBrevDto
import no.nav.familie.kontrakter.ef.iverksett.Brevmottaker
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.OppgaveIdentV2
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class NæringsinntektKontrollBrevTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var næringsinntektKontrollBrev: NæringsinntektKontrollBrev

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Autowired
    private lateinit var iverksettClient: IverksettClient

    private val frittståendeBrevSlot = slot<FrittståendeBrevDto>()

    @Test
    fun `Send varselbrev til selvstendig næringsdrivende med 10 prosent endring eller mer`() {
        every {
            iverksettClient.sendFrittståendeBrev(capture(frittståendeBrevSlot))
        } just runs

        val oppgave = lagTestOppgave()
        val personIdent = "01010199999"
        val fagsak = testoppsettService.lagreFagsak(fagsak(identer = setOf(PersonIdent(personIdent))))

        val behandling = behandlingRepository.insert(behandling(fagsak))

        val aty =
            lagAndelTilkjentYtelse(
                10000,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                kildeBehandlingId = behandling.id,
            )
        val ty = lagTilkjentYtelse(listOf(aty), behandlingId = behandling.id, grunnbeløpsmåned = Grunnbeløpsperioder.forrigeGrunnbeløp.periode.fom)
        tilkjentYtelseRepository.insert(ty)

        val næringsinntektForBeregning = NæringsinntektDataForBeregning(oppgave, personIdent, fagsak, behandling.id, ty, 100_000, 100_000, 150_000)
        næringsinntektKontrollBrev.sendBrev(næringsinntektForBeregning)

        Assertions.assertThat(frittståendeBrevSlot.captured.personIdent).isEqualTo(forventetFrittståendeBrevDto(fagsak).personIdent)
        Assertions.assertThat(frittståendeBrevSlot.captured.tittel).isEqualTo(forventetFrittståendeBrevDto(fagsak).tittel)
        Assertions.assertThat(frittståendeBrevSlot.captured.eksternFagsakId).isEqualTo(forventetFrittståendeBrevDto(fagsak).eksternFagsakId)
        Assertions.assertThat(frittståendeBrevSlot.captured.stønadType).isEqualTo(forventetFrittståendeBrevDto(fagsak).stønadType)
        Assertions.assertThat(frittståendeBrevSlot.captured.saksbehandlerIdent).isEqualTo(forventetFrittståendeBrevDto(fagsak).saksbehandlerIdent)
        Assertions.assertThat(frittståendeBrevSlot.captured.journalførendeEnhet).isEqualTo(forventetFrittståendeBrevDto(fagsak).journalførendeEnhet)
        Assertions.assertThat(frittståendeBrevSlot.captured.mottakere).isEqualTo(forventetFrittståendeBrevDto(fagsak).mottakere)
        Assertions.assertThat(frittståendeBrevSlot.captured.fil).isEqualTo(forventetFrittståendeBrevDto(fagsak).fil)
    }

    private fun forventetFrittståendeBrevDto(
        fagsak: Fagsak,
    ): FrittståendeBrevDto {
        val forventetBrevMottaker =
            Brevmottaker(
                ident = "01010199999",
                navn = "01010199999 mellomnavn Etternavn",
                mottakerRolle = Brevmottaker.MottakerRolle.BRUKER,
                identType = Brevmottaker.IdentType.PERSONIDENT,
            )
        val forventetPdf =
            this::class.java.classLoader
                .getResource("dummy/pdf_dummy.pdf")!!
                .readBytes()

        return FrittståendeBrevDto("01010199999", fagsak.eksternId, StønadType.OVERGANGSSTØNAD, null, "Inntekt endret for selvstendig næringsdrivende", forventetPdf, "4489", saksbehandlerIdent = "VL", listOf(forventetBrevMottaker))
    }

    private fun lagTestOppgave() =
        no.nav.familie.kontrakter.felles.oppgave
            .Oppgave(identer = listOf(OppgaveIdentV2(ident = "01010199999", gruppe = IdentGruppe.FOLKEREGISTERIDENT)))
}
