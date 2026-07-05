package `in`.mato.signzy
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import `in`.mato.signzy.ui.theme.SignZyTheme
import `in`.mato.signzy.ui.digitalBank.DigitalBankApp
import `in`.mato.signzy.ui.digitalBank.DigitalBankViewModel

@AndroidEntryPoint
class MainActivity:ComponentActivity() {
    private val viewModel:DigitalBankViewModel by viewModels()
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SignZyTheme {
                DigitalBankApp(viewModel=viewModel)
            }
        }
    }
}