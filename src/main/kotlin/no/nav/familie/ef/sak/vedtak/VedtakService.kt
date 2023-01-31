package no.nav.familie.ef.sak.vedtak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingsflyt.task.FerdigstillOppgaveTask
import no.nav.familie.ef.sak.behandlingsflyt.task.OpprettOppgaveTask
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.StegUtfall
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.SikkerhetContext
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.tilVedtak
import no.nav.familie.ef.sak.vedtak.dto.tilVedtakDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.internal.TaskService
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class VedtakService(
    private val vedtakRepository: VedtakRepository,
    private val tilkjentYtelseRepository: TilkjentYtelseRepository,
    private val oppgaveService: OppgaveService,
    private val behandlingService: BehandlingService,
    private val behandlingshistorikkService: BehandlingshistorikkService,
    private val taskService: TaskService
) {

    fun lagreVedtak(vedtakDto: VedtakDto, behandlingId: UUID, stønadstype: StønadType): UUID {
        return vedtakRepository.insert(vedtakDto.tilVedtak(behandlingId, stønadstype)).behandlingId
    }

    fun slettVedtakHvisFinnes(behandlingId: UUID) {
        vedtakRepository.deleteById(behandlingId)
    }

    fun hentVedtak(behandlingId: UUID): Vedtak {
        return vedtakRepository.findByIdOrThrow(behandlingId)
    }

    fun hentVedtaksresultat(behandlingId: UUID): ResultatType {
        return hentVedtak(behandlingId).resultatType
    }

    fun hentVedtakForBehandlinger(behandlingIder: Set<UUID>): List<Vedtak> {
        return vedtakRepository.findAllByIdOrThrow(behandlingIder) { it.behandlingId }
    }

    fun hentVedtakDto(behandlingId: UUID): VedtakDto {
        return hentVedtakHvisEksisterer(behandlingId) ?: error("Finner ikke vedtak for behandling=$behandlingId")
    }

    fun hentVedtakHvisEksisterer(behandlingId: UUID): VedtakDto? {
        return vedtakRepository.findByIdOrNull(behandlingId)?.tilVedtakDto()
    }

    fun oppdaterSaksbehandler(behandlingId: UUID, saksbehandlerIdent: String) {
        val vedtak = hentVedtak(behandlingId)
        val oppdatertVedtak = vedtak.copy(saksbehandlerIdent = saksbehandlerIdent)
        vedtakRepository.update(oppdatertVedtak)
    }

    fun oppdaterBeslutter(behandlingId: UUID, beslutterIdent: String) {
        val vedtak = hentVedtak(behandlingId)
        val oppdatertVedtak = vedtak.copy(beslutterIdent = beslutterIdent)
        vedtakRepository.update(oppdatertVedtak)
    }

    fun hentForventetInntektForBehandlingIds(behandlingId: UUID, dato: LocalDate): Int? {
        val vedtak = vedtakRepository.findByIdOrNull(behandlingId)
        if (vedtak?.erVedtakAktivtForDato(dato) == true) {
            return vedtak.inntekter?.inntekter?.firstOrNull {
                it.periode.inneholder(YearMonth.from(dato))
            }?.inntekt?.toInt()
        }

        return null
    }

    fun hentForventetInntektForBehandlingIds(behandlingIds: Collection<UUID>): Map<UUID, ForventetInntektForBehandling> {
        return vedtakRepository.findAllById(behandlingIds).map { vedtak ->
            if (vedtak.erVedtakAktivtForDato(LocalDate.now())) {
                createForventetInntektForBehandling(vedtak)
            } else {
                ForventetInntektForBehandling(vedtak.behandlingId, null, null)
            }
        }.associateBy { it.behandlingId }
    }

    private fun createForventetInntektForBehandling(vedtak: Vedtak): ForventetInntektForBehandling {
        return ForventetInntektForBehandling(
            vedtak.behandlingId,
            createForventetInntektForMåned(vedtak, YearMonth.now().minusMonths(1)),
            createForventetInntektForMåned(vedtak, YearMonth.now().minusMonths(2))
        )
    }

    private fun createForventetInntektForMåned(vedtak: Vedtak, forventetInntektForDato: YearMonth): Int? {
        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(vedtak.behandlingId)
        return tilkjentYtelse?.andelerTilkjentYtelse?.firstOrNull {
            it.periode.inneholder(forventetInntektForDato)
        }?.inntekt
    }

    fun hentHarAktivtVedtak(behandlingId: UUID, localDate: LocalDate = LocalDate.now()): Boolean {
        return hentVedtak(behandlingId).erVedtakAktivtForDato(localDate)
    }

    @Transactional
    fun angreSendTilBeslutter(saksbehandling: Saksbehandling) {
        val vedtak = hentVedtak(behandlingId = saksbehandling.id)

        validerKanAngreSendTilBeslutter(saksbehandling, vedtak)

        behandlingService.oppdaterStegPåBehandling(saksbehandling.id, steg = StegType.SEND_TIL_BESLUTTER)
        behandlingService.oppdaterStatusPåBehandling(saksbehandling.id, status = BehandlingStatus.UTREDES)
        behandlingshistorikkService.opprettHistorikkInnslag(
            behandlingId = saksbehandling.id,
            stegtype = saksbehandling.steg,
            utfall = StegUtfall.ANGRE_SEND_TIL_BESLUTTER,
            metadata = null
        )

        ferdigstillGodkjenneVedtakOppgave(saksbehandling)
        opprettBehandleSakOppgave(saksbehandling)
    }

    private fun opprettBehandleSakOppgave(saksbehandling: Saksbehandling) {
        taskService.save(
            OpprettOppgaveTask.opprettTask(
                OpprettOppgaveTask.OpprettOppgaveTaskData(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.BehandleSak,
                    beskrivelse = "Angret send til beslutter"
                )
            )
        )
    }

    private fun ferdigstillGodkjenneVedtakOppgave(saksbehandling: Saksbehandling) {
        oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype = Oppgavetype.GodkjenneVedtak, saksbehandling)?.let {
            taskService.save(
                FerdigstillOppgaveTask.opprettTask(
                    behandlingId = saksbehandling.id,
                    oppgavetype = Oppgavetype.GodkjenneVedtak,
                    it.gsakOppgaveId,
                    personIdent = null
                )
            )
        }
    }

    private fun validerKanAngreSendTilBeslutter(saksbehandling: Saksbehandling, vedtak: Vedtak) {
        val innloggetSaksbehandler = SikkerhetContext.hentSaksbehandler()
        feilHvis(vedtak.saksbehandlerIdent != innloggetSaksbehandler, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter om du ikke er saksbehandler på vedtaket" }
        feilHvis(saksbehandling.steg != StegType.BESLUTTE_VEDTAK, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter når behandling er i steg ${saksbehandling.steg}" }
        feilHvis(saksbehandling.status != BehandlingStatus.FATTER_VEDTAK, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter når behandlingen har status ${saksbehandling.status}" }

        val efOppgave = oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype = Oppgavetype.GodkjenneVedtak, saksbehandling = saksbehandling) ?: error("Fant ingen godkjenne vedtak oppgave")
        val tilordnetRessurs = oppgaveService.hentOppgave(efOppgave.gsakOppgaveId).tilordnetRessurs
        feilHvis(tilordnetRessurs != null && tilordnetRessurs != innloggetSaksbehandler, httpStatus = HttpStatus.BAD_REQUEST) { "Kan ikke angre send til beslutter når oppgave er plukket av $tilordnetRessurs" }
    }
}

data class PersonIdentMedForventetInntekt(
    val personIdent: String,
    val forventetInntektForMåned: ForventetInntektForBehandling
)

data class ForventetInntektForBehandling(
    val behandlingId: UUID,
    val forventetInntektForrigeMåned: Int?,
    val forventetInntektToMånederTilbake: Int?
)

data class ForventetInntektForPersonIdent(
    val personIdent: String,
    val forventetInntektForrigeMåned: Int?,
    val forventetInntektToMånederTilbake: Int?
)

fun Vedtak.erVedtakAktivtForDato(dato: LocalDate) = this.perioder?.perioder?.any {
    it.periode.inneholder(YearMonth.from(dato))
} ?: false
