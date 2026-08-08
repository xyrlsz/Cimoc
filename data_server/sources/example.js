/**
 * XCimoc 动态漫画源示例脚本（模板）
 * -------------------------------------------------
 * 完整规范见仓库 docs/js-source.md。
 *
 * 必备：
 *   - SOURCE 元数据对象（type/title/baseUrl）
 *   - getSearchRequest / parseSearch / getInfoRequest / parseInfo
 *   - getImagesRequest / parseImages
 * 可选：
 *   - getChapterRequest / parseChapter（详情页不含章节时使用）
 *   - getLazyRequest / parseLazy（图片懒加载）
 *   - getCheckRequest / parseCheck（更新检查）
 *   - getUrl / getHeader / getCategoryRequest / parseCategory / getCategories
 *
 * SDK 提供的全局函数：fetch / DOM / selectAll / evalDecrypt / LZ64Decrypt /
 *   getNumber / md5 / urlEncode / urlDecode / base64Encode / base64Decode
 */

// 注意：SOURCE 必须用 var（全局 var/函数声明才会成为全局对象属性，
// 引擎用 JS_GetPropertyStr 读取；const 是词法绑定读不到）。
var SOURCE = {
    type: 900,                  // 唯一类型号（避免与内置源冲突）
    title: '示例源',
    baseUrl: 'https://example.com',
    hosts: ['example.com'],     // 可选：外链识别（详情页打开浏览器时用）
    cidRegex: '(\\d+)',         // 可选：从 URL 提取 cid 的正则
    webConfig: {                // 可选：WebView 渲染配置
        search: { useWebParser: false, autoScroll: false },
        info: { useWebParser: false },
        chapter: { useWebParser: false },
        images: { useWebParser: false }
    },
    categories: {               // 可选：分类（占位符 {subject}/{area}/.../{page}）
        format: 'https://example.com/list/{subject}-p{page}',
        subject: [
            { title: '全部', value: '' },
            { title: '热血', value: 'hot' },
            { title: '恋爱', value: 'love' }
        ],
        area: [
            { title: '全部', value: '' },
            { title: '日漫', value: 'jp' },
            { title: '国漫', value: 'cn' }
        ]
    }
};

/* ==================== 搜索 ==================== */

function getSearchRequest(keyword, page) {
    return {
        url: SOURCE.baseUrl + '/search?q=' + encodeURIComponent(keyword) + '&page=' + page,
        headers: { Referer: SOURCE.baseUrl + '/' }
    };
}

function parseSearch(html, page) {
    var body = DOM(html);
    var nodes = body.select('ul.search-list > li');
    var list = [];
    for (var i = 0; i < nodes.length; i++) {
        var node = nodes[i];
        var titleNodes = node.select('h3.title a');
        if (titleNodes.length === 0) continue;
        list.push({
            cid: getNumber(titleNodes[0].href()),
            title: titleNodes[0].text(),
            cover: node.select('img.cover').length > 0 ? node.select('img.cover')[0].src() : '',
            update: node.select('span.update').length > 0 ? node.select('span.update')[0].text() : '',
            author: node.select('p.author').length > 0 ? node.select('p.author')[0].text() : ''
        });
    }
    return list;
}

/* ==================== 详情 ==================== */

function getInfoRequest(cid) {
    return { url: SOURCE.baseUrl + '/manhua/' + cid + '/' };
}

function parseInfo(html, cid) {
    var body = DOM(html);
    var result = {
        title: body.text('.detail h1'),
        cover: body.src('.detail .cover img'),
        update: body.text('.detail .update'),
        intro: body.text('.detail .intro'),
        author: body.text('.detail .author'),
        finish: body.text('.detail .status').indexOf('完结') >= 0,
        chapters: []
    };
    // 章节可直接在 parseInfo 里返回（无需再请求章节页）。
    // 页面通常按最新在前排列，这里逆序成最旧在前；path 去掉扩展名，
    // 由 getImagesRequest 再拼回 .html。
    var chapterNodes = body.select('.chapter-list a');
    for (var i = chapterNodes.length - 1; i >= 0; i--) {
        var href = chapterNodes[i].href();
        result.chapters.push({
            title: chapterNodes[i].text(),
            path: href.substring(href.lastIndexOf('/') + 1).replace(/\.html$/, '')
        });
    }
    return result;
}

// 若详情页不含章节列表，可改为实现 getChapterRequest + parseChapter：
// function getChapterRequest(html, cid) {
//     return { url: SOURCE.baseUrl + '/manhua/' + cid + '/chapters' };
// }
// function parseChapter(html) {
//     var body = DOM(html);
//     var list = [];
//     body.select('.chapter-list a').forEach(function (a) {
//         list.push({ title: a.text(), path: getNumber(a.href()) });
//     });
//     return list;
// }

/* ==================== 图片 ==================== */

function getImagesRequest(cid, path) {
    return { url: SOURCE.baseUrl + '/manhua/' + cid + '/' + path + '.html' };
}

function parseImages(html) {
    var body = DOM(html);
    var imgs = body.select('.reader img.page-img');
    var list = [];
    for (var i = 0; i < imgs.length; i++) {
        list.push({
            url: imgs[i].src(),
            lazy: false
        });
    }
    return list;
}

/* ==================== 可选：更新检查 ==================== */

function getCheckRequest(cid) {
    return getInfoRequest(cid);
}

function parseCheck(html) {
    return DOM(html).text('.detail .update');
}

/* ==================== 可选：headers / url ==================== */

function getHeader() {
    return {
        'User-Agent': 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36',
        'Referer': SOURCE.baseUrl + '/'
    };
}

function getUrl(cid) {
    return SOURCE.baseUrl + '/manhua/' + cid + '/';
}
