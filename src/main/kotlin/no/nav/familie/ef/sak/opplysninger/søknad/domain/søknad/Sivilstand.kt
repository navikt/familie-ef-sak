package no.nav.familie.ef.sak.opplysninger.søknad.domain.søknad

import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate

data class Sivilstand(val erUformeltGift: Boolean? = null,
                      val erUformeltGiftDokumentasjon: Dokumentasjon? = null,
                      val erUformeltSeparertEllerSkilt: Boolean? = null,
                      val erUformeltSeparertEllerSkiltDokumentasjon: Dokumentasjon? = null,
                      @Column("sokt_om_skilsmisse_separasjon")
                      val søktOmSkilsmisseSeparasjon: Boolean? = null,
                      @Column("dato_sokt_separasjon")
                      val datoSøktSeparasjon: LocalDate? = null,
                      val separasjonsbekreftelse: Dokumentasjon? = null,
                      @Column("arsak_enslig")
                      val årsakEnslig: String? = null,
                      val samlivsbruddsdokumentasjon: Dokumentasjon? = null,
                      val samlivsbruddsdato: LocalDate? = null,
                      val fraflytningsdato: LocalDate? = null,
                      @Column("endring_samversordning_dato")
                      val endringSamværsordningDato: LocalDate? = null,
                      @Embedded(onEmpty = Embedded.OnEmpty.USE_NULL, prefix = "tidligere_samboer_")
                      val tidligereSamboer: PersonMinimum? = null)
