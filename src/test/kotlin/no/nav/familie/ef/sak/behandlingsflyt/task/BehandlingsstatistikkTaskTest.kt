package no.nav.familie.ef.sak.behandlingsflyt.task

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingService
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.iverksett.IverksettClient
import no.nav.familie.ef.sak.oppgave.OppgaveService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.repository.saksbehandling
import no.nav.familie.ef.sak.repository.årsakRevurdering
import no.nav.familie.ef.sak.vedtak.VedtakRepository
import no.nav.familie.ef.sak.vedtak.domain.BarnetilsynWrapper
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.ef.felles.AvslagÅrsak
import no.nav.familie.kontrakter.ef.felles.BehandlingType
import no.nav.familie.kontrakter.ef.felles.Opplysningskilde
import no.nav.familie.kontrakter.ef.felles.Revurderingsårsak
import no.nav.familie.kontrakter.ef.iverksett.BehandlingKategori
import no.nav.familie.kontrakter.ef.iverksett.BehandlingMetode
import no.nav.familie.kontrakter.ef.iverksett.BehandlingsstatistikkDto
import no.nav.familie.kontrakter.ef.iverksett.Hendelse
import no.nav.familie.kontrakter.ef.iverksett.ÅrsakRevurderingDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.prosessering.domene.Task
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.TimeZone

internal class BehandlingsstatistikkTaskTest {
    init {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Europe/Oslo")))
    }

    val personIdent = "123456789012"
    val fagsak = fagsak(identer = fagsakpersoner(setOf(personIdent)))
    val behandling =
        behandling(
            fagsak,
            resultat = BehandlingResultat.INNVILGET,
            type = FØRSTEGANGSBEHANDLING,
            kategori = BehandlingKategori.NASJONAL,
            kravMottatt = LocalDate.of(2022, 3, 1),
        )
    val avslåttBehandling =
        behandling(
            fagsak,
            resultat = BehandlingResultat.AVSLÅTT,
            type = FØRSTEGANGSBEHANDLING,
            kategori = BehandlingKategori.NASJONAL,
            kravMottatt = LocalDate.of(2022, 3, 1),
        )
    val avslåttSaksbehandling = saksbehandling(fagsak, avslåttBehandling)
    val saksbehandling = saksbehandling(fagsak, behandling)
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
    val behandlingMetode = BehandlingMetode.MANUELL
    val payload =
        BehandlingsstatistikkTaskPayload(
            behandling.id,
            hendelse,
            hendelseTidspunkt.toLocalDateTime(),
            saksbehandlerId,
            oppgaveId,
            behandlingMetode,
        )

    val oppgaveMock = mockk<Oppgave>()
    val grunnlagsdataMock = mockk<GrunnlagsdataMedMetadata>()
    val iverksettClient = mockk<IverksettClient>()
    val behandlingService = mockk<BehandlingService>()
    val søknadService = mockk<SøknadService>()
    val fagsakService = mockk<FagsakService>()
    val grunnlagsdataService = mockk<GrunnlagsdataService>()
    val vedtakRepository = mockk<VedtakRepository>()
    val oppgaveService = mockk<OppgaveService>()
    val årsakRevurderingService = mockk<ÅrsakRevurderingService>()

    val behandlingsstatistikkTask =
        BehandlingsstatistikkTask(
            iverksettClient = iverksettClient,
            behandlingService = behandlingService,
            søknadService = søknadService,
            vedtakRepository = vedtakRepository,
            oppgaveService = oppgaveService,
            grunnlagsdataService = grunnlagsdataService,
            årsakRevurderingService = årsakRevurderingService,
        )

