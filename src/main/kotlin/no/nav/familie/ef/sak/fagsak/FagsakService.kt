package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.fagsak.dto.FagsakDto
import no.nav.familie.ef.sak.fagsak.dto.tilDto
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FagsakService(private val fagsakRepository: FagsakRepository,
                    private val behandlingService: BehandlingService,
                    private val pdlClient: PdlClient,
                    private val tilkjentYtelseService: TilkjentYtelseService,
                    private val featureToggleService: FeatureToggleService) {

    fun hentEllerOpprettFagsakMedBehandlinger(personIdent: String, stønadstype: Stønadstype): FagsakDto {
        return fagsakTilDto(hentEllerOpprettFagsak(personIdent, stønadstype))
    }

    fun hentEllerOpprettFagsak(personIdent: String,
                               stønadstype: Stønadstype): Fagsak {
        val personIdenter = pdlClient.hentPersonidenter(personIdent, true)
        val gjeldendePersonIdent = personIdenter.gjeldende().ident
        val fagsak = fagsakRepository.findBySøkerIdent(personIdenter.identer(), stønadstype)

        return fagsak?.let {
            return if (featureToggleService.isEnabled("familie.ef.sak.synkroniser-personidenter")) {
                fagsakMedOppdatertPersonIdent(fagsak, gjeldendePersonIdent)
            } else {
                fagsak
            }
        } ?: opprettFagsak(stønadstype, gjeldendePersonIdent)
    }

    fun finnFagsak(personIdenter: Set<String>, stønadstype: Stønadstype): Fagsak? =
            fagsakRepository.findBySøkerIdent(personIdenter, stønadstype)

    fun hentFagsakMedBehandlinger(fagsakId: UUID): FagsakDto {
        return fagsakTilDto(hentFagsak(fagsakId))
    }

    fun fagsakTilDto(fagsak: Fagsak): FagsakDto {
        val behandlinger: List<Behandling> = behandlingService.hentBehandlinger(fagsak.id)
        val erLøpende = erLøpende(behandlinger)
        return fagsak.tilDto(behandlinger = behandlinger.map(Behandling::tilDto), erLøpende = erLøpende)
    }

    fun erLøpende(behandlinger: List<Behandling>): Boolean {
        return behandlinger.filter {
            it.type != BehandlingType.BLANKETT &&
            it.resultat !== BehandlingResultat.HENLAGT &&
            it.resultat !== BehandlingResultat.AVSLÅTT &&
            it.status == BehandlingStatus.FERDIGSTILT
        }.maxByOrNull { it.sporbar.opprettetTid }
                       ?.let { tilkjentYtelseService.harLøpendeUtbetaling(it.id) } ?: false
    }

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId)

    fun fagsakMedOppdatertPersonIdent(fagsakId: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        return if (featureToggleService.isEnabled("familie.ef.sak.synkroniser-personidenter")) {
            val gjeldendePersonIdent = pdlClient.hentPersonidenter(fagsak.hentAktivIdent(), true).gjeldende().ident
            fagsakMedOppdatertPersonIdent(fagsak, gjeldendePersonIdent)
        } else {
            fagsak
        }
    }

    fun hentFagsakForBehandling(behandlingId: UUID): Fagsak {
        return fagsakRepository.finnFagsakTilBehandling(behandlingId)
               ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")
    }

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId.id

    fun hentFagsakPåEksternId(eksternFagsakId: Long): Fagsak = fagsakRepository.finnMedEksternId(eksternFagsakId)
                                                               ?: error("Kan ikke finne fagsak med eksternId=$eksternFagsakId")

    fun hentAktivIdent(fagsakId: UUID): String = fagsakRepository.finnAktivIdent(fagsakId)

    private fun fagsakMedOppdatertPersonIdent(fagsak: Fagsak, gjeldendePersonIdent: String): Fagsak {
        return when (fagsak.erAktivIdent(gjeldendePersonIdent)) {
            true -> fagsak
            false -> fagsakRepository.update(fagsak.fagsakMedOppdatertGjeldendeIdent(gjeldendePersonIdent))
        }
    }

    private fun opprettFagsak(stønadstype: Stønadstype, personIdent: String) =
            fagsakRepository.insert(Fagsak(stønadstype = stønadstype, søkerIdenter = setOf(FagsakPerson(ident = personIdent))))

}