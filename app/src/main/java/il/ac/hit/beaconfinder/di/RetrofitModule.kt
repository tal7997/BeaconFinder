package il.ac.hit.beaconfinder.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import il.ac.hit.beaconfinder.data.AppDatabase
import il.ac.hit.beaconfinder.data.MainRepository
import il.ac.hit.beaconfinder.data.TagDao
import il.ac.hit.beaconfinder.features_group.GroupViewmodel
import il.ac.hit.beaconfinder.firebase.FirebaseLinkUtils
import il.ac.hit.beaconfinder.firebase.FirebaseUtils
import javax.inject.Singleton

/**
 * This class is used by Hilt to register services for injection
 */
@Module
@InstallIn(SingletonComponent::class)
object RetrofitModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(appContext, AppDatabase::class.java, "AppDb")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Singleton
    @Provides
    fun provideRepository(db: AppDatabase, firebase: FirebaseUtils, firebaseLinks: FirebaseLinkUtils): MainRepository =
        MainRepository(db, firebase, firebaseLinks)



    @Singleton
    @Provides
    fun provideGroupviewmodel(@ApplicationContext appContext: Context ,MainRepository : MainRepository) : GroupViewmodel {
        return GroupViewmodel(appContext,MainRepository)
    }


}