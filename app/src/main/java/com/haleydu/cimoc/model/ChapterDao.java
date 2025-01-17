package com.haleydu.cimoc.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;


@Dao
public interface ChapterDao {

//    @Insert
//    void insert(Chapter chapter);
//
//    @Update
//    void update(Chapter chapter);
//
//    @Query("SELECT * FROM chapter WHERE id = :id")
//    Chapter findById(long id);
//
//    @Query("SELECT * FROM chapter WHERE source_comic = :sourceComic")
//    List<Chapter> findBySourceComic(long sourceComic);
//
//    @Query("DELETE FROM chapter WHERE id = :id")
//    void deleteById(long id);

//    SQLiteDatabase getDatabase();

    @Query("SELECT * FROM chapter WHERE source_comic = :sourceComic")
    Flowable<List<Chapter>> findBySourceComicRx(Long sourceComic);

    @Query("SELECT * FROM chapter WHERE path = :path AND title = :title")
    List<Chapter> findByPathAndTitle(String path, String title);

    @Query("SELECT * FROM chapter WHERE id = :id")
    Chapter findById(long id);


    default void insertOrReplace(Chapter chapter){
        if(chapter.getId()!=null){
            Chapter tmp = findById(chapter.getId());
            if(tmp!=null){
                update(chapter);
                return;
            }
        }
        insert(chapter);

    }

    @Update
    void update(Chapter chapter);

    @Query("DELETE FROM chapter WHERE id = :id")
    void deleteById(long id);

    @Insert
    long insert(Chapter chapter);
}