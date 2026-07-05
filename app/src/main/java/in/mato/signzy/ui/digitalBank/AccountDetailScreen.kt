package `in`.mato.signzy.ui.digitalBank

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import `in`.mato.signzy.R
import `in`.mato.signzy.ui.mlKit.DownloadState
import `in`.mato.signzy.ui.theme.AppColors

@Composable
fun AccountDetailsScreen(
    state:DigitalBankViewState,
    onBack:()->Unit,
    onDoKyc:(Int)->Unit
) {
    val customer=state.selectedCustomer?:return
    val context=LocalContext.current
    val isDark=state.isDarkTheme
    val bgColor=if(isDark) AppColors.DarkBackground else AppColors.LightBackground
    val cardColor=if(isDark) AppColors.DarkSurface else Color.White
    val textPrimary=if(isDark) AppColors.DarkText else AppColors.LightText
    val textSecondary=if(isDark) AppColors.DarkMutedText else AppColors.LightMutedText
    val dividerColor=if(isDark) AppColors.DarkBorder else AppColors.LightChip
    Scaffold(
        containerColor=bgColor,
        topBar={
            Row(
                modifier=Modifier.fillMaxWidth().background(cardColor).statusBarsPadding().padding(horizontal=8.dp,vertical=12.dp),
                verticalAlignment=Alignment.CenterVertically
            ) {
                IconButton(onClick=onBack) {
                    Icon(Icons.Default.ArrowBack,contentDescription=stringResource(R.string.back),tint=textPrimary)
                }
                Text(
                    text=stringResource(R.string.account_details),
                    fontWeight=FontWeight.Bold,
                    fontSize=18.sp,
                    color=textPrimary
                )
            }
        }
    ) {paddingValues->
        LazyColumn(
            modifier=Modifier.fillMaxSize().padding(paddingValues).padding(horizontal=16.dp)
        ) {
            item {
                Card(
                    modifier=Modifier.fillMaxWidth().padding(vertical=12.dp),
                    shape=RoundedCornerShape(20.dp),
                    colors=CardDefaults.cardColors(containerColor=cardColor),
                    elevation=CardDefaults.cardElevation(defaultElevation=2.dp)
                ) {
                    Row(
                        modifier=Modifier.padding(16.dp),
                        verticalAlignment=Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model=ImageRequest.Builder(context)
                                .data(customer.selfiePath?:customer.avatar)
                                .crossfade(true)
                                .build(),
                            contentDescription=stringResource(R.string.avatar),
                            modifier=Modifier.size(64.dp).clip(CircleShape).background(if(isDark) AppColors.DarkBorder else AppColors.LightChip),
                            contentScale=ContentScale.Crop
                        )
                        Spacer(modifier=Modifier.width(16.dp))
                        Column(modifier=Modifier.weight(1f)) {
                            Text(
                                text=customer.name,
                                fontWeight=FontWeight.Bold,
                                fontSize=18.sp,
                                color=textPrimary
                            )
                            Text(
                                text=stringResource(R.string.account_number_prefix,customer.maskedAccount),
                                fontSize=13.sp,
                                color=textSecondary
                            )
                            Spacer(modifier=Modifier.height(4.dp))
                            val isVerified=customer.kycStatus=="VERIFIED"
                            val badgeColor=if(isVerified) (if(isDark) AppColors.VerifiedBadgeDark else AppColors.VerifiedBadgeLight) else (if(isDark) AppColors.PendingBadgeDark else AppColors.PendingBadgeLight)
                            val badgeTextColor=if(isVerified) (if(isDark) AppColors.VerifiedTextDark else AppColors.VerifiedTextLight) else (if(isDark) AppColors.PendingTextDark else AppColors.PendingTextLight)
                            Box(
                                modifier=Modifier.clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor)
                                    .padding(horizontal=8.dp,vertical=4.dp)
                            ) {
                                Text(
                                    text=if(isVerified) stringResource(R.string.kyc_verified) else stringResource(R.string.kyc_pending),
                                    fontSize=11.sp,
                                    fontWeight=FontWeight.Bold,
                                    color=badgeTextColor
                                )
                            }
                        }
                        Column(horizontalAlignment=Alignment.End) {
                            Text(
                                text=stringResource(R.string.account_balance,customer.currency,String.format("%,.0f",customer.balance)),
                                fontWeight=FontWeight.ExtraBold,
                                fontSize=18.sp,
                                color=textPrimary
                            )
                            Text(
                                text=stringResource(R.string.account_type_suffix,customer.accountType),
                                fontSize=12.sp,
                                color=textSecondary
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    text=stringResource(R.string.kyc_details),
                    fontWeight=FontWeight.Bold,
                    fontSize=14.sp,
                    color=textSecondary,
                    modifier=Modifier.padding(vertical=8.dp)
                )
                Column(
                    modifier=Modifier.clip(RoundedCornerShape(16.dp)).background(cardColor).padding(16.dp),
                    verticalArrangement=Arrangement.spacedBy(12.dp)
                ) {
                    DetailRow(stringResource(R.string.date_of_birth),customer.dob,isDark)
                    DetailRow(stringResource(R.string.nationality),customer.nationality,isDark)
                    DetailRow(stringResource(R.string.address),customer.address,isDark)
                    DetailRow(stringResource(R.string.contact),customer.contact,isDark)
                    DetailRow(stringResource(R.string.ifsc),customer.ifsc,isDark)
                    HorizontalDivider(color=dividerColor,thickness=1.dp)
                    if(state.bankDetails!=null) {
                        DetailRow(stringResource(R.string.bank),state.bankDetails.bank,isDark)
                        DetailRow(stringResource(R.string.branch),state.bankDetails.branch,isDark)
                        DetailRow(stringResource(R.string.city),state.bankDetails.city,isDark)
                        DetailRow(stringResource(R.string.state),state.bankDetails.state,isDark)
                    } else {
                        Row(
                            modifier=Modifier.fillMaxWidth(),
                            horizontalArrangement=Arrangement.SpaceBetween,
                            verticalAlignment=Alignment.CenterVertically
                        ) {
                            Text(stringResource(R.string.bank_and_branch),fontSize=13.sp,color=textSecondary)
                            CircularProgressIndicator(modifier=Modifier.size(16.dp),strokeWidth=2.dp,color=AppColors.PrimaryBlue)
                        }
                    }
                }
            }
            item {
                Text(
                    text=stringResource(R.string.kyc_selfie),
                    fontWeight=FontWeight.Bold,
                    fontSize=14.sp,
                    color=textSecondary,
                    modifier=Modifier.padding(top=20.dp,bottom=8.dp)
                )
                Card(
                    modifier=Modifier.fillMaxWidth().padding(bottom=32.dp),
                    shape=RoundedCornerShape(16.dp),
                    colors=CardDefaults.cardColors(containerColor=cardColor),
                    elevation=CardDefaults.cardElevation(defaultElevation=2.dp)
                ) {
                    Column(modifier=Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment=Alignment.CenterVertically
                        ) {
                            if(customer.selfiePath!=null) {
                                AsyncImage(
                                    model=ImageRequest.Builder(context)
                                        .data(customer.selfiePath)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription=stringResource(R.string.captured_selfie_photo),
                                    modifier=Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(if(isDark) AppColors.DarkBorder else AppColors.LightChip),
                                    contentScale=ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier=Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)).background(if(isDark) AppColors.DarkBorder else AppColors.LightChip),
                                    contentAlignment=Alignment.Center
                                ) {
                                    Text(stringResource(R.string.no_photo),fontSize=11.sp,color=textSecondary,fontWeight=FontWeight.Bold)
                                }
                            }
                            Spacer(modifier=Modifier.width(16.dp))
                            Column(modifier=Modifier.weight(1f)) {
                                Text(
                                    text=stringResource(R.string.selfie_camera_note),
                                    fontSize=13.sp,
                                    fontWeight=FontWeight.Bold,
                                    color=textPrimary
                                )
                                Text(
                                    text=stringResource(R.string.no_system_camera_note),
                                    fontSize=12.sp,
                                    color=textSecondary
                                )
                            }
                        }
                        Spacer(modifier=Modifier.height(16.dp))
                        if(customer.kycStatus=="PENDING") {
                            Button(
                                onClick={onDoKyc(customer.id)},
                                modifier=Modifier.fillMaxWidth().height(46.dp),
                                colors=ButtonDefaults.buttonColors(containerColor=AppColors.PrimaryBlue),
                                shape=RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.do_kyc),color=Color.White,fontWeight=FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick={onDoKyc(customer.id)},
                                modifier=Modifier.fillMaxWidth().height(46.dp),
                                border=ButtonDefaults.outlinedButtonBorder.copy(width=1.dp),
                                colors=ButtonDefaults.outlinedButtonColors(contentColor=AppColors.PrimaryBlue),
                                shape=RoundedCornerShape(10.dp)
                            ) {
                                Text(stringResource(R.string.retake_selfie),fontWeight=FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun DetailRow(label:String,value:String,isDarkTheme:Boolean=false) {
    Row(
        modifier=Modifier.fillMaxWidth(),
        horizontalArrangement=Arrangement.SpaceBetween,
        verticalAlignment=Alignment.Top
    ) {
        Text(
            text=label,
            fontSize=13.sp,
            color=if(isDarkTheme) AppColors.DarkMutedText else AppColors.LightMutedText,
            modifier=Modifier.weight(1f)
        )
        Text(
            text=value,
            fontSize=13.sp,
            fontWeight=FontWeight.Medium,
            color=if(isDarkTheme) AppColors.DarkText else AppColors.LightText,
            modifier=Modifier.weight(2f),
            textAlign=TextAlign.End
        )
    }
}


/*
@Preview(showBackground = true)
@Composable
fun prev(){
    AccountDetailsScreen(
        state = DigitalBankViewState(
            query = "",
            selectedTab= "PENDING",
            selectedAccountType="All",
            selectedCustomer = null,
            isLoadingDetails=false,
            bankDetails= null,
            errorMessage = null,
            modelDownloadState= DownloadState.Idle,
            isRefreshing=false,
            isDarkTheme = false
        ),
        onBack = {},
        onDoKyc = {}
    )
}
*/
