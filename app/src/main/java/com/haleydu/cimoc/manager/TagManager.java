package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.model.Tag;
import com.haleydu.cimoc.utils.ObservableUtils;

import java.util.List;

import rx.Observable;


/**
 * Created by Hiroshi on 2016/10/10.
 * Modified for Room database.
 */
public class TagManager {

    public static final long TAG_CONTINUE = -101;
    public static final long TAG_FINISH = -100;

    private static TagManager mInstance;

    private AppDatabase mDatabase;

    private TagManager(AppGetter getter) {
        mDatabase = getter.getAppInstance().getAppDatabase();
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

    public List<Tag> list() {
        return mDatabase.tagDao().getAllTags();
    }

    public Observable<List<Tag>> listInRx() {
        return ObservableUtils.V3toV1(mDatabase.tagDao().getAllTagsRx().toObservable());
    }

    public Tag load(String title) {
        return mDatabase.tagDao().findByTitle(title);
    }

    public void insert(Tag tag) {
        long id = mDatabase.tagDao().insert(tag);
        tag.setId(id);
    }

    public void update(Tag tag) {
        mDatabase.tagDao().update(tag);
    }

    public void delete(Tag tag) {
        mDatabase.tagDao().delete(tag);
    }
}