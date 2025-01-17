package com.haleydu.cimoc.model;



import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.haleydu.cimoc.model.Task;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;

//import io.reactivex.Flowable;
//import io.reactivex.rxjava3.core.Observable;

@Dao
public interface TaskDao {

//    @Insert
//    void insert(Task task);
//
//    @Update
//    void update(Task task);

    @Query("SELECT * FROM task WHERE id = :id")
    Task findById(long id);

    @Query("SELECT * FROM task WHERE key = :key")
    List<Task> findByKey(long key);

//    @Query("DELETE FROM task WHERE id = :id")
//    void deleteById(long id);

    @Query("SELECT * FROM task")
    List<Task> findAll();

    @Query("SELECT * FROM task")
    List<Task> getAllTasks();

    @Query("SELECT * FROM task WHERE max != 0")
    List<Task> getValidTasks();

    @Query("SELECT * FROM task WHERE `key` = :key")
    List<Task> getTasksByKey(long key);

    @Query("SELECT * FROM task WHERE `key` = :key")
    Flowable<List<Task>> getTasksByKeyRx(long key);

    @Query("SELECT * FROM task")
    Flowable<List<Task>> getAllTasksRx();

    @Insert
    long insert(Task task);

    @Insert
    void insertTasks(Iterable<Task> tasks);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Query("DELETE FROM task WHERE id = :id")
    void deleteById(long id);

    @Delete
    void deleteTasks(Iterable<Task> tasks);

    @Query("DELETE FROM task WHERE `key` = :comicId")
    void deleteByComicId(long comicId);

    @Query("SELECT * FROM task WHERE `key` = :key AND path = :path LIMIT 1")
    Task findTaskByKeyAndPath(long key, String path);
}