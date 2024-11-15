package no.nav.familie.ef.sak.mapper

import no.nav.familie.ef.sak.opplysninger.søknad.domain.Aksjeselskap
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Aktivitet
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidsgiver
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidssituasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Arbeidssøker
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Dokumentasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.GjelderDeg
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Selvstendig
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Situasjon
import no.nav.familie.ef.sak.opplysninger.søknad.domain.SøknadBarn
import no.nav.familie.ef.sak.opplysninger.søknad.domain.TidligereUtdanning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.UnderUtdanning
import no.nav.familie.ef.sak.opplysninger.søknad.domain.Virksomhet
import no.nav.familie.ef.sak.opplysninger.søknad.mapper.AktivitetMapper
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

internal class AktivitetMapperTest {
    @Test
    internal fun `sjekker at mappet aktivitet har fått satt alle verdier`() {
        val dto = AktivitetMapper.tilDto(aktivitet(), situasjon(), barn(), LocalDate.now())
        sjekkAtAlleVerdierErSatt(dto)
    }

    private fun situasjon(): Situasjon =
        Situasjon(
            arbeidskontrakt = dokumentliste(),
            barnMedSærligeBehov = dokumentliste(),
            barnsSykdom = dokumentliste(),
            gjelderDetteDeg =
                GjelderDeg(
                    listOf(
                        " Jeg har søkt om barnepass, men ikke fått plass enda; " +
                            "Jeg har barn som har behov for særlig tilsyn på grunn av fysiske, " +
                            "psykiske eller store sosiale problemer",
                    ),
                ),
            manglendeBarnepass = dokumentliste(),
            oppsigelseDokumentasjon = dokumentliste(),
            oppsigelseReduksjonTidspunkt = LocalDate.now(),
            oppsigelseReduksjonÅrsak = "Årsak",
            oppstartNyJobb = LocalDate.now(),
            oppstartUtdanning = null,
            sagtOppEllerRedusertStilling = "Ja",
            sykdom = dokumentliste(),
            utdanningstilbud = dokumentliste(),
        )

    private fun dokumentliste() = Dokumentasjon(harSendtInnTidligere = false, dokumenter = emptyList())

    private fun aktivitet(): Aktivitet =
        Aktivitet(
            arbeidsforhold = arbeidsforhold(),
            arbeidssøker = arbeidssøker(),
            hvordanErArbeidssituasjonen = Arbeidssituasjon(listOf("Jeg er ansatt i eget aksjeselskap (AS)")),
            firmaer = selvstendig(),
            underUtdanning = underUtdanning(),
            aksjeselskap = aksjeselskap(),
            virksomhet = virksomhet(),
            tidligereUtdanninger = tidligereUtdanning(),
            erIArbeid = "JA",
        )

    private fun aksjeselskap(): Set<Aksjeselskap> = setOf(Aksjeselskap(navn = "navn", arbeidsmengde = 30))

    private fun virksomhet() =
        Virksomhet(
            virksomhetsbeskrivelse = "virksomhet",
            dokumentasjon = dokumentliste(),
        )

    private fun underUtdanning(): UnderUtdanning =
        UnderUtdanning(
            heltidEllerDeltid = "Heltid",
            hvaErMåletMedUtdanningen = "Kose seg",
            hvorMyeSkalDuStudere = 50,
            offentligEllerPrivat = "privat",
            skoleUtdanningssted = "Oslo",
            utdanningEtterGrunnskolen = true,
            fra = LocalDate.now(),
            til = LocalDate.now().plusYears(1),
            linjeKursGrad = "Kurs",
            semesteravgift = 30000,
            studieavgift = 0,
            eksamensgebyr = 1500,
        )

    private fun tidligereUtdanning(): Set<TidligereUtdanning> =
        setOf(
            TidligereUtdanning(
                linjeKursGrad = "linje",
                fra = YearMonth.now().minusYears(5),
                til = YearMonth.now().minusYears(1),
            ),
        )

    private fun selvstendig(): Set<Selvstendig> =
        setOf(
            Selvstendig(
                arbeidsmengde = 50,
                etableringsdato = LocalDate.now(),
                firmanavn = "SelvstendigFirmanavn",
                hvordanSerArbeidsukenUt = "fin",
                organisasjonsnummer = "987654321",
                overskudd = 30000,
            ),
        )

    private fun arbeidsforhold(): Set<Arbeidsgiver> =
        (
            setOf(
                Arbeidsgiver(
                    arbeidsgivernavn = "Arbeidsgivernavn",
                    fastEllerMidlertidig = "Fast",
                    harSluttdato = true,
                    sluttdato = LocalDate.now(),
                    arbeidsmengde = 50,
                ),
            )
        )

    private fun arbeidssøker(): Arbeidssøker =
        Arbeidssøker(
            hvorØnskerDuArbeid = "På dagen",
            kanDuBegynneInnenEnUke = true,
            kanDuSkaffeBarnepassInnenEnUke = true,
            registrertSomArbeidssøkerNav = true,
            villigTilÅTaImotTilbudOmArbeid = true,
            ønskerDuMinst50ProsentStilling = true,
            ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon = dokumentliste(),
        )

    private fun barn(): Set<SøknadBarn> =
        setOf(
            SøknadBarn(
                id = UUID.randomUUID(),
                navn = "navn",
                fødselsnummer = null,
                harSkalHaSammeAdresse = false,
                skalBoHosSøker = null,
                ikkeRegistrertPåSøkersAdresseBeskrivelse = null,
                erBarnetFødt = false,
                fødselTermindato = LocalDate.now().plusMonths(1),
                terminbekreftelse = dokumentliste(),
                annenForelder = null,
                samvær = null,
                skalHaBarnepass = false,
                særligeTilsynsbehov = "Ja",
                årsakBarnepass = null,
                barnepassordninger = emptySet(),
                lagtTilManuelt = true,
            ),
        )
}
