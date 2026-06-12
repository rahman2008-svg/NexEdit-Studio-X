package com.example.database

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val savedProjectDao: SavedProjectDao) {
    val allProjects: Flow<List<SavedProject>> = savedProjectDao.getAllProjects()

    suspend fun insert(project: SavedProject) {
        savedProjectDao.insertProject(project)
    }

    suspend fun delete(project: SavedProject) {
        savedProjectDao.deleteProject(project)
    }

    suspend fun deleteById(id: Int) {
        savedProjectDao.deleteProjectById(id)
    }
}
