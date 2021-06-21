package no.nav.familie.ef.sak.task

import io.mockk.slot
import no.nav.familie.kontrakter.ef.iverksett.BehandlingStatistikkDto
import no.nav.familie.ef.sak.iverksett.IverksettClient
import org.junit.jupiter.api.Test
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.prosessering.domene.Task
import java.util.UUID
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import io.mockk.Runs
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import java.time.ZonedDateTime

data class BehandlingStatistikkDto(
    val behandlingId: UUID,
    val personIdent: String,
    val gjeldendeSaksbehandlerId: String,
    val saksnummer: String,
    val hendelseTidspunkt: ZonedDateTime,
    val søknadstidspunkt: ZonedDateTime? = null,
    val hendelse: Hendelse,
    val behandlingResultat: String? = null,
    val resultatBegrunnelse: String? = null,
    val opprettetEnhet: String,
    val ansvarligEnhet: String,
    val strengtFortroligAdresse: Boolean,
    val stønadstype: StønadType,
    val behandlingstype: BehandlingType
)

internal class BehandlingsstatistikkTaskTest {

    @Test
    internal fun `skal sende behandlingsstatistikk`() {

        val behandlingsstatistikkSlot = slot<BehandlingStatistikkDto>();

        val behandlingsstatistikk = BehandlingStatistikkDto(
            behandlingId = UUID.randomUUID(),
            personIdent = "123456789012",
            gjeldendeSaksbehandlerId = "389221",
            saksnummer = "392423",
            hendelseTidspunkt = ZonedDateTime.now(),
            søknadstidspunkt = ZonedDateTime.now(),
            hendelse = Hendelse.BESLUTTET,
            opprettetEnhet = "A",
            ansvarligEnhet = "A",
            strengtFortroligAdresse = false,
            stønadstype = StønadType.OVERGANGSSTØNAD,
            behandlingstype = BehandlingType.FØRSTEGANGSBEHANDLING
        )

        val iverksettClient = mockk<IverksettClient>()

        val behandlingsstatistikkTask = BehandlingsstatistikkTask(iverksettClient)

        val task = Task(type = "behandlingsstatistikkTask", payload = objectMapper.writeValueAsString(behandlingsstatistikk))

        every {
            iverksettClient.sendBehandlingsstatistikk(capture(behandlingsstatistikkSlot));
        } just Runs

        behandlingsstatistikkTask.doTask(task);

        assertThat(behandlingsstatistikk.behandlingId).isEqualTo(behandlingsstatistikkSlot.captured.behandlingId)
        assertThat(behandlingsstatistikk.personIdent).isEqualTo(behandlingsstatistikkSlot.captured.personIdent)
        assertThat(behandlingsstatistikk.gjeldendeSaksbehandlerId).isEqualTo(behandlingsstatistikkSlot.captured.gjeldendeSaksbehandlerId)
        assertThat(behandlingsstatistikk.saksnummer).isEqualTo(behandlingsstatistikkSlot.captured.saksnummer)
        assertThat(behandlingsstatistikk.hendelseTidspunkt).isEqualTo(behandlingsstatistikkSlot.captured.hendelseTidspunkt)
        assertThat(behandlingsstatistikk.søknadstidspunkt).isEqualTo(behandlingsstatistikkSlot.captured.søknadstidspunkt)
        assertThat(behandlingsstatistikk.hendelse).isEqualTo(behandlingsstatistikkSlot.captured.hendelse)
        assertThat(behandlingsstatistikk.opprettetEnhet).isEqualTo(behandlingsstatistikkSlot.captured.opprettetEnhet)
        assertThat(behandlingsstatistikk.ansvarligEnhet).isEqualTo(behandlingsstatistikkSlot.captured.ansvarligEnhet)
        assertThat(behandlingsstatistikk.strengtFortroligAdresse).isEqualTo(behandlingsstatistikkSlot.captured.strengtFortroligAdresse)
        assertThat(behandlingsstatistikk.stønadstype).isEqualTo(behandlingsstatistikkSlot.captured.stønadstype)
        assertThat(behandlingsstatistikk.behandlingstype).isEqualTo(behandlingsstatistikkSlot.captured.behandlingstype)
    }
}