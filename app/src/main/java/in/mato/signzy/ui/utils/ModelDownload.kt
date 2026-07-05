package `in`.mato.signzy.ui.utils

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.mato.signzy.R
import `in`.mato.signzy.ui.mlKit.DownloadState
import `in`.mato.signzy.ui.theme.AppColors

@Composable
fun ModelDownloadScreen(
    downloadState:DownloadState,
    isDarkTheme:Boolean=false,
) {
    val bgColor=if(isDarkTheme) AppColors.DarkBackground else Color.White
    val textPrimary=if(isDarkTheme) AppColors.DarkText else AppColors.LightText
    val textSecondary=if(isDarkTheme) AppColors.DarkMutedText else AppColors.LightMutedText
    Box(
        modifier=Modifier.fillMaxSize().background(bgColor),
        contentAlignment=Alignment.Center
    ) {
        Column(
            horizontalAlignment=Alignment.CenterHorizontally,
            modifier=Modifier.padding(32.dp)
        ) {
            Text(
                text=stringResource(R.string.setting_up_ml_kit),
                fontSize=20.sp,
                fontWeight=FontWeight.Bold,
                color=textPrimary
            )
            Spacer(modifier=Modifier.height(8.dp))
            Text(
                text=stringResource(R.string.downloading_face_models),
                fontSize=14.sp,
                color=textSecondary,
                textAlign=TextAlign.Center
            )
            Spacer(modifier=Modifier.height(24.dp))
            val progress=if(downloadState is DownloadState.Downloading) downloadState.progress else 0f
            Box(contentAlignment=Alignment.Center) {
                CircularProgressIndicator(
                    progress={progress},
                    modifier=Modifier.size(80.dp),
                    color=AppColors.PrimaryBlue,
                    strokeWidth=6.dp
                )
                Text(
                    text=stringResource(R.string.download_percent,(progress*100).toInt()),
                    fontSize=14.sp,
                    fontWeight=FontWeight.Bold,
                    color=AppColors.PrimaryBlue
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun prev(){
    ModelDownloadScreen(
        downloadState = DownloadState.Idle,
        isDarkTheme = true
    )
}