package io.demars.stellarwallet.mvvm.local

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.demars.stellarwallet.interfaces.OnLoadEffects
import io.demars.stellarwallet.mvvm.remote.RemoteRepository
import org.stellar.sdk.requests.EventListener
import org.stellar.sdk.requests.SSEStream
import org.stellar.sdk.responses.effects.EffectResponse
import timber.log.Timber

class EffectsRepository private constructor(private val remoteRepository: RemoteRepository) {
    private var effectsList: ArrayList<EffectResponse> = ArrayList()
    private var effectListLiveData = MutableLiveData<ArrayList<EffectResponse>>()
    private var isBusy = false
    /**
     * Returns an observable for ALL the effects table changes
     */
    fun loadList(forceRefresh:Boolean): LiveData<ArrayList<EffectResponse>> {
        if (forceRefresh || effectsList.isEmpty()) {
            forceRefresh()
        }
        return effectListLiveData
    }

    @Synchronized fun forceRefresh() {
        Timber.d("Force refresh effects")
        if (isBusy){
            Timber.d("ignoring force refresh, it is busy.")
            return
        }
        isBusy = true
        fetchEffectsList(true)
    }

    fun clear() {
        effectsList.clear()
    }

    private fun notifyLiveData(data : ArrayList<EffectResponse>){
        Timber.d("notifyLiveData size {${data.size}}")
        effectListLiveData.postValue(data)
    }

    /**
     * Makes a call to the webservice. Keep it private since the view/viewModel should be 100% abstracted
     * from the data sources implementation.
     */
    private fun fetchEffectsList(notifyFirsTime : Boolean = false) {
        var cursor = ""
        if (!effectsList.isEmpty()) {
            cursor = effectsList.last().pagingToken
            if (notifyFirsTime) {
                notifyLiveData(effectsList)
            }
        }

        remoteRepository.getEffects(cursor, 200, object : OnLoadEffects {
            override fun onError(errorMessage: String) {
                isBusy = false
            }

            override fun onLoadEffects(result: java.util.ArrayList<EffectResponse>?) {
                Timber.d("fetched ${result?.size} effects from cursor $cursor")
                if (result != null) {
                    if (!result.isEmpty()) {
                        //is the first time let's notify the ui
                        val isFirstTime = effectsList.isEmpty()
                        effectsList.addAll(result)
                        if (isFirstTime) notifyLiveData(effectsList)
                        Timber.d("recursive call to getEffects")
                        fetchEffectsList()
                    }
                }
            }
        })
    }

    companion object {

        private var instance: EffectsRepository? = null

        fun getInstance(): EffectsRepository {
            if (instance == null) {
                instance = EffectsRepository(RemoteRepository())
            }
            return instance as EffectsRepository
        }
    }
}