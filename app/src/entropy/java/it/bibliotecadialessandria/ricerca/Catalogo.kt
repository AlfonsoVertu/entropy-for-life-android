package it.bibliotecadialessandria.ricerca

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.File
import java.text.Normalizer

/**
 * Il catalogo di un canale SENZA sito (10/11/2026, Entropy for Life): tutti i video in un JSON
 * (`assets/catalogo.json`, costruito da `strumenti/catalogo.py`) con titolo, descrizione, tag, temi e rubriche.
 * La ricerca la fa il telefono, senza rete, nel titolo, nella descrizione e nei tag (a scelta).
 *
 * NIENTE CHIAMATE AI NOSTRI SERVER (Alfonso, 10/11): i video nuovi l'app li prende dal feed pubblico di YouTube,
 * li classifica con le regole che viaggiano nel catalogo (`regole`: temi e rubriche a parole chiave) e li
 * aggiunge in locale (`aggiunti.json`). Il catalogo completo si rinnova solo con una versione nuova dell'app.
 */
object Catalogo {
    private const val FILE = "catalogo.json"
    private const val AGGIUNTI = "aggiunti.json"
    private const val OGNI = 3600_000L
    private const val UA = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130 Mobile Safari/537.36"

    private class Voce(val v: Video, val tipo: String, val temi: List<String>, val rubriche: List<String>,
                       val titolo: String, val descrizione: String, val tag: String)
    private class Dati(val filtri: JSONObject, val regole: JSONObject, val voci: List<Voce>)

    @Volatile private var dati: Dati? = null
    private lateinit var app: Context

    fun prepara(c: Context) {
        app = c.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { runCatching { carica(); integra(c) } }
    }

    private fun carica(): Dati {
        dati?.let { return it }
        val o = JSONObject(app.assets.open(FILE).bufferedReader().use { it.readText() })
        val aggiunti = File(app.filesDir, AGGIUNTI).takeIf { it.exists() }
            ?.let { f -> runCatching { JSONArray(f.readText()) }.getOrNull() } ?: JSONArray()
        return leggi(o, aggiunti).also { dati = it }
    }

