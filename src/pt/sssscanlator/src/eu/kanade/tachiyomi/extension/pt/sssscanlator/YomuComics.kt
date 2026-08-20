package eu.kanade.tachiyomi.extension.pt.sssscanlator

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.util.asJsoup
import keiyoushi.annotation.Source
import keiyoushi.lib.cryptoaes.CryptoAES
import keiyoushi.network.rateLimit
import keiyoushi.utils.firstInstanceOrNull
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import uy.kohesive.injekt.injectLazy
import java.text.SimpleDateFormat
import java.util.Locale

@Source
abstract class YomuComics : HttpSource() {

    override val supportsLatest = true

    private val json: Json by injectLazy()

    override val client: OkHttpClient = network.client.newBuilder()
        .rateLimit(5)
        .build()

    override fun headersBuilder() = super.headersBuilder()
        .add("Referer", "$baseUrl/")

    private fun decryptResponse(response: Response): String {
        val garimpoResponse = json.decodeFromString<GarimpoResponse>(response.body.string())
        val cipherText = garimpoResponse.garimpo.let {
            if (it.startsWith("YOMU_")) it.removePrefix("YOMU_").reversed() else it
        }
        return CryptoAES.decrypt(cipherText, "yomu_trolling_scrapers_v2")
    }

    override fun popularMangaRequest(page: Int): Request {
        val url = "$baseUrl/api/library".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", PAGE_SIZE.toString())
            .addQueryParameter("sort", "popular")
            .addQueryParameter("type", DEFAULT_TYPE)
            .build()

<<<<<<< HEAD
        val result = client.get(url).parseAs<JsonObject>()
        return decrypting { result.toMangasPage() }
=======
        return GET(url, bibliotecaHeaders)
>>>>>>> upstream/main
    }

    override fun popularMangaParse(response: Response): MangasPage = parseLibraryResponse(response)

    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$baseUrl/api/library".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", PAGE_SIZE.toString())
            .addQueryParameter("sort", "recent")
            .addQueryParameter("type", DEFAULT_TYPE)
            .build()

        return GET(url, bibliotecaHeaders)
    }

<<<<<<< HEAD
    override suspend fun fetchMangaUpdate(
        manga: SManga,
        chapters: List<SChapter>,
        fetchDetails: Boolean,
        fetchChapters: Boolean,
    ): SMangaUpdate {
        val body = client.get(baseUrl + manga.url, rscHeaders).use { it.body.string() }
        val series = decrypting { body.parseSeriesPage() }
=======
    override fun latestUpdatesParse(response: Response): MangasPage = parseLibraryResponse(response)
>>>>>>> upstream/main

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val genre = filters.firstInstanceOrNull<GenreFilter>()?.selectedValue.orEmpty()
        val type = filters.firstInstanceOrNull<TypeFilter>()?.selectedValue ?: DEFAULT_TYPE
        val status = filters.firstInstanceOrNull<StatusFilter>()?.selectedValue ?: DEFAULT_STATUS
        val sort = filters.firstInstanceOrNull<SortFilter>()?.selectedValue ?: DEFAULT_SORT

        val url = "$baseUrl/api/library".toHttpUrl().newBuilder().apply {
            addQueryParameter("page", page.toString())
            addQueryParameter("limit", PAGE_SIZE.toString())
            addQueryParameter("sort", sort)
            addQueryParameter("type", type)

            if (genre.isNotBlank()) {
                addQueryParameter("genre", genre)
            }

            if (status != DEFAULT_STATUS) {
                addQueryParameter("status", status)
            }

            if (query.isNotBlank()) {
                addQueryParameter("search", query)
            }
        }.build()

        return GET(url, bibliotecaHeaders)
    }

    override fun searchMangaParse(response: Response): MangasPage = parseLibraryResponse(response)

<<<<<<< HEAD
        return decrypting { payload.pages }
    }

    /** The site rotates its payload obfuscation every few weeks, so it is re-read on the first failure. */
    private suspend fun <T> decrypting(block: () -> T): T = try {
        block()
    } catch (_: PayloadException) {
        PayloadCipher.scheme = fetchScheme()
        block()
    }

    private suspend fun fetchScheme(): PayloadScheme {
        val search = client.get("$baseUrl/search").use { it.body.string() }
        val slug = MANGA_SLUG_REGEX.find(search)?.groupValues?.get(1)
            ?: throw Exception("Nenhuma obra encontrada para inspecionar o site")

        val page = client.get("$baseUrl/obra/$slug", rscHeaders).use { it.body.string() }

        return CHUNK_REGEX.findAll(page)
            .map { it.value }
            .distinct()
            .firstNotNullOfOrNull { chunk ->
                PayloadCipher.schemeFrom(client.get("$baseUrl/_next/$chunk").use { it.body.string() })
            }
            ?: throw Exception("Não foi possível descobrir como o site está cifrando as respostas")
=======
    private fun parseLibraryResponse(response: Response): MangasPage {
        val decryptedStr = decryptResponse(response)
        val mangas = json.decodeFromString<List<SearchMangaDto>>(decryptedStr)
        val hasNextPage = mangas.size == PAGE_SIZE
        return MangasPage(mangas.map { it.toSManga() }, hasNextPage)
>>>>>>> upstream/main
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val html = response.body.string()
        val document = org.jsoup.Jsoup.parse(html)
        val manga = SManga.create()

        val titleElement = document.selectFirst("h1")
        manga.title = titleElement?.text() ?: ""

        val badgeTexts = extractBadgeTexts(titleElement)
        val statusText = badgeTexts.firstOrNull(::isStatusBadge)
        val genres = badgeTexts.filterNot(::isStatusBadge)

        manga.genre = genres.joinToString().takeUnless(String::isBlank)
        manga.status = parseStatus(statusText)

        val autorSpan = document.selectFirst("span:containsOwn(Autor)")
        manga.author = autorSpan?.nextElementSibling()?.text()

        val artistaSpan = document.selectFirst("span:containsOwn(Artista)")
        manga.artist = artistaSpan?.nextElementSibling()?.text()

        val descStr = html.substringAfter("\\\"description\\\":\\\"", "")
            .substringBefore("\\\",\\\"author\\\"", "")
            .takeIf { it.isNotEmpty() }
            ?: html.substringAfter("\"description\":\"", "")
                .substringBefore("\",\"author\"", "")

        if (descStr.isNotEmpty()) {
            manga.description = descStr
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\u0026", "&")
        } else {
            manga.description = document.select("p").map { it.text() }.maxByOrNull { it.length } ?: ""
        }

        return manga
    }

    override fun getChapterUrl(chapter: SChapter): String = baseUrl + chapter.url

    override fun chapterListParse(response: Response): List<SChapter> {
        val document = response.asJsoup()
        val chapters = mutableListOf<SChapter>()

        val elements = document.select("a.group[href^=/ler/]")
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ROOT)

        for (element in elements) {
            val url = element.attr("href")
            val titleElement = element.selectFirst("span[title]")
            val title = titleElement?.attr("title") ?: titleElement?.text() ?: ""

            val chapter = SChapter.create()
            chapter.url = url
            chapter.name = title

            val dateText = element.select("span").map { it.text() }
                .lastOrNull { it.contains("/") || it.contains("Há", ignoreCase = true) } ?: ""

            if (dateText.isNotEmpty()) {
                if (dateText.contains("/")) {
                    try {
                        chapter.date_upload = dateFormat.parse(dateText)?.time ?: 0L
                    } catch (e: Exception) {
                        chapter.date_upload = 0L
                    }
                } else {
                    chapter.date_upload = System.currentTimeMillis()
                }
            } else {
                chapter.date_upload = 0L
            }

            val numMatch = Regex("""/ler/[^/]+/(\d+)""").find(url)
            if (numMatch != null) {
                chapter.chapter_number = numMatch.groupValues[1].toFloatOrNull() ?: -1f
            }

            chapters.add(chapter)
        }

        return chapters.distinctBy { it.url }
    }

<<<<<<< HEAD
    companion object {
        private const val PAGE_SIZE = 30
        private val MANGA_PATH_SEGMENTS = listOf("obra", "ler")
        private val MANGA_SLUG_REGEX = """/obra/([a-z0-9-]+)""".toRegex()
        private val CHUNK_REGEX = """static/chunks/[^"\\]+\.js""".toRegex()
=======
    override fun pageListRequest(chapter: SChapter): Request {
        val chapterPageUrl = getChapterUrl(chapter)
        return GET(chapterPageUrl, headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val html = response.body.string()

        val imageRegex = """https?://[^"\\]+?/obras/[^"\\]+?\.(?:webp|png|jpg|jpeg)""".toRegex()
        val matches = imageRegex.findAll(html).map { it.value }.distinct().toList()

        val pagesUrls = matches.filter {
            !it.contains("/capa/") && !it.contains("/media/")
        }

        if (pagesUrls.isEmpty()) {
            throw Exception(
                "Nenhuma página encontrada. URL: ${response.request.url} Length: ${html.length}",
            )
        }

        return pagesUrls.mapIndexed { index, url ->
            Page(index, imageUrl = url)
        }
    }

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun imageRequest(page: Page): Request {
        val requestHeaders = headers.newBuilder()
            .set("Referer", "$baseUrl/")
            .build()

        return GET(page.imageUrl!!, requestHeaders)
    }

    override fun getFilterList() = FilterList(
        GenreFilter(),
        TypeFilter(),
        StatusFilter(),
        SortFilter(),
    )

    private companion object {
        const val PAGE_SIZE = 20
        const val DEFAULT_TYPE = "manhwa"
        const val DEFAULT_STATUS = "all"
        const val DEFAULT_SORT = "popular"
    }

    private val bibliotecaHeaders by lazy {
        headers.newBuilder()
            .set("Referer", "$baseUrl/biblioteca")
            .build()
>>>>>>> upstream/main
    }
}
