package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.domain.BehandlingResultat
import no.nav.familie.ef.sak.behandling.domain.BehandlingStatus
import no.nav.familie.ef.sak.behandling.domain.BehandlingType
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDao
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Fagsaker
import no.nav.familie.ef.sak.fagsak.domain.PersonIdent
import no.nav.familie.ef.sak.fagsak.domain.Stønadstype
import no.nav.familie.ef.sak.fagsak.domain.tilFagsak
import no.nav.familie.ef.sak.fagsak.dto.FagsakDto
import no.nav.familie.ef.sak.fagsak.dto.tilDto
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PdlClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.tilkjentytelse.TilkjentYtelseService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakService(private val fagsakRepository: FagsakRepository,
                    private val fagsakPersonService: FagsakPersonService,
                    private val behandlingService: BehandlingService,
                    private val pdlClient: PdlClient,
                    private val tilkjentYtelseService: TilkjentYtelseService,
                    private val featureToggleService: FeatureToggleService) {

    fun hentEllerOpprettFagsakMedBehandlinger(personIdent: String, stønadstype: Stønadstype): FagsakDto {
        return fagsakTilDto(hentEllerOpprettFagsak(personIdent, stønadstype))
    }

    @Transactional
    fun hentEllerOpprettFagsak(personIdent: String,
                               stønadstype: Stønadstype): Fagsak {
        val personIdenter = pdlClient.hentPersonidenter(personIdent, true)
        val gjeldendePersonIdent = personIdenter.gjeldende().ident
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent)
        val oppdatertPerson = oppdatertPerson(person, gjeldendePersonIdent)
        val fagsak = fagsakRepository.findByFagsakPersonIdAndStønadstype(oppdatertPerson.id, stønadstype)
                     ?: opprettFagsak(stønadstype, oppdatertPerson)

        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    fun settFagsakTilMigrert(fagsakId: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        feilHvis(fagsak.migrert) {
            "Fagsak er allerede migrert"
        }
        return fagsakRepository.update(fagsak.copy(migrert = true)).tilFagsakMedPerson()
    }

    fun finnFagsak(personIdenter: Set<String>, stønadstype: Stønadstype): Fagsak? =
            fagsakRepository.findBySøkerIdent(personIdenter, stønadstype)?.tilFagsakMedPerson()

    fun hentFagsakMedBehandlinger(fagsakId: UUID): FagsakDto {
        return fagsakTilDto(hentFagsak(fagsakId))
    }

    fun fagsakTilDto(fagsak: Fagsak): FagsakDto {
        val behandlinger: List<Behandling> = behandlingService.hentBehandlinger(fagsak.id)
        val erLøpende = erLøpende(behandlinger)
        return fagsak.tilDto(behandlinger = behandlinger.map(Behandling::tilDto), erLøpende = erLøpende)
    }

    fun finnFagsakerForFagsakPersonId(fagsakPersonId: UUID): Fagsaker {
        val fagsaker = fagsakRepository.findByFagsakPersonId(fagsakPersonId)
                .map { it.tilFagsakMedPerson() }
                .associateBy { it.stønadstype }
        return Fagsaker(
                overgangsstønad = fagsaker[Stønadstype.OVERGANGSSTØNAD],
                barnetilsyn = fagsaker[Stønadstype.BARNETILSYN],
                skolepenger = fagsaker[Stønadstype.SKOLEPENGER]
        )
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

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId).tilFagsakMedPerson()

    fun fagsakMedOppdatertPersonIdent(fagsakId: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        val person = fagsakPersonService.hentPerson(fagsak.fagsakPersonId)
        val gjelendeIdent = pdlClient.hentPersonidenter(person.hentAktivIdent(), true).gjeldende().ident
        val oppdatertPerson = oppdatertPerson(person, gjelendeIdent)
        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    private fun oppdatertPerson(person: FagsakPerson,
                                gjeldendePersonIdent: String) =
            if (featureToggleService.isEnabled("familie.ef.sak.synkroniser-personidenter")) {
                fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent)
            } else {
                person
            }

    fun hentFagsakForBehandling(behandlingId: UUID): Fagsak {
        return fagsakRepository.finnFagsakTilBehandling(behandlingId)?.tilFagsakMedPerson()
               ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")
    }

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId.id

    fun hentFagsakPåEksternId(eksternFagsakId: Long): Fagsak =
            fagsakRepository.finnMedEksternId(eksternFagsakId)?.tilFagsakMedPerson()
            ?: error("Kan ikke finne fagsak med eksternId=$eksternFagsakId")

    fun hentAktivIdent(fagsakId: UUID): String = fagsakRepository.finnAktivIdent(fagsakId)

    fun hentAktiveIdenter(fagsakId: Set<UUID>): Map<UUID, String> {
        if (fagsakId.isEmpty()) return emptyMap()

        val aktiveIdenter = fagsakRepository.finnAktivIdenter(fagsakId)
        feilHvis(!aktiveIdenter.map { it.first }.containsAll(fagsakId)) {
            "Finner ikke ident til fagsaker ${aktiveIdenter.map { it.first }.filterNot(fagsakId::contains)}"
        }
        return aktiveIdenter.associateBy({ it.first }, { it.second })
    }

    private fun opprettFagsak(stønadstype: Stønadstype, fagsakPerson: FagsakPerson): FagsakDao {
        return fagsakRepository.insert(FagsakDao(stønadstype = stønadstype,
                                                 fagsakPersonId = fagsakPerson.id))
    }

    fun FagsakDao.tilFagsakMedPerson(personIdenter: Set<PersonIdent> = fagsakPersonService.hentIdenter(this.fagsakPersonId)): Fagsak {
        return this.tilFagsak(personIdenter)
    }

}