    @BeforeEach
    internal fun setUp() {
        every { behandlingService.hentSaksbehandling(behandling.id) } returns saksbehandling
        every { fagsakService.hentFagsak(fagsak.id) } returns fagsak
        every { oppgaveService.hentOppgave(oppgaveId) } returns oppgaveMock
        every { søknadService.finnDatoMottattForSøknad(any()) } returns søknadstidspunkt.toLocalDateTime()
        every { grunnlagsdataService.hentGrunnlagsdata(behandling.id) } returns grunnlagsdataMock
        every { vedtakRepository.findByIdOrNull(behandling.id) } returns
            Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.INNVILGE,
                periodeBegrunnelse = periodeBegrunnelse,
                inntektBegrunnelse = inntektBegrunnelse,
                saksbehandlerIdent = saksbehandlerId,
                beslutterIdent = beslutterId,
            )
        every { oppgaveMock.tildeltEnhetsnr } returns tildeltEnhet
        every { oppgaveMock.opprettetAvEnhetsnr } returns opprettetEnhet
        every { grunnlagsdataMock.grunnlagsdata.søker.adressebeskyttelse } returns null
        every { årsakRevurderingService.hentÅrsakRevurdering(behandling.id) } returns årsakRevurdering()
    }

    @Test
    internal fun `skal sende behandlingsstatistikk`() {
        val behandlingsstatistikkSlot = slot<BehandlingsstatistikkDto>()

        every { iverksettClient.sendBehandlingsstatistikk(capture(behandlingsstatistikkSlot)) } just Runs

        val task =
            Task(
                type = "behandlingsstatistikkTask",
                payload = objectMapper.writeValueAsString(payload),
            )

        behandlingsstatistikkTask.doTask(task)

        val behandlingsstatistikk = behandlingsstatistikkSlot.captured
        assertThat(behandlingsstatistikk.behandlingId).isEqualTo(behandling.id)
        assertThat(behandlingsstatistikk.personIdent).isEqualTo(personIdent)
        assertThat(behandlingsstatistikk.gjeldendeSaksbehandlerId).isEqualTo(beslutterId)
        assertThat(behandlingsstatistikk.hendelseTidspunkt).isEqualTo(hendelseTidspunkt)
        assertThat(behandlingsstatistikk.hendelse).isEqualTo(hendelse)
        assertThat(behandlingsstatistikk.opprettetEnhet).isEqualTo(opprettetEnhet)
        assertThat(behandlingsstatistikk.ansvarligEnhet).isEqualTo(tildeltEnhet)
        assertThat(behandlingsstatistikk.strengtFortroligAdresse).isEqualTo(false)
        assertThat(behandlingsstatistikk.stønadstype).isEqualTo(StønadType.OVERGANGSSTØNAD)
        assertThat(behandlingsstatistikk.behandlingstype).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        assertThat(behandlingsstatistikk.resultatBegrunnelse).isEqualTo(periodeBegrunnelse)
        assertThat(behandlingsstatistikk.henvendelseTidspunkt).isEqualTo(søknadstidspunkt)
        assertThat(behandlingsstatistikk.kravMottatt).isEqualTo(behandling.kravMottatt)
        assertThat(behandlingsstatistikk.årsakRevurdering)
            .isEqualTo(ÅrsakRevurderingDto(Opplysningskilde.MELDING_MODIA, Revurderingsårsak.ANNET))
        assertThat(behandlingsstatistikk.kategori).isEqualTo(BehandlingKategori.NASJONAL)
    }

    @Test
    internal fun `skal bruke opprettetDato for behandlingen dersom søknad ikke foreligger`() {
        val behandlingsstatistikkSlot = slot<BehandlingsstatistikkDto>()
        every { søknadService.finnDatoMottattForSøknad(any()) } returns null
        every { iverksettClient.sendBehandlingsstatistikk(capture(behandlingsstatistikkSlot)) } just Runs

        val task =
            Task(
                type = "behandlingsstatistikkTask",
                payload = objectMapper.writeValueAsString(payload),
            )

        behandlingsstatistikkTask.doTask(task)

        val behandlingsstatistikk = behandlingsstatistikkSlot.captured
        assertThat(behandlingsstatistikk.henvendelseTidspunkt)
            .isEqualTo(behandling.sporbar.opprettetTid.atZone(ZoneId.of("Europe/Oslo")))
    }

    @Test
    internal fun `skal sende avslagsårsak dersom denne finnes`() {
        val behandlingsstatistikkSlot = slot<BehandlingsstatistikkDto>()
        every { iverksettClient.sendBehandlingsstatistikk(capture(behandlingsstatistikkSlot)) } just Runs
        every { behandlingService.hentSaksbehandling(behandling.id) } returns avslåttSaksbehandling
        every { vedtakRepository.findByIdOrNull(behandling.id) } returns
            Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.AVSLÅ,
                periodeBegrunnelse = periodeBegrunnelse,
                inntektBegrunnelse = inntektBegrunnelse,
                saksbehandlerIdent = saksbehandlerId,
                beslutterIdent = beslutterId,
                avslåÅrsak = AvslagÅrsak.MINDRE_INNTEKTSENDRINGER,
            )
        val task =
            Task(
                type = "behandlingsstatistikkTask",
                payload = objectMapper.writeValueAsString(payload),
            )

        behandlingsstatistikkTask.doTask(task)

        val behandlingsstatistikk = behandlingsstatistikkSlot.captured
        assertThat(behandlingsstatistikk.avslagÅrsak).isEqualTo(AvslagÅrsak.MINDRE_INNTEKTSENDRINGER)
        assertThat(behandlingsstatistikk.behandlingResultat).isEqualTo(BehandlingResultat.AVSLÅTT.name)
    }

    @Test
    internal fun `skal sende behandlingsstatistikk for barnetilsyn`() {
        val begrunnelse = "Begrunnelse barnetilsyn"
        val behandlingsstatistikkSlot = slot<BehandlingsstatistikkDto>()

        every { iverksettClient.sendBehandlingsstatistikk(capture(behandlingsstatistikkSlot)) } just Runs
        every { behandlingService.hentSaksbehandling(behandling.id) } returns
            saksbehandling.copy(stønadstype = StønadType.BARNETILSYN)
        every { vedtakRepository.findByIdOrNull(behandling.id) } returns
            Vedtak(
                behandlingId = behandling.id,
                resultatType = ResultatType.INNVILGE,
                inntektBegrunnelse = inntektBegrunnelse,
                barnetilsyn =
                    BarnetilsynWrapper(
                        emptyList(),
                        begrunnelse,
                    ),
                saksbehandlerIdent = saksbehandlerId,
                beslutterIdent = beslutterId,
            )

        val task =
            Task(
                type = "behandlingsstatistikkTask",
                payload = objectMapper.writeValueAsString(payload),
            )

        behandlingsstatistikkTask.doTask(task)

        val behandlingsstatistikk = behandlingsstatistikkSlot.captured
        assertThat(behandlingsstatistikk.behandlingId).isEqualTo(behandling.id)
        assertThat(behandlingsstatistikk.personIdent).isEqualTo(personIdent)
        assertThat(behandlingsstatistikk.gjeldendeSaksbehandlerId).isEqualTo(beslutterId)
        assertThat(behandlingsstatistikk.hendelseTidspunkt).isEqualTo(hendelseTidspunkt)
        assertThat(behandlingsstatistikk.hendelse).isEqualTo(hendelse)
        assertThat(behandlingsstatistikk.opprettetEnhet).isEqualTo(opprettetEnhet)
        assertThat(behandlingsstatistikk.ansvarligEnhet).isEqualTo(tildeltEnhet)
        assertThat(behandlingsstatistikk.strengtFortroligAdresse).isEqualTo(false)
        assertThat(behandlingsstatistikk.stønadstype).isEqualTo(StønadType.BARNETILSYN)
        assertThat(behandlingsstatistikk.behandlingstype).isEqualTo(BehandlingType.FØRSTEGANGSBEHANDLING)
        assertThat(behandlingsstatistikk.resultatBegrunnelse).isEqualTo(begrunnelse)
        assertThat(behandlingsstatistikk.henvendelseTidspunkt).isEqualTo(søknadstidspunkt)
        assertThat(behandlingsstatistikk.kategori).isEqualTo(BehandlingKategori.NASJONAL)
    }
}
