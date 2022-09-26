package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
import no.nav.familie.kontrakter.ef.felles.BehandlingÅrsak
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnService(
    private val barnRepository: BarnRepository,
    private val søknadService: SøknadService,
    private val behandlingService: BehandlingService
) {

    /**
     * Barn som blir lagrede er:
     * [StønadType.BARNETILSYN]: Alle barnen fra registeret, med data fra søknaden
     * [StønadType.OVERGANGSSTØNAD], [StønadType.SKOLEPENGER]:
     *  Hvis papirsøknad: Innsendte terminbarn plus barn fra registeret
     *  Ellers: Barn/terminbarn fra søknaden
     *
     *  Kun barn under 18 er med
     */
    fun opprettBarnPåBehandlingMedSøknadsdata(
        behandlingId: UUID,
        fagsakId: UUID,
        grunnlagsdataBarn: List<BarnMedIdent>,
        stønadstype: StønadType,
        ustrukturertDokumentasjonType: UstrukturertDokumentasjonType = UstrukturertDokumentasjonType.IKKE_VALGT,
        barnSomSkalFødes: List<BarnSomSkalFødes> = emptyList(),
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn = VilkårsbehandleNyeBarn.IKKE_VALGT
    ) {
        val barnUnder18 = grunnlagsdataBarn.filter { it.fødsel.gjeldende().erUnder18År() }
        val barnPåBehandlingen: List<BehandlingBarn> = when (stønadstype) {
            StønadType.BARNETILSYN -> barnForBarnetilsyn(barnSomSkalFødes, behandlingId, barnUnder18)
            StønadType.OVERGANGSSTØNAD, StønadType.SKOLEPENGER ->
                kobleBarnForOvergangsstønadOgSkolepenger(
                    fagsakId,
                    behandlingId,
                    ustrukturertDokumentasjonType,
                    barnUnder18,
                    barnSomSkalFødes,
                    vilkårsbehandleNyeBarn
                )
        }
        barnRepository.insertAll(barnPåBehandlingen)
    }

    /**
     * Barnetilsyn bruker barn fra registeret, og kobler det til søknadsbarn hvis det finnes i søknaden
     */
    private fun barnForBarnetilsyn(
        barnSomSkalFødes: List<BarnSomSkalFødes>,
        behandlingId: UUID,
        grunnlagsdataBarn: List<BarnMedIdent>
    ): List<BehandlingBarn> {
        feilHvis(barnSomSkalFødes.isNotEmpty()) {
            "Kan ikke håndtere barnSomSkalFødes i barnetilsyn"
        }
        val barnFraSøknad = hentSøknadsbarnForBehandling(behandlingId).associate { it.fødselsnummer to it.id }
        return grunnlagsdataBarn.map { barn ->
            BehandlingBarn(
                behandlingId = behandlingId,
                søknadBarnId = barnFraSøknad[barn.personIdent],
                personIdent = barn.personIdent,
                navn = barn.navn.visningsnavn()
            )
        }
    }

    private fun kobleBarnForOvergangsstønadOgSkolepenger(
        fagsakId: UUID,
        behandlingId: UUID,
        ustrukturertDokumentasjonType: UstrukturertDokumentasjonType,
        grunnlagsdataBarn: List<BarnMedIdent>,
        barnSomSkalFødes: List<BarnSomSkalFødes>,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn
    ): List<BehandlingBarn> {
        feilHvis(
            ustrukturertDokumentasjonType != UstrukturertDokumentasjonType.PAPIRSØKNAD &&
                barnSomSkalFødes.isNotEmpty()
        ) {
            "Kan ikke legge til terminbarn med ustrukturertDokumentasjonType=$ustrukturertDokumentasjonType"
        }
        feilHvis(
            vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VALGT &&
                ustrukturertDokumentasjonType != UstrukturertDokumentasjonType.ETTERSENDING
        ) {
            "Kun ettersending forventes å sende inn vilkårsbehandle nye barn"
        }
        return when (ustrukturertDokumentasjonType) {
            UstrukturertDokumentasjonType.PAPIRSØKNAD -> kobleBarnSomSkalFødesPlusAlleRegisterbarn(
                behandlingId,
                barnSomSkalFødes,
                grunnlagsdataBarn
            )
            UstrukturertDokumentasjonType.ETTERSENDING -> barnForEttersending(
                fagsakId,
                behandlingId,
                vilkårsbehandleNyeBarn,
                grunnlagsdataBarn
            )
            UstrukturertDokumentasjonType.IKKE_VALGT -> kobleBehandlingBarnOgRegisterBarnTilBehandlingBarn(
                finnSøknadsbarnOgMapTilBehandlingBarn(behandlingId = behandlingId),
                grunnlagsdataBarn,
                behandlingId
            )
        }
    }

    private fun barnForEttersending(
        fagsakId: UUID,
        behandlingId: UUID,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn,
        grunnlagsdataBarn: List<BarnMedIdent>
    ): List<BehandlingBarn> = when (vilkårsbehandleNyeBarn) {
        VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE ->
            vilkårsbehandleBarnForEttersending(fagsakId, behandlingId, grunnlagsdataBarn)
        VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE -> emptyList()
        VilkårsbehandleNyeBarn.IKKE_VALGT ->
            throw Feil("Må ha valgt om man skal vilkårsbehandle nye barn når man ettersender på ny behandling")
    }

    private fun vilkårsbehandleBarnForEttersending(
        fagsakId: UUID,
        behandlingId: UUID,
        grunnlagsdataBarn: List<BarnMedIdent>
    ): List<BehandlingBarn> {
        val forrigeBehandling = behandlingService.finnSisteIverksatteBehandlingMedEventuellAvslått(fagsakId)
        feilHvis(forrigeBehandling == null) {
            "Kan ikke behandle ettersending når det ikke finnes en tidligere behandling"
        }
        val barnSomSkalFødesFraForrigeBehandling = barnRepository.findByBehandlingId(forrigeBehandling.id)
            .filter { it.personIdent == null }
            .mapNotNull { it.fødselTermindato }
            .map { BarnSomSkalFødes(it) }
        return kobleBarnSomSkalFødesPlusAlleRegisterbarn(
            behandlingId,
            barnSomSkalFødesFraForrigeBehandling,
            grunnlagsdataBarn
        )
    }

    /**
     * Papirsøknad kobler [barnSomSkalFødes] til [grunnlagsdataBarn]
     * Samt legger til de barn fra grunnlagsdata som mangler, sånn at alle registerbarn blir med
     */
    private fun kobleBarnSomSkalFødesPlusAlleRegisterbarn(
        behandlingId: UUID,
        barnSomSkalFødes: List<BarnSomSkalFødes>,
        grunnlagsdataBarn: List<BarnMedIdent>
    ): List<BehandlingBarn> {
        val barnSomSkalFødesSomBehandlingBarn = barnSomSkalFødes.map { it.tilBehandlingBarn(behandlingId) }
        val kobledeBarn = kobleBehandlingBarnOgRegisterBarnTilBehandlingBarn(
            barnSomSkalFødesSomBehandlingBarn,
            grunnlagsdataBarn,
            behandlingId
        )
        return kobledeBarnPlusRegisterbarn(behandlingId, grunnlagsdataBarn, kobledeBarn)
    }

    private fun kobleBehandlingBarnOgRegisterBarnTilBehandlingBarn(
        barnFraSøknad: List<BehandlingBarn>,
        grunnlagsdataBarn: List<BarnMedIdent>,
        behandlingId: UUID
    ): List<BehandlingBarn> {
        return BarnMatcher.kobleBehandlingBarnOgRegisterBarn(barnFraSøknad, grunnlagsdataBarn)
            .map {
                BehandlingBarn(
                    id = it.behandlingBarn.id,
                    behandlingId = behandlingId,
                    personIdent = it.barn?.personIdent,
                    søknadBarnId = it.behandlingBarn.søknadBarnId,
                    navn = it.barn?.navn?.visningsnavn(),
                    fødselTermindato = it.behandlingBarn.fødselTermindato
                )
            }
    }

    /**
     * Legger sammen koblede barn plus de fra registeret som mangler
     * Sånn at man får journalført en papirsøknad med terminbarn, som kobles sammen med fødte barn. Plus alle andre barn
     */
    private fun kobledeBarnPlusRegisterbarn(
        behandlingId: UUID,
        grunnlagsdataBarn: List<BarnMedIdent>,
        kobledeBarn: List<BehandlingBarn>
    ): List<BehandlingBarn> {
        val kobledeBarnIdenter = kobledeBarn.mapNotNull { it.personIdent }.toSet()
        val ukobledeBarn = grunnlagsdataBarn.filterNot { kobledeBarnIdenter.contains(it.personIdent) }
        return kobledeBarn + mapBarnTilBehandlingBarn(behandlingId, ukobledeBarn)
    }

    private fun mapBarnTilBehandlingBarn(
        behandlingId: UUID,
        grunnlagsdataBarn: List<BarnMedIdent>
    ) = grunnlagsdataBarn.map {
        BehandlingBarn(
            behandlingId = behandlingId,
            personIdent = it.personIdent,
            navn = it.navn.visningsnavn()
        )
    }

    /**
     * @param forrigeBehandlingId finnes her fordi det kan være
     * en avslått forrigeBehandlingId i de tilfeller behandling.forrigeBehandlingId er null
     */
    fun opprettBarnForRevurdering(
        behandling: Behandling,
        forrigeBehandlingId: UUID,
        stønadstype: StønadType,
        grunnlagsdataBarn: List<BarnMedIdent>,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn
    ) {
        val barnPåForrigeBehandling = barnRepository.findByBehandlingId(forrigeBehandlingId)
        validerVilkårsbehandleNyeBarn(behandling, stønadstype, barnPåForrigeBehandling, vilkårsbehandleNyeBarn)

        val aktuelleBarn =
            barnForRevurdering(behandling, grunnlagsdataBarn, barnPåForrigeBehandling, vilkårsbehandleNyeBarn)

        barnRepository.insertAll(aktuelleBarn)
    }

    private fun barnForRevurdering(
        behandling: Behandling,
        grunnlagsdataBarn: List<BarnMedIdent>,
        barnPåForrigeBehandling: List<BehandlingBarn>,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn
    ): List<BehandlingBarn> {
        val pdlBarn = grunnlagsdataBarn.filter { it.fødsel.gjeldende().erUnder18År() }
        val kobledeBarn = kobleBarn(behandling.id, pdlBarn, barnPåForrigeBehandling)

        return when (vilkårsbehandleNyeBarn) {
            VilkårsbehandleNyeBarn.IKKE_VALGT -> throw Feil("Kan ikke velge IKKE_VALGT for VilkårsbehandleNyeBarn")
            VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE -> kobledeBarn + nyeBarn(pdlBarn, kobledeBarn, behandling.id)
            VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE -> kobledeBarn
        }
    }

    private fun validerVilkårsbehandleNyeBarn(
        saksbehandling: Behandling,
        stønadstype: StønadType,
        barnPåForrigeBehandling: List<BehandlingBarn>,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn
    ) {
        feilHvis(vilkårsbehandleNyeBarn == VilkårsbehandleNyeBarn.IKKE_VALGT) {
            "Forventer at man sender inn vilkårsbehandleNyeBarn"
        }
        feilHvis(stønadstype == StønadType.BARNETILSYN && vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE) {
            "Må vilkårsbehandle alle nye barn på "
        }

        feilHvis(
            saksbehandling.årsak != BehandlingÅrsak.G_OMREGNING &&
                vilkårsbehandleNyeBarn == VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE &&
                barnPåForrigeBehandling.isNotEmpty()
        ) {
            "Alle barn skal være med i revurderingen av en barnetilsynbehandling."
        }
        feilHvis(saksbehandling.årsak == BehandlingÅrsak.G_OMREGNING && vilkårsbehandleNyeBarn != VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE) {
            "Kan ikke sende inn nye barn på revurdering med årsak G-omregning"
        }
    }

    private fun kobleBarn(
        behandlingId: UUID,
        pdlBarn: List<BarnMedIdent>,
        barnPåForrigeBehandling: List<BehandlingBarn>
    ) = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(barnPåForrigeBehandling, pdlBarn)
        .map {
            it.behandlingBarn.copy(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                personIdent = it.barn?.personIdent ?: it.behandlingBarn.personIdent,
                navn = it.barn?.navn?.visningsnavn() ?: it.behandlingBarn.navn
            )
        }

    private fun nyeBarn(
        pdlBarn: List<BarnMedIdent>,
        kobledeBarn: List<BehandlingBarn>,
        behandlingId: UUID
    ): List<BehandlingBarn> {
        val nyeBarn = pdlBarn.filter { barn -> kobledeBarn.none { it.personIdent == barn.personIdent } }
            .map {
                BehandlingBarn(
                    behandlingId = behandlingId,
                    personIdent = it.personIdent,
                    navn = it.navn.visningsnavn()
                )
            }
        return nyeBarn
    }

    private fun finnSøknadsbarnOgMapTilBehandlingBarn(behandlingId: UUID): List<BehandlingBarn> {
        val barnFraSøknad = hentSøknadsbarnForBehandling(behandlingId)
        return barnFraSøknad.map {
            BehandlingBarn(
                behandlingId = behandlingId,
                søknadBarnId = it.id,
                personIdent = it.fødselsnummer,
                navn = it.navn,
                fødselTermindato = it.fødselTermindato
            )
        }
    }

    private fun hentSøknadsbarnForBehandling(behandlingId: UUID) =
        søknadService.hentSøknadsgrunnlag(behandlingId)?.barn ?: emptyList()

    fun finnBarnPåBehandling(behandlingId: UUID): List<BehandlingBarn> = barnRepository.findByBehandlingId(behandlingId)

    fun validerBarnFinnesPåBehandling(behandlingId: UUID, barn: Set<UUID>) {
        val barnPåBehandling = finnBarnPåBehandling(behandlingId).map { it.id }.toSet()
        feilHvis(barn.any { !barnPåBehandling.contains(it) }) {
            "Et barn som ikke finnes på behandling=$behandlingId er lagt til, innsendte=$barn"
        }
    }

    fun hentBehandlingBarnForBarnIder(barnId: List<UUID>): List<BehandlingBarn> {
        return barnRepository.findAllByIdOrThrow(barnId.toSet()) { it.id }
    }
}
