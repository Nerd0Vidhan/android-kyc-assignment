package `in`.mato.signzy.ui.digitalBank

import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import coil.request.ImageRequest
import `in`.mato.signzy.R
import `in`.mato.signzy.domain.model.Customer
import `in`.mato.signzy.ui.mlKit.CameraScreen
import `in`.mato.signzy.ui.mlKit.DownloadState
import `in`.mato.signzy.ui.theme.AppColors
import `in`.mato.signzy.ui.utils.ModelDownloadScreen

@Composable
fun DigitalBankApp(
    viewModel:DigitalBankViewModel
) {


    val state by viewModel.viewState.collectAsState()
    val navController=rememberNavController()
    val context=LocalContext.current
    val view=LocalView.current
    val isDark=state.isDarkTheme

    if(!view.isInEditMode) {
        SideEffect {
            val window=(context as? android.app.Activity)?.window
            if(window!=null) {
                val insetsController=WindowCompat.getInsetsController(window,view)
                insetsController.isAppearanceLightStatusBars=!isDark
            }
        }
    }

    var activeKycCustomerId by remember{mutableStateOf<Int?>(null)}
    var showPermissionDialog by remember{mutableStateOf(false)}

    val launchKycCamera:()->Unit={

        if(state.modelDownloadState is DownloadState.Completed) {
            navController.navigate("CameraCapture")
        } else {
            viewModel.handleIntent(DigitalBankIntent.TriggerModelDownload {
                navController.navigate("CameraCapture") {
                    popUpTo("DownloadingModel") {inclusive=true}
                }
            })

            navController.navigate("DownloadingModel")
        }
    }

    val cameraPermissionLauncher=rememberLauncherForActivityResult(
        contract=ActivityResultContracts.RequestPermission()
    ) {isGranted->
        if(isGranted) {
            launchKycCamera()
        }
    }

    val handleDoKyc:(Int)->Unit={id->
        activeKycCustomerId=id
        val hasPermission= ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED

        if(hasPermission) {
            launchKycCamera()
        } else {
            showPermissionDialog=true
        }
    }

    Box(modifier=Modifier.fillMaxSize().background(if(state.isDarkTheme) AppColors.DarkBackground else AppColors.LightBackground)) {

        NavHost(
            navController=navController,
            startDestination="AccountsList",

        ) {
            composable(
                route="AccountsList",
                exitTransition={slideOutHorizontally(targetOffsetX={width->-width/3})+fadeOut()},
                popEnterTransition={slideInHorizontally(initialOffsetX={width->-width/3})+fadeIn()}
            ) {

                AccountsScreen(
                    viewModel=viewModel,
                    onCustomerClick={cust->
                        navController.navigate("AccountDetails/${cust.id}")
                    },
                    onDoKyc=handleDoKyc
                )

            }
            composable(
                route="AccountDetails/{customerId}",
                arguments=listOf(navArgument("customerId"){type=NavType.IntType}),
                enterTransition={slideInHorizontally(initialOffsetX={width->width})+fadeIn()},
                exitTransition={slideOutHorizontally(targetOffsetX={width->-width/3})+fadeOut()},
                popEnterTransition={slideInHorizontally(initialOffsetX={width->-width/3})+fadeIn()},
                popExitTransition={slideOutHorizontally(targetOffsetX={width->width})+fadeOut()}
            ) {backStackEntry->

                val customerId=backStackEntry.arguments?.getInt("customerId")?:0
                LaunchedEffect(customerId) {
                    viewModel.handleIntent(DigitalBankIntent.SelectCustomer(customerId))
                }
                AccountDetailsScreen(
                    state=state,
                    onBack={
                        navController.popBackStack()
                    },
                    onDoKyc=handleDoKyc
                )
            }
            composable(
                route="DownloadingModel",
                enterTransition={slideInHorizontally(initialOffsetX={width->width})+fadeIn()},
                exitTransition={slideOutHorizontally(targetOffsetX={width->-width/3})+fadeOut()},
                popExitTransition={slideOutHorizontally(targetOffsetX={width->width})+fadeOut()}
            ) {
                ModelDownloadScreen(
                    downloadState=state.modelDownloadState,
                    isDarkTheme=state.isDarkTheme
                )
            }
            composable(
                route="CameraCapture",
                enterTransition={slideInHorizontally(initialOffsetX={width->width})+fadeIn()},
                exitTransition={slideOutHorizontally(targetOffsetX={width->-width/3})+fadeOut()},
                popExitTransition={slideOutHorizontally(targetOffsetX={width->width})+fadeOut()}
            ) {
                CameraScreen(
                    onBack={
                        navController.popBackStack()
                    },
                    onSelfieCaptured={path->
                        activeKycCustomerId?.let{id->
                            viewModel.handleIntent(DigitalBankIntent.CompleteKyc(id,path))
                        }
                        navController.popBackStack()
                    }
                )
            }
        }
        if(showPermissionDialog) {
            AlertDialog(
                onDismissRequest={showPermissionDialog=false},
                title={Text(stringResource(R.string.camera_permission_title))},
                text={Text(stringResource(R.string.camera_permission_message))},
                confirmButton={
                    TextButton(
                        onClick={
                            showPermissionDialog=false
                            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    ) {
                        Text(stringResource(R.string.okay))
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel:DigitalBankViewModel,
    onCustomerClick:(Customer)->Unit,
    onDoKyc:(Int)->Unit
) {

    val state by viewModel.viewState.collectAsState()
    val pagingItems=viewModel.customersPagingFlow.collectAsLazyPagingItems()
    val isDark=state.isDarkTheme

    val bgColor=if(isDark) AppColors.DarkBackground else AppColors.LightBackground
    val cardColor=if(isDark) AppColors.DarkSurface else Color.White
    val textPrimary=if(isDark) AppColors.DarkText else AppColors.LightText
    val textSecondary=if(isDark) AppColors.DarkMutedText else AppColors.LightMutedText
    val borderColor=if(isDark) AppColors.DarkBorder else AppColors.LightBorder
    val searchContainerBg=if(isDark) AppColors.DarkSurface else AppColors.LightField

    Scaffold(
        containerColor=bgColor,
        topBar={
            Column(modifier=Modifier.background(cardColor).statusBarsPadding().padding(horizontal=16.dp,vertical=8.dp)) {

                Row(
                    modifier=Modifier.fillMaxWidth(),
                    horizontalArrangement=Arrangement.SpaceBetween,
                    verticalAlignment=Alignment.CenterVertically
                ) {
                    Text(
                        text=stringResource(R.string.digital_bank),
                        fontWeight=FontWeight.Bold,
                        fontSize=24.sp,
                        color=textPrimary
                    )
                    IconButton(
                        onClick={
                            viewModel.handleIntent(DigitalBankIntent.ToggleTheme(!isDark))
                        }
                    ) {
                        Icon(
                            imageVector=if(isDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription=stringResource(R.string.toggle_theme),
                            tint=if(isDark) AppColors.WarningIcon else AppColors.LightControlText
                        )
                    }
                }

                Text(
                    text=if(state.selectedTab=="PENDING") stringResource(R.string.pending_kyc) else stringResource(R.string.verified_kyc),
                    fontSize=14.sp,
                    color=textSecondary,
                    modifier=Modifier.padding(top=2.dp)
                )

                Spacer(modifier=Modifier.height(12.dp))

                OutlinedTextField(
                    value=state.query,
                    onValueChange={viewModel.handleIntent(DigitalBankIntent.Search(it))},
                    placeholder={Text(stringResource(R.string.search_account_hint))},
                    leadingIcon={Icon(Icons.Default.Search,contentDescription=stringResource(R.string.search))},
                    modifier=Modifier.fillMaxWidth(),
                    shape=RoundedCornerShape(12.dp),
                    colors=OutlinedTextFieldDefaults.colors(
                        focusedTextColor=textPrimary,
                        unfocusedTextColor=textPrimary,
                        focusedBorderColor=AppColors.PrimaryBlue,
                        unfocusedBorderColor=borderColor,
                        focusedContainerColor=searchContainerBg,
                        unfocusedContainerColor=searchContainerBg
                    ),
                    singleLine=true
                )

                Spacer(modifier=Modifier.height(12.dp))

                Row(
                    horizontalArrangement=Arrangement.spacedBy(8.dp),
                    modifier=Modifier.fillMaxWidth()
                ) {

                    val chips=listOf(
                        "All" to stringResource(R.string.filter_all),
                        "Savings" to stringResource(R.string.filter_savings),
                        "Current" to stringResource(R.string.filter_current),
                        "NRI" to stringResource(R.string.filter_nri)
                    )

                    chips.forEach{chip->
                        FilterChip(
                            selected=state.selectedAccountType==chip.first,
                            onClick={viewModel.handleIntent(DigitalBankIntent.SelectAccountType(chip.first))},
                            label={Text(chip.second)},
                            colors=FilterChipDefaults.filterChipColors(
                                selectedContainerColor=AppColors.PrimaryBlue,
                                selectedLabelColor=Color.White,
                                containerColor=if(isDark) AppColors.DarkBorder else AppColors.LightChip,
                                labelColor=if(isDark) AppColors.DarkMutedText else AppColors.LightControlText
                            ),
                            border=null,
                            shape=RoundedCornerShape(20.dp)
                        )
                    }
                }
            }
        },
        bottomBar={
            Surface(
                shadowElevation=8.dp,
                color=cardColor,
                modifier=Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.wrapContentHeight(), thickness = 2.dp , color = borderColor)

                    Row(
                        modifier=Modifier.fillMaxWidth().height(60.dp).navigationBarsPadding()
                    ) {
                        val tabs=listOf(
                            "VERIFIED" to stringResource(R.string.tab_verified),
                            "PENDING" to stringResource(R.string.tab_pending)
                        )

                        tabs.forEach{tab->
                            val isSelected=state.selectedTab==tab.first
                            val indicatorColor by animateColorAsState(if(isSelected) AppColors.PrimaryBlue else Color.Transparent)
                            Box(
                                modifier=Modifier.weight(1f).fillMaxHeight()
                                    .clickable{viewModel.handleIntent(DigitalBankIntent.ChangeTab(tab.first))}
                                    .background(cardColor),
                                contentAlignment=Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment=Alignment.CenterHorizontally,
                                    verticalArrangement=Arrangement.Center,
                                    modifier=Modifier.fillMaxHeight()
                                ) {
                                    Box(
                                        modifier=Modifier.fillMaxWidth().height(3.dp).background(indicatorColor)
                                    )
                                    Spacer(modifier=Modifier.weight(1f))
                                    Text(
                                        text=tab.second,
                                        fontWeight=if(isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color=if(isSelected) AppColors.PrimaryBlue else textSecondary,
                                        fontSize=13.sp
                                    )
                                    Spacer(modifier=Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) {paddingValues->
        PullToRefreshBox(
            isRefreshing=state.isRefreshing,
            onRefresh={
                viewModel.handleIntent(DigitalBankIntent.RefreshCustomers)
                pagingItems.refresh()
            },
            modifier=Modifier.fillMaxSize().padding(paddingValues)
        ) {
            if(pagingItems.loadState.refresh is LoadState.Loading) {
                CircularProgressIndicator(modifier=Modifier.align(Alignment.Center),color=AppColors.PrimaryBlue)
            } else {
                if(pagingItems.itemCount==0) {
                    Column(
                        modifier=Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement=Arrangement.Center,
                        horizontalAlignment=Alignment.CenterHorizontally
                    ) {
                        Text(
                            text=stringResource(R.string.no_accounts_found),
                            fontSize=18.sp,
                            fontWeight=FontWeight.Bold,
                            color=textPrimary
                        )
                        Spacer(modifier=Modifier.height(4.dp))
                        Text(
                            text=stringResource(R.string.empty_accounts_message),
                            fontSize=14.sp,
                            color=textSecondary,
                            textAlign=TextAlign.Center
                        )
                    }
                } else {
                    LazyVerticalGrid(
                        columns=GridCells.Fixed(2),
                        contentPadding=PaddingValues(12.dp),
                        horizontalArrangement=Arrangement.spacedBy(12.dp),
                        verticalArrangement=Arrangement.spacedBy(12.dp),
                        modifier=Modifier.fillMaxSize()
                    ) {
                        items(
                            count=pagingItems.itemCount,
                            key={index->pagingItems[index]?.id?:index}
                        ){index->
                            pagingItems[index]?.let{customer->
                                CustomerCard(
                                    customer=customer,
                                    isDarkTheme=isDark,
                                    onClick={onCustomerClick(customer)},
                                    onDoKyc={onDoKyc(customer.id)},
                                    modifier=Modifier.animateItem()
                                )
                            }
                        }
                        if(pagingItems.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier=Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment=Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier=Modifier.size(24.dp),strokeWidth=2.dp,color=AppColors.PrimaryBlue)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerCard(
    customer:Customer,
    isDarkTheme:Boolean,
    onClick:()->Unit,
    onDoKyc:()->Unit,
    modifier:Modifier=Modifier
) {
    val context=LocalContext.current
    val cardColor=if(isDarkTheme) AppColors.DarkSurface else Color.White
    val textPrimary=if(isDarkTheme) AppColors.DarkText else AppColors.LightText
    val textSecondary=if(isDarkTheme) AppColors.DarkMutedText else AppColors.LightMutedText
    Card(
        modifier=modifier.fillMaxWidth().clickable{onClick()},
        shape=RoundedCornerShape(16.dp),
        colors=CardDefaults.cardColors(containerColor=cardColor),
        elevation=CardDefaults.cardElevation(defaultElevation=2.dp)
    ) {

        Column(modifier=Modifier.padding(12.dp)) {
            Row(
                modifier=Modifier.fillMaxWidth(),
                horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.Top
            ) {

                AsyncImage(
                    model=ImageRequest.Builder(context)
                        .data(customer.selfiePath?:customer.avatar)
                        .crossfade(true)
                        .build(),
                    contentDescription=stringResource(R.string.avatar),
                    modifier=Modifier
                        .size(48.dp).clip(CircleShape)
                        .background(if(isDarkTheme) AppColors.DarkBorder else AppColors.LightChip),
                    contentScale=ContentScale.Crop
                )

                val badgeColor=
                    if(customer.kycStatus=="VERIFIED") (
                            if(isDarkTheme) AppColors.VerifiedBadgeDark
                            else AppColors.VerifiedBadgeLight)
                    else (
                            if(isDarkTheme) AppColors.PendingBadgeDark
                            else AppColors.PendingBadgeLight
                    )

                val badgeTextColor=
                    if(customer.kycStatus=="VERIFIED") (
                            if(isDarkTheme) AppColors.VerifiedTextDark
                            else AppColors.VerifiedTextLight)
                    else (
                            if(isDarkTheme) AppColors.PendingTextDark
                            else AppColors.PendingTextLight
                    )

                Box(
                    modifier=Modifier.clip(RoundedCornerShape(8.dp)).background(badgeColor).padding(horizontal=6.dp,vertical=3.dp)
                ) {
                    Text(
                        text=customer.kycStatus,
                        fontSize=10.sp,
                        fontWeight=FontWeight.Bold,
                        color=badgeTextColor
                    )
                }
            }
            Spacer(modifier=Modifier.height(12.dp))
            Text(
                text=customer.name,
                fontWeight=FontWeight.Bold,
                fontSize=15.sp,
                color=textPrimary,
                maxLines=1
            )
            Text(
                text=customer.maskedAccount,
                fontSize=12.sp,
                color=textSecondary
            )
            Spacer(modifier=Modifier.height(10.dp))
            Text(
                text=stringResource(R.string.account_balance,customer.currency,String.format("%,.0f",customer.balance)),
                fontWeight=FontWeight.ExtraBold,
                fontSize=16.sp,
                color=textPrimary
            )
            if(customer.kycStatus=="PENDING") {
                Spacer(modifier=Modifier.height(8.dp))
                Button(
                    onClick={onDoKyc()},
                    modifier=Modifier.fillMaxWidth().height(36.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=AppColors.PrimaryBlue),
                    shape=RoundedCornerShape(8.dp),
                    contentPadding=PaddingValues(0.dp)
                ) {
                    Text(stringResource(R.string.do_kyc),fontSize=12.sp,color=Color.White,fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}