package no.nav.familie.ef.sak.patch

import no.nav.familie.ef.sak.OppslagSpringRunnerTest
import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.FØRSTEGANGSBEHANDLING
import no.nav.familie.ef.sak.behandling.domain.BehandlingType.REVURDERING
import no.nav.familie.ef.sak.behandlingsflyt.steg.StegType
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseRepository
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.VedtaksperiodeType
import no.nav.familie.ef.sak.vedtak.dto.Innvilget
import no.nav.familie.ef.sak.vedtak.dto.Opphør
import no.nav.familie.ef.sak.vedtak.dto.ResultatType
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vedtak.dto.VedtaksperiodeDto
import no.nav.familie.ef.sak.økonomi.lagTilkjentYtelse
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak.SØKNAD
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.YearMonth

internal class PatchStardatoServiceTest : OppslagSpringRunnerTest() {

    @Autowired lateinit var behandlingService: BehandlingService
    @Autowired lateinit var vedtakService: VedtakService
    @Autowired lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository
    @Autowired lateinit var patchStardatoService: PatchStardatoService

    private val behandling1dato = YearMonth.of(2021, 3)
    private val behandling2dato = behandling1dato.plusMonths(1)
    private val behandling3dato = behandling1dato.minusMonths(1)
    private val behandling4dato = behandling1dato.minusMonths(2)

    @Test
    internal fun `skal oppdatere startdato på innvilget vedtak`() {
        val fagsak = testoppsettService.lagreFagsak(fagsak(fagsakpersoner(setOf("1"))))
        val behandling = opprettBehandling(fagsak, lagInnvilget(behandling1dato), FØRSTEGANGSBEHANDLING)
        val behandling2 = opprettBehandling(fagsak, lagInnvilget(behandling2dato))
        val behandling3 = opprettBehandling(fagsak, lagInnvilget(behandling3dato))
        val behandling4 = opprettBehandling(fagsak, lagOpphør(behandling4dato))

        patchStardatoService.patch(fagsak.id, true)
        assertStartdato(behandling, behandling1dato)
        assertStartdato(behandling2, behandling1dato) // beholder 1 sin dato pga innvilget er frem i tiden
        assertStartdato(behandling3, behandling3dato)
        assertStartdato(behandling4, behandling4dato)
    }

    private fun opprettBehandling(fagsak: Fagsak,
                                  vedtakDto: VedtakDto,
                                  behandlingType: BehandlingType = REVURDERING): Behandling {
        val behandling = behandlingService.opprettBehandling(behandlingType, fagsak.id, behandlingsårsak = SØKNAD)
        val behandlingResultat = when (vedtakDto) {
            is Innvilget -> BehandlingResultat.INNVILGET
            is Opphør -> BehandlingResultat.OPPHØRT
            else -> error("VedtakDto=${vedtakDto::class.java.simpleName} matcher ikke")
        }
        vedtakService.lagreVedtak(vedtakDto, behandling.id)
        tilkjentYtelseRepository.insert(lagTilkjentYtelse(emptyList(), behandlingId = behandling.id))
        behandlingService.oppdaterStegPåBehandling(behandling.id, StegType.BEHANDLING_FERDIGSTILT)
        behandlingService.oppdaterResultatPåBehandling(behandling.id, behandlingResultat)
        behandlingService.oppdaterStatusPåBehandling(behandling.id, BehandlingStatus.FERDIGSTILT)
        return behandling
    }

    private fun assertStartdato(behandling: Behandling, måned: YearMonth) {
        assertThat(tilkjentYtelseRepository.findByBehandlingId(behandling.id)!!.startdato)
                .isEqualTo(måned.atDay(1))
    }

    private fun lagOpphør(måned: YearMonth) =
            Opphør(opphørFom = måned, begrunnelse = null)

    private fun lagInnvilget(måned: YearMonth) =
            Innvilget(resultatType = ResultatType.INNVILGE,
                      perioder = listOf(VedtaksperiodeDto(måned,
                                                          måned,
                                                          AktivitetType.IKKE_AKTIVITETSPLIKT,
                                                          VedtaksperiodeType.HOVEDPERIODE)),
                      periodeBegrunnelse = null,
                      inntektBegrunnelse = null,
                      inntekter = emptyList())
}