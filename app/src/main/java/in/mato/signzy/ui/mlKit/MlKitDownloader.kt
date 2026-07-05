package `in`.mato.signzy.ui.mlKit

import android.content.Context
import com.google.android.gms.common.moduleinstall.InstallStatusListener
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.android.gms.common.moduleinstall.ModuleInstallStatusUpdate
import com.google.mlkit.vision.face.FaceDetection
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class DownloadState {

    object Idle:DownloadState()
    data class Downloading(val progress:Float):DownloadState()
    object Completed:DownloadState()
    data class Failed(val error:String):DownloadState()
}

@Singleton
class MlKitDownloader @Inject constructor(
    @ApplicationContext private val context:Context
) {

    private val _downloadState=MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState:StateFlow<DownloadState> =_downloadState
    fun checkAvailability(onResult:(Boolean)->Unit) {
        val client=ModuleInstall.getClient(context)
        val api=FaceDetection.getClient()
        client.areModulesAvailable(api)
            .addOnSuccessListener{response->
                onResult(response.areModulesAvailable())
            }
            .addOnFailureListener{
                onResult(false)
            }
    }

    fun downloadModel(onSuccess:()->Unit) {
        val client=ModuleInstall.getClient(context)
        val api=FaceDetection.getClient()
        client.areModulesAvailable(api)
            .addOnSuccessListener{response->
                if(response.areModulesAvailable()) {
                    _downloadState.value=DownloadState.Completed
                    onSuccess()
                } else {
                    _downloadState.value=DownloadState.Downloading(0.0f)
                    lateinit var listener:InstallStatusListener
                    listener=InstallStatusListener{update->
                        val info=update.progressInfo
                        when(update.installState) {
                            ModuleInstallStatusUpdate.InstallState.STATE_DOWNLOADING-> {
                                if(info!=null) {
                                    val progress=info.bytesDownloaded.toFloat()/info.totalBytesToDownload
                                    _downloadState.value=DownloadState.Downloading(progress)
                                }
                            }
                            ModuleInstallStatusUpdate.InstallState.STATE_COMPLETED-> {
                                _downloadState.value=DownloadState.Completed
                                client.unregisterListener(listener)
                                onSuccess()
                            }
                            ModuleInstallStatusUpdate.InstallState.STATE_FAILED,
                            ModuleInstallStatusUpdate.InstallState.STATE_CANCELED-> {
                                _downloadState.value=DownloadState.Failed("Download failed")
                                client.unregisterListener(listener)
                            }
                        }
                    }
                    val request=ModuleInstallRequest.newBuilder()
                        .addApi(api)
                        .setListener(listener)
                        .build()
                    client.installModules(request)
                        .addOnFailureListener{e->
                            _downloadState.value=DownloadState.Failed(e.message?:"Installation failed")
                        }
                }
            }
            .addOnFailureListener{e->
                _downloadState.value=DownloadState.Failed(e.message?:"Check availability failed")
            }
    }

}
