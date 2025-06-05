package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.behandling.revurdering.ÅrsakRevurderingService
import no.nav.familie.ef.sak.brev.BrevClient
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadDatoerDto
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleHelper.lagBeregnetSamvær
import no.nav.familie.ef.sak.samværsavtale.SamværsavtaleService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BlankettService(
    private val vurderingService: VurderingService,
    private val brevClient: BrevClient,
    private val blankettRepository: BlankettRepository,
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val personopplysningerService: PersonopplysningerService,
    private val vedtakService: VedtakService,
    private val årsakRevurderingService: ÅrsakRevurderingService,
    private val samværsavtaleService: SamværsavtaleService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun lagBlankett(behandlingId: UUID): ByteArray {
        logger.info("Start - lag saksbehandlingsblankett for behandlingId=$behandlingId")
        val saksbehandling = behandlingService.hentSaksbehandling(behandlingId)
        logger.info("Start hent vilkårsvurderinger for behandlingId=$behandlingId")
        val vilkårVurderinger = vurderingService.hentEllerOpprettVurderinger(behandlingId)
        logger.info("Ferdig med å hente vilkårsvurderinger for behandlingId=$behandlingId")

        val blankettPdfRequest =
            BlankettPdfRequest(
                behandling = BlankettPdfBehandling(
                    årsak = saksbehandling.årsak,
                    stønadstype = saksbehandling.stønadstype,
                    årsakRevurdering = årsakRevurderingService.hentÅrsakRevurdering(behandlingId)?.tilDto(),
                    tidligereVedtaksperioder = vilkårVurderinger.grunnlag.tidligereVedtaksperioder,
                    harKontantstøttePerioder = vilkårVurderinger.grunnlag.harKontantstøttePerioder,
                    kontantstøttePerioderFraKs = vilkårVurderinger.grunnlag.kontantstøttePerioder,
                    registeropplysningerOpprettetDato = vilkårVurderinger.grunnlag.registeropplysningerOpprettetTid.toLocalDate(),
                ),
                personopplysninger = lagPersonopplysningerDto(saksbehandling),
                vilkår = vilkårVurderinger,
                vedtak = hentVedtak(behandlingId),
                søknadsdatoer = lagSøknadsdatoer(behandlingId),
                harAvsluttetArbeidsforhold = vilkårVurderinger.grunnlag.harAvsluttetArbeidsforhold,
                beregnetSamvær = samværsavtaleService.hentSamværsavtalerForBehandling(behandlingId).map { lagBeregnetSamvær(it) },
            )
        logger.info("Ferdig med å lage blankettPdfRequest for behandlingId=$behandlingId ")
        val blankettPdfAsByteArray = brevClient.genererBlankett(blankettPdfRequest)
        logger.info("Ferdig med å generere blankettPdf for behandlingId=$behandlingId")
        oppdaterEllerOpprettBlankett(behandlingId, blankettPdfAsByteArray)
        logger.info("Ferdig med å oppdatere eller opprette blankett for behandlingId=$behandlingId, størrelse=${blankettPdfAsByteArray.size} bytes")
        return blankettPdfAsByteArray
    }

    private fun oppdaterEllerOpprettBlankett(
        behandlingId: UUID,
        pdf: ByteArray,
    ): Blankett {
        val blankett = Blankett(behandlingId, Fil(pdf))
        if (blankettRepository.existsById(behandlingId)) {
            return blankettRepository.update(blankett)
        }
        return blankettRepository.insert(blankett)
    }

    private fun lagSøknadsdatoer(behandlingId: UUID): SøknadDatoerDto? {
        val søknadsgrunnlag = søknadService.hentSøknadsgrunnlag(behandlingId) ?: return null
        return SøknadDatoerDto(
            søknadsdato = søknadsgrunnlag.datoMottatt,
            søkerStønadFra = søknadsgrunnlag.søkerFra,
        )
    }

    private fun lagPersonopplysningerDto(saksbehandling: Saksbehandling): PersonopplysningerDto = PersonopplysningerDto(hentGjeldendeNavn(saksbehandling.ident), saksbehandling.ident)

    private fun hentVedtak(behandlingId: UUID): VedtakDto = vedtakService.hentVedtakDto(behandlingId)

    private fun hentGjeldendeNavn(hentAktivIdent: String): String {
        val navnMap = personopplysningerService.hentGjeldeneNavn(listOf(hentAktivIdent))
        return navnMap.getValue(hentAktivIdent)
    }
}
