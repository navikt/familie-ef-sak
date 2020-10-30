package no.nav.familie.ef.sak.util

fun <K,V> Map<K,List<V>>.hasList(key: K) : Boolean = this.get(key)?.size ?: 0!=0

fun <K,V> mergeMultiMap (vararg maps: Map<K,List<V>>): Map<K,List<V>> =
        maps.flatMap { it.asSequence() }
                .groupBy({ it.key }, { it.value })
                .mapValues { (_,lister)->lister.flatten() }

