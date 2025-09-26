package com.xyrlsz.xcimoc.source

import android.os.Build
import com.xyrlsz.xcimoc.model.Chapter
import com.xyrlsz.xcimoc.model.Comic
import com.xyrlsz.xcimoc.model.ImageUrl
import com.xyrlsz.xcimoc.model.Source
import com.xyrlsz.xcimoc.parser.MangaParser
import com.xyrlsz.xcimoc.parser.NodeIterator
import com.xyrlsz.xcimoc.parser.SearchIterator
import com.xyrlsz.xcimoc.parser.UrlFilter
import com.xyrlsz.xcimoc.soup.Node
import com.xyrlsz.xcimoc.utils.DecryptionUtils
import com.xyrlsz.xcimoc.utils.IdCreator
import com.xyrlsz.xcimoc.utils.StringUtils
import okhttp3.Headers
import okhttp3.Request
import java.io.UnsupportedEncodingException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Created by FEILONG on 2017/12/21.
 * need fix
 */
class MangaBZ(source: Source?) : MangaParser() {
    @Throws(UnsupportedEncodingException::class)
    override fun getSearchRequest(keyword: String, page: Int): Request {
        var url = "http://www.mangabz.com/search?title=$keyword&page=$page"
        return Request.Builder().url(url).build()
    }

    override fun getSearchIterator(html: String, page: Int): SearchIterator {
        val body = Node(html)
        return object : NodeIterator(body.list(".mh-item")) {
            override fun parse(node: Node): Comic {
                var cid = node.attr("a", "href").trim('/')
                val title = node.text(".title")
                val cover = node.attr(".mh-cover", "src")
                val update = node.text(".chapter > a")
                val author = ""
                return Comic(TYPE, cid, title, cover, update, author)
            }
        }
    }

    override fun getUrl(cid: String): String {
        return "http://www.mangabz.com/$cid/"
    }

    override fun initUrlFilterList() {
        filter.add(UrlFilter("www.mangabz.com", ".*", 0))
    }

    override fun getInfoRequest(cid: String): Request {
        val url = "http://www.mangabz.com/$cid/"
        return Request.Builder().url(url).build()
    }

    @Throws(UnsupportedEncodingException::class)
    override fun parseInfo(html: String, comic: Comic): Comic {
        val body = Node(html)
        val title = body.text(".detail-info-title")
        val cover = body.src(".detail-info-cover")
        val update = StringUtils.match(
            "(..月..號 | ....-..-..)",
            body.text(".detail-list-form-title"), 1
        )
        val author = body.text(".detail-info-tip > span > a")
        val intro = body.text(".detail-info-content")
        val status = isFinish(".detail-list-form-title")
        comic.setInfo(title, cover, update, intro, author, status)
        return comic;
    }

    override fun parseChapter(html: String, comic: Comic, sourceComic: Long): List<Chapter> {
        val list: MutableList<Chapter> = LinkedList()
        var i = 0
        for (node in Node(html).list("#chapterlistload > a")) {
            var title = node.attr("title")
            if (title == "") title = node.text()
            val path = node.href().trim('/')

            //list.add(Chapter(title, path))
            val id = IdCreator.createChapterId(sourceComic, i++)
            list.add(
                Chapter(
                    id,
                    sourceComic,
                    title,
                    path
                )
            )
        }
        return list
    }

    var _cid = ""
    var _path = ""

    override fun getImagesRequest(cid: String, path: String): Request {
        val url = "http://www.mangabz.com/$path/"
        this._cid = cid
        this._path = path
        return Request.Builder()
            .url(url)
            .build()
    }

    fun getValFromRegex(html: String, keyword: String, searchfor: String): String? {
        val re = Regex("""var\s+""" + keyword + """\s*=\s*""" + searchfor + """\s*;""")
        val match = re.find(html)
        return match?.groups?.get(1)?.value
    }

    override fun parseImages(html: String, chapter: Chapter): List<ImageUrl> {
        val list: MutableList<ImageUrl> = LinkedList()
        try {
            // get page num
            val mid = getValFromRegex(html, "MANGABZ_MID", "(\\w+)")!!
            val cid = getValFromRegex(html, "MANGABZ_CID", "(\\w+)")!!
            val sign = getValFromRegex(html, "MANGABZ_VIEWSIGN", """\"(\w+)\"""")!!
            val pageCount = getValFromRegex(html, "MANGABZ_IMAGE_COUNT", "(\\d+)")!!.toInt()
            for (i in 1..pageCount) {
                val url =
                    "http://www.mangabz.com/$_path/chapterimage.ashx?cid=$cid&page=$i&key=&_cid=$cid&_mid=$mid&_sign=$sign&_dt="
                //list.add(ImageUrl(i + 1, url, true))

                val comicChapter = chapter.id
                val id = IdCreator.createImageId(comicChapter, i);
                list.add(ImageUrl(id, comicChapter, i + 1, url, true))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return list
    }

    override fun getLazyRequest(url: String?): Request? {
        val dateFmt = "yyyy-MM-dd+HH:mm:ss"
        val dateStr =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val current = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern(dateFmt)
                current.format(formatter)
            } else {
                var date = Date();
                val formatter = SimpleDateFormat(dateFmt)
                formatter.format(date)
            }


        return Request.Builder()
            .addHeader("Referer", "http://www.mangabz.com/$_path/")
            .addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.149 Safari/537.36"
            )
            .url(url + dateStr).build()
    }

    override fun parseLazy(html: String?, url: String?): String? {
        val image = DecryptionUtils.evalDecrypt(html).split(',').get(0)
        return image
    }

    override fun getCheckRequest(cid: String?): Request? {
        return getInfoRequest(cid!!)
    }

    override fun parseCheck(html: String?): String? {
        return StringUtils.match(
            "(..月..號 | ....-..-..)",
            Node(html).text(".detail-list-form-title"), 1
        )
    }

    override fun getHeader(): Headers {
//        return Headers.of("Referer", "http://www.mangabz.com/")
        return Headers.Builder()
            .add("Referer", "http://www.mangabz.com/")
            .build()
    }

    companion object {
        fun getDefaultSource(): Source {
            return Source(null, DEFAULT_TITLE, TYPE, true, "http://www.mangabz.com/");
        }

        const val TYPE = 82
        const val DEFAULT_TITLE = "MangaBZ"
    }

    init {
        init(source)
    }
}