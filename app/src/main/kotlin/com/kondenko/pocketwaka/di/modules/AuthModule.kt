package com.kondenko.pocketwaka.di.modules

import com.kondenko.pocketwaka.data.auth.repository.AccessTokenRepository
import com.kondenko.pocketwaka.data.auth.repository.EncryptedKeysRepository
import com.kondenko.pocketwaka.data.auth.service.AccessTokenService
import com.kondenko.pocketwaka.di.Auth
import com.kondenko.pocketwaka.domain.auth.GetAccessToken
import com.kondenko.pocketwaka.domain.auth.GetAppId
import com.kondenko.pocketwaka.domain.auth.GetAppSecret
import com.kondenko.pocketwaka.domain.auth.GetAuthUrl
import com.kondenko.pocketwaka.screens.auth.AuthPresenter
import com.kondenko.pocketwaka.utils.encryption.StringEncryptor
import com.kondenko.pocketwaka.utils.encryption.TokenEncryptor
import com.kondenko.pocketwaka.utils.extensions.create
import org.koin.dsl.module
import retrofit2.Retrofit

object AuthModule {

    fun create() = module {
        factory { StringEncryptor() }
        factory { TokenEncryptor(stringEncryptor = get<StringEncryptor>()) }
        factory { EncryptedKeysRepository() }
        factory { get<Retrofit>(Auth).create<AccessTokenService>() }
        factory { AccessTokenRepository(service = get(), prefs = get()) }
        factory { GetAuthUrl(schedulers = get(), getAppId = get()) }
        factory { GetAppId(schedulers = get(), encryptedKeysRepository = get(), stringEncryptor = get<StringEncryptor>()) }
        factory { GetAppSecret(schedulers = get(), encryptedKeysRepository = get(), stringEncryptor = get<StringEncryptor>()) }
        factory {
            GetAccessToken(
                    schedulers = get(),
                    timeProvider = get(),
                    tokenEncryptor = get<TokenEncryptor>(),
                    accessTokenRepository = get(),
                    getAppId = get(),
                    getAppSecret = get()
            )
        }
        single { AuthPresenter(get() as GetAuthUrl, get() as GetAccessToken) }
    }

}