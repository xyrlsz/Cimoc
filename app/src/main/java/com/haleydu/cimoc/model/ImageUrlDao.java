package com.haleydu.cimoc.model;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.haleydu.cimoc.model.ImageUrl;

import java.util.List;

import io.reactivex.rxjava3.core.Flowable;


@Dao
public interface ImageUrlDao {

//    @Insert
//    void insert(ImageUrl imageUrl);
//
//    @Update
//    void update(ImageUrl imageUrl);
//
//    @Query("SELECT * FROM image_url WHERE id = :id")
//    ImageUrl findById(long id);

    @Query("SELECT * FROM image_url WHERE comic_chapter = :comicChapter")
    List<ImageUrl> findByComicChapter(long comicChapter);

//    @Query("DELETE FROM image_url WHERE id = :id")
//    void deleteById(long id);

    @Query("SELECT * FROM image_url WHERE comic_chapter = :comicChapter")
    Flowable<List<ImageUrl>> findByComicChapterRx(Long comicChapter);

    @Query("SELECT * FROM image_url WHERE comic_chapter = :comicChapter")
    List<ImageUrl> findByComicChapter(Long comicChapter);

    @Query("SELECT * FROM image_url WHERE id = :id")
    ImageUrl findById(long id);


    default void insertOrReplace(ImageUrl imageUrl){
        if(imageUrl.getId()!=null){
            ImageUrl tmp = findById(imageUrl.getId());
            if(tmp!=null){
                update(imageUrl);
                return;
            }
        }
        insert(imageUrl);

    }

    @Update
    void update(ImageUrl imageUrl);

    @Query("DELETE FROM image_url WHERE id = :id")
    void deleteById(long id);

    @Insert
    long insert(ImageUrl imageUrl);
}