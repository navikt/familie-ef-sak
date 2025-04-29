package no.nav.familie.ef.sak.blankett

import no.nav.familie.ef.sak.behandling.dto.ÅrsakRevurderingDto
import no.nav.familie.ef.sak.brev.dto.Avsnitt
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.KontantstøttePeriode
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadDatoerDto
import no.nav.familie.ef.sak.samværsavtale.dto.SamværsavtaleDto
import no.nav.familie.ef.sak.vedtak.dto.VedtakDto
import no.nav.familie.ef.sak.vilkår.dto.TidligereVedtaksperioderDto
import no.nav.familie.ef.sak.vilkår.dto.VilkårDto
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDate

data class BlankettPdfRequest(
    val behandling: BlankettPdfBehandling,
    val personopplysninger: PersonopplysningerDto,
    val vilkår: VilkårDto,
    val vedtak: VedtakDto,
    val søknadsdatoer: SøknadDatoerDto?,
    val harAvsluttetArbeidsforhold: Boolean?,
    val samværsavtaler: List<SamværsavtaleDto>,
    val samværsavtalerV2: List<Avsnitt>
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
