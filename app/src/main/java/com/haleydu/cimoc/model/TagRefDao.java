package com.haleydu.cimoc.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.haleydu.cimoc.model.TagRef;

import java.util.List;

@Dao
public interface TagRefDao {

//    @Insert
//    void insert(TagRef tagRef);

    @Update
    void update(TagRef tagRef);

    @Query("SELECT * FROM tag_ref WHERE id = :id")
    TagRef findById(long id);

//    @Query("SELECT * FROM tag_ref WHERE tid = :tid")
//    List<TagRef> findByTagId(long tid);

//    @Query("SELECT * FROM tag_ref WHERE cid = :cid")
//    List<TagRef> findByComicId(long cid);

    @Query("DELETE FROM tag_ref WHERE id = :id")
    void deleteById(long id);
//
//    @Query("DELETE FROM tag_ref WHERE tid = :tid AND cid = :cid")
//    void deleteByTagAndComicId(long tid, long cid);


    @Query("SELECT * FROM tag_ref WHERE tid = :tid")
    List<TagRef> findByTagId(long tid);

    @Query("SELECT * FROM tag_ref WHERE cid = :cid")
    List<TagRef> findByComicId(long cid);

    @Query("SELECT * FROM tag_ref WHERE tid = :tid AND cid = :cid LIMIT 1")
    TagRef findByTagAndComicId(long tid, long cid);

    @Insert
    long insert(TagRef tagRef);

    @Insert
    void insertAll(Iterable<TagRef> tagRefs);

    @Query("DELETE FROM tag_ref WHERE tid = :tid")
    void deleteByTagId(long tid);

    @Query("DELETE FROM tag_ref WHERE cid = :cid")
    void deleteByComicId(long cid);

    @Query("DELETE FROM tag_ref WHERE tid = :tid AND cid = :cid")
    void deleteByTagAndComicId(long tid, long cid);
}