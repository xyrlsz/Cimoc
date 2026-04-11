package com.xyrlsz.xcimoc.manager;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.Tag;
import com.xyrlsz.xcimoc.model.Tag_;
import com.xyrlsz.xcimoc.utils.StringUtils;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import rx.Observable;
import rx.schedulers.Schedulers;


/**
 * Created by Hiroshi on 2016/10/10.
 * Modified to use ObjectBox (参照 ComicManager)
 */
public class TagManager {

    public static final long TAG_CONTINUE = -101;
    public static final long TAG_FINISH = -100;

    private static TagManager mInstance;

    // 1. 修改：使用 ObjectBox 的 Box 替代 TagDao
    private final Box<Tag> mTagBox;

    private TagManager(AppGetter getter) {
        // 2. 修改：从 BoxStore 获取 Box
        BoxStore boxStore = getter.getAppInstance().getBoxStore();
        mTagBox = boxStore.boxFor(Tag.class);
    }

    public static TagManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (TagManager.class) {
                if (mInstance == null) {
                    mInstance = new TagManager(getter);
                }
            }
        }
        return mInstance;
    }

    // 3. 修改：使用 ObjectBox Query 查询，包装在 Observable 中
    public List<Tag> list() {
        return mTagBox.getAll();
    }

    public Observable<List<Tag>> listInRx() {
        return Observable.fromCallable(() ->
                mTagBox.query().build().find()
        ).subscribeOn(Schedulers.io());
    }

    // 4. 修改：load 方法。根据 title 查询
    public Tag load(String title) {
        if (StringUtils.isEmpty(title)) return null;

        return mTagBox.query(Tag_.title.equal(title))

                .build()
                .findFirst();
    }

    // 5. 修改：CRUD 操作
    public void insert(Tag tag) {
        // ObjectBox put 会自动处理 ID，无需手动 set
        mTagBox.put(tag);
    }

    public void update(Tag tag) {
        mTagBox.put(tag);
    }

    public void delete(Tag entity) {
        mTagBox.remove(entity);
    }

}