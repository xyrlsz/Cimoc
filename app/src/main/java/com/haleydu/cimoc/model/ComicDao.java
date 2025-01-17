package com.haleydu.cimoc.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import androidx.room.Update;


import java.util.Collection;
import java.util.List;

import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;


@Dao
public interface ComicDao {

//    @Insert
//    void insert(Comic comic);
//
//    @Update
//    void update(Comic comic);
//
//    @Query("SELECT * FROM comic WHERE id = :id")
//    Comic findById(long id);
//
//    @Query("SELECT * FROM comic WHERE source = :source AND cid = :cid")
//    Comic findBySourceAndCid(int source, String cid);
//
//    @Query("DELETE FROM comic WHERE id = :id")
//    void deleteById(long id);

    @Query("SELECT * FROM comic")
    List<Comic> findAll();

    @Query("SELECT * FROM comic WHERE local = 1")
    List<Comic> findLocalComics();

    @Update
    void updateComics(List<Comic> comics);
    @Query("SELECT * FROM comic WHERE download IS NOT NULL")
    List<Comic> findByDownloadNotNull();

    @Query("SELECT * FROM comic WHERE local = :local")
    List<Comic> findByLocal(boolean local);

    @Query("SELECT * FROM comic WHERE local = :local")
    Flowable<List<Comic>> findByLocalRx(boolean local);

    @Query("SELECT * FROM comic WHERE favorite IS NOT NULL OR history IS NOT NULL")
    Flowable<List<Comic>> findByFavoriteOrHistoryRx();

    @Query("SELECT * FROM comic WHERE favorite IS NOT NULL")
    List<Comic> findByFavoriteNotNull();

    @Query("SELECT * FROM comic WHERE favorite IS NOT NULL ORDER BY highlight DESC, favorite DESC")
    Flowable<List<Comic>> findByFavoriteNotNullRx();

    @Query("SELECT * FROM comic WHERE favorite IS NOT NULL AND finish = :finish ORDER BY highlight DESC, favorite DESC")
    Flowable<List<Comic>> findByFavoriteAndFinishRx(boolean finish);

    @Query("SELECT * FROM comic WHERE history IS NOT NULL ORDER BY history DESC")
    Flowable<List<Comic>> findByHistoryNotNullRx();

    @Query("SELECT * FROM comic WHERE download IS NOT NULL ORDER BY download DESC")
    Flowable<List<Comic>> findByDownloadNotNullRx();


    @Query("SELECT comic.* FROM comic " +
            "INNER JOIN tag_ref ON comic.id = tag_ref.cid " +
            "WHERE tag_ref.tid = :tid " +
            "ORDER BY comic.highlight DESC, comic.favorite DESC")
    Flowable<List<Comic>> findByTagIdRx(long tid);

    @Query("SELECT * FROM comic WHERE favorite IS NOT NULL AND id NOT IN (:ids)")
    Flowable<List<Comic>> findByFavoriteNotInRx(Collection<Long> ids);

    @Query("SELECT COUNT(*) FROM comic WHERE source = :source AND favorite IS NOT NULL")
    long countBySource(int source);

    @Query("SELECT * FROM comic WHERE id = :id")
    Comic findById(long id);

    @Query("SELECT * FROM comic WHERE source = :source AND cid = :cid")
    Comic findBySourceAndCid(int source, String cid);

    @Query("SELECT * FROM comic WHERE history IS NOT NULL ORDER BY history DESC LIMIT 1")
    Single<Comic> findLastRx();

    @Query("UPDATE comic SET highlight = 0 WHERE highlight = 1")
    void cancelHighlight();

    @Update
    void update(Comic comic);



    default void insertOrReplace(Comic comic){
        if(comic.getId()!=null){
            Comic tmp = findById(comic.getId());
            if(tmp!=null){
                update(comic);
                return;
            }
        }
        insert(comic);

    }

    @Delete
    void delete(Comic comic);

    @Query("DELETE FROM comic WHERE id = :id")
    void deleteById(long id);

    @Insert
    long insert(Comic comic);
}