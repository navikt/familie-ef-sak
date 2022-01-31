package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.BarnMedIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnService(
        private val barnRepository: BarnRepository,
        private val søknadService: SøknadService,
        private val fagsakService: FagsakService,
) {

    fun opprettBarnPåBehandlingMedSøknadsdata(behandlingId: UUID, fagsakId: UUID, grunnlagsdataBarn: List<BarnMedIdent>) {
        val barnFraSøknad = finnSøknadsbarnSomBehandlingBarn(behandlingId = behandlingId, fagsakId = fagsakId)
        val barnPåBehandlingen = BarnMatcher.kobleBehandlingBarnOgRegisterBarn(barnFraSøknad, grunnlagsdataBarn)
                .map {
                    BehandlingBarn(id = it.behandlingBarn.id,
                                   behandlingId = behandlingId,
                                   personIdent = it.barn?.personIdent,
                                   søknadBarnId = it.behandlingBarn.søknadBarnId,
                                   navn = it.barn?.navn?.visningsnavn(),
                                   fødselTermindato = it.behandlingBarn.fødselTermindato)
                }

        barnRepository.insertAll(barnPåBehandlingen)
    }

    private fun finnSøknadsbarnSomBehandlingBarn(behandlingId: UUID, fagsakId: UUID): List<BehandlingBarn> {
        val fagsak = fagsakService.hentFagsak(fagsakId)
        val barnFraSøknad = when (fagsak.stønadstype) {
                                Stønadstype.OVERGANGSSTØNAD -> søknadService.hentOvergangsstønad(behandlingId)?.barn
                                Stønadstype.BARNETILSYN -> søknadService.hentBarnetilsyn(behandlingId)?.barn
                                Stønadstype.SKOLEPENGER -> søknadService.hentSkolepenger(behandlingId)?.barn
                            } ?: emptyList()
        return barnFraSøknad.map {
            BehandlingBarn(behandlingId = behandlingId,
                           søknadBarnId = it.id,
                           personIdent = it.fødselsnummer,
                           navn = it.navn,
                           fødselTermindato = it.fødselTermindato)
        }
    }

    fun finnBarnPåBehandling(behandlingId: UUID): List<BehandlingBarn> = barnRepository.findByBehandlingId(behandlingId)

}
