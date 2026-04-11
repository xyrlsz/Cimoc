package com.xyrlsz.xcimoc.manager;

import com.xyrlsz.xcimoc.component.AppGetter;
import com.xyrlsz.xcimoc.model.Task;
import com.xyrlsz.xcimoc.model.Task_;

import java.util.List;

import io.objectbox.Box;
import io.objectbox.BoxStore;
import rx.Observable;
import rx.schedulers.Schedulers;


/**
 * Created by Hiroshi on 2016/9/4.
 */
public class TaskManager {

    private static volatile TaskManager mInstance;

    // 1. 修改：使用 ObjectBox 的 Box 替代 TaskDao
    private final Box<Task> mTaskBox;

    private TaskManager(AppGetter getter) {
        // 2. 修改：从 BoxStore 获取 Box
        BoxStore boxStore = getter.getAppInstance().getBoxStore();
        mTaskBox = boxStore.boxFor(Task.class);
    }

    public static TaskManager getInstance(AppGetter getter) {
        if (mInstance == null) {
            synchronized (TaskManager.class) {
                if (mInstance == null) {
                    mInstance = new TaskManager(getter);
                }
            }
        }
        return mInstance;
    }

    // 3. 修改：查询方法
    public List<Task> list() {
        return mTaskBox.getAll();
    }

    public List<Task> listValid() {
        return mTaskBox.query()
                .notEqual(Task_.max, 0) // 对应 .notEq(0)
                .build()
                .find();
    }

    public List<Task> list(long key) {
        return mTaskBox.query()
                .equal(Task_.key, key)
                .build()
                .find();
    }

    // 4. 修改：RxJava 查询
    public Observable<List<Task>> listInRx(long key) {
        return Observable.fromCallable(() ->
                mTaskBox.query()
                        .equal(Task_.key, key)
                        .build()
                        .find()
        ).subscribeOn(Schedulers.io());
    }

    public Observable<List<Task>> listInRx() {
        return Observable.fromCallable(mTaskBox::getAll
        ).subscribeOn(Schedulers.io());
    }

    // 5. 修改：插入操作
    public void insert(Task task) {
        long id = mTaskBox.put(task);
        task.setId(id);
    }

    // ObjectBox put 支持 Iterable 且默认在事务中运行
    public void insertInTx(Iterable<Task> entities) {
        mTaskBox.getStore().runInTx(() -> {
            for (Task task : entities) {
                mTaskBox.put(task);
            }
        });
    }

    public void update(Task task) {
        mTaskBox.put(task);
    }

    // 6. 修改：删除操作
    public void delete(Task task) {
        mTaskBox.remove(task);
    }

    public void delete(long id) {
        mTaskBox.remove(id);
    }

    public void deleteInTx(Iterable<Task> entities) {

        mTaskBox.getStore().runInTx(() -> {
            for (Task task : entities) {
                mTaskBox.remove(task);
            }
        });
    }

    public void deleteByComicId(long id) {
        // ObjectBox 没有 buildDelete，使用 query().build().remove()
        mTaskBox.query()
                .equal(Task_.key, id)
                .build()
                .remove();
    }

    // 7. 修改：insertIfNotExist 逻辑
    // 使用 Store.runInTx 包裹循环查询和插入
    public void insertIfNotExist(final Iterable<Task> entities) {
        mTaskBox.getStore().runInTx(() -> {
            for (Task task : entities) {
                // 查询是否存在相同的 Key 和 Path
                Task existing = mTaskBox.query(Task_.path.equal(task.getPath()))
                        .equal(Task_.key, task.getKey())

                        .build()
                        .findFirst();

                if (existing == null) {
                    mTaskBox.put(task);
                }
            }
        });
    }

}