package no.nav.familie.ef.sak.api.gui.dto

import java.time.LocalDate

data class DeltBosted(val avtaleOmDeltBosted: Boolean,
                      val datoForAvtale: LocalDate?,
                      val begrunnelse: LocalDate?,
                      val fradatoKontrakt: LocalDate?,
                      val tilDatoKontrakt: LocalDate?,
                      val fradatoDeltBosted: LocalDate?)
