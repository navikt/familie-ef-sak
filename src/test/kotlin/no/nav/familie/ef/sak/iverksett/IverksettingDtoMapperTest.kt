package no.nav.familie.ef.sak.iverksett

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.familie.ef.sak.arbeidsfordeling.ArbeidsfordelingService
import no.nav.familie.ef.sak.barn.BarnService
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.behandlingshistorikk.BehandlingshistorikkService
import no.nav.familie.ef.sak.behandlingshistorikk.domain.Behandlingshistorikk
import no.nav.familie.ef.sak.brev.BrevmottakereRepository
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.felles.util.opprettGrunnlagsdata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.simulering.SimuleringService
import no.nav.familie.ef.sak.tilbakekreving.TilbakekrevingService
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekreving
import no.nav.familie.ef.sak.tilbakekreving.domain.Tilbakekrevingsvalg
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.Vedtak
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.kontrakter.felles.simulering.Simuleringsoppsummering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.data.repository.findByIdOrNull
import java.math.BigDecimal
import java.time.LocalDateTime

internal class IverksettingDtoMapperTest {

    private val tilbakekrevingService = mockk<TilbakekrevingService>(relaxed = true)
    private val simuleringService = mockk<SimuleringService>()
    private val fagsakService = mockk<FagsakService>()
    private val vedtakService = mockk<VedtakService>()
    private val behandlingshistorikkService = mockk<BehandlingshistorikkService>()
    private val barnService = mockk<BarnService>()
    private val grunnlagsdataService = mockk<GrunnlagsdataService>()
    private val brevmottakereRepository = mockk<BrevmottakereRepository>()
    private val arbeidsfordelingService = mockk<ArbeidsfordelingService>(relaxed = true)

    private val iverksettingDtoMapper =
            IverksettingDtoMapper(arbeidsfordelingService = arbeidsfordelingService,
                                  behandlingshistorikkService = behandlingshistorikkService,
                                  fagsakService = fagsakService,
                                  grunnlagsdataService = grunnlagsdataService,
                                  simuleringService = simuleringService,
                                  barnService = barnService,
                                  tilbakekrevingService = tilbakekrevingService,
                                  tilkjentYtelseService = mockk(relaxed = true),
                                  vedtakService = vedtakService,
                                  vilkårsvurderingRepository = mockk(relaxed = true),
                                  brevmottakereRepository = brevmottakereRepository)

    private val fagsak = fagsak(fagsakpersoner(setOf("1")))
    private val behandling = behandling(fagsak)

    @BeforeEach
    internal fun setUp() {
        every { fagsakService.hentFagsakForBehandling(behandling.id) } returns fagsak
        every { vedtakService.hentVedtak(behandling.id) } returns Vedtak(behandling.id, ResultatType.INNVILGE)
        val behandlingshistorikk =
                Behandlingshistorikk(behandlingId = behandling.id, opprettetAv = "", steg = StegType.SEND_TIL_BESLUTTER)
        every { behandlingshistorikkService.finnSisteBehandlingshistorikk(any(), any()) } returns behandlingshistorikk
        every { brevmottakereRepository.findByIdOrNull(any()) } returns null
    }

    @Test
    internal fun `Skal mappe tilbakekreving med varseltekst og feilutbetaling`() {
        val forventetVarseltekst = "forventetVarseltekst"
        val simuleringsoppsummering = Simuleringsoppsummering(
                perioder = emptyList(),
                fomDatoNestePeriode = null,
                etterbetaling = BigDecimal.ZERO,
                feilutbetaling = BigDecimal.TEN,
                fom = null,
                tomDatoNestePeriode = null,
                forfallsdatoNestePeriode = null,
                tidSimuleringHentet = null,
                tomSisteUtbetaling = null
        )

        every {
            tilbakekrevingService.hentTilbakekreving(behandlingId = behandling.id)
        } returns Tilbakekreving(behandlingId = behandling.id,
                                 valg = Tilbakekrevingsvalg.OPPRETT_MED_VARSEL,
                                 varseltekst = forventetVarseltekst,
                                 begrunnelse = "ingen")
        every {
            simuleringService.hentLagretSimuleringsresultat(behandlingId = behandling.id)
        } returns simuleringsoppsummering.copy(feilutbetaling = BigDecimal.TEN)

        val tilbakekreving = iverksettingDtoMapper.mapTilbakekreving(behandling.id)
        assertThat(tilbakekreving?.tilbakekrevingMedVarsel?.varseltekst).isEqualTo(forventetVarseltekst)
        assertThat(tilbakekreving?.tilbakekrevingMedVarsel?.sumFeilutbetaling).isEqualTo(BigDecimal.TEN)
    }

    @Test
    internal fun `tilDto - skal kunne mappe person uten barn`() {
        every { barnService.finnBarnPåBehandling(any()) } returns emptyList()
        every { grunnlagsdataService.hentGrunnlagsdata(any()) } returns GrunnlagsdataMedMetadata(opprettGrunnlagsdata(), false, LocalDateTime.now())
        iverksettingDtoMapper.tilDto(behandling, "bes")

        verify(exactly = 1) { grunnlagsdataService.hentGrunnlagsdata(any()) }
        verify(exactly = 1) { arbeidsfordelingService.hentNavEnhetIdEllerBrukMaskinellEnhetHvisNull(any()) }
    }

}

