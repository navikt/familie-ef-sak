package no.nav.familie.ef.sak.brev

data class BrevRequest(
    val flettefelter: Flettefelter,
)

data class Flettefelter(
    val navn: List<String>? = emptyList(),
    val fodselsnummer: List<String>? = emptyList(),
    val forventetInntekt: List<Int>? = emptyList(),
)