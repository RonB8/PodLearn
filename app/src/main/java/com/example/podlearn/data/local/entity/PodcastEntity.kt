package com.example.podlearn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "podcasts")
data class PodcastEntity(
    @PrimaryKey val id: String,
    val feedUrl: String,
    val title: String,
    val imageUrl: String?,
    val description: String?,
)
