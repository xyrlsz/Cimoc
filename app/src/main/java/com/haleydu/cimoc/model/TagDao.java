package com.haleydu.cimoc.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.haleydu.cimoc.model.Tag;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;

//import io.reactivex.Flowable;

//import io.reactivex.rxjava3.core.Observable;


@Dao
public interface TagDao {


    @Query("SELECT * FROM tag WHERE id = :id")
    Tag findById(long id);

    @Query("SELECT * FROM tag")
    List<Tag> findAll();

    @Query("DELETE FROM tag WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM tag")
    List<Tag> getAllTags();

    @Query("SELECT * FROM tag")
    Flowable<List<Tag>> getAllTagsRx();

    @Query("SELECT * FROM tag WHERE title = :title LIMIT 1")
    Tag findByTitle(String title);

    @Insert
    long insert(Tag tag);

    @Update
    void update(Tag tag);

    @Delete
    void delete(Tag tag);
}