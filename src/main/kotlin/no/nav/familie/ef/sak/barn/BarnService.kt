package no.nav.familie.ef.sak.barn

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.opplysninger.mapper.BarnMatcher
import no.nav.familie.ef.sak.opplysninger.personopplysninger.GrunnlagsdataService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class BarnService(
        private val barnRepository: BarnRepository,
        private val søknadService: SøknadService,
        private val fagsakService: FagsakService,
        private val grunnlagsdataService: GrunnlagsdataService,
) {

    fun opprettBarnPåBehandlingMedSøknadsdata(behandlingId: UUID, fagsakId: UUID) {
        val barnFraSøknad = finnSøknadsbarn(fagsakId)
        val grunnlagsdata = grunnlagsdataService.hentGrunnlagsdata(behandlingId)

        val barnPåBehandlingen = BarnMatcher.kobleSøknadsbarnOgRegisterBarn(barnFraSøknad, grunnlagsdata.grunnlagsdata.barn)
                .map {
                    BehandlingBarn(behandlingId = behandlingId,
                                   personIdent = it.barn?.personIdent,
                                   søknadBarnId = it.søknadsbarn.id,
                                   navn = it.barn?.navn?.visningsnavn(),
                                   fødselTermindato = it.søknadsbarn.fødselTermindato)
                }

        barnRepository.insertAll(barnPåBehandlingen)
    }

    private fun finnSøknadsbarn(behandlingId: UUID): Collection<SøknadBarn> {
        val fagsak = fagsakService.hentFagsak(behandlingId)
        val barnFraSøknad = when (fagsak.stønadstype) {
                                Stønadstype.OVERGANGSSTØNAD -> søknadService.hentOvergangsstønad(behandlingId)?.barn
                                Stønadstype.BARNETILSYN -> søknadService.hentBarnetilsyn(behandlingId)?.barn
                                Stønadstype.SKOLEPENGER -> søknadService.hentSkolepenger(behandlingId)?.barn
                            } ?: emptyList()
        return barnFraSøknad
    }

}
