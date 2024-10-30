package no.nav.familie.ef.sak.vedtak.uttrekk

import no.nav.familie.ef.sak.AuditLoggerEvent
import no.nav.familie.ef.sak.fagsak.FagsakService
import no.nav.familie.ef.sak.infrastruktur.sikkerhet.TilgangService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.PersonService
import no.nav.familie.ef.sak.opplysninger.personopplysninger.arbeidssøker.ArbeidssøkerClient
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.Adressebeskyttelse
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.PdlPersonKort
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.gjeldende
import no.nav.familie.ef.sak.opplysninger.personopplysninger.pdl.visningsnavn
import no.nav.familie.ef.sak.repository.findByIdOrThrow
import no.nav.familie.ef.sak.vedtak.domain.AktivitetType
import no.nav.familie.ef.sak.vedtak.domain.Vedtaksperiode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

@Service
class UttrekkArbeidssøkerService(
    private val tilgangService: TilgangService,
    private val uttrekkArbeidssøkerRepository: UttrekkArbeidssøkerRepository,
    private val fagsakService: FagsakService,
    private val personService: PersonService,
    private val arbeidssøkerClient: ArbeidssøkerClient,
) {
    fun forrigeMåned(): () -> YearMonth = { YearMonth.now().minusMonths(1) }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprettUttrekkArbeidssøkere(
        årMåned: YearMonth,
        fagsakId: UUID,
        behandlingIdForVedtak: UUID,
        personIdent: String,
    ) {
        val registrertSomArbeidssøker = erRegistrertSomArbeidssøker(personIdent, årMåned)
        uttrekkArbeidssøkerRepository.insert(
            UttrekkArbeidssøkere(
                fagsakId = fagsakId,
                vedtakId = behandlingIdForVedtak,
                årMåned = årMåned,
                registrertArbeidssøker = registrertSomArbeidssøker,
            ),
        )
    }

    fun hentUttrekkArbeidssøkere(
        årMåned: YearMonth = forrigeMåned().invoke(),
        visKontrollerte: Boolean = false,
    ): UttrekkArbeidssøkereDto {
        tilgangService.validerHarSaksbehandlerrolle()
        val arbeidssøkere = uttrekkArbeidssøkerRepository.findAllByÅrMånedAndRegistrertArbeidssøkerIsFalse(årMåned)
        val filtrerteArbeidsssøkere = mapTilDtoOgFiltrer(arbeidssøkere)

        val totaltAntallUkontrollerte = arbeidssøkere.count { !it.kontrollert }
        val antallKontrollert = filtrerteArbeidsssøkere.count { it.kontrollert }
        val antallManglerKontrollUtenTilgang = totaltAntallUkontrollerte - (filtrerteArbeidsssøkere.size - antallKontrollert)

        val filtrerteKontrollert =
            if (visKontrollerte) filtrerteArbeidsssøkere else filtrerteArbeidsssøkere.filter { !it.kontrollert }

        return UttrekkArbeidssøkereDto(
            årMåned = årMåned,
            antallTotalt = filtrerteArbeidsssøkere.size,
            antallKontrollert = antallKontrollert,
            antallManglerKontrollUtenTilgang = antallManglerKontrollUtenTilgang,
            arbeidssøkere = filtrerteKontrollert,
        )
    }

    fun settKontrollert(
        id: UUID,
        kontrollert: Boolean,
    ): UttrekkArbeidssøkerDto {
        tilgangService.validerHarSaksbehandlerrolle()
        val uttrekkArbeidssøkere = uttrekkArbeidssøkerRepository.findByIdOrThrow(id)
        tilgangService.validerTilgangTilFagsak(uttrekkArbeidssøkere.fagsakId, AuditLoggerEvent.UPDATE)

        val oppdatertArbeidssøker =
            if (uttrekkArbeidssøkere.kontrollert == kontrollert) {
                uttrekkArbeidssøkere
            } else {
                uttrekkArbeidssøkerRepository.update(uttrekkArbeidssøkere.medKontrollert(kontrollert = kontrollert))
            }
        return tilDtoMedAdressebeskyttelse(oppdatertArbeidssøker, hentPersondataTilFagsak(listOf(oppdatertArbeidssøker))).first
    }

    fun hentArbeidssøkereForUttrekk(årMåned: YearMonth): List<VedtaksperioderForUttrekk> {
        val startdato = årMåned.atDay(1)
        val sluttdato = årMåned.atEndOfMonth()
        val arbeidssøkere =
            uttrekkArbeidssøkerRepository.hentVedtaksperioderForSisteFerdigstilteBehandlinger(startdato, sluttdato)
        return arbeidssøkere.filter { harPeriodeSomArbeidssøker(it, startdato, sluttdato) }
    }

    fun uttrekkFinnes(
        årMåned: YearMonth,
        fagsakId: UUID,
    ): Boolean = uttrekkArbeidssøkerRepository.existsByÅrMånedAndFagsakId(årMåned, fagsakId)

    private fun erRegistrertSomArbeidssøker(
        personIdent: String,
        årMåned: YearMonth,
    ): Boolean {
        val sisteIMåneden = årMåned.atEndOfMonth()
        val perioder =
            arbeidssøkerClient
                .hentPerioder(
                    personIdent,
                    sisteIMåneden,
                    sisteIMåneden,
                ).perioder
        return perioder.any { it.fraOgMedDato <= sisteIMåneden && (it.tilOgMedDato == null || it.tilOgMedDato >= sisteIMåneden) }
    }

    /**
     * Filtrerer vekk personer som man ikke har tilgang til
     */
    private fun mapTilDtoOgFiltrer(arbeidsssøkere: List<UttrekkArbeidssøkere>): List<UttrekkArbeidssøkerDto> {
        if (arbeidsssøkere.isEmpty()) return emptyList()
        val arbeidsssøkereMedAdresseBeskyttelse = tilDtoMedAdressebeskyttelse(arbeidsssøkere)
        return tilgangService.filtrerUtFortroligDataForRolle(arbeidsssøkereMedAdresseBeskyttelse) { it.second }.map { it.first }
    }

    private fun tilDtoMedAdressebeskyttelse(arbeidsssøkere: List<UttrekkArbeidssøkere>): List<Pair<UttrekkArbeidssøkerDto, Adressebeskyttelse?>> {
        val persondataPåFagsak = hentPersondataTilFagsak(arbeidsssøkere)
        return arbeidsssøkere.map { tilDtoMedAdressebeskyttelse(it, persondataPåFagsak) }
    }

    private fun tilDtoMedAdressebeskyttelse(
        it: UttrekkArbeidssøkere,
        persondataPåFagsak: Map<UUID, Persondata>,
    ): Pair<UttrekkArbeidssøkerDto, Adressebeskyttelse?> {
        val persondata = persondataPåFagsak[it.fagsakId] ?: error("Finner ikke data til fagsak=${it.fagsakId}")
        val pdlPersonKort = persondata.pdlPersonKort
        val adressebeskyttelse = pdlPersonKort.adressebeskyttelse.gjeldende()
        val dto =
            it.tilDto(
                personIdent = persondata.personIdent,
                navn = pdlPersonKort.navn.gjeldende().visningsnavn(),
                adressebeskyttelse = adressebeskyttelse,
            )
        return dto to adressebeskyttelse
    }

    private fun hentPersondataTilFagsak(arbeidsssøkere: List<UttrekkArbeidssøkere>): Map<UUID, Persondata> {
        val personIdentPåFagsak = fagsakService.hentAktiveIdenter(arbeidsssøkere.map { it.fagsakId }.toSet())
        val personKortPåPersonIdent = personService.hentPersonKortBolk(personIdentPåFagsak.values.toList())

        return personIdentPåFagsak.entries.associateBy({ it.key }) {
            Persondata(it.value, personKortPåPersonIdent[it.value] ?: error("Finner ikke data til ident=${it.value}"))
        }
    }

    private fun harPeriodeSomArbeidssøker(
        it: VedtaksperioderForUttrekk,
        startdato: LocalDate,
        sluttdato: LocalDate,
    ) = it.perioder.perioder.any {
        it.datoFra <= startdato &&
            it.datoTil >= sluttdato &&
            erArbeidssøker(it)
    }

    private fun erArbeidssøker(it: Vedtaksperiode) =
        (
            it.aktivitet == AktivitetType.FORSØRGER_REELL_ARBEIDSSØKER ||
                it.aktivitet == AktivitetType.FORLENGELSE_STØNAD_PÅVENTE_ARBEID_REELL_ARBEIDSSØKER
        )

    private data class Persondata(
        val personIdent: String,
        val pdlPersonKort: PdlPersonKort,
    )
}