    private fun piano(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    private fun lista(a: JSONArray?) = a?.let { t -> (0 until t.length()).map { t.getString(it) } }.orEmpty()

    private fun voce(x: JSONObject): Voce {
        val temi = lista(x.optJSONArray("temi")); val rub = lista(x.optJSONArray("rubriche")); val tag = lista(x.optJSONArray("tag"))
        val sec = x.optInt("durata")
        val data = x.optString("data").split("-").takeIf { it.size == 3 }?.let { (y, m, d) -> "$d/$m/$y" }.orEmpty()
        val v = Video(x.getString("id"), x.optString("titolo"), if (sec > 0) "${maxOf(1, sec / 60)}'" else "",
            data, x.optString("tipo"), temi.filter { it != "Altro" }.joinToString(", "), rub.joinToString(", "), "", "")
        return Voce(v, x.optString("tipo"), temi, rub, piano(v.titolo), piano(x.optString("descrizione")),
            piano(tag.joinToString(" ") + " " + temi.joinToString(" ") + " " + rub.joinToString(" ")))
    }

    private fun leggi(o: JSONObject, aggiunti: JSONArray): Dati {
        val a = o.getJSONArray("video")
        val base = (0 until a.length()).map { voce(a.getJSONObject(it)) }
        val ids = base.mapTo(HashSet()) { it.v.youtubeId }
        // i video aggiunti dal feed vanno in cima (sono i piu' recenti), senza doppioni
        val nuovi = (0 until aggiunti.length()).map { voce(aggiunti.getJSONObject(it)) }.filter { it.v.youtubeId !in ids }
        val filtri = JSONObject()
        o.getJSONArray("filtri").let { f -> for (i in 0 until f.length()) f.getJSONObject(i).let { filtri.put(it.getString("id"), it) } }
        return Dati(filtri, o.optJSONObject("regole") ?: JSONObject(), nuovi.sortedByDescending { it.v.data.split("/").reversed().joinToString("") } + base)
    }

    /** Le categorie di un titolo secondo le regole del catalogo (le stesse di strumenti/catalogo.py). */
    private fun classifica(gruppo: JSONObject?, testo: String): List<String> {
        gruppo ?: return emptyList()
        val t = " " + testo.lowercase() + " "
        return gruppo.keys().asSequence().filter { k -> lista(gruppo.optJSONArray(k)).any { it in t } }.toList()
    }

    /**
     * I video nuovi dal feed pubblico di YouTube (video, short, dirette), aggiunti in locale. Al massimo una volta
     * l'ora; la chiama anche il controllo periodico delle notifiche. Nessuna chiamata ai nostri server.
     */
    suspend fun integra(c: Context, forza: Boolean = false) = withContext(Dispatchers.IO) {
        if (!::app.isInitialized) app = c.applicationContext
        val pref = app.getSharedPreferences("catalogo", Context.MODE_PRIVATE)
        if (!forza && System.currentTimeMillis() - pref.getLong("integrato", 0L) < OGNI) return@withContext
        val d = carica()
        val presenti = d.voci.mapTo(HashSet()) { it.v.youtubeId }
        val file = File(app.filesDir, AGGIUNTI)
        val aggiunti = file.takeIf { it.exists() }?.let { runCatching { JSONArray(it.readText()) }.getOrNull() } ?: JSONArray()
        var nuovi = 0
        for ((tipo, pl) in listOf("Video" to "UULF", "Short" to "UUSH", "Diretta" to "UULV")) {
            val doc = runCatching {
                Jsoup.connect("https://www.youtube.com/feeds/videos.xml?playlist_id=$pl${Marca.CANALE_ID}")
                    .userAgent(UA).timeout(15_000).ignoreContentType(true).parser(Parser.xmlParser()).get()
            }.getOrNull() ?: continue
            for (e in doc.getElementsByTag("entry")) {
                val id = e.getElementsByTag("yt:videoId").firstOrNull()?.text() ?: continue
                if (id in presenti) continue
                val titolo = e.getElementsByTag("title").firstOrNull()?.text().orEmpty()
                val descr = e.getElementsByTag("media:description").firstOrNull()?.text().orEmpty()
                val temi = classifica(d.regole.optJSONObject("temi"), titolo).ifEmpty { listOf("Altro") }
                aggiunti.put(JSONObject().put("id", id).put("titolo", titolo).put("tipo", tipo)
                    .put("data", e.getElementsByTag("published").firstOrNull()?.text().orEmpty().take(10))
                    .put("temi", JSONArray(temi)).put("rubriche", JSONArray(classifica(d.regole.optJSONObject("rubriche"), titolo)))
                    .put("tag", JSONArray()).put("descrizione", descr.take(1500)))
                presenti.add(id); nuovi++
            }
        }
        pref.edit().putLong("integrato", System.currentTimeMillis()).apply()
        if (nuovi > 0) { file.writeText(aggiunti.toString()); dati = null; carica() }
    }

    suspend fun modulo(forza: Boolean = false): Modulo = withContext(Dispatchers.IO) {
        val d = carica()
        fun gruppo(id: String, titolo: String, acceso: Boolean): Gruppo? {
            val f = d.filtri.optJSONObject(id) ?: return null
            val op = f.getJSONArray("opzioni")
            return Gruppo(titolo, (0 until op.length()).map { Opzione("$id:${op.getString(it)}", op.getString(it), acceso) })
        }
        Modulo(
            listOfNotNull(
                Gruppo("Cerca le parole nel", listOf(Opzione("dove:titolo", "Titolo", true),
                    Opzione("dove:descrizione", "Descrizione", true), Opzione("dove:tag", "Tag, temi e rubriche", true))),
                gruppo("tipo", "Tipo", true), gruppo("tema", "Tema", false), gruppo("rubrica", "Rubrica", false)),
            listOf(Testo("Descrizione", "Parole da cercare", ""), Testo("MaxRisultati", "Quanti risultati al massimo", "100")),
            emptyList(),
        )
    }

    /**
     * Parole: ognuna deve stare in almeno uno dei campi scelti (titolo, descrizione, tag; nessuno = titolo).
     * Tipo: quelli accesi (nessuno = tutti). Tema e rubrica: almeno uno degli accesi (nessuno = tutti).
     */
    suspend fun cerca(accese: Set<String>, valori: Map<String, String>): Esito = withContext(Dispatchers.IO) {
        val d = carica()
        fun scelti(id: String) = accese.filter { it.startsWith("$id:") }.map { it.substringAfter(':') }.toSet()
        val tipi = scelti("tipo"); val temi = scelti("tema"); val rub = scelti("rubrica")
        val dove = scelti("dove").ifEmpty { setOf("titolo") }
        val parole = piano(valori["Descrizione"].orEmpty()).split(Regex("\\s+")).filter { it.length > 1 }
        val trovati = d.voci.filter { x ->
            (tipi.isEmpty() || x.tipo in tipi) &&
                (temi.isEmpty() || x.temi.any { it in temi }) &&
                (rub.isEmpty() || x.rubriche.any { it in rub }) &&
                parole.all { p -> ("titolo" in dove && p in x.titolo) || ("descrizione" in dove && p in x.descrizione) || ("tag" in dove && p in x.tag) }
        }
        // prima chi ha le parole nel titolo, poi nei tag, poi solo nella descrizione; a parita', i piu' recenti
        fun punti(x: Voce) = parole.sumOf { p -> (if (p in x.titolo) 4 else 0) + (if (p in x.tag) 2 else 0) + (if (p in x.descrizione) 1 else 0) }
        val ordinati = if (parole.isEmpty()) trovati else trovati.sortedByDescending { punti(it) }
        val max = valori["MaxRisultati"]?.toIntOrNull()?.coerceIn(1, 1000) ?: 100
        Esito(ordinati.take(max).map { it.v }, trovati.size)
    }

    /** Le app di un solo canale avvisano di tutto quello che pubblica. */
    fun paroleCasa(c: Context): List<String> = emptyList()
    /** Solo per le app con piu' canali: qui «Novita'» legge il feed del canale. */
    suspend fun recenti(tipo: String): List<Video> = emptyList()
}
