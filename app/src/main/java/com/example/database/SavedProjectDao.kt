package com.example.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedProjectDao {
    @Query("SELECT * FROM saved_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<SavedProject>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: SavedProject)

    @Delete
    suspend fun deleteProject(project: SavedProject)

    @Query("DELETE FROM saved_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)
}
