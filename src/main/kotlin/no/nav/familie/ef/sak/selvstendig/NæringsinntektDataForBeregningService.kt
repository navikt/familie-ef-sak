package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.amelding.InntektService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.sigrun.SigrunService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.util.UUID

@Service
class NæringsinntektDataForBeregningService(
    val behandlingService: BehandlingService,
    val fagsakService: FagsakService,
    val tilkjentYtelseService: TilkjentYtelseService,
    val inntektService: InntektService,
    val sigrunService: SigrunService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")

    fun hentNæringsinntektDataForBeregning(
        oppgaver: List<Oppgave>,
        årstallIFjor: Int,
    ): List<NæringsinntektDataForBeregning> {
        val næringsinntektDataForBeregningList =
            oppgaver.map {
                val personIdent =
                    it.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                        ?: it.personident ?: throw Exception("Fant ikke registrert ident på oppgave ${it.id}")
                val fagsakOvergangsstønad = fagsakService.finnFagsaker(setOf(personIdentForOppgave(it))).firstOrNull { it.stønadstype == StønadType.OVERGANGSSTØNAD } ?: throw RuntimeException("Fant ikke fagsak for overgangsstønad for person: ${personIdentForOppgave(it)}")
                val behandlingId = behandlingService.finnSisteIverksatteBehandling(fagsakOvergangsstønad.id)?.id ?: throw RuntimeException("Fant ingen gjeldende behandling for fagsakId: ${fagsakOvergangsstønad.id}")
                val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)
                val næringsinntektDataForBeregning =
                    NæringsinntektDataForBeregning(
                        oppgave = it,
                        personIdent = personIdent,
                        fagsak = fagsakOvergangsstønad,
                        behandlingId = behandlingId,
                        tilkjentYtelse = tilkjentYtelse,
                        fjoråretsNæringsinntekt = hentFjoråretsNæringsinntekt(fagsakOvergangsstønad.fagsakPersonId, årstallIFjor),
                        fjoråretsPersonInntekt = inntektService.hentÅrsinntekt(personIdent, årstallIFjor),
                        forventetInntektIFjor = forventetInntektSnittIFjor(tilkjentYtelse, årstallIFjor),
                    )
                secureLogger.info("Næringsinntektsdata for beregning: $næringsinntektDataForBeregning")
                secureLogger.info("${næringsinntektDataForBeregning.antallMånederMedVedtakForÅr(årstallIFjor)} måneder med vedtak for fagsakId: ${fagsakOvergangsstønad.id} eksternFagsakId: ${fagsakOvergangsstønad.eksternId}")
                secureLogger.info("Forrige års inntekt for person uten ytelse fra offentlig: ${næringsinntektDataForBeregning.fjoråretsPersonInntekt} - næringsinntekt: ${næringsinntektDataForBeregning.fjoråretsNæringsinntekt} (Fagsak: ${fagsakOvergangsstønad.id})")
                næringsinntektDataForBeregning
            }

        return næringsinntektDataForBeregningList
    }

    private fun personIdentForOppgave(it: Oppgave) =
        (
            it.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                ?: it.personident ?: throw Exception("Fant ikke registrert ident på oppgave ${it.id}")
        )

    private fun hentFjoråretsNæringsinntekt(
        fagsakPersonId: UUID,
        årstallIFjor: Int,
    ): Int {
        val inntekt = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakPersonId).filter { it.inntektsår == årstallIFjor }
        val næringsinntekt = inntekt.sumOf { it.næring } + inntekt.sumOf { it.svalbard?.næring ?: 0 }
        secureLogger.info("Inntekt for person $inntekt - næringsinntekt er beregnet til: $næringsinntekt")
        return næringsinntekt
    }

    private fun forventetInntektSnittIFjor(
        tilkjentYtelse: TilkjentYtelse,
        årstallIFjor: Int,
    ): Int {
        val inntektsperioder =
            tilkjentYtelse.andelerTilkjentYtelse.map {
                (
                    it.periode
                        .snitt(Månedsperiode(YearMonth.of(årstallIFjor, 1), YearMonth.of(årstallIFjor, 12)))
                        ?.lengdeIHeleMåneder()
                        ?.toInt() ?: 0
                ) to it.inntekt
            }

        val sumAntallMånederStønadIFjor = inntektsperioder.sumOf { it.first }
        if (sumAntallMånederStønadIFjor == 0) return 0

        return inntektsperioder.sumOf { (inntekt, antallMåneder) ->
            inntekt * (antallMåneder / sumAntallMånederStønadIFjor)
        }
    }
}
