/**
 * XCimoc 动态漫画源 JS 运行时 SDK
 * ------------------------------------------
 * 该脚本在源脚本之前注入 QuickJS 引擎，为源脚本提供：
 *   - fetch()      同步 HTTP 请求（基于 OkHttp）
 *   - DOM()        CSS 选择器 DOM 解析（基于 jsoup）
 *   - evalDecrypt() 任意 JS 表达式求值（等价于内置源的 evalDecrypt）
 *   - LZ64Decrypt() LZString base64 解压
 *   - md5 / urlEncode / urlDecode / base64Encode / base64Decode
 *   - getNumber()  提取字符串中的数字
 *
 * 底层能力通过 C 层安装的全局函数 hostCall(method, argsJson) 回调到 Java。
 */

/* ---------------- host 桥接 ---------------- */

function __host(method) {
    var args = Array.prototype.slice.call(arguments, 1);
    return hostCall(method, JSON.stringify(args));
}

/* ---------------- 网络请求 ---------------- */

/**
 * 同步发起 HTTP 请求。
 * @param {string} url 请求地址
 * @param {object} [options] {method, headers, body}
 *   body 取值：
 *     - string                原始字符串（POST 默认按表单发送）
 *     - object（普通键值）     表单字段
 *     - {multipart: [[name,value],...]}  multipart/form-data
 *     - {json: 对象或字符串}    JSON body
 * @returns {{status:number, body:string, headers:object}}
 *   网络层错误（无法连接等）会抛出异常；HTTP 非 2xx 不抛，由 status 判断。
 */
function fetch(url, options) {
    options = options || {};
    var method = options.method || 'GET';
    var headers = options.headers || {};
    var body = options.body !== undefined && options.body !== null ? options.body : null;
    return __host('fetch', String(url), String(method), headers, body);
}

/* ---------------- DOM 解析 ---------------- */

/**
 * 用 CSS 选择器解析 HTML，返回节点对象数组。
 * 每个节点：{ text(), attr(name), href(), src(), dataUrl(), html(), select(css) }
 * @param {string} html
 * @param {string} css CSS 选择器
 */
function selectAll(html, css) {
    var list = __host('dom_selectAll', String(html), String(css));
    if (!list) return [];
    return list.map(function (n) {
        return {
            text: function () { return n.text; },
            attr: function (name) { return n.attrs && n.attrs[name] !== undefined ? n.attrs[name] : null; },
            href: function () { return this.attr('href'); },
            src: function () { return this.attr('src'); },
            dataUrl: function () { return this.attr('data-url'); },
            html: function () { return n.html; },
            select: function (css2) { return selectAll(n.html, css2); }
        };
    });
}

/**
 * 创建一个 DOM 解析对象（与内置源 Node 类似的用法）。
 * @param {string} html
 */
function DOM(html) {
    return {
        /** 选择一组节点 */
        select: function (css) { return selectAll(html, css); },
        /** 取第一个匹配元素的文本；css 为空时取整个文档文本 */
        text: function (css) { return __host('dom_text', String(html), css ? String(css) : ''); },
        /** 取第一个匹配元素的指定属性 */
        attr: function (css, name) { return __host('dom_attr', String(html), String(css), String(name)); },
        src: function (css) { return __host('dom_attr', String(html), String(css), 'src'); },
        href: function (css) { return __host('dom_attr', String(html), String(css), 'href'); },
        dataUrl: function (css) { return __host('dom_attr', String(html), String(css), 'data-url'); },
        /** 返回原始 HTML */
        html: function () { return html; }
    };
}

/* ---------------- 解密 / 工具 ---------------- */

/** 执行任意 JS 表达式并返回结果（等价于内置源的 DecryptionUtils.evalDecrypt）。 */
function evalDecrypt(code) {
    return eval(code);
}

/**
 * 执行一段 JS 并返回指定全局变量/表达式的结果字符串（等价于
 * DecryptionUtils.evalDecrypt(code, varName)）。如 "newImgs"。
 */
function evalDecryptVar(code, varName) {
    var r = eval(code);
    if (varName) {
        try {
            var v = eval(varName);
            if (v !== undefined && v !== null) return v;
        } catch (e) { /* ignore */ }
    }
    return r;
}

/** 从字符串中提取第一段连续数字（等价于 StringUtils.getNumber）。 */
function getNumber(s) {
    var m = String(s).match(/\d+/);
    return m ? m[0] : '';
}

/** MD5 摘要（十六进制小写）。 */
function md5(s) {
    return __host('md5', String(s));
}

function urlEncode(s) {
    return __host('urlencode', String(s));
}

function urlDecode(s) {
    return __host('urldecode', String(s));
}

function base64Encode(s) {
    return __host('base64encode', String(s));
}

function base64Decode(s) {
    return __host('base64decode', String(s));
}

/** URL 安全 Base64 解码（字符表 - _，用于部分站点的自定义编码）。 */
function base64UrlDecode(s) {
    return __host('base64url_decode', String(s));
}

