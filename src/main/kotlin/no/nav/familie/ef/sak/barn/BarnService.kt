package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.journalføring.dto.UstrukturertDokumentasjonType
import no.nav.familie.ef.sak.journalføring.dto.VilkårsbehandleNyeBarn
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.mapper.MatchetBehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.repository.findAllByIdOrThrow
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnService(
    private val barnRepository: BarnRepository,
    private val søknadService: SøknadService,
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
                navn = barn.navn.visningsnavn(),
            )
        }
    }

    private fun kobleBarnForOvergangsstønadOgSkolepenger(
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
        return when (ustrukturertDokumentasjonType) {
            UstrukturertDokumentasjonType.PAPIRSØKNAD -> kobleBarnSomSkalFødesPlusAlleRegisterbarn(
                behandlingId,
                barnSomSkalFødes,
                grunnlagsdataBarn
            )
            UstrukturertDokumentasjonType.ETTERSENDING -> barnForEttersending(
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
        behandlingId: UUID,
        vilkårsbehandleNyeBarn: VilkårsbehandleNyeBarn,
        grunnlagsdataBarn: List<BarnMedIdent>
    ): List<BehandlingBarn> = when (vilkårsbehandleNyeBarn) {
        VilkårsbehandleNyeBarn.VILKÅRSBEHANDLE -> mapBarnTilBehandlingBarn(behandlingId, grunnlagsdataBarn)
        VilkårsbehandleNyeBarn.IKKE_VILKÅRSBEHANDLE -> emptyList()
        VilkårsbehandleNyeBarn.IKKE_VALGT ->
            throw Feil("Må ha valgt om man skal vilkårsbehandle nye barn når man ettersender på ny behandling")
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
            navn = it.navn.visningsnavn(),
        )
    }

    fun opprettBarnForRevurdering(
        behandlingId: UUID,
        forrigeBehandlingId: UUID,
        nyeBarnPåRevurdering: List<BehandlingBarn>,
        grunnlagsdataBarn: List<BarnMedIdent>,
        stønadstype: StønadType
    ) {
        val kobledeBarn: List<MatchetBehandlingBarn> = kobleAktuelleBarn(
            forrigeBehandlingId = forrigeBehandlingId,
            nyeBarnPåRevurdering = nyeBarnPåRevurdering,
            grunnlagsdataBarn = grunnlagsdataBarn,
            stønadstype = stønadstype
        )

        val alleBarnPåRevurdering = kobledeBarn.map {
            it.behandlingBarn.copy(
                id = UUID.randomUUID(),
                behandlingId = behandlingId,
                personIdent = it.barn?.personIdent ?: it.behandlingBarn.personIdent,
                navn = it.barn?.navn?.visningsnavn() ?: it.behandlingBarn.navn
            )
        }

        barnRepository.insertAll(alleBarnPåRevurdering)
    }

    private fun validerAtAlleBarnErMedPåRevurderingen(
        kobledeBarn: List<BehandlingBarn>,
        grunnlagsdataBarn: List<BarnMedIdent>
    ) {
        val grunnlagsdataBarnIdenter = grunnlagsdataBarn.map { it.personIdent }
        val kobledeBarnIdenter = kobledeBarn.mapNotNull { it.personIdent }

        feilHvisIkke(kobledeBarnIdenter.containsAll(grunnlagsdataBarnIdenter)) {
            "Alle barn skal være med i revurderingen av en barnetilsynbehandling."
        }
    }

    private fun kobleAktuelleBarn(
        forrigeBehandlingId: UUID,
        nyeBarnPåRevurdering: List<BehandlingBarn>,
        grunnlagsdataBarn: List<BarnMedIdent>,
        stønadstype: StønadType
    ): List<MatchetBehandlingBarn> {
        val barnPåForrigeBehandling = barnRepository.findByBehandlingId(forrigeBehandlingId)
        val alleAktuelleBarn = barnPåForrigeBehandling + nyeBarnPåRevurdering

        if (stønadstype == StønadType.BARNETILSYN) {
            validerAtAlleBarnErMedPåRevurderingen(alleAktuelleBarn, grunnlagsdataBarn)
        }

        return BarnMatcher.kobleBehandlingBarnOgRegisterBarn(alleAktuelleBarn, grunnlagsdataBarn)
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
