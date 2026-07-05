package `in`.mato.signzy.ui.mlKit

import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import `in`.mato.signzy.R
import `in`.mato.signzy.ui.theme.AppColors
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraScreen(
    onBack:()->Unit,
    onSelfieCaptured:(String)->Unit
) {

    val context=LocalContext.current
    var isMlKitEnabled by remember{mutableStateOf(true)}
    var isFaceCentered by remember{mutableStateOf(false)}
    var capturedPath by remember{mutableStateOf<String?>(null)}
    var isCapturing by remember{mutableStateOf(false)}
    val cameraExecutor=remember{Executors.newSingleThreadExecutor()}

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier=Modifier.fillMaxSize().background(AppColors.CameraBackground)) {
        if(capturedPath==null) {
            CameraPreviewView(
                isFlashOn=false,
                isMlKitEnabled=isMlKitEnabled,
                onFaceCentered={isFaceCentered=it},
                onImageCaptured={path->
                    isCapturing=false
                    capturedPath=path
                },
                cameraExecutor=cameraExecutor,
                isCapturing=isCapturing,
                onCaptureError={e->
                    isCapturing=false
                    Toast.makeText(context,context.getString(R.string.capture_error,e.message),Toast.LENGTH_SHORT).show()
                }
            )

            CustomOvalCutoutOverlay(isFaceCentered=isFaceCentered)

            Box(
                modifier=Modifier.fillMaxWidth().statusBarsPadding().padding(top=16.dp),
                contentAlignment=Alignment.TopCenter
            ) {
                FilterChip(
                    selected=isMlKitEnabled,
                    onClick={isMlKitEnabled=!isMlKitEnabled},
                    label={
                        Text(
                            text=if(isMlKitEnabled) stringResource(R.string.ai_face_centering_on) else stringResource(R.string.ai_face_centering_off),
                            fontSize=11.sp,
                            fontWeight=FontWeight.Bold
                        )
                    },
                    colors=FilterChipDefaults.filterChipColors(
                        selectedContainerColor=AppColors.CameraReady,
                        selectedLabelColor=AppColors.CameraDarkContent,
                        containerColor=AppColors.CameraBlocked,
                        labelColor=AppColors.CameraLightContent
                    ),
                    border=null
                )
            }

        } else {

            AsyncImage(
                model=capturedPath,
                contentDescription=stringResource(R.string.captured_selfie),
                modifier=Modifier.fillMaxSize(),
                contentScale=ContentScale.Crop
            )

            CustomOvalCutoutOverlay(isFaceCentered=true,isStatic=true)
        }

        Box(
            modifier=Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom=48.dp),
            contentAlignment=Alignment.Center
        ) {
            AnimatedVisibility(
                visible=capturedPath==null,
                enter=fadeIn()+slideInVertically(initialOffsetY={it}),
                exit=fadeOut()+slideOutVertically(targetOffsetY={it})
            ) {
                Button(
                    onClick={
                        if(!isFaceCentered) {
                            Toast.makeText(context,context.getString(R.string.center_face_to_capture),Toast.LENGTH_SHORT).show()
                        } else {
                            isCapturing=true
                        }
                    },
                    modifier=Modifier.size(80.dp),
                    shape=CircleShape,
                    colors=ButtonDefaults.buttonColors(
                        containerColor=if(isFaceCentered) AppColors.CameraReady else AppColors.CameraLightContent
                    ),
                    contentPadding=PaddingValues(0.dp)
                ) {

                    Box(
                        modifier=Modifier.size(64.dp).border(4.dp,AppColors.CameraDarkContent,CircleShape).background(
                            if(isFaceCentered) AppColors.CameraReady else AppColors.CameraLightContent,
                            CircleShape
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible=capturedPath!=null,
                enter=fadeIn()+slideInVertically(initialOffsetY={it}),
                exit=fadeOut()+slideOutVertically(targetOffsetY={it})
            ) {
                Row(
                    horizontalArrangement=Arrangement.spacedBy(24.dp),
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    Button(
                        onClick={
                            capturedPath?.let{File(it).delete()}
                            capturedPath=null
                        },
                        colors=ButtonDefaults.buttonColors(containerColor=AppColors.CameraBlocked),
                        shape=RoundedCornerShape(24.dp),
                        modifier=Modifier.height(50.dp).width(140.dp)
                    ) {
                        Icon(Icons.Default.Refresh,contentDescription=stringResource(R.string.recapture),tint=AppColors.CameraLightContent)
                        Spacer(modifier=Modifier.width(8.dp))
                        Text(stringResource(R.string.recapture),color=AppColors.CameraLightContent)
                    }
                    Button(
                        onClick={
                            capturedPath?.let{onSelfieCaptured(it)}
                        },
                        colors=ButtonDefaults.buttonColors(containerColor=AppColors.CameraReady),
                        shape=RoundedCornerShape(24.dp),
                        modifier=Modifier.height(50.dp).width(140.dp)
                    ) {
                        Icon(Icons.Default.Done,contentDescription=stringResource(R.string.confirm),tint=AppColors.CameraDarkContent)
                        Spacer(modifier=Modifier.width(8.dp))
                        Text(stringResource(R.string.continue_action),color=AppColors.CameraDarkContent)
                    }
                }
            }
        }
    }
}


@Composable
fun CameraPreviewView(
    isFlashOn:Boolean,
    isMlKitEnabled:Boolean,
    onFaceCentered:(Boolean)->Unit,
    onImageCaptured:(String)->Unit,
    cameraExecutor:ExecutorService,
    isCapturing:Boolean,
    onCaptureError:(ImageCaptureException)->Unit
) {
    val context=LocalContext.current
    val lifecycleOwner= androidx.lifecycle.compose.LocalLifecycleOwner.current
    val previewView=remember{PreviewView(context)}
    val imageCapture=remember{
        ImageCapture.Builder().build()
    }

    LaunchedEffect(isCapturing) {
        if(isCapturing) {
            val selfieFile=File(context.cacheDir,"selfie_${System.currentTimeMillis()}.jpg")
            val outputOptions=ImageCapture.OutputFileOptions.Builder(selfieFile).build()
            imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                object:ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults:ImageCapture.OutputFileResults) {
                        onImageCaptured(selfieFile.absolutePath)
                    }
                    override fun onError(exception:ImageCaptureException) {
                        onCaptureError(exception)
                    }
                }
            )
        }
    }
    AndroidView(
        factory={previewView},
        modifier=Modifier.fillMaxSize()
    ) {view->
        val cameraProviderFuture=ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider=cameraProviderFuture.get()
            val preview=Preview.Builder().build().apply{
                surfaceProvider=view.surfaceProvider
            }
            val cameraSelector=CameraSelector.DEFAULT_FRONT_CAMERA
            val faceDetectorOptions=FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .build()
            val faceDetector=FaceDetection.getClient(faceDetectorOptions)
            val imageAnalyzer=ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply{
                    setAnalyzer(cameraExecutor) {imageProxy->
                        analyzeFaceFrame(imageProxy,faceDetector,isMlKitEnabled,onFaceCentered)
                    }
                }
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalyzer
                )
            }catch(e:Exception) {
                Log.d("CameraExcveption","Caught Error : ${e.printStackTrace()}")
            }
        },ContextCompat.getMainExecutor(context))
    }
}
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun analyzeFaceFrame(
    imageProxy:ImageProxy,
    faceDetector:com.google.mlkit.vision.face.FaceDetector,
    isMlKitEnabled:Boolean,
    onFaceCentered:(Boolean)->Unit
) {
    if(!isMlKitEnabled) {
        onFaceCentered(true)
        imageProxy.close()
        return
    }
    val mediaImage=imageProxy.image
    if(mediaImage!=null) {
        val image=InputImage.fromMediaImage(mediaImage,imageProxy.imageInfo.rotationDegrees)
        faceDetector.process(image)
            .addOnSuccessListener{faces->
                if(faces.size==1) {
                    val face=faces[0]
                    val rotation=imageProxy.imageInfo.rotationDegrees
                    val width=if(rotation%180==90) imageProxy.height else imageProxy.width
                    val height=if(rotation%180==90) imageProxy.width else imageProxy.height
                    val centerX=width/2f
                    val centerY=height/2f
                    val faceX=face.boundingBox.centerX().toFloat()
                    val faceY=face.boundingBox.centerY().toFloat()
                    val isCentered=Math.abs(faceX-centerX)<(width*0.16f)&&
                                     Math.abs(faceY-centerY)<(height*0.16f)&&
                                     face.boundingBox.width()>(width*0.25f)
                    onFaceCentered(isCentered)
                } else {
                    onFaceCentered(false)
                }
            }
            .addOnFailureListener{
                onFaceCentered(false)
            }
            .addOnCompleteListener{
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}


@Composable
fun CustomOvalCutoutOverlay(
    isFaceCentered:Boolean,
    isStatic:Boolean=false
) {
    val borderAnimColor by animateFloatAsState(targetValue=if(isFaceCentered) 1f else 0f)
    val borderColor=if(borderAnimColor>0.5f) AppColors.CameraReady else AppColors.CameraLightContent
    Box(modifier=Modifier.fillMaxSize()) {
        Box(
            modifier=Modifier.fillMaxSize()
                .blur(if(isStatic) 0.dp else 16.dp)
                .graphicsLayer{alpha=0.99f}
        ) {
            Canvas(modifier=Modifier.fillMaxSize()) {
                val ovalWidth=size.width*0.75f
                val ovalHeight=size.height*0.48f
                drawRect(color=AppColors.CameraCutoutBg.copy(alpha=0.65f))
                drawOval(
                    color=AppColors.CameraCutout,
                    topLeft=Offset(size.width/2f-ovalWidth/2f,size.height/2f-ovalHeight/2f),
                    size=Size(ovalWidth,ovalHeight),
                    blendMode=BlendMode.Clear
                )
            }
        }
        Canvas(modifier=Modifier.fillMaxSize()) {
            val ovalWidth=size.width*0.75f
            val ovalHeight=size.height*0.48f
            drawOval(
                color=borderColor,
                topLeft=Offset(size.width/2f-ovalWidth/2f,size.height/2f-ovalHeight/2f),
                size=Size(ovalWidth,ovalHeight),
                style=Stroke(width=8f)
            )
        }
    }
}
