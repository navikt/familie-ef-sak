package no.nav.familie.ef.sak.selvstendig

import no.nav.familie.ef.sak.amelding.InntektService
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.oppgave.OppgaveUtil
import no.nav.familie.ef.sak.sigrun.SigrunService
import no.nav.familie.ef.sak.tilkjentytelse.AndelsHistorikkService
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import no.nav.familie.ef.sak.tilkjentytelse.domain.TilkjentYtelse
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.kontrakter.felles.Behandlingstema
import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.kontrakter.felles.Tema
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.kontrakter.felles.oppgave.IdentGruppe
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class NæringsinntektKontrollService(
    val oppgaveService: OppgaveService,
    val fagsakService: FagsakService,
    val behandlingService: BehandlingService,
    val vedtakService: VedtakService,
    val sigrunService: SigrunService,
    val inntektService: InntektService,
    val andelsHistorikkService: AndelsHistorikkService,
    val tilkjentYtelseService: TilkjentYtelseService,
) {
    private val secureLogger = LoggerFactory.getLogger("secureLogger")
    private val årstallIFjor = YearMonth.now().minusYears(1).year
    private val månedsperiodeIFjor = Månedsperiode(YearMonth.of(årstallIFjor, 1), YearMonth.of(årstallIFjor, 12))

    fun finnFagsakerMedOver10ProsentHøyereNæringsinntektEnnForventet(): List<UUID> {
        val fagsakIds = mutableListOf<UUID>()
        if (LeaderClient.isLeader() == true) {
            val oppgaver = hentOppgaverForSelvstendigeTilInntektskontroll()

            oppgaver.forEach { oppgave ->
                val personIdent =
                    oppgave.identer?.firstOrNull { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT }?.ident
                        ?: oppgave.personident ?: throw Exception("Fant ikke registrert ident på oppgave ${oppgave.id}")
                secureLogger.info("Kontrollerer person med ident: $personIdent")

                val fagsakOvergangsstønad = fagsakService.finnFagsaker(setOf(personIdent)).firstOrNull { it.stønadstype == StønadType.OVERGANGSSTØNAD } ?: throw RuntimeException("Fant ikke fagsak for overgangsstønad for person: $personIdent")
                val behandlingId = behandlingService.finnSisteIverksatteBehandling(fagsakOvergangsstønad.id)?.id ?: throw RuntimeException("Fant ingen gjeldende behandling for fagsakId: ${fagsakOvergangsstønad.id}")
                val tilkjentYtelse = tilkjentYtelseService.hentForBehandling(behandlingId)

                val antallMåneder = antallMånederMedVedtakForFjoråret(tilkjentYtelse)
                secureLogger.info("$antallMåneder måneder med vedtak for fagsakId: ${fagsakOvergangsstønad.id} eksternFagsakId: ${fagsakOvergangsstønad.eksternId}")
                val næringsinntekt = hentFjoråretsNæringsinntekt(fagsakOvergangsstønad.fagsakPersonId)

                if (antallMåneder > 3 && næringsinntekt > INNTEKTSGRENSE_FOR_KONTROLL_AV_AKTIVITET) {
                    val fjoråretsPersoninntekt = inntektService.hentÅrsinntekt(personIdent, årstallIFjor)
                    secureLogger.info("Forrige års inntekt for person uten ytelse fra offentlig: $fjoråretsPersoninntekt")
                    if (fjoråretsPersoninntekt == 0) {
                        val forventetInntekt = forventetInntektSnitt(tilkjentYtelse)
                        // secureLogger.info("Beregnet inntekt i snitt for år $årstallIFjor og behandlingId ${behandling.id} er: $forventetInntekt")
                        if (næringsinntekt > (forventetInntekt * 1.1)) {
                            secureLogger.info("Har 10% høyere næringsinntekt for person: $personIdent (Næringsinntekt: $næringsinntekt - ForventetInntekt: $forventetInntekt)")
                            fagsakIds.add(fagsakOvergangsstønad.id)
                        }
                    }
                }
            }
        }
        return fagsakIds
    }

    private fun hentFjoråretsNæringsinntekt(fagsakPersonId: UUID): Int {
        val inntekt = sigrunService.hentInntektForAlleÅrMedInntekt(fagsakPersonId).filter { it.inntektsår == årstallIFjor }
        val næringsinntekt = inntekt.sumOf { it.næring } + inntekt.sumOf { it.svalbard?.næring ?: 0 }
        secureLogger.info("Inntekt for person $inntekt - næringsinntekt er beregnet til: $næringsinntekt")
        return næringsinntekt
    }

    private fun forventetInntektSnitt(tilkjentYtelse: TilkjentYtelse): Int {
        val antallMånederInntektList =
            tilkjentYtelse.andelerTilkjentYtelse.map {
                (
                    it.periode
                        .snitt(månedsperiodeIFjor)
                        ?.lengdeIHeleMåneder()
                        ?.toInt() ?: 0
                ) to it.inntekt
            }

        return antallMånederInntektList.sumOf { (inntekt, antallMåneder) ->
            inntekt * (antallMåneder / 12)
        }
    }

    private fun hentOppgaverForSelvstendigeTilInntektskontroll(): List<Oppgave> {
        val mappeIdForSelvstendigNæringsdrivende = oppgaveService.finnMapper(OppgaveUtil.ENHET_NR_NAY).single { it.navn == "61 Selvstendig næringsdrivende" }.id
        secureLogger.info("MappeId for selvstendige: $mappeIdForSelvstendigNæringsdrivende")
        val finnOppgaveRequest =
            FinnOppgaveRequest(
                tema = Tema.ENF,
                behandlingstema = Behandlingstema.Overgangsstønad,
                oppgavetype = Oppgavetype.Fremlegg,
                fristTomDato = LocalDate.of(YearMonth.now().year, 12, 15),
                mappeId = mappeIdForSelvstendigNæringsdrivende.toLong(),
            )
        val oppgaverForSelvstendige = oppgaveService.hentOppgaver(finnOppgaveRequest)
        secureLogger.info("Antall oppgaver for selvstendige med frist 15. desember: ${oppgaverForSelvstendige.oppgaver.size}")

        if (oppgaverForSelvstendige.oppgaver.isEmpty() ||
            oppgaverForSelvstendige.oppgaver
                .first()
                .identer
                ?.any { it.gruppe == IdentGruppe.FOLKEREGISTERIDENT } == false
        ) {
            secureLogger.info("Fant ingen oppgaver for selvstendige med frist 15. desember")
            return emptyList()
        }
        return oppgaverForSelvstendige.oppgaver
    }

    private fun antallMånederMedVedtakForFjoråret(
        tilkjentYtelse: TilkjentYtelse,
    ): Long {
        val perioder = tilkjentYtelse.andelerTilkjentYtelse.map { it.periode.snitt(månedsperiodeIFjor) }
        val sum = perioder.sumOf { it?.lengdeIHeleMåneder() ?: 0 }
        return sum
    }

    companion object {
        private const val INNTEKTSGRENSE_FOR_KONTROLL_AV_AKTIVITET = 50_000
    }
}
