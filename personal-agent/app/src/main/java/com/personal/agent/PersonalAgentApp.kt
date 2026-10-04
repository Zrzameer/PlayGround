package com.personal.agent

import android.app.Application
import androidx.room.Room
import com.personal.agent.data.AppDatabase

class PersonalAgentApp : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "personal-agent.db")
            .fallbackToDestructiveMigration()
            .build()
    }
}
