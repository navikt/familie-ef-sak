package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvisIkke
import no.nav.familie.ef.sak.journalføring.dto.BarnSomSkalFødes
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.mapper.MatchetBehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
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
) {

    /**
     * Barn som blir lagrede er:
     * [StønadType.BARNETILSYN]: Alle barnen fra registeret, med data fra søknaden
     * [StønadType.OVERGANGSSTØNAD], [StønadType.SKOLEPENGER]:
     *  Hvis papirsøknad: Innsendte terminbarn plus barn fra registeret
     *  Ellers: Barn/terminbarn fra søknaden
     */
    fun opprettBarnPåBehandlingMedSøknadsdata(
        behandlingId: UUID,
        fagsakId: UUID,
        grunnlagsdataBarn: List<BarnMedIdent>,
        stønadstype: StønadType,
        behandlingsårsak: BehandlingÅrsak,
        barnSomSkalFødes: List<BarnSomSkalFødes> = emptyList()
    ) {
        val barnPåBehandlingen: List<BehandlingBarn> = when (stønadstype) {
            StønadType.BARNETILSYN -> barnForBarnetilsyn(barnSomSkalFødes, behandlingId, grunnlagsdataBarn)
            StønadType.OVERGANGSSTØNAD, StønadType.SKOLEPENGER ->
                kobleBarnForOvergangsstønadOgSkolepenger(
                    behandlingId,
                    behandlingsårsak,
                    grunnlagsdataBarn,
                    barnSomSkalFødes
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
        behandlingsårsak: BehandlingÅrsak,
        grunnlagsdataBarn: List<BarnMedIdent>,
        barnSomSkalFødes: List<BarnSomSkalFødes>
    ): List<BehandlingBarn> {
        feilHvis(behandlingsårsak != BehandlingÅrsak.PAPIRSØKNAD && barnSomSkalFødes.isNotEmpty()) {
            "Kan ikke legge til terminbarn med behandlingsårsak=$behandlingsårsak"
        }
        return if (behandlingsårsak == BehandlingÅrsak.PAPIRSØKNAD) {
            barnForPapirsøknad(behandlingId, barnSomSkalFødes, grunnlagsdataBarn)
        } else {
            kobleBehandlingBarnOgRegisterBarnTilBehandlingBarn(
                finnSøknadsbarnOgMapTilBehandlingBarn(behandlingId = behandlingId),
                grunnlagsdataBarn,
                behandlingId
            )
        }
    }

    /**
     * Papirsøknad kobler [barnSomSkalFødes] til [grunnlagsdataBarn]
     * Samt legger til de barn fra grunnlagsdata som mangler, sånn at alle registerbarn blir med
     */
    private fun barnForPapirsøknad(
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
        val barnFraRegisterSomIkkeBlittKoblede =
            barnFraRegisterSomIkkeBlittKoblede(kobledeBarn, grunnlagsdataBarn, behandlingId)
        return kobledeBarn + barnFraRegisterSomIkkeBlittKoblede
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

    private fun barnFraRegisterSomIkkeBlittKoblede(
        kobledeBarn: List<BehandlingBarn>,
        grunnlagsdataBarn: List<BarnMedIdent>,
        behandlingId: UUID
    ): List<BehandlingBarn> {
        val kobledeBarnIdenter = kobledeBarn.mapNotNull { it.personIdent }.toSet()
        return grunnlagsdataBarn.filterNot { kobledeBarnIdenter.contains(it.personIdent) }.map {
            BehandlingBarn(
                behandlingId = behandlingId,
                personIdent = it.personIdent,
                navn = it.navn.visningsnavn(),
            )
        }
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
