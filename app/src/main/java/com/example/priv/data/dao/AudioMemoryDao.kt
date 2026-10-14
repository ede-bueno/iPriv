package com.example.priv.data.dao

import androidx.room.*
import com.example.priv.data.entity.AudioMemoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioMemoryDao {

    @Query("SELECT * FROM audio_memories ORDER BY createdAt DESC")
    fun getAllAudioMemories(): Flow<List<AudioMemoryEntity>>

    @Query("SELECT * FROM audio_memories WHERE id = :id LIMIT 1")
    fun getAudioMemoryById(id: String): Flow<AudioMemoryEntity?>

    @Query("SELECT * FROM audio_memories WHERE id = :id LIMIT 1")
    suspend fun getAudioMemoryByIdDirect(id: String): AudioMemoryEntity?

    @Query("SELECT * FROM audio_memories WHERE linkedMemoryId = :memoryId ORDER BY createdAt DESC")
    fun getAudioMemoriesForLinkedMemory(memoryId: String): Flow<List<AudioMemoryEntity>>

    @Query("SELECT * FROM audio_memories WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteAudioMemories(): Flow<List<AudioMemoryEntity>>

    @Query("""
        SELECT * FROM audio_memories 
        WHERE transcriptionText LIKE '%' || :query || '%' 
           OR title LIKE '%' || :query || '%' 
           OR summary LIKE '%' || :query || '%'
           OR tags LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchAudioMemories(query: String): Flow<List<AudioMemoryEntity>>

    @Query("""
        SELECT * FROM audio_memories 
        WHERE title LIKE '%' || :query || '%' 
           OR transcriptionText LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchByTitleOrTranscription(query: String): Flow<List<AudioMemoryEntity>>

    @Query("""
        SELECT * FROM audio_memories 
        WHERE tags LIKE '%' || :tag || '%'
        ORDER BY createdAt DESC
    """)
    fun getAudioMemoriesByTag(tag: String): Flow<List<AudioMemoryEntity>>

    @Query("SELECT COUNT(*) FROM audio_memories")
    fun getAudioCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM audio_memories")
    fun getTotalDurationMs(): Flow<Long>

    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM audio_memories")
    fun getTotalSizeBytes(): Flow<Long>

    @Query("SELECT * FROM audio_memories")
    suspend fun getAllAudioMemoriesDirect(): List<AudioMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioMemory(audioMemory: AudioMemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudioMemories(audioMemories: List<AudioMemoryEntity>)

    @Update
    suspend fun updateAudioMemory(audioMemory: AudioMemoryEntity)

    @Query("""
        UPDATE audio_memories 
        SET transcriptionText = :transcriptionText, 
            transcriptionStatus = :status, 
            updatedAt = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateTranscription(
        id: String,
        transcriptionText: String,
        status: String = "EDITED",
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE audio_memories SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateFavorite(
        id: String,
        isFavorite: Boolean,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE audio_memories SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTitle(
        id: String,
        title: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("UPDATE audio_memories SET tags = :tags, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTags(
        id: String,
        tags: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE audio_memories 
        SET title = :title, 
            tags = :tags, 
            transcriptionText = :transcriptionText, 
            transcriptionStatus = 'EDITED',
            updatedAt = :updatedAt 
        WHERE id = :id
    """)
    suspend fun updateAudioMetadata(
        id: String,
        title: String,
        tags: String,
        transcriptionText: String,
        updatedAt: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM audio_memories WHERE id = :id")
    suspend fun deleteAudioMemoryById(id: String)

    @Delete
    suspend fun deleteAudioMemory(audioMemory: AudioMemoryEntity)

    @Query("DELETE FROM audio_memories")
    suspend fun clearAllAudioMemories()
}
