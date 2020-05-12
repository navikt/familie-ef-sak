package no.nav.familie.ef.sak.vurdering.medlemskap

//import io.mockk.mockk
import io.mockk.mockk
import no.nav.familie.ef.sak.integration.dto.pdl.*
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap.pdlPerson
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap.pdlSøker
import no.nav.familie.ef.sak.no.nav.familie.ef.sak.vurdering.medlemskap.søknad
import no.nav.familie.kontrakter.ef.søknad.Medlemskapsdetaljer
import no.nav.familie.kontrakter.ef.søknad.Stønadsstart
import no.nav.familie.kontrakter.ef.søknad.Søknadsfelt
import no.nav.familie.kontrakter.felles.medlemskap.Medlemskapsinfo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month

internal class MedlemskapRegelsettTest {

    @Test
    fun `vurderingMedlemskapSøker returnerer Nei hvis søker er registrert som ikke medlem`() {
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val pdlPerson: PdlPerson = pdlPerson()
        val pdlSøker = pdlSøker(foedsel = listOf(Foedsel(null, LocalDate.of(1999, 4, 5), null, null, null)),
                                folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("BOSATT", "bo")),
                                statsborgerskap = listOf(Statsborgerskap("SE", LocalDate.of(2002, 2, 2), null)))
        val søknad = søknad(medlemskapsdetaljer = Søknadsfelt("", Medlemskapsdetaljer(Søknadsfelt("", true), mockk(), mockk())),
                            stønadsstart = Søknadsfelt("", Stønadsstart(Søknadsfelt("", Month.AUGUST), Søknadsfelt("", 2014))))
        val medlemskapshistorikk = Medlemskapshistorikk(pdlPerson, medlemskapsinfoUtenUnntak)
        val medlemskapsgrunnlag = Medlemskapsgrunnlag(pdlSøker, medlemskapshistorikk, søknad)

        val evaluering = MedlemskapRegelsett().vurderingMedlemskapSøker.evaluer(medlemskapsgrunnlag)

        evaluering.resultat

    }

    @Test
    fun `vurderingMedlemskapSøker returnerer Ja hvis søker er registrert som medlem`() {
        val medlemskapsinfoUtenUnntak = Medlemskapsinfo("3213213", emptyList(), emptyList(), emptyList())
        val pdlSøker = pdlSøker(foedsel = listOf(Foedsel(null, LocalDate.of(1999, 4, 5), null, null, null)),
                                folkeregisterpersonstatus = listOf(Folkeregisterpersonstatus("BOSATT", "bo")),
                                bostedsadresse = listOf(Bostedsadresse(null,
                                                                       null,
                                                                       Folkeregistermetadata(LocalDateTime.of(1999, 4, 5, 0, 0),
                                                                                             null),
                                                                       null,
                                                                       null)),
                                statsborgerskap = listOf(Statsborgerskap("SE", LocalDate.of(2002, 2, 2), null)))
        val søknad = søknad(medlemskapsdetaljer = Søknadsfelt("", Medlemskapsdetaljer(Søknadsfelt("", false), mockk(), mockk())),
                            stønadsstart = Søknadsfelt("", Stønadsstart(Søknadsfelt("", Month.AUGUST), Søknadsfelt("", 2014))))
        val medlemskapshistorikk = Medlemskapshistorikk(pdlSøker.person!!, medlemskapsinfoUtenUnntak)
        val medlemskapsgrunnlag = Medlemskapsgrunnlag(pdlSøker, medlemskapshistorikk, søknad)

        val evaluering = MedlemskapRegelsett().vurderingMedlemskapSøker.evaluer(medlemskapsgrunnlag)

        evaluering.resultat

    }
}
