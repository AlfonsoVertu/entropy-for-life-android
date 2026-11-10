package it.bibliotecadialessandria.ricerca

/** Tutto quello che cambia da un canale all'altro, per Entropy for Life (beta, 10/11/2026). */
object Marca {
    const val NOME = "Entropy for Life"
    const val MOTTO = "I video di biologia di Giacomo Moro Mauretto, a portata di mano"
    /** Il canale YouTube e il suo id senza «UC». */
    const val CANALE = "https://www.youtube.com/@EntropyforLife"
    const val CANALE_ID = "QPnCKNfzKn4OmPrx1KDWvg"
    const val SITO = "https://entropyforlife.it/"
    /** La cartella su mondo.workingwithweb.eu con l'APK, versione.json e catalogo.json. */
    const val MONDO = "entropy-for-life"
    const val CERCA_SOTTO = "Per tema, rubrica e tipo"
    const val CERCO = "Cerco fra i video..."
    const val NON_RISPONDE = "Non riesco a leggere il catalogo dei video. Riprova."
    const val NOTA_CERCA = "Ricerca fra tutti i video pubblici del canale · app non ufficiale"
    const val PIEDE = "App non ufficiale · i video sono di Entropy for Life (Giacomo Moro Mauretto)"
    const val CREDITI = "Video e contenuti sono di Giacomo Moro Mauretto, Entropy for Life. Questa app li " +
        "rende solo più comodi da cercare; non è affiliata al canale. I temi sono assegnati in automatico " +
        "dai titoli e possono sbagliare."
    const val AVVISI_NOTA = "Un avviso quando Entropy for Life pubblica un video, uno short o una diretta"

    /** I link del canale, in quest'ordine in home (dopo le sezioni dell'app) e nei crediti. */
    val LINK = listOf(
        Collegamento(R.drawable.ic_canale, "YouTube", "Il canale", CANALE),
        Collegamento(R.drawable.ic_instagram, "Instagram", "@entropyforlife", "https://www.instagram.com/entropyforlife"),
        Collegamento(R.drawable.ic_community, "Facebook", "La pagina", "https://www.facebook.com/entropyforlife"),
        Collegamento(R.drawable.ic_post, "Telegram", "Il canale Telegram", "https://t.me/entropyforlife"),
        Collegamento(R.drawable.ic_sito, "Il sito", "Eventi e articoli", SITO),
        Collegamento(R.drawable.ic_dona, "Sostieni Giacomo", "Tipeee", "https://it.tipeee.com/entropyforlife"),
    )
}
