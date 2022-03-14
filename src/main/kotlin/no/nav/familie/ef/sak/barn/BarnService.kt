package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.mapper.MatchetBehandlingBarn
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnService(
        private val barnRepository: BarnRepository,
        private val søknadService: SøknadService,
) {

    fun opprettBarnPåBehandlingMedSøknadsdata(behandlingId: UUID,
                                              fagsakId: UUID,
                                              grunnlagsdataBarn: List<BarnMedIdent>,
                                              stønadstype: Stønadstype) {
        val barnPåBehandlingen: List<BehandlingBarn> = when (stønadstype) {
            Stønadstype.BARNETILSYN -> {
                val søknadsbarnForBarnetilsyn = hentSøknadsbarnForBehandling(behandlingId)
                grunnlagsdataBarn.map { barn ->
                    BehandlingBarn(
                            behandlingId = behandlingId,
                            søknadBarnId = søknadsbarnForBarnetilsyn.find { it.fødselsnummer != null && it.fødselsnummer == barn.personIdent }?.id,
                            personIdent = barn.personIdent,
                            navn = barn.navn.visningsnavn(),
                    )
                }
            }
            Stønadstype.OVERGANGSSTØNAD -> {
                val barnFraSøknad = finnSøknadsbarnOgMapTilBehandlingBarn(behandlingId = behandlingId)
                BarnMatcher.kobleBehandlingBarnOgRegisterBarn(barnFraSøknad, grunnlagsdataBarn)
                        .map {
                            BehandlingBarn(id = it.behandlingBarn.id,
                                           behandlingId = behandlingId,
                                           personIdent = it.barn?.personIdent,
                                           søknadBarnId = it.behandlingBarn.søknadBarnId,
                                           navn = it.barn?.navn?.visningsnavn(),
                                           fødselTermindato = it.behandlingBarn.fødselTermindato)
                        }

            }
            else -> {
                throw NotImplementedError("Støtter kun overgangsstønad og barnetilsyn")
            }
        }
        barnRepository.insertAll(barnPåBehandlingen)
    }

    fun opprettBarnForRevurdering(behandlingId: UUID,
                                  forrigeBehandlingId: UUID,
                                  nyeBarnPåRevurdering: List<BehandlingBarn>,
                                  grunnlagsdataBarn: List<BarnMedIdent>) {
        val kobledeBarn: List<MatchetBehandlingBarn> = kobleAktuelleBarn(forrigeBehandlingId = forrigeBehandlingId,
                                                                         nyeBarnPåRevurdering = nyeBarnPåRevurdering,
                                                                         grunnlagsdataBarn = grunnlagsdataBarn)

        val alleBarnPåRevurdering = kobledeBarn.map {
            it.behandlingBarn.copy(id = UUID.randomUUID(),
                                   behandlingId = behandlingId,
                                   personIdent = it.barn?.personIdent ?: it.behandlingBarn.personIdent,
                                   navn = it.barn?.navn?.visningsnavn() ?: it.behandlingBarn.navn)
        }

        barnRepository.insertAll(alleBarnPåRevurdering)
    }

    private fun kobleAktuelleBarn(forrigeBehandlingId: UUID,
                                  nyeBarnPåRevurdering: List<BehandlingBarn>,
                                  grunnlagsdataBarn: List<BarnMedIdent>): List<MatchetBehandlingBarn> {
        val barnPåForrigeBehandling = barnRepository.findByBehandlingId(forrigeBehandlingId)
        val alleAktuelleBarn = barnPåForrigeBehandling + nyeBarnPåRevurdering

        return BarnMatcher.kobleBehandlingBarnOgRegisterBarn(alleAktuelleBarn, grunnlagsdataBarn)
    }

    private fun finnSøknadsbarnOgMapTilBehandlingBarn(behandlingId: UUID): List<BehandlingBarn> {
        val barnFraSøknad = hentSøknadsbarnForBehandling(behandlingId)
        return barnFraSøknad.map {
            BehandlingBarn(behandlingId = behandlingId,
                           søknadBarnId = it.id,
                           personIdent = it.fødselsnummer,
                           navn = it.navn,
                           fødselTermindato = it.fødselTermindato)
        }
    }

    private fun hentSøknadsbarnForBehandling(behandlingId: UUID) = søknadService.hentSøknadsgrunnlag(behandlingId)?.barn ?: emptyList()

    fun finnBarnPåBehandling(behandlingId: UUID): List<BehandlingBarn> = barnRepository.findByBehandlingId(behandlingId)

}