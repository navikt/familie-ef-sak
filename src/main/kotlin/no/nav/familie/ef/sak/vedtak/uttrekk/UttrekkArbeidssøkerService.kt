package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.exception.feilHvis
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class UttrekkArbeidssøkerService(
        private val tilgangService: TilgangService,
        private val uttrekkArbeidssøkerRepository: UttrekkArbeidssøkerRepository,
        private val fagsakService: FagsakService,
        private val personService: PersonService
) {

    fun forrigeMåned(): () -> YearMonth = { YearMonth.now().minusMonths(1) }

    @Transactional
    fun opprettUttrekkArbeidssøkere(årMåned: YearMonth = forrigeMåned().invoke()) {
        hentArbeidssøkereForUttrekk(årMåned).forEach {
            uttrekkArbeidssøkerRepository.insert(UttrekkArbeidssøkere(fagsakId = it.fagsakId,
                                                                      vedtakId = it.behandlingIdForVedtak,
                                                                      årMåned = årMåned))
        }
    }

    fun settKontrollert(id: UUID, kontrollert: Boolean) {
        val uttrekkArbeidssøkere = uttrekkArbeidssøkerRepository.findByIdOrThrow(id)
        if (uttrekkArbeidssøkere.kontrollert == kontrollert) return

        tilgangService.validerTilgangTilFagsak(uttrekkArbeidssøkere.fagsakId)
        uttrekkArbeidssøkerRepository.update(uttrekkArbeidssøkere.medKontrollert(kontrollert = kontrollert))
    }

    fun hentUttrekkArbeidssøkere(årMåned: YearMonth = forrigeMåned().invoke(),
                                 side: Int = 1,
                                 visKontrollerte: Boolean = true): UttrekkArbeidssøkereDto {
        val antallKontrollert = uttrekkArbeidssøkerRepository.countByÅrMånedAndKontrollertIsTrue(årMåned)
        val paginerteArbeidssøkere = hentPaginerteArbeidssøkere(årMåned, side, visKontrollerte)
        val arbeidsssøkere = paginerteArbeidssøkere.content
        val filtrerteArbeidsssøkere = mapTilDtoOgFiltrer(arbeidsssøkere)
        return UttrekkArbeidssøkereDto(årMåned = årMåned,
                                       antallTotalt = paginerteArbeidssøkere.totalElements.toInt(),
                                       antallKontrollert = antallKontrollert,
                                       arbeidssøkere = filtrerteArbeidsssøkere,
                                       antallManglerTilgang = arbeidsssøkere.size - filtrerteArbeidsssøkere.size)
    }

    /**
     * Filtrerer vekk personer som man ikke har tilgang til
     */
    private fun mapTilDtoOgFiltrer(arbeidsssøkere: List<UttrekkArbeidssøkere>): List<UttrekkArbeidsssøkerDto> {
        if (arbeidsssøkere.isEmpty()) return emptyList()
        val arbeidsssøkereMedAdresseBeskyttelse = tilDtoMedAdressebeskyttelse(arbeidsssøkere)
        return tilgangService.filtrerUtFortroligDataForRolle(arbeidsssøkereMedAdresseBeskyttelse) { it.second }.map { it.first }
    }

    private fun tilDtoMedAdressebeskyttelse(arbeidsssøkere: List<UttrekkArbeidssøkere>)
            : List<Pair<UttrekkArbeidsssøkerDto, Adressebeskyttelse?>> {
        val persondataPåFagsak = hentPersondataTilFagsak(arbeidsssøkere)
        return arbeidsssøkere.map {
            val persondata = persondataPåFagsak[it.fagsakId] ?: error("Finner ikke data til fagsak=${it.fagsakId}")
            val pdlPersonKort = persondata.pdlPersonKort
            val dto = it.tilDto(personIdent = persondata.personIdent, navn = pdlPersonKort.navn.gjeldende().visningsnavn())
            dto to pdlPersonKort.adressebeskyttelse.gjeldende()
        }
    }

    private fun hentPersondataTilFagsak(arbeidsssøkere: List<UttrekkArbeidssøkere>): Map<UUID, Persondata> {
        val personIdentPåFagsak = fagsakService.hentAktiveIdenter(arbeidsssøkere.map { it.fagsakId }.toSet())
        val personKortPåPersonIdent = personService.hentPdlPersonKort(personIdentPåFagsak.values.toList())

        return personIdentPåFagsak.entries.associateBy({ it.key }) {
            Persondata(it.value, personKortPåPersonIdent[it.value] ?: error("Finner ikke data til ident=${it.value}"))
        }
    }

    private fun hentPaginerteArbeidssøkere(årMåned: YearMonth,
                                           side: Int,
                                           visKontrollerte: Boolean): Page<UttrekkArbeidssøkere> {
        feilHvis(side < 1) { "Side må være større enn 0, men var side=$side" }

        val pageable = PageRequest.of(side - 1, 20, Sort.by("id"))
        return if (visKontrollerte) {
            uttrekkArbeidssøkerRepository.findAllByÅrMåned(årMåned, pageable)
        } else {
            uttrekkArbeidssøkerRepository.findAllByÅrMånedAndKontrollertIsFalse(årMåned, pageable)
        }
    }

    fun hentArbeidssøkereForUttrekk(årMåned: YearMonth = forrigeMåned().invoke()): List<VedtaksperioderForUttrekk> {
        val startdato = årMåned.atDay(1)
        val sluttdato = årMåned.atEndOfMonth()
        val arbeidssøkere =
                uttrekkArbeidssøkerRepository.hentVedtaksperioderForSisteFerdigstilteBehandlinger(startdato, sluttdato)
        return arbeidssøkere.filter { harPeriodeSomArbeidssøker(it, startdato, sluttdato) }
    }

    private fun harPeriodeSomArbeidssøker(it: VedtaksperioderForUttrekk,
                                          startdato: LocalDate,
                                          sluttdato: LocalDate) =
            it.perioder.perioder.any {
                it.datoFra <= startdato
                && it.datoTil >= sluttdato
                && erArbeidssøker(it)
            }

    private fun erArbeidssøker(it: Vedtaksperiode) =
            (it.aktivitet == AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER
             || it.aktivitet == AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER)

    private data class Persondata(val personIdent: String, val pdlPersonKort: PdlPersonKort)
}
