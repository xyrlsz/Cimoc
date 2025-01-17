package com.haleydu.cimoc.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;


import com.haleydu.cimoc.model.Source;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;


@Dao
public interface SourceDao {

//    @Insert
//    void insert(Source source);
//
//    @Update
//    void update(Source source);

    @Query("SELECT * FROM source WHERE id = :id")
    Source findById(long id);

    @Query("SELECT * FROM source WHERE type = :type")
    Source findByType(int type);

    @Query("SELECT * FROM source")
    List<Source> findAll();

    @Query("DELETE FROM source WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM source")
    List<Source> getAllSources();

    default void insertSources(List<Source> sources){
        for(Source s:sources){
            if(s.getId()!=null){
                Source tmp = findById(s.getId());
                if(tmp!=null){
                    update(s);
                    return;
                }
            }
            insert(s);
        }
    }
    @Delete
    void deleteSources(List<Source> sources);
    @Query("SELECT * FROM source ORDER BY type ASC")
    Flowable<List<Source>> getAllSourcesObservable();

    @Query("SELECT * FROM source WHERE enable = 1 ORDER BY type ASC")
    Flowable<List<Source>> getEnableSourcesObservable();

    @Query("SELECT * FROM source WHERE enable = 1 ORDER BY type ASC")
    List<Source> getEnableSources();

    @Query("SELECT * FROM source WHERE type = :type")
    Flowable<Source> getSourceByType(int type);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insert(Source source);

    @Update
    void update(Source source);
}