package com.haleydu.cimoc.manager;

import com.haleydu.cimoc.component.AppGetter;
import com.haleydu.cimoc.database.AppDatabase;
import com.haleydu.cimoc.model.Task;
import com.haleydu.cimoc.utils.ObservableUtils;

import java.util.List;

import rx.Observable;


/**
 * Created by Hiroshi on 2016/9/4.
 * Modified for Room database.
 */
public class TaskManager {

    private static TaskManager mInstance;

    private AppDatabase mDatabase;

    private TaskManager(AppGetter getter) {
        mDatabase = getter.getAppInstance().getAppDatabase();
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

    public List<Task> list() {
        return mDatabase.taskDao().getAllTasks();
    }

    public List<Task> listValid() {
        return mDatabase.taskDao().getValidTasks();
    }

    public List<Task> list(long key) {
        return mDatabase.taskDao().getTasksByKey(key);
    }

    public Observable<List<Task>> listInRx(long key) {
        return ObservableUtils.V3toV1(mDatabase.taskDao().getTasksByKeyRx(key).toObservable());
    }

    public Observable<List<Task>> listInRx() {
        return ObservableUtils.V3toV1( mDatabase.taskDao().getAllTasksRx().toObservable());
    }

    public void insert(Task task) {
        long id = mDatabase.taskDao().insert(task);
        task.setId(id);
    }

    public void insertInTx(Iterable<Task> entities) {
        mDatabase.taskDao().insertTasks(entities);
    }

    public void update(Task task) {
        mDatabase.taskDao().update(task);
    }

    public void delete(Task task) {
        mDatabase.taskDao().delete(task);
    }

    public void delete(long id) {
        mDatabase.taskDao().deleteById(id);
    }

    public void deleteInTx(Iterable<Task> entities) {
        mDatabase.taskDao().deleteTasks(entities);
    }

    public void deleteByComicId(long id) {
        mDatabase.taskDao().deleteByComicId(id);
    }

    public void insertIfNotExist(final Iterable<Task> entities) {
        mDatabase.runInTransaction(() -> {
            for (Task task : entities) {
                Task existingTask = mDatabase.taskDao().findTaskByKeyAndPath(task.getKey(), task.getPath());
                if (existingTask == null) {
                    mDatabase.taskDao().insert(task);
                }
            }
        });
    }
}