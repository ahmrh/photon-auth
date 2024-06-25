package com.ahmrh.amryauth.ui.screen.auth

import android.os.CountDownTimer
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ahmrh.amryauth.common.TOTPFunction
import com.ahmrh.amryauth.common.UiState
import com.ahmrh.amryauth.data.Repository
import com.ahmrh.amryauth.data.local.database.Auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: Repository
) : ViewModel() {

    private var _authsUiState: MutableStateFlow<UiState<List<Auth>>> =
        MutableStateFlow(UiState.Idle)
    val authsUiState: StateFlow<UiState<List<Auth>>>
        get() = _authsUiState

    private var _timeLiveData: MutableLiveData<Long> = MutableLiveData(0)
    val timeLiveData: LiveData<Long> get() = _timeLiveData

    init {
        initUi()
        startTimer()


        val string = TOTPFunction.generate("POFARSCDUPTMDH6JAOGLNDA2RYK77JVA")

    }

    private fun initUi(){

        viewModelScope.launch {
            val authsFlow = repository.getAllAuths()
            authsFlow
                .catch { e ->
                    _authsUiState.emit(UiState.Error(e.localizedMessage.toString()))
                }
                .collect { auths ->
                    _authsUiState.emit(UiState.Success(auths))
                }
        }
    }

    private fun startTimer(){
        val timer = object : CountDownTimer(15000, 1000){

            override fun onTick(millisUntilFinished: Long) {
                _timeLiveData.value = millisUntilFinished / 1000
            }

            override fun onFinish() {

            }
        }
        timer.start()
    }

    fun insertAuth(key: String, username: String) {
        viewModelScope.launch {
            repository.insertAuth(Auth(username = username, key = key))
            Log.d(TAG, authsUiState.value.toString())
        }
    }

    fun deleteAuth(auth: Auth){
        viewModelScope.launch{
            repository.deleteAuth(auth)
            Log.d(TAG, authsUiState.value.toString())

        }
    }

    fun deleteAuthById(id: Int){
        viewModelScope.launch{
            repository.deleteAuthById(id)
            Log.d(TAG, authsUiState.value.toString())

        }
    }
    companion object {
        private const val TAG = "AuthViewModel"
    }


}