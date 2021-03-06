package no.nav.familie.ef.sak.task

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.api.beregning.ResultatType
import no.nav.familie.ef.sak.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.VedtakRepository
import no.nav.familie.ef.sak.repository.domain.BehandlingResultat
import no.nav.familie.ef.sak.repository.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.repository.domain.Vedtak
import no.nav.familie.ef.sak.service.BehandlingService
import no.nav.familie.ef.sak.service.FagsakService
import no.nav.familie.ef.sak.service.GrunnlagsdataService
import no.nav.familie.ef.sak.service.OppgaveService
import no.nav.familie.ef.sak.service.SøknadService
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.StønadType
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsstatistikkDto
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone
import java.util.UUID

internal class BehandlingsstatistikkTaskTest {

    @BeforeEach
    internal fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Oslo")))
    }

    @Test
    internal fun `skal sende behandlingsstatistikk`() {

        val personIdent = "123456789012"
        val fagsak = fagsak(identer = fagsakpersoner(setOf(personIdent)) )
        val behandling = behandling(fagsak, resultat = BehandlingResultat.INNVILGET, type = FØRSTEGANGSBEHANDLING)
        val hendelse = Hendelse.BESLUTTET
        val hendelseTidspunkt = ZonedDateTime.now()
        val søknadstidspunkt = ZonedDateTime.now().minusDays(5)
        val oppgaveId = 1L
        val saksbehandlerId = "389221"
        val beslutterId = "389221"
        val opprettetEnhet = "4489"
        val tildeltEnhet = "4488"
        val periodeBegrunnelse = "Lorem ipsum"
        val inntektBegrunnelse = "Inntektus loremus ipsums"

        val payload = BehandlingsstatistikkTaskPayload(
                behandling.id,
                hendelse,
                hendelseTidspunkt.toLocalDateTime(),
                saksbehandlerId,
                oppgaveId
        )


        val behandlingsstatistikkSlot = slot<BehandlingsstatistikkDto>();

        val oppgaveMock = mockk<Oppgave>()
        val grunnlagsdataMock = mockk<GrunnlagsdataMedMetadata>()
        val iverksettClient = mockk<IverksettClient>()
        val behandlingService = mockk<BehandlingService>()
        val søknadService = mockk<SøknadService>()
        val fagsakService = mockk<FagsakService>()
        val grunnlagsdataService = mockk<GrunnlagsdataService>()
        val vedtakRepository = mockk<VedtakRepository>()
        val oppgaveService = mockk<OppgaveService>()

        every { iverksettClient.sendBehandlingsstatistikk(capture(behandlingsstatistikkSlot)) } just Runs
        every { behandlingService.hentBehandling(behandling.id) } returns behandling
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { oppgaveService.hentOppgave(oppgaveId) } returns oppgaveMock
        every { søknadService.finnDatoMottattForSøknad(any()) } returns søknadstidspunkt.toLocalDateTime()
        every { grunnlagsdataService.hentGrunnlagsdata(behandling.id)} returns grunnlagsdataMock
        every { vedtakRepository.findByIdOrNull(behandling.id) } returns Vedtak(behandlingId =behandling.id,
                                                                         resultatType = ResultatType.INNVILGE,
                                                                         periodeBegrunnelse = periodeBegrunnelse,
                                                                         inntektBegrunnelse = inntektBegrunnelse,
                                                                         saksbehandlerIdent = saksbehandlerId,
                                                                         beslutterIdent = beslutterId)
        every { oppgaveMock.tildeltEnhetsnr } returns tildeltEnhet
        every { oppgaveMock.opprettetAvEnhetsnr } returns opprettetEnhet
        every { grunnlagsdataMock.grunnlagsdata.søker.adressebeskyttelse } returns null


        val behandlingsstatistikkTask = BehandlingsstatistikkTask(iverksettClient = iverksettClient,
                                                                  behandlingService = behandlingService,
                                                                  fagsakService = fagsakService,
                                                                  søknadService = søknadService,
                                                                  vedtakRepository = vedtakRepository,
                                                                  oppgaveService = oppgaveService,
                                                                  grunnlagsdataService = grunnlagsdataService)

        val task = Task(type = "behandlingsstatistikkTask",
                        payload = objectMapper.writeValueAsString(payload))

        behandlingsstatistikkTask.doTask(task);

        val behandlingsstatistikk = behandlingsstatistikkSlot.captured
        assertThat(behandlingsstatistikk.behandlingId).isEqualTo(behandling.id)
        assertThat(behandlingsstatistikk.personIdent).isEqualTo(personIdent)
        assertThat(behandlingsstatistikk.gjeldendeSaksbehandlerId).isEqualTo(beslutterId)
        assertThat(behandlingsstatistikk.hendelseTidspunkt).isEqualTo(hendelseTidspunkt)
        assertThat(behandlingsstatistikk.søknadstidspunkt).isEqualTo(søknadstidspunkt)
        assertThat(behandlingsstatistikk.hendelse).isEqualTo(hendelse)
        assertThat(behandlingsstatistikk.opprettetEnhet).isEqualTo(opprettetEnhet)
        assertThat(behandlingsstatistikk.ansvarligEnhet).isEqualTo(tildeltEnhet)
        assertThat(behandlingsstatistikk.strengtFortroligAdresse).isEqualTo(false)
        assertThat(behandlingsstatistikk.stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)
        assertThat(behandlingsstatistikk.behandlingstype).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        assertThat(behandlingsstatistikk.resultatBegrunnelse).isEqualTo(periodeBegrunnelse)
    }
}