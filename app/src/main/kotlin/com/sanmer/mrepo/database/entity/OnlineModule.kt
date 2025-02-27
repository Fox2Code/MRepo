package com.sanmer.mrepo.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import com.sanmer.mrepo.model.module.OnlineModule
import com.sanmer.mrepo.model.module.States

@Entity(tableName = "online_module", primaryKeys = ["id", "repo_url"])
data class OnlineModuleEntity(
    val id: String,
    @ColumnInfo(name = "repo_url") val repoUrl: String,
    val name: String,
    val version: String,
    @ColumnInfo(name = "version_code") val versionCode: Int,
    val author: String,
    val description: String,
    val license: String,
    @Embedded val states: StatesEntity
)

fun OnlineModuleEntity.toModule() = OnlineModule(
    id = id,
    name = name,
    version = version,
    versionCode = versionCode,
    author = author,
    description = description,
    license = license,
    states = states.toStates(),
    repoUrls = mutableListOf(repoUrl)
)

fun OnlineModule.toEntity(repoUrl: String) = OnlineModuleEntity(
    id = id,
    repoUrl = repoUrl,
    name = name,
    version = version,
    versionCode = versionCode,
    author = author,
    description = description,
    license = license,
    states = states.toEntity()
)

@Entity(tableName = "states")
data class StatesEntity(
    val zipUrl: String,
    val changelog: String,
)

fun States.toEntity() = StatesEntity(
    zipUrl = zipUrl,
    changelog = changelog
)

fun StatesEntity.toStates() = States(
    zipUrl = zipUrl,
    changelog = changelog
)