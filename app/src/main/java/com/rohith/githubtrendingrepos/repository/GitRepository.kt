package com.rohith.githubtrendingrepos.repository

import androidx.lifecycle.LiveData
import com.rohith.githubtrendingrepos.api.GithubService
import com.rohith.githubtrendingrepos.api.IN_QUALIFIER
import com.rohith.githubtrendingrepos.database.Repo
import com.rohith.githubtrendingrepos.database.GitDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository for fetching  repos from the network and storing them on disk
 */
class GitRepository(private val database: GitDatabase) {

    val repos: LiveData<List<Repo>> = database.gitDao.getRepos()

    suspend fun refreshRepos() {
        val apiQuery = "Android$IN_QUALIFIER"
        withContext(Dispatchers.IO) {
            val playlist = GithubService.create().searchRepos(apiQuery, 1, 30)
            database.gitDao.insertAll(playlist.items)
        }
    }

}
