package no.nav.familie.ef.sak.fagsak

import no.nav.familie.ef.sak.behandling.BehandlingService
import no.nav.familie.ef.sak.behandling.domain.Behandling
import no.nav.familie.ef.sak.behandling.dto.tilDto
import no.nav.familie.ef.sak.fagsak.domain.Fagsak
import no.nav.familie.ef.sak.fagsak.domain.FagsakDomain
import no.nav.familie.ef.sak.fagsak.domain.FagsakPerson
import no.nav.familie.ef.sak.fagsak.domain.Fagsaker
import no.nav.familie.ef.sak.fagsak.domain.tilFagsakMedPerson
import no.nav.familie.ef.sak.fagsak.dto.FagsakDto
import no.nav.familie.ef.sak.fagsak.dto.tilDto
import no.nav.familie.ef.sak.infotrygd.InfotrygdService
import no.nav.familie.ef.sak.infrastruktur.exception.Feil
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.featuretoggle.FeatureToggleService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlIdent
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.identer
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.kontrakter.felles.ef.StønadType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FagsakService(
    private val fagsakRepository: FagsakRepository,
    private val fagsakPersonService: FagsakPersonService,
    private val behandlingService: BehandlingService,
    private val personService: PersonService,
    private val featureToggleService: FeatureToggleService,
    private val infotrygdService: InfotrygdService,
) {
    fun hentEllerOpprettFagsakMedBehandlinger(
        personIdent: String,
        stønadstype: StønadType,
    ): FagsakDto = fagsakTilDto(hentEllerOpprettFagsak(personIdent, stønadstype))

    @Transactional
    fun hentEllerOpprettFagsak(
        personIdent: String,
        stønadstype: StønadType,
    ): Fagsak {
        val personIdenter = personService.hentPersonIdenter(personIdent)
        val gjeldendePersonIdent = personIdenter.gjeldende()
        val person = fagsakPersonService.hentEllerOpprettPerson(personIdenter.identer(), gjeldendePersonIdent.ident)
        val oppdatertPerson = oppdatertPerson(person, gjeldendePersonIdent)
        val fagsak =
            fagsakRepository.findByFagsakPersonIdAndStønadstype(oppdatertPerson.id, stønadstype)
                ?: opprettFagsak(stønadstype, oppdatertPerson)

        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    @Transactional
    fun finnFagsakEllerOpprettHvisPersonFinnesIInfotrygd(
        personIdenter: Set<String>,
        gjeldendePersonIdent: String,
    ): List<Fagsak> {
        val fagsaker = fagsakRepository.findBySøkerIdent(personIdenter)

        if (fagsaker.isEmpty()) {
            if (infotrygdService.eksisterer(gjeldendePersonIdent)) {
                fagsakPersonService.hentEllerOpprettPerson(personIdenter, gjeldendePersonIdent)
                return listOf()
            }
            return listOf()
        }
        return fagsaker.map { it.tilFagsakMedPerson(fagsakPersonService.hentIdenter(it.fagsakPersonId)) }
    }

    fun settFagsakTilMigrert(fagsakId: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        feilHvis(fagsak.migrert) {
            "Fagsak er allerede migrert"
        }
        return fagsakRepository.update(fagsak.copy(migrert = true)).tilFagsakMedPerson()
    }

    fun finnFagsak(
        personIdenter: Set<String>,
        stønadstype: StønadType,
    ): Fagsak? =
        fagsakRepository.findBySøkerIdent(personIdenter, stønadstype)?.tilFagsakMedPerson()

    fun finnFagsaker(personIdenter: Set<String>): List<Fagsak> =
        fagsakRepository.findBySøkerIdent(personIdenter).map { it.tilFagsakMedPerson() }

    fun hentFagsakMedBehandlinger(fagsakId: UUID): FagsakDto = fagsakTilDto(hentFagsak(fagsakId))

    fun fagsakTilDto(fagsak: Fagsak): FagsakDto {
        val behandlinger: List<Behandling> = behandlingService.hentBehandlinger(fagsak.id)
        val erLøpende = erLøpende(fagsak)
        return fagsak.tilDto(
            behandlinger =
                behandlinger.map {
                    it.tilDto(fagsak.stønadstype)
                },
            erLøpende = erLøpende,
        )
    }

    fun finnFagsakerForFagsakPersonId(fagsakPersonId: UUID): Fagsaker {
        val fagsaker =
            fagsakRepository
                .findByFagsakPersonId(fagsakPersonId)
                .map { it.tilFagsakMedPerson() }
                .associateBy { it.stønadstype }
        return Fagsaker(
            overgangsstønad = fagsaker[StønadType.OVERGANGSSTØNAD],
            barnetilsyn = fagsaker[StønadType.BARNETILSYN],
            skolepenger = fagsaker[StønadType.SKOLEPENGER],
        )
    }

    fun erLøpende(fagsak: Fagsak): Boolean = fagsakRepository.harLøpendeUtbetaling(fagsak.id)

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId).tilFagsakMedPerson()

    fun fagsakMedOppdatertPersonIdent(fagsakId: UUID): Fagsak {
        val fagsak = fagsakRepository.findByIdOrThrow(fagsakId)
        val person = fagsakPersonService.hentPerson(fagsak.fagsakPersonId)
        val gjeldendeIdent = personService.hentPersonIdenter(person.hentAktivIdent()).gjeldende()
        val oppdatertPerson = oppdatertPerson(person, gjeldendeIdent)
        return fagsak.tilFagsakMedPerson(oppdatertPerson.identer)
    }

    fun fagsakerMedOppdatertePersonIdenter(fagsakId: List<UUID>): List<Fagsak> {
        val fagsaker = fagsakRepository.findAllById(fagsakId)
        val personer = fagsakPersonService.hentPersoner(fagsaker.map { it.fagsakPersonId }).associateBy { it.id }

        val gjeldendeIdenter = personService.hentIdenterBolk(personer.values.map { it.hentAktivIdent() })

        return fagsaker.map {
            val person = personer[it.fagsakPersonId]!!
            val gjeldendeIdent = gjeldendeIdenter[person.hentAktivIdent()]
            val oppdatertPerson = gjeldendeIdent?.let { oppdatertPerson(person, gjeldendeIdent) } ?: person
            it.tilFagsakMedPerson(oppdatertPerson.identer)
        }
    }

    private fun oppdatertPerson(
        person: FagsakPerson,
        gjeldendePersonIdent: PdlIdent,
    ) = fagsakPersonService.oppdaterIdent(person, gjeldendePersonIdent.ident)

    fun hentFagsakDtoForBehandling(behandlingId: UUID) = fagsakTilDto(hentFagsakForBehandling(behandlingId))

    fun hentFagsakForBehandling(behandlingId: UUID): Fagsak =
        fagsakRepository.finnFagsakTilBehandling(behandlingId)?.tilFagsakMedPerson()
            ?: throw Feil("Finner ikke fagsak til behandlingId=$behandlingId")

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId

    fun hentFagsakPåEksternId(eksternFagsakId: Long): Fagsak =
        fagsakRepository
            .finnMedEksternId(eksternFagsakId)
            ?.tilFagsakMedPerson()
            ?: error("Finner ikke fagsak til eksternFagsakId=$eksternFagsakId")

    fun hentFagsakDtoPåEksternId(eksternFagsakId: Long): FagsakDto =
        hentFagsakPåEksternIdHvisEksisterer(eksternFagsakId)
            ?: error("Kan ikke finne fagsak med eksternId=$eksternFagsakId")

    fun hentFagsakPåEksternIdHvisEksisterer(eksternFagsakId: Long): FagsakDto? =
        fagsakRepository
            .finnMedEksternId(eksternFagsakId)
            ?.tilFagsakMedPerson()
            ?.let { fagsakTilDto(it) }

    fun hentAktivIdent(fagsakId: UUID): String = fagsakRepository.finnAktivIdent(fagsakId)

    fun hentAktiveIdenter(fagsakId: Set<UUID>): Map<UUID, String> {
        if (fagsakId.isEmpty()) return emptyMap()

        val aktiveIdenter = fagsakRepository.finnAktivIdenter(fagsakId)
        feilHvis(!aktiveIdenter.map { it.first }.containsAll(fagsakId)) {
            "Finner ikke ident til fagsaker ${aktiveIdenter.map { it.first }.filterNot(fagsakId::contains)}"
        }
        return aktiveIdenter.associateBy({ it.first }, { it.second })
    }

    private fun opprettFagsak(
        stønadstype: StønadType,
        fagsakPerson: FagsakPerson,
    ): FagsakDomain =
        fagsakRepository.insert(
            FagsakDomain(
                stønadstype = stønadstype,
                fagsakPersonId = fagsakPerson.id,
            ),
        )

    fun FagsakDomain.tilFagsakMedPerson(): Fagsak {
        val personIdenter = fagsakPersonService.hentIdenter(this.fagsakPersonId)
        return this.tilFagsakMedPerson(personIdenter)
    }
}
