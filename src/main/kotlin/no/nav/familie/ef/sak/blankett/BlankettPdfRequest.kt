package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.brev.dto.Avsnitt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.KontantstøttePeriode
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadDatoerDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vilkår.dto.TidligereVedtaksperioderDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårDto
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDate
import java.util.UUID

data class BlankettPdfRequest(
    val behandling: BlankettPdfBehandling,
    val personopplysninger: PersonopplysningerDto,
    val vilkår: VilkårDto,
    val vedtak: VedtakDto,
    val søknadsdatoer: SøknadDatoerDto?,
    val harAvsluttetArbeidsforhold: Boolean?,
    val beregnetSamvær: List<BeregnetSamvær>,
)

data class BlankettPdfBehandling(
    val årsak: BehandlingÅrsak,
    val stønadstype: StønadType,
    val årsakRevurdering: ÅrsakRevurderingDto?,
    val tidligereVedtaksperioder: TidligereVedtaksperioderDto?,
    val harKontantstøttePerioder: Boolean?,
    val kontantstøttePerioderFraKs: List<KontantstøttePeriode>,
    val registeropplysningerOpprettetDato: LocalDate,
)

data class PersonopplysningerDto(
    val navn: String,
    val personIdent: String,
)

data class BeregnetSamvær(
    val behandlingBarnId: UUID,
    val uker: List<Avsnitt>,
    val oppsummering: String,
)
