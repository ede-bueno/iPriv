package com.example.priv.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.priv.data.dao.*
import com.example.priv.data.entity.*
import com.example.priv.data.seed.SeedData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        MemoryEntity::class,
        MemoryAttachmentEntity::class,
        PersonEntity::class,
        MomentEntity::class,
        CollectionEntity::class,
        TagEntity::class,
        SpaceEntity::class,
        SpaceMemberEntity::class,
        GroupEntity::class,
        SyncOperationEntity::class,
        MemoryPersonCrossRef::class,
        MemoryMomentCrossRef::class,
        MemoryCollectionCrossRef::class,
        MemoryTagCrossRef::class,
        GroupPersonCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class PrivDatabase : RoomDatabase() {

    abstract fun memoryDao(): MemoryDao
    abstract fun personDao(): PersonDao
    abstract fun momentDao(): MomentDao
    abstract fun collectionDao(): CollectionDao
    abstract fun tagDao(): TagDao
    abstract fun spaceDao(): SpaceDao
    abstract fun syncQueueDao(): SyncQueueDao

    companion object {
        @Volatile
        private var INSTANCE: PrivDatabase? = null

        fun getInstance(context: Context): PrivDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PrivDatabase::class.java,
                    "priv_memories_db"
                )
                .addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Populate seed data on first creation
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).let { privDb ->
                                SeedData.populateSeedData(
                                    privDb.memoryDao(),
                                    privDb.personDao(),
                                    privDb.momentDao(),
                                    privDb.collectionDao(),
                                    privDb.tagDao(),
                                    privDb.spaceDao()
                                )
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
