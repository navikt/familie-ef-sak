package no.nav.familie.ef.sak.service

import no.nav.familie.ef.sak.api.Feil
import no.nav.familie.ef.sak.api.dto.*
import no.nav.familie.ef.sak.integration.dto.pdl.gjeldende
import no.nav.familie.ef.sak.mapper.KjønnMapper
import no.nav.familie.ef.sak.mapper.PersonopplysningerMapper
import no.nav.familie.ef.sak.repository.FagsakRepository
import no.nav.familie.ef.sak.repository.domain.Fagsak
import no.nav.familie.ef.sak.repository.domain.FagsakPerson
import no.nav.familie.ef.sak.repository.domain.Stønadstype
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.lang.NullPointerException
import java.util.*

@Service
class FagsakService(private val fagsakRepository: FagsakRepository, private val behandlingService: BehandlingService, private val personService: PersonService) {

    fun hentEllerOpprettFagsak(personIdent: String, stønadstype: Stønadstype): FagsakDto {
        val fagsak = (fagsakRepository.findBySøkerIdent(personIdent, stønadstype)
                      ?: fagsakRepository.insert(Fagsak(stønadstype = stønadstype,
                                                        søkerIdenter = setOf(FagsakPerson(ident = personIdent)))))

        val behandlinger = behandlingService.hentBehandlinger(fagsak.id)

        return FagsakDto(id = fagsak.id,
                         personIdent = fagsak.hentAktivIdent(),
                         stønadstype = fagsak.stønadstype,
                         behandlinger = behandlinger)
    }

    fun soekPerson(personIdent: String): Søkeresultat {
        val fagsak = fagsakRepository.findBySøkerIdent(personIdent, stønadstype = Stønadstype.OVERGANGSSTØNAD) ?: throw  Feil(message = "Finner ikke fagsak for søkte personen",
                   frontendFeilmelding = "Finner ikke fagsak for søkte personen")
        val person = personService.hentSøker(fagsak.hentAktivIdent())

        return Søkeresultat(personIdent = fagsak.hentAktivIdent(),
                            kjønn = KjønnMapper.tilKjønn(person),
                            visningsnavn = NavnDto.fraNavn(person.navn.gjeldende()).visningsnavn,
                            fagsaker = listOf(fagsak.id)
        )
    }

    fun hentFagsakMedBehandlinger(fagsakId: UUID): FagsakDto =
            hentFagsak(fagsakId).tilDto(behandlinger = behandlingService.hentBehandlinger(fagsakId))

    fun hentFagsak(fagsakId: UUID): Fagsak = fagsakRepository.findByIdOrThrow(fagsakId)

    fun hentEksternId(fagsakId: UUID): Long = fagsakRepository.findByIdOrThrow(fagsakId).eksternId.id

}