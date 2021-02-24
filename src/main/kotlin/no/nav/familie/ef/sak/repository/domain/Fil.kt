package no.nav.familie.ef.sak.repository.domain

data class Fil(val bytes: ByteArray) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return (bytes.contentEquals((other as Fil).bytes))
    }

    override fun hashCode(): Int {
        return bytes.contentHashCode()
    }
}