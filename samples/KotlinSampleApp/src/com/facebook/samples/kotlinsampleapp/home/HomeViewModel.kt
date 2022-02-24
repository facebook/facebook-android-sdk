package com.facebook.samples.kotlinsampleapp.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.facebook.Profile
import com.facebook.ProfileTracker
import com.facebook.login.LoginBehavior
import com.facebook.login.LoginManager

class HomeViewModel : ViewModel() {

  private val profileTracker =
      object : ProfileTracker() {
        override fun onCurrentProfileChanged(oldProfile: Profile?, currentProfile: Profile?) {
          if (currentProfile != null) this@HomeViewModel.updateProfile(currentProfile)
          else this@HomeViewModel.resetProfile()
        }
      }
  private val _profileViewState = MutableLiveData(HomeViewState(Profile.getCurrentProfile()))

  val homeViewState: LiveData<HomeViewState> = _profileViewState

  override fun onCleared() {
    profileTracker.stopTracking()
    super.onCleared()
  }

  fun selectLoginBehavior(loginBehavior: LoginBehavior) {
    _profileViewState.value = _profileViewState.value?.copy(loginBehavior = loginBehavior)
  }

  fun logout() {
    LoginManager.getInstance().logOut()
  }

  private fun updateProfile(profile: Profile) {
    _profileViewState.value = _profileViewState.value?.copy(profile = profile)
  }

  private fun resetProfile() {
    _profileViewState.value = _profileViewState.value?.copy(profile = null)
  }
}

@Immutable
data class HomeViewState(
    val profile: Profile? = null,
    val loginBehavior: LoginBehavior = LoginBehavior.NATIVE_WITH_FALLBACK,
    val availableLoginBehaviors: List<LoginBehavior> =
        listOf(LoginBehavior.NATIVE_WITH_FALLBACK, LoginBehavior.WEB_ONLY)
)
