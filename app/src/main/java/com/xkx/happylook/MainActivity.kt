package com.xkx.happylook

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.xkx.happylook.core.TargetFactory
import com.xkx.happylook.core.ViewerTarget
import com.xkx.happylook.ui.HappyLookRoot
import com.xkx.happylook.ui.theme.HappyLookTheme

class MainActivity : ComponentActivity() {

    /** 外部送进来的文件。用 State 承载，onNewIntent 时更新后界面会自动响应。 */
    private val incoming = mutableStateOf<List<ViewerTarget>>(emptyList())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incoming.value = TargetFactory.fromIntent(this, intent)
        setContent {
            HappyLookTheme {
                HappyLookRoot(
                    incomingTargets = incoming.value,
                    onIncomingConsumed = { incoming.value = emptyList() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targets = TargetFactory.fromIntent(this, intent)
        if (targets.isNotEmpty()) incoming.value = targets
    }
}
