package no.nav.familie.ef.sak.no.nav.familie.ef.sak.vedtak.uttrekk
/*
import no.nav.familie.ef.sak.behandling.BehandlingRepository
import no.nav.familie.ef.sak.fagsak.FagsakRepository
import no.nav.familie.ef.sak.repository.behandling
import no.nav.familie.ef.sak.repository.fagsak
import no.nav.familie.ef.sak.repository.fagsakpersoner
import no.nav.familie.ef.sak.vedtak.uttrekk.UttrekkArbeidssøkerRepository
import no.nav.familie.ef.sak.vedtak.uttrekk.UttrekkArbeidssøkere
import no.nav.security.token.support.core.api.ProtectedWithClaims
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth

@RestController
@RequestMapping(path = ["/api/uttrekk"])
@ProtectedWithClaims(issuer = "azuread")
@Validated
class OpprettMangeArbeidsssøkereController(
        private val fagsakRepository: FagsakRepository,
        private val behandlingRepository: BehandlingRepository,
        private val uttrekkArbeidssøkerRepository: UttrekkArbeidssøkerRepository
) {

    @PostMapping("opprett-mange")
    fun opprett() {
        val fagsak = fagsakRepository.insert(fagsak(fagsakpersoner(setOf("1"))))
        val behandling = behandlingRepository.insert(behandling(fagsak))
        for (i in 1..30) {
            val arbeidssøkere = UttrekkArbeidssøkere(fagsakId = fagsak.id,
                                                     vedtakId = behandling.id,
                                                     årMåned = YearMonth.now().minusMonths(1))
            uttrekkArbeidssøkerRepository.insert(arbeidssøkere)
        }
    }

}
*/