package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaBarnetilsyn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadsskjemaOvergangsstønad
import no.nav.familie.kontrakter.ef.søknad.Barn
import no.nav.familie.kontrakter.ef.søknad.TestsøknadBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

@Profile("integrasjonstest")
@Service
class SøknadHelperService {
    @Autowired
    private lateinit var barnRepository: BarnRepository

    @Autowired
    private lateinit var søknadService: SøknadService

    fun lagreSøknad(
        behandling: Behandling,
        barn: List<Barn>,
    ): SøknadsskjemaOvergangsstønad {
        val søknad =
            TestsøknadBuilder
                .Builder()
                .setBarn(barn)
                .build()
                .søknadOvergangsstønad
        søknadService.lagreSøknadForOvergangsstønad(søknad, behandling.id, behandling.fagsakId, "1L")
        val overgangsstønad =
            søknadService.hentOvergangsstønad(behandling.id)
                ?: error("Fant ikke overgangsstønad for testen")
        barnRepository.insertAll(søknadBarnTilBehandlingBarn(overgangsstønad.barn, behandling.id))
        return overgangsstønad
    }

    fun lagreSøknadForBarnetilsyn(
        behandling: Behandling,
        barn: List<Barn>,
    ): SøknadsskjemaBarnetilsyn {
        val søknad =
            TestsøknadBuilder
                .Builder()
                .setBarn(barn)
                .build()
                .søknadBarnetilsyn
        søknadService.lagreSøknadForBarnetilsyn(søknad, behandling.id, behandling.fagsakId, "1L")
        val barnetilsyn = søknadService.hentBarnetilsyn(behandling.id) ?: error("Fant ikke overgangsstønad for testen")
        barnRepository.insertAll(søknadBarnTilBehandlingBarn(barnetilsyn.barn, behandling.id))
        return barnetilsyn
    }
}
