package no.nav.familie.ef.sak.opplysninger.personopplysninger.endringer

import no.nav.familie.ef.sak.behandling.Saksbehandling
import no.nav.familie.ef.sak.felles.util.norskFormat
import no.nav.familie.ef.sak.opplysninger.personopplysninger.domene.GrunnlagsdataMedMetadata
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.AnnenForelderMinimumDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.BarnDto
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.Folkeregisterpersonstatus
import no.nav.familie.ef.sak.opplysninger.personopplysninger.dto.PersonopplysningerDto
import no.nav.familie.kontrakter.felles.ef.StønadType
import java.time.LocalDate

private typealias PersonendringDetaljerFn<T> = (T, T) -> EndringFelt?

object UtledEndringerUtil {
    fun finnEndringer(
        tidligerePersonopplysninger: PersonopplysningerDto,
        nyePersonopplysninger: PersonopplysningerDto,
        behandling: Saksbehandling,
        tidligereGrunnlagsdata: GrunnlagsdataMedMetadata,
        nyGrunnlagsdata: GrunnlagsdataMedMetadata,
    ): Endringer {
        val endringer = finnEndringerIPersonopplysninger(tidligerePersonopplysninger, nyePersonopplysninger)
        return if (behandling.stønadstype == StønadType.OVERGANGSSTØNAD) {
            endringer.copy(perioder = finnEndringerIPerioder(tidligereGrunnlagsdata, nyGrunnlagsdata))
        } else {
            endringer
        }
    }

    fun finnEndringerIPersonopplysninger(
        tidligere: PersonopplysningerDto,
        nye: PersonopplysningerDto,
    ): Endringer =
        Endringer(
            folkeregisterpersonstatus =
                utledEndringer(
                    tidligere.folkeregisterpersonstatus,
                    nye.folkeregisterpersonstatus,
                ),
            fødselsdato = utledEndringer(tidligere.fødselsdato, nye.fødselsdato),
            dødsdato = utledEndringer(tidligere.dødsdato, nye.dødsdato),
            statsborgerskap = utledEndringerUtenDetaljer(tidligere.statsborgerskap, nye.statsborgerskap),
            sivilstand = utledEndringerUtenDetaljer(tidligere.sivilstand, nye.sivilstand),
            adresse = utledEndringerUtenDetaljer(tidligere.adresse, nye.adresse),
            fullmakt = utledEndringerUtenDetaljer(tidligere.fullmakt, nye.fullmakt),
            barn = utledEndringerBarn(tidligere.barn, nye.barn),
            annenForelder = utledEndringerAndreForelder(tidligere.barn, nye.barn),
            innflyttingTilNorge = utledEndringerUtenDetaljer(tidligere.innflyttingTilNorge, nye.innflyttingTilNorge),
            utflyttingFraNorge = utledEndringerUtenDetaljer(tidligere.utflyttingFraNorge, nye.utflyttingFraNorge),
            oppholdstillatelse = utledEndringerUtenDetaljer(tidligere.oppholdstillatelse, nye.oppholdstillatelse),
            vergemål = utledEndringerUtenDetaljer(tidligere.vergemål, nye.vergemål),
        )

    fun finnEndringerIPerioder(
        tidligereGrunnlagsdata: GrunnlagsdataMedMetadata,
        nyGrunnlagsdata: GrunnlagsdataMedMetadata,
    ) = EndringUtenDetaljer(tidligereGrunnlagsdata.grunnlagsdata.tidligereVedtaksperioder?.sak != nyGrunnlagsdata.grunnlagsdata.tidligereVedtaksperioder?.sak)

    private fun <T> utledEndringerUtenDetaljer(
        tidligere: T,
        nye: T,
    ) = EndringUtenDetaljer(tidligere != nye)

    private fun <T> utledEndringer(
        tidligere: T,
        ny: T,
    ): Endring<EndringVerdi> =
        if (tidligere != ny) {
            Endring(true, EndringVerdi(format(tidligere), format(ny)))
        } else {
            Endring(false)
        }

