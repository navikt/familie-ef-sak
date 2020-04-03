package no.nav.familie.ef.sak.api.gui.dto

import no.nav.familie.ef.sak.integration.dto.pdl.PdlAnnenForelder
import no.nav.familie.ef.sak.integration.dto.pdl.PdlBarn
import no.nav.familie.ef.sak.integration.dto.pdl.PdlSøker
import no.nav.familie.kontrakter.ef.søknad.Søknad

class GuiSak(val søker: PdlSøker,
             val barn: List<PdlBarn>,
             val annenForelder: PdlAnnenForelder,
             val søknad: Søknad)