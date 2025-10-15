<img src="./screenshot/icon02.png" width="180">

# 应用简介

Android 平台在线漫画阅读器\
Online manga reader based on Android

forked from https://github.com/Haleydu/Cimoc

# 下载

可以前往项目的[Action](https://github.com/xyrlsz/Cimoc/actions)
处或者[Release](https://github.com/xyrlsz/Cimoc/releases)处下载。

# 漫画源

<!-- > 漫画源工作情况可以在[project](https://github.com/xyrlsz/Cimoc/projects/1)中进行查看，请尽量不要重复issues -->
> - 各位大佬们提交漫画源相关issue请按照[模板](.github/ISSUE_TEMPLATE/comic-source-issues.md)填写，方便检查问题。
> - 部分漫画源可能中国大陆无法访问，需要使用代理，具体请自行搜索。
# 功能简介

- 翻页阅读（Page Reader）
- 卷纸阅读（Scroll Reader）
- 检查漫画更新（Check Manga For Update）
- 下载漫画（Download Manga）
- 本地漫画（Local Reader）
- 本地备份恢复（Local Backup）
- WebDav云备份功能(WebDav Backup)

# 软件使用说明

- 安装完成后，直接点击右上角的搜索，即可搜索到漫画，需要手机安装[WebView组件](https://play.google.com/store/apps/details?id=com.google.android.webview)
- 报毒问题已解决

# 感谢以下的开源项目及作者

- [Android Open Source Project](http://source.android.com/)
- [ButterKnife](https://github.com/JakeWharton/butterknife)
- [GreenDAO](https://github.com/greenrobot/greenDAO)
- [OkHttp](https://github.com/square/okhttp)
- [Fresco](https://github.com/facebook/fresco)
- [Jsoup](https://github.com/jhy/jsoup)
- [DiscreteSeekBar](https://github.com/AnderWeb/discreteSeekBar)
- [RxJava](https://github.com/ReactiveX/RxJava)
- [RxAndroid](https://github.com/ReactiveX/RxAndroid)
- [RecyclerViewPager](https://github.com/lsjwzh/RecyclerViewPager)
- [PhotoDraweeView](https://github.com/ongakuer/PhotoDraweeView)
- [Rhino](https://github.com/mozilla/rhino)
- [BlazingChain](https://github.com/tommyettinger/BlazingChain)
- [AppUpdater](https://gitee.com/jenly1314/AppUpdater)
- [Opencc4j](https://github.com/houbb/opencc4j)
- [sardine-android](https://github.com/thegrizzlylabs/sardine-android)

# 应用截图

<img src="./screenshot/03.png" width="250">

# 增加图源（欢迎pr）

- 继承 MangaParser 类，参照 Parser 接口的注释

> 在app/src/main/java/com/xyrlsz/xcimoc/source目录里面随便找一个复制一下
> 注释是这个：app/src/main/java/com/xyrlsz/xcimoc/parser/MangaParser.java

<!-- - （可选）继承 MangaCategory 类，参照 Category 接口的注释
> 这个没什么大用的感觉，个人不常用，直接删掉不会有什么影响 -->

- 在 SourceManger 的 getParser() 方法中加入相应分支

> case 里面无脑添加

<!-- - 在 UpdateHelper 的 initSource() 方法中初始化图源，以及修改initComicSourceTable()方法 -->
- 在 UpdateHelper 的 initComicSourceTable() 方法中初始化图源
- 修改"app/src/main/java/com/xyrlsz/xcimoc/ui/activity/BrowserFilter.java"的registUrlListener()方法

# 软件更新方向：

- 能正常搜索解析网络上大部分免费的漫画
- 界面简洁为主
- 解决apk影响体验的问题

# 软件服务器

- 该软件不再需要服务器，完全单机

# 关于淘宝售卖和会员破解

- 本程序没有任何破解网站VIP的功能，仅仅作为网页浏览器显示网站免费浏览部分，淘宝卖家自行添加的破解或其他功能与本程序无任何关系。

# 免责声明：

- 本软件仅用于学习交流，不得用于商业用途
- 本软件不对因使用本软件而导致的任何损失或损害承担责任
- 所有漫画均来自用户在第三方网站上的手动搜索

