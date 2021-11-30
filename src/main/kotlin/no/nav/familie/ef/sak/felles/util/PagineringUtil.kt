package no.nav.familie.ef.sak.felles.util

object PagineringUtil {

    fun <T> paginer(list: List<T>, side: Int, antallPerSide: Int = 20): List<T> {
        require(side > 0) { "Side $side må være større enn 0" }
        require(antallPerSide > 0) { "Antall per side $antallPerSide må være større enn 0" }
        return if ((side - 1) * antallPerSide > list.size) {
            emptyList()
        } else {
            list.subList((side - 1) * antallPerSide, minOf(side * antallPerSide, list.size))
        }
    }
}