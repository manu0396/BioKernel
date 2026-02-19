package com.neogenesis.biokernel

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.fragment.app.FragmentActivity
import com.neogenesis.biokernel.navigation.BioKernelNavGraph
import com.neogenesis.components.theme.BioKernelTheme
import org.koin.androidx.compose.KoinAndroidContext

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KoinAndroidContext {
                BioKernelTheme {
                    Surface {
                        BioKernelNavGraph()
                    }
                }
            }
        }
    }
}