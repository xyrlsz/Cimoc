package com.xyrlsz.xcimocob.presenter;

import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.xyrlsz.xcimocob.core.Download;
import com.xyrlsz.xcimocob.core.Local;
import com.xyrlsz.xcimocob.core.Manga;
import com.xyrlsz.xcimocob.core.Storage;
import com.xyrlsz.xcimocob.manager.ChapterManager;
import com.xyrlsz.xcimocob.manager.ComicManager;
import com.xyrlsz.xcimocob.manager.ImageUrlManager;
import com.xyrlsz.xcimocob.manager.SourceManager;
import com.xyrlsz.xcimocob.model.Chapter;
import com.xyrlsz.xcimocob.model.Comic;
import com.xyrlsz.xcimocob.model.ImageUrl;
import com.xyrlsz.xcimocob.rx.RxBus;
import com.xyrlsz.xcimocob.rx.RxEvent;
import com.xyrlsz.xcimocob.saf.CimocDocumentFile;
import com.xyrlsz.xcimocob.ui.view.ReaderView;
import com.xyrlsz.xcimocob.utils.StringUtils;
import com.xyrlsz.xcimocob.utils.pictureUtils;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.functions.Consumer;

/**
 * Created by Hiroshi on 2016/7/8.
 */
public class ReaderPresenter extends BasePresenter<ReaderView> {

    private final static int LOAD_NULL = 0;
    private final static int LOAD_INIT = 1;
    private final static int LOAD_PREV = 2;
    private final static int LOAD_NEXT = 3;

    private ReaderChapterManger mReaderChapterManger;
    private ImageUrlManager mImageUrlManager;
    private ComicManager mComicManager;
    private SourceManager mSourceManager;
    private Comic mComic;

    private boolean isShowNext = true;
    private boolean isShowPrev = true;
    private int count = 0;
    private int status = LOAD_INIT;

    @Override
    protected void onViewAttach() {
        mComicManager = ComicManager.getInstance(mBaseView);
        mSourceManager = SourceManager.getInstance(mBaseView);
        mImageUrlManager = ImageUrlManager.getInstance(mBaseView);
        ChapterManager.getInstance(mBaseView);
    }

    @Override
    protected void initSubscription() {
        addSubscription(RxEvent.EVENT_PICTURE_PAGING, rxEvent -> mBaseView.onPicturePaging((ImageUrl) rxEvent.getData()));
    }

