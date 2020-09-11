package no.nav.familie.ef.sak.api.gui.dto

import no.nav.familie.kontrakter.ef.søknad.Dokument
import java.time.LocalDateTime

data class DeltBosted(val avtaleOmDeltBosted: Boolean?,
                      val begrunnelse: List<Dokument>?,
                      val fradatoKontrakt: LocalDateTime?,
                      val tilDatoKontrakt: LocalDateTime?)
