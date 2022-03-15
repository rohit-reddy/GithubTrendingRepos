package com.rohith.githubtrendingrepos.database

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface GitDao {
    @Query("select * from Repo")
    fun getRepos(): LiveData<List<Repo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll( repos: List<Repo>)
}



@Database(entities = [Repo::class], version = 1)
abstract class GitDatabase: RoomDatabase() {
    abstract val gitDao: GitDao
}

private lateinit var INSTANCE: GitDatabase

fun getDatabase(context: Context): GitDatabase {
    synchronized(GitDatabase::class.java) {
        if (!::INSTANCE.isInitialized) {
            INSTANCE = Room.databaseBuilder(context.applicationContext,
                    GitDatabase::class.java,
                    "repos").build()
        }
    }
    return INSTANCE
}