    public void lazyLoad(final ImageUrl imageUrl) {
        mCompositeSubscription.add(Manga.loadLazyUrl(mSourceManager.getParser(mComic.getSource()), imageUrl.getUrl())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String url) {
                        if (url == null) {
                            mBaseView.onImageLoadFail(imageUrl.getId());
                        } else {
                            mBaseView.onImageLoadSuccess(imageUrl.getId(), url);
                        }
                    }
                }, new Consumer<Throwable>() {
                    @Override
                    public void accept(Throwable throwable) {
                        mBaseView.onImageLoadFail(imageUrl.getId());
                    }
                }));
    }

    public void loadInit(long id, Chapter[] array) {
        mComic = mComicManager.load(id);

        for (int i = 0; i != array.length; ++i) {
            if (array[i].getPath().equals(mComic.getLast())) {
                this.mReaderChapterManger = new ReaderChapterManger(array, i);
                images(getObservable(array[i]));
            }
        }
    }

    public void loadNext() {
        if (mBaseView == null || status != LOAD_NULL || !isShowNext) {
            return;
        }
        Chapter chapter = mReaderChapterManger.getNextChapter();
        if (chapter != null) {
            status = LOAD_NEXT;
            images(getObservable(chapter));
            mBaseView.onNextLoading();
        } else {
            isShowNext = false;
            mBaseView.onNextLoadNone();
        }
    }

    public void loadPrev() {
        if (mBaseView == null || status != LOAD_NULL || !isShowPrev) {
            return;
        }
        Chapter chapter = mReaderChapterManger.getPrevChapter();
        if (chapter != null) {
            status = LOAD_PREV;
            images(getObservable(chapter));
            mBaseView.onPrevLoading();
        } else {
            isShowPrev = false;
            mBaseView.onPrevLoadNone();
        }
    }

    private Observable<List<ImageUrl>> getObservable(Chapter chapter) {
        if (mComic.getLocal()) {
            CimocDocumentFile dir = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ?
                    CimocDocumentFile.fromSubTreeUri(mBaseView.getAppInstance(), Uri.parse(chapter.getPath())) :
                    CimocDocumentFile.fromFile(new File(Objects.requireNonNull(Uri.parse(chapter.getPath()).getPath())));
            return Local.images(dir, chapter);
        }

        Log.i("Reader", "[reader] getObservable local=" + mComic.getLocal()
                + " complete=" + chapter.isComplete() + " path=" + chapter.getPath());
        return chapter.isComplete() ? Download.images(mBaseView.getAppInstance().getDocumentFile(),
                mComic, chapter, mSourceManager.getParser(mComic.getSource()).getTitle()) :
                Manga.getChapterImage(chapter, mSourceManager.getParser(mComic.getSource()), mComic.getCid(), chapter.getPath());
    }

    public void toNextChapter() {
        Chapter chapter = mReaderChapterManger.nextChapter();
        if (chapter != null && mBaseView != null) {
            updateChapter(chapter, true);
        }
    }

    public void toPrevChapter() {
        Chapter chapter = mReaderChapterManger.prevChapter();
        if (chapter != null && mBaseView != null) {
            updateChapter(chapter, false);
        }
    }

    private void updateChapter(Chapter chapter, boolean isNext) {
        if (mBaseView != null) {
            mBaseView.onChapterChange(chapter);
        }
        mComic.setLast(chapter.getPath());
        mComic.setChapter(chapter.getTitle());
        mComic.setPage(isNext ? 1 : chapter.getCount());
        mComicManager.update(mComic);
        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_UPDATE, mComic.getId()));
    }

    public void savePicture(InputStream inputStream, String url, String title, int page) {
        if (mBaseView == null) return;
        mCompositeSubscription.add(Storage.savePicture(mBaseView.getAppInstance().getContentResolver(),
                        mBaseView.getAppInstance().getDocumentFile(), inputStream, buildPictureName(title, page, url))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uri -> {
                    if (mBaseView != null) {
                        mBaseView.onPictureSaveSuccess(uri);
                    }
                }, throwable -> {
                    throwable.printStackTrace();
                    if (mBaseView != null) {
                        mBaseView.onPictureSaveFail();
                    }
                }));
    }

    private String buildPictureName(String title, int page, String url) {
        String suffix = StringUtils.split(url, "\\.", -1);
        String suffixOriginal = suffix.split("\\?")[0].toLowerCase(Locale.ROOT);
        if (!pictureUtils.isPictureFormat(suffixOriginal)) {
            suffixOriginal = "jpg";
        }
        suffix = suffixOriginal;
        return StringUtils.format("%s_%s_%03d.%s", StringUtils.filter(mComic.getTitle()), StringUtils.filter(title), page, suffix);
    }

    public void updateComic(int page) {
        if (status != LOAD_INIT) {
            mComic.setPage(page);
            mComicManager.update(mComic);
            RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_COMIC_UPDATE, mComic.getId()));
        }
    }

    public void switchNight() {
        RxBus.getInstance().post(new RxEvent(RxEvent.EVENT_SWITCH_NIGHT));
    }

    /**
     * 自动检测图片列表的页码起始值，统一修正为从 1 开始。
     * 某些源站返回的页码可能从 0 开始，此方法会检测并整体偏移。
     */
    private void normalizePageNumbers(List<ImageUrl> list) {
        if (list == null || list.isEmpty()) return;

        int i = 1;
        for (ImageUrl image : list) {
            image.setNum(i);
            i++;
        }

    }

    private void images(Observable<List<ImageUrl>> observable) {
        mCompositeSubscription.add(observable
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> {
                    if (mBaseView == null) {
                        status = LOAD_NULL;
                        return;
                    }

                    mImageUrlManager.updateOrInsert(list);
                    Chapter chapter;
                    switch (status) {
                        case LOAD_INIT:
                            chapter = mReaderChapterManger.moveNext();
                            chapter.setCount(list.size());
                            if (!chapter.getTitle().equals(mComic.getTitle())) {
                                mComic.setChapter(chapter.getTitle());
                                mComicManager.update(mComic);
                            }
                            mBaseView.onChapterChange(chapter);
                            mBaseView.onInitLoadSuccess(list, mComic.getPage(), mComic.getSource(), mComic.getLocal());
                            break;
                        case LOAD_PREV:
                            chapter = mReaderChapterManger.movePrev();
                            chapter.setCount(list.size());
                            mBaseView.onPrevLoadSuccess(list);
                            break;
                        case LOAD_NEXT:
                            chapter = mReaderChapterManger.moveNext();
                            chapter.setCount(list.size());
                            mBaseView.onNextLoadSuccess(list);
                            break;
                    }
                    status = LOAD_NULL;
                }, throwable -> {
                    if (mBaseView == null) {
                        status = LOAD_NULL;
                        return;
                    }
                    try {
                        Chapter chapter;
                        List<ImageUrl> list;
                        switch (status) {
                            case LOAD_INIT:
                                chapter = mReaderChapterManger.moveNext();
                                list = mImageUrlManager.getListImageUrl(chapter.getId());
                                if (list != null && !list.isEmpty()) {
                                    chapter.setCount(list.size());
                                    if (!chapter.getTitle().equals(mComic.getTitle())) {
                                        mComic.setChapter(chapter.getTitle());
                                        mComicManager.update(mComic);
                                    }
                                    mBaseView.onChapterChange(chapter);
                                    mBaseView.onInitLoadSuccess(list, mComic.getPage(), mComic.getSource(), mComic.getLocal());
                                }
                                break;
                            case LOAD_PREV:
                                chapter = mReaderChapterManger.movePrev();
                                list = mImageUrlManager.getListImageUrl(chapter.getId());
                                if (list != null && !list.isEmpty()) {
                                    chapter.setCount(list.size());
                                    mBaseView.onPrevLoadSuccess(list);
                                }
                                break;
                            case LOAD_NEXT:
                                chapter = mReaderChapterManger.moveNext();
                                list = mImageUrlManager.getListImageUrl(chapter.getId());
                                if (list != null && !list.isEmpty()) {
                                    chapter.setCount(list.size());
                                    mBaseView.onNextLoadSuccess(list);
                                }
                                break;
                        }
                        status = LOAD_NULL;
                    } finally {
                        if (mBaseView != null) {
                            mBaseView.onParseError();
                        }
                        if (status != LOAD_INIT && ++count < 2) {
                            status = LOAD_NULL;
                        }
                    }
                }));
    }

    private static class ReaderChapterManger {

        private final Chapter[] array;
        private int index;
        private int prev;
        private int next;

        ReaderChapterManger(Chapter[] array, int index) {
            this.array = array;
            this.index = index;
            prev = index + 1;
            next = index;
        }

        Chapter getPrevChapter() {
            return prev < array.length ? array[prev] : null;
        }

        Chapter getNextChapter() {
            return next >= 0 ? array[next] : null;
        }

        Chapter prevChapter() {
            if (index + 1 < prev) {
                return array[++index];
            }
            return null;
        }

        Chapter nextChapter() {
            if (index - 1 > next) {
                return array[--index];
            }
            return null;
        }

        Chapter movePrev() {
            return array[prev++];
        }

        Chapter moveNext() {
            return array[next--];
        }

    }

}
