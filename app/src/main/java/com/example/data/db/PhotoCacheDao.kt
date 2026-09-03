package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PhotoCacheDao {

    @Query("SELECT * FROM photo_cache WHERE uriString = :uriString LIMIT 1")
    suspend fun getByUri(uriString: String): PhotoCacheEntity?

    @Query("SELECT * FROM photo_cache WHERE uriString = :uriString AND lastModified = :lastModified AND fileSize = :fileSize LIMIT 1")
    suspend fun getValidCached(uriString: String, lastModified: Long, fileSize: Long): PhotoCacheEntity?

    @Query("SELECT * FROM photo_cache WHERE treeUriString = :treeUriString")
    suspend fun getAllForTree(treeUriString: String): List<PhotoCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: PhotoCacheEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<PhotoCacheEntity>)

    @Query("DELETE FROM photo_cache WHERE treeUriString = :treeUriString")
    suspend fun clearForTree(treeUriString: String)

    @Query("DELETE FROM photo_cache WHERE uriString = :uriString")
    suspend fun deleteByUri(uriString: String)

    @Query("DELETE FROM photo_cache")
    suspend fun clearAll()
}
