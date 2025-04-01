package no.nav.familie.ef.sak.testutil

import no.nav.familie.ef.sak.barn.BarnRepository
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.infrastruktur.config.PdlClientConfig
import no.nav.familie.ef.sak.opplysninger.søknad.SøknadService
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
        barn: List<Barn> =
            listOf(
                TestsøknadBuilder.Builder().defaultBarn("Barn Barnesen", PdlClientConfig.BARN_FNR),
                TestsøknadBuilder.Builder().defaultBarn("Barn2 Barnesen", PdlClientConfig.BARN2_FNR),
            ),
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
}
