package com.kondenko.pocketwaka.di

import com.kondenko.pocketwaka.di.modules.*

val koinModules = listOf(
      appModule,
      networkModule,
      persistenceModule,
      firebaseModule,
      analyticsModule,
      mainModule,
      menuModule,
      authModule,
      usersModule,
      summaryModule,
      statsModule,
      commitsModule,
      durationsModule
)