    private val barnEndringer: List<PersonendringDetaljerFn<BarnDto>> =
        listOf(
            formatterEndring(BarnDto::borHosSøker, "Bor hos søker"),
            formatterEndring(BarnDto::dødsdato, "Dødsdato"),
            formatterEndring(BarnDto::fødselsdato, "Fødselsdato"),
            formatterEndring({ it.annenForelder?.personIdent }, "Annen forelder"),
            formatterEndring(BarnDto::harDeltBostedNå, "Delt bosted"),
            formatterEndring(BarnDto::deltBosted, "Delt bosted perioder"),
            // TODO adresse ?? Er den interessant å vise som endret hvis man ikke har endring i borHosSøker ? si eks at barnet på > 18 flytter
        )

    private val annenForelderEndringer: List<PersonendringDetaljerFn<AnnenForelderMinimumDto>> =
        listOf(
            formatterEndring(AnnenForelderMinimumDto::dødsdato, "Dødsdato"),
            formatterEndring(AnnenForelderMinimumDto::bostedsadresse, "Bostedsadresse"),
        )

    private fun utledEndringerBarn(
        tidligere: List<BarnDto>,
        nye: List<BarnDto>,
    ) = utledPersonendringer(tidligere, nye, { it.personIdent }, barnEndringer)

    private fun utledEndringerAndreForelder(
        tidligere: List<BarnDto>,
        nye: List<BarnDto>,
    ): Endring<List<Personendring>> {
        val tidligereForeldrer = tidligere.mapNotNull { it.annenForelder }.distinct()
        val nyeForeldrer = nye.mapNotNull { it.annenForelder }.distinct()
        return utledPersonendringer(tidligereForeldrer, nyeForeldrer, { it.personIdent }, annenForelderEndringer)
    }

    private fun <T> utledPersonendringer(
        tidligere: List<T>,
        nye: List<T>,
        ident: (T) -> String,
        endringer: List<PersonendringDetaljerFn<T>>,
    ): Endring<List<Personendring>> {
        val tidligerePåIdent = tidligere.associateBy { ident(it) }
        val nyePåIdent = nye.associateBy { ident(it) }

        val endringerPåNye =
            nyePåIdent.mapNotNull { (ident, nyPerson) ->
                val tidligerePerson = tidligerePåIdent[ident]
                if (tidligerePerson != null) {
                    endringer
                        .mapNotNull { it(tidligerePerson, nyPerson) }
                        .takeIf { it.isNotEmpty() }
                        ?.let { Personendring(ident, it) }
                } else {
                    Personendring(ident, ny = true)
                }
            }
        val fjernede =
            tidligerePåIdent.keys
                .filterNot { nyePåIdent.containsKey(it) }
                .map { Personendring(it, fjernet = true) }
        val alleEndringer = fjernede + endringerPåNye
        return Endring(alleEndringer.isNotEmpty(), alleEndringer)
    }

    /**
     * @return en funksjon som tar inn tidligere og ny person
     * Funksjonen returnerer en verdi hvis det er en endring, og null hvis ikke
     */
    private fun <T, VERDI : Any> formatterEndring(
        verdi: (T) -> VERDI?,
        felt: String,
        harEndring: (VERDI?, VERDI?) -> Boolean = { tidligere, ny -> tidligere != ny },
    ): PersonendringDetaljerFn<T> =
        { tidligere: T, ny: T ->
            val tidligereVerdi = verdi(tidligere)
            val nyVerdi = verdi(ny)
            if (harEndring(tidligereVerdi, nyVerdi)) {
                EndringFelt(felt, format(tidligereVerdi), format(nyVerdi))
            } else {
                null
            }
        }

    private fun format(verdi: Any?): String =
        when (verdi) {
            null -> "Mangler verdi"
            is Boolean -> if (verdi) "Ja" else "Nei"
            is LocalDate -> verdi.norskFormat()
            is Folkeregisterpersonstatus -> verdi.visningsnavn
            is List<*> -> ""
            else -> "$verdi"
        }
}
