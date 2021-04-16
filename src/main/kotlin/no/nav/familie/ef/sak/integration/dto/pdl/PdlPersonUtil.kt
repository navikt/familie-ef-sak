package no.nav.familie.ef.sak.integration.dto.pdl


fun Navn.visningsnavn(): String {
    return if (mellomnavn == null) "$fornavn $etternavn"
    else "$fornavn $mellomnavn $etternavn"
}

fun List<Navn>.gjeldende(): Navn = this.find { !it.metadata.historisk } ?: this.first()
fun List<Bostedsadresse>.gjeldende(): Bostedsadresse? = this.find { !it.metadata.historisk }
fun List<Oppholdsadresse>.gjeldende(): Oppholdsadresse? = this.find { !it.metadata.historisk }
fun List<Sivilstand>.gjeldende(): Sivilstand = this.find { !it.metadata.historisk } ?: this.first()
fun List<Fødsel>.gjeldende(): Fødsel? = this.find { !it.metadata.historisk } ?: this.firstOrNull()
fun List<DeltBosted>.gjeldende(): DeltBosted? = this.find { !it.metadata.historisk }
fun List<Folkeregisterpersonstatus>.gjeldende(): Folkeregisterpersonstatus? = this.find { !it.metadata.historisk }
fun List<Dødsfall>.gjeldende(): Dødsfall? = this.firstOrNull()
fun List<Adressebeskyttelse>.gjeldende(): Adressebeskyttelse? = this.find { !it.metadata.historisk }
fun List<Folkeregisteridentifikator>.gjeldende(): Folkeregisteridentifikator = this.single()