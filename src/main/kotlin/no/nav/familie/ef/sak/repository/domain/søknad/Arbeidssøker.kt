package no.nav.familie.ef.sak.repository.domain.søknad

import org.springframework.data.relational.core.mapping.Column

data class Arbeidssøker(@Column("registrert_som_arbeidssoker_nav")
                        val registrertSomArbeidssøkerNav: Boolean,
                        @Column("villig_til_a_ta_imot_tilbud_om_arbeid")
                        val villigTilÅTaImotTilbudOmArbeid: Boolean,
                        val kanDuBegynneInnenEnUke: Boolean,
                        val kanDuSkaffeBarnepassInnenEnUke: Boolean?,
                        @Column("hvor_onsker_du_arbeid")
                        val hvorØnskerDuArbeid: String,
                        @Column("onsker_du_minst_50_prosent_stilling")
                        val ønskerDuMinst50ProsentStilling: Boolean,
                        @Column("ikke_villig_til_a_ta_tilbud_om_arbeid")
                        val ikkeVilligTilÅTaImotTilbudOmArbeidDokumentasjon: Dokumentasjon? = null)
