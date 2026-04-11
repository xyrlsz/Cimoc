package com.xyrlsz.xcimoc.presenter;

import android.util.Pair;

import androidx.collection.LongSparseArray;

import com.xyrlsz.xcimoc.core.Local;
import com.xyrlsz.xcimoc.manager.ComicManager;
import com.xyrlsz.xcimoc.manager.TaskManager;
import com.xyrlsz.xcimoc.model.Comic;
import com.xyrlsz.xcimoc.model.MiniComic;
import com.xyrlsz.xcimoc.model.Task;
import com.xyrlsz.xcimoc.rx.ToAnotherList;
import com.xyrlsz.xcimoc.saf.CimocDocumentFile;
import com.xyrlsz.xcimoc.ui.view.LocalView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by Hiroshi on 2017/5/14.
 */

public class LocalPresenter extends BasePresenter<LocalView> {

    private ComicManager mComicManager;
    private TaskManager mTaskManager;

    @Override
    protected void onViewAttach() {
        mComicManager = ComicManager.getInstance(mBaseView);
        mTaskManager = TaskManager.getInstance(mBaseView);
    }

    // TODO 提取出来
    public Comic load(long id) {
        return mComicManager.load(id);
    }

    public void load() {
        mCompositeSubscription.add(mComicManager.listLocalInRx()
                .compose(new ToAnotherList<>(new Func1<Comic, Object>() {
                    @Override
                    public MiniComic call(Comic comic) {
                        return new MiniComic(comic);
                    }
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<List<Object>>() {
                    @Override
                    public void call(List<Object> list) {
                        mBaseView.onComicLoadSuccess(list);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        mBaseView.onComicLoadFail();
                    }
                }));
    }

    private Pair<Map<String, Comic>, Set<String>> buildHash() {
        LongSparseArray<List<String>> array = new LongSparseArray<>();
        Map<String, Comic> map = new HashMap<>();
        Set<String> set = new HashSet<>();
        for (Task task : mTaskManager.list()) {
            List<String> list = array.get(task.getKey());
            if (list == null) {
                list = new ArrayList<>();
                array.put(task.getKey(), list);
            }
            list.add(task.getPath());
        }
        for (Comic comic : mComicManager.listLocal()) {
            map.put(comic.getCid(), comic);
            set.addAll(array.get(comic.getId(), new ArrayList<String>()));
        }
        return Pair.create(map, set);
    }

    private List<Comic> processScanResult(final List<Pair<Comic, ArrayList<Task>>> pairs) {
        try {
            return mComicManager.callInTx(() -> {
                Pair<Map<String, Comic>, Set<String>> hash = buildHash();
                List<Comic> result = new ArrayList<>();
                for (Pair<Comic, ArrayList<Task>> pr : pairs) {
                    Comic comic = hash.first.get(pr.first.getCid());
                    if (comic != null) {
                        for (Task task : pr.second) {
                            task.setKey(comic.getId());
                            if (!hash.second.contains(task.getPath())) {
                                mTaskManager.insert(task);
                            }
                        }
                    } else {
                        mComicManager.insert(pr.first);
                        for (Task task : pr.second) {
                            task.setKey(pr.first.getId());
                            mTaskManager.insert(task);
                        }
                        result.add(pr.first);
                    }
                }
                return result;
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void scan(CimocDocumentFile doc) {
        mCompositeSubscription.add(Local.scan(doc)
                .map(this::processScanResult)
                .compose(new ToAnotherList<>((Func1<Comic, Object>) MiniComic::new))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(list -> mBaseView.onLocalScanSuccess(list), throwable -> mBaseView.onExecuteFail()));
    }

    public void deleteComic(long id) {
        mCompositeSubscription.add(Observable.just(id)
                .doOnNext(id1 -> mComicManager.runInTx(new Runnable() {
                    @Override
                    public void run() {
                        mTaskManager.deleteByComicId(id1);
                        mComicManager.deleteByKey(id1);
                    }
                })).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(id2 -> mBaseView.onLocalDeleteSuccess(id2), throwable -> mBaseView.onExecuteFail()));
    }

}
