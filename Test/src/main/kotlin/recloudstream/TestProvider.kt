package recloudstream

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element
import kotlin.text.RegexOption

class demoTryProvider : MainAPI() {

    override var mainUrl = "https://erored.com/"
    override var name = "Ero Red"
    override val hasMainPage = true
    override var lang = "en"
    override val supportedTypes = setOf(
        TvType.NSFW
    )


    override val mainPage = mainPageOf(
        "/scandal" to "Scandal Videos",
        "/porn" to "Daily Videos",
        "/celebrities" to "Celebreties Videos",
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // This is necessary for load more posts on homepage
        val doc = if(request.data == "" && page == 1) {
            app.get("$mainUrl").document
        }
        else if (request.data == "" && page > 1)
        {
            app.get("$mainUrl/page/$page").document
        }
        else
        {
            app.get("$mainUrl${request.data}page/$page").document
        }
        val home = doc.select("content-loop clear").mapNotNull { toResult(it) }

        return newHomePageResponse(HomePageList(request.name, home,isHorizontalImages = false),hasNext = true)
    }
    private fun toResult(post: Element): SearchResponse {
        val url = post.select("div a").attr("href")
        // Log.d("salman731 url",url)
        val title = post.select("div h2 a").text()
        //Log.d("salman731 title",title)
        val imageUrl = post.select("div img").attr("src")
        //Log.d("salman731 imageUrl",imageUrl)
        return newMovieSearchResponse(title, url, TvType.Movie) {
            this.posterUrl = imageUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/search/$query").document
        val searchResponse = doc.select("content-loop clear")
        return searchResponse.mapNotNull { toResult(it) }
    }



    override suspend fun load(url: String): LoadResponse? {
        val doc = app.get(url).document
        val title = doc.select(".page-body h2").text()
        val imageUrl = doc.select(".page-body img").attr("src")
        val info = doc.select(".page-body p:nth-of-type(1)").text()
        val story = ("(?<=Storyline,).*|(?<=Story : ).*|(?<=Storyline : ).*|(?<=Description : ).*|(?<=Description,).*(?<=Story,).*").toRegex().find(info)?.value
        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = imageUrl
            if(story != null) {
                this.plot = story.trim()
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val buttonElements = doc.select(".buttn.red")
        Log.d("salman731 buttonElements",data + buttonElements.size.toString())
        buttonElements.forEach { item->
            val shortLinkUrl = item.attr("href")
            val sDoc = app.post(shortLinkUrl).document
            val links = sDoc.select(".col-sm-8.col-sm-offset-2.well.view-well a")
            links.forEach { item->
                val link = item.attr("href")
                Log.d("salman731 link",link)
                loadExtractor(link,subtitleCallback,callback)
            }
            Log.d("salman731 links",links.size.toString())


        }
        return true
    }
}
