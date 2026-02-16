package com.neogenesis.biokernel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import com.neogenesis.biokernel.navigation.BioKernelNavGraph
import com.neogenesis.components.theme.BioKernelTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BioKernelTheme {
                Surface {
                    BioKernelNavGraph()
                }
            }
        }
    }
}