package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.behandling.ÅrsakRevurderingService
import no.nav.familie.ef.sak.felles.domain.Fil
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonopplysningerService
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadDatoerDto
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.vedtak.VedtakService
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vilkår.VurderingService
import no.nav.familie.ef.sak.vilkår.dto.tilDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BlankettService(
    private val vurderingService: VurderingService,
    private val blankettClient: BlankettClient,
    private val blankettRepository: BlankettRepository,
    private val behandlingService: BehandlingService,
    private val søknadService: SøknadService,
    private val personopplysningerService: PersonopplysningerService,
    private val vedtakService: VedtakService,
    private val årsakRevurderingService: ÅrsakRevurderingService,
    private val grunnlagsdataService: GrunnlagsdataService,
) {

    fun lagBlankett(behandlingId: UUID): ByteArray {
        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        val vilkårVurderinger = vurderingService.hentEllerOpprettVurderinger(behandlingId)
        val registergrunnlagData = grunnlagsdataService.hentGrunnlagsdata(behandlingId)
        val grunnlagsdata = registergrunnlagData.grunnlagsdata
        grunnlagsdata.tidligereVedtaksperioder.tilDto()
        val tidligereVedtaksperioder = grunnlagsdata.tidligereVedtaksperioder.tilDto()
        println("lagBlankett - tidligereVedtaksperioder: $tidligereVedtaksperioder") // TODO slett

        val blankettPdfRequest = BlankettPdfRequest(
            BlankettPdfBehandling(
                årsak = behandling.årsak,
                stønadstype = behandling.stønadstype,
                årsakRevurdering = årsakRevurderingService.hentÅrsakRevurdering(behandlingId)?.tilDto(),
                tidligereVedtaksperioder = tidligereVedtaksperioder,
            ),
            lagPersonopplysningerDto(behandling),
            vurderingService.hentEllerOpprettVurderinger(behandlingId),
            hentVedtak(behandlingId),
            lagSøknadsdatoer(behandlingId),
            vilkårVurderinger.grunnlag.harAvsluttetArbeidsforhold,
        )
        val blankettPdfAsByteArray = blankettClient.genererBlankett(blankettPdfRequest)
        oppdaterEllerOpprettBlankett(behandlingId, blankettPdfAsByteArray)
        return blankettPdfAsByteArray
    }

    private fun oppdaterEllerOpprettBlankett(behandlingId: UUID, pdf: ByteArray): Blankett {
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

    private fun lagPersonopplysningerDto(saksbehandling: Saksbehandling): PersonopplysningerDto {
        return PersonopplysningerDto(hentGjeldendeNavn(saksbehandling.ident), saksbehandling.ident)
    }

    private fun hentVedtak(behandlingId: UUID): VedtakDto {
        return vedtakService.hentVedtakDto(behandlingId)
    }

    private fun hentGjeldendeNavn(hentAktivIdent: String): String {
        val navnMap = personopplysningerService.hentGjeldeneNavn(listOf(hentAktivIdent))
        return navnMap.getValue(hentAktivIdent)
    }
}
