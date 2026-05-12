package com.kaushalyakarnataka.app.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiSession(
    val loading: Boolean = true,
    val uid: String? = null,
    val profile: UserProfile? = null,
)

class MainViewModel(
    private val repo: KaushalyaRepository,
    application: Application,
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("kk_prefs", Context.MODE_PRIVATE)

    private val _language = MutableStateFlow(
        when (prefs.getString("lang", "en")) {
            "kn" -> AppLanguage.KN
            else -> AppLanguage.EN
        },
    )
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        prefs.edit().putString("lang", if (lang == AppLanguage.KN) "kn" else "en").apply()
        _language.value = lang
    }

    private val _session = MutableStateFlow(UiSession())
    val session: StateFlow<UiSession> = _session.asStateFlow()

    private var profileJob: Job? = null

    init {
        viewModelScope.launch {
            repo.observeAuthUid().collect { uid ->
                profileJob?.cancel()
                if (uid == null) {
                    _session.value = UiSession(loading = false, uid = null, profile = null)
                } else {
                    _session.value = UiSession(loading = true, uid = uid, profile = null)
                    profileJob = launch {
                        repo.observeProfile(uid).collect { profile ->
                            _session.value = UiSession(loading = false, uid = uid, profile = profile)
                        }
                    }
                }
            }
        }
    }

    fun signOut() {
        repo.signOut()
    }

    companion object {
        fun factory(application: Application, repo: KaushalyaRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return MainViewModel(repo, application) as T
                }
            }
    }
}
