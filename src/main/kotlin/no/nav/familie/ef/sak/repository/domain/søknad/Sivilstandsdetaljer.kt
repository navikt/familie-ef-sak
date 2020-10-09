package no.nav.familie.ef.sak.repository.domain.søknad

import java.time.LocalDate

data class Sivilstandsdetaljer(val erUformeltGift: Boolean? = null,
                               val erUformeltGiftDokumentasjon: Dokumentasjon? = null,
                               val erUformeltSeparertEllerSkilt: Boolean? = null,
                               val erUformeltSeparertEllerSkiltDokumentasjon: Dokumentasjon? = null,
                               val søktOmSkilsmisseSeparasjon: Boolean? = null,
                               val datoSøktSeparasjon: LocalDate? = null,
                               val separasjonsbekreftelse: Dokumentasjon? = null,
                               val årsakEnslig: String? = null,
                               val samlivsbruddsdokumentasjon: Dokumentasjon? = null,
                               val samlivsbruddsdato: LocalDate? = null,
                               val fraflytningsdato: LocalDate? = null,
                               val endringSamværsordningDato: LocalDate? = null,
                               val tidligereSamboerdetaljer: PersonMinimum? = null)