/** 繁体转简体（基于宿主 STConvertUtils.T2S）。 */
function t2s(s) {
    return __host('t2s', String(s));
}

/**
 * AES-CBC/PKCS5 解密。
 * @param {string} data 密文，支持 Hex 或 Base64（自动识别）
 * @param {string} key  密钥（UTF-8，不足 16 字节自动补 0）
 * @param {string} iv   初始向量（UTF-8）
 * @returns {string} 明文字符串，失败时返回空串
 */
function aesCbcDecrypt(data, key, iv) {
    return __host('aes_cbc', String(data), String(key), String(iv));
}

/**
 * AES-CBC/PKCS5 解密（IV 为密文 Base64 解码后的前 16 字节）。
 * @param {string} dataBase64 Base64 密文（前 16 字节为 IV，其余为密文）
 * @param {string} key        密钥（UTF-8，不足 16 字节自动补 0）
 * @returns {string} 明文字符串，失败时返回空串
 */
function aesCbcDecryptWithIvPrefix(dataBase64, key) {
    return __host('aes_cbc_raw', String(dataBase64), String(key));
}

/**
 * 跨调用保存一个字符串（按当前源 type + key，进程内）。
 * 用于 JS 引擎无状态场景下传递 _mid 等中间值；建议 key 带上 cid 以支持并发。
 */
function setState(key, value) {
    return __host('set_state', String(SOURCE.type), key,
        value === undefined || value === null ? '' : String(value));
}

/** 读取跨调用状态。 */
function getState(key) {
    return __host('get_state', String(SOURCE.type), key);
}

/** 把毫秒/秒时间戳格式化为 yyyy-MM-dd HH:mm:ss（本地时区）。 */
function formatTimestamp(secondsOrMillis) {
    var v = Number(secondsOrMillis);
    if (String(secondsOrMillis).length <= 10) {
        v *= 1000;
    }
    var d = new Date(v);
    function p(n) { return n < 10 ? '0' + n : '' + n; }
    return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) +
        ' ' + p(d.getHours()) + ':' + p(d.getMinutes()) + ':' + p(d.getSeconds());
}

/* ================ 与内置源 StringUtils / Node 等价的工具 ================ */

/** 对应 Node.splitHref：把 href 拆成若干段后取第 index 段（支持负数）。 */
function splitHref(str, index) {
    if (str === null || str === undefined) return null;
    str = String(str).replace(/^.*\..*?\//, '');
    str = str.replace(/[/.=?]/g, ' ').trim();
    if (index < 0) index = str.split(/\s+/).length + index;
    var arr = str.split(/\s+/);
    return (index < 0 || index >= arr.length) ? null : arr[index];
}

/** 对应 StringUtils.match(regex, input, group)：返回第一个匹配的指定分组。 */
function match(regex, input, group) {
    try {
        var m = new RegExp(regex).exec(String(input));
        if (m) {
            var g = group === undefined || group === null ? 0 : group;
            return m[g] !== undefined ? String(m[g]).trim() : null;
        }
    } catch (e) { /* ignore */ }
    return null;
}

/** 对应 StringUtils.match(regex, input, group...)：返回多个分组数组。 */
function matchArray(regex, input) {
    var groups = Array.prototype.slice.call(arguments, 2);
    try {
        var m = new RegExp(regex).exec(String(input));
        if (m) {
            var result = [];
            for (var i = 0; i < groups.length; i++) {
                result.push(m[groups[i]] !== undefined ? m[groups[i]] : null);
            }
            return result;
        }
    } catch (e) { /* ignore */ }
    return null;
}

/** 对应 StringUtils.split(str, regex, position)：按正则拆分后取第 position 段（支持负数）。 */
function split(str, regex, position) {
    if (str === null || str === undefined) return null;
    var arr = String(str).split(new RegExp(regex));
    if (position < 0) position = arr.length + position;
    return (position < 0 || position >= arr.length) ? null : arr[position];
}

/** 对应 StringUtils.substring(str, start[, end])（end 为负表示从末尾倒数）。 */
function substring(str, start, end) {
    if (str === null || str === undefined) return null;
    str = String(str);
    if (end === undefined || end === null) end = -1;
    if (end < 0) end = str.length + 1 + end;
    if (start >= 0 && start <= str.length()) {
        return str.substring(start, Math.min(end, str.length));
    }
    return null;
}

/** 对应 StringUtils.extractJson：提取字符串中第一段平衡的 {...} 或 [...]。 */
function extractJson(input) {
    if (input === null || input === undefined) return null;
    input = String(input);
    var start = -1, braceCount = 0, bracketCount = 0, inString = false, prev = '';
    for (var i = 0; i < input.length; i++) {
        var c = input.charAt(i);
        if (start === -1) {
            if (c === '{' || c === '[') {
                start = i;
                if (c === '{') braceCount++; else bracketCount++;
            }
            continue;
        }
        if (c === '"' && prev !== '\\') inString = !inString;
        if (!inString) {
            if (c === '{') braceCount++;
            else if (c === '}') braceCount--;
            else if (c === '[') bracketCount++;
            else if (c === ']') bracketCount--;
            if (braceCount === 0 && bracketCount === 0) {
                return input.substring(start, i + 1);
            }
        }
        prev = c;
    }
    return null;
}

/** 对应 StringUtils.format：支持 %s / %d / %% 的简化版（不做类型强校验）。 */
function format(fmt) {
    var args = Array.prototype.slice.call(arguments, 1);
    var ai = 0;
    return String(fmt).replace(/%%/g, '\u0000').replace(/%[sd]/g, function () {
        return ai < args.length ? String(args[ai++]) : '';
    }).replace(/\u0000/g, '%');
}

/**
 * LZString base64 解压（等价于内置源的 DecryptionUtils.LZ64Decrypt）。
 */
var LZString = (function () {
    var keyStrBase64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";

    function getBaseValue(alphabet, character) {
        var l = alphabet.length;
        for (var i = 0; i < l; i++) {
            if (alphabet[i] === character) return i;
        }
        return -1;
    }

    function _decompress(length, resetValue, getNextValue) {
        var dictionary = [], next, enlargeIn = 4, dictSize = 4, numBits = 3,
            entry = "", result = [], i, w, bits, resb, maxpower, power, c,
            data = { val: getNextValue(0), position: resetValue, index: 1 };
        for (i = 0; i < 3; i += 1) dictionary[i] = i;
        bits = 0; maxpower = Math.pow(2, 2); power = 1;
        while (power !== maxpower) {
            resb = data.val & data.position;
            data.position >>= 1;
            if (data.position === 0) { data.position = resetValue; data.val = getNextValue(data.index++); }
            bits |= (resb > 0 ? 1 : 0) * power;
            power <<= 1;
        }
        switch (next = bits) {
            case 0:
                bits = 0; maxpower = Math.pow(2, 8); power = 1;
                while (power !== maxpower) {
                    resb = data.val & data.position;
                    data.position >>= 1;
                    if (data.position === 0) { data.position = resetValue; data.val = getNextValue(data.index++); }
                    bits |= (resb > 0 ? 1 : 0) * power;
                    power <<= 1;
                }
                c = String.fromCharCode(bits);
                break;
            case 1:
                bits = 0; maxpower = Math.pow(2, 16); power = 1;
                while (power !== maxpower) {
                    resb = data.val & data.position;
                    data.position >>= 1;
                    if (data.position === 0) { data.position = resetValue; data.val = getNextValue(data.index++); }
                    bits |= (resb > 0 ? 1 : 0) * power;
                    power <<= 1;
                }
                c = String.fromCharCode(bits);
                break;
            case 2:
                return "";
        }
        dictionary[3] = c;
        w = c;
        result.push(c);
        while (true) {
            if (data.index > length) return "";
            bits = 0; maxpower = Math.pow(2, numBits); power = 1;
            while (power !== maxpower) {
                resb = data.val & data.position;
                data.position >>= 1;
                if (data.position === 0) { data.position = resetValue; data.val = getNextValue(data.index++); }
                bits |= (resb > 0 ? 1 : 0) * power;
                power <<= 1;
            }
            switch (c = bits) {
                case 0:
                    bits = 0; maxpower = Math.pow(2, 8); power = 1;
                    while (power !== maxpower) {
                        resb = data.val & data.position;
                        data.position >>= 1;
                        if (data.position === 0) { data.position = resetValue; data.val = getNextValue(data.index++); }
                        bits |= (resb > 0 ? 1 : 0) * power;
                        power <<= 1;
                    }
                    dictionary[dictSize++] = String.fromCharCode(bits);
                    c = dictSize - 1;
                    enlargeIn--;
                    break;
                case 1:
                    bits = 0; maxpower = Math.pow(2, 16); power = 1;
                    while (power !== maxpower) {
                        resb = data.val & data.position;
                        data.position >>= 1;
                        if (data.position === 0) { data.position = resetValue; data.val = getNextValue(data.index++); }
                        bits |= (resb > 0 ? 1 : 0) * power;
                        power <<= 1;
                    }
                    dictionary[dictSize++] = String.fromCharCode(bits);
                    c = dictSize - 1;
                    enlargeIn--;
                    break;
                case 2:
                    return result.join('');
            }
            if (enlargeIn === 0) { enlargeIn = Math.pow(2, numBits); numBits++; }
            if (dictionary[c]) {
                entry = dictionary[c];
            } else {
                if (c === dictSize) entry = w + w.charAt(0);
                else return null;
            }
            result.push(entry);
            dictionary[dictSize++] = w + entry.charAt(0);
            enlargeIn--;
            w = entry;
            if (enlargeIn === 0) { enlargeIn = Math.pow(2, numBits); numBits++; }
        }
    }

    function decompressFromBase64(input) {
        if (input == null) return "";
        if (input === "") return null;
        return _decompress(input.length, 32, function (index) {
            return getBaseValue(keyStrBase64, input.charAt(index));
        });
    }

    return { decompressFromBase64: decompressFromBase64 };
})();

function LZ64Decrypt(str) {
    return LZString.decompressFromBase64(str);
}
