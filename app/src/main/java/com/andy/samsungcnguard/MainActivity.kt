package com.andy.samsungcnguard

import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var listContainer: LinearLayout
    private var hasRootAccess = false

    // 由 XposedInit 自 Hook 返回 true
    fun isModuleActive(): Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootScroll = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#F4F6F9"))
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(32))
        }

        // 标题
        val titleView = TextView(this).apply {
            text = "三星国行安全组件一键关闭"
            textSize = 22f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A1C1E"))
        }
        val subtitleView = TextView(this).apply {
            text = "LSPosed 运行时屏蔽 + Root 一键冻结系统安全包"
            textSize = 13f
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(0, dp(4), 0, dp(16))
        }
        rootLayout.addView(titleView)
        rootLayout.addView(subtitleView)

        // 状态卡片
        val statusCard = createCard()
        val statusInner = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }
        statusText = TextView(this).apply {
            text = "正在检测 LSPosed 与 Root 状态..."
            textSize = 14f
            setTextColor(Color.parseColor("#1A1C1E"))
            setLineSpacing(dp(4).toFloat(), 1f)
        }
        statusInner.addView(statusText)
        statusCard.addView(statusInner)
        rootLayout.addView(statusCard)

        // 一键操作按钮栏
        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(12), 0, dp(16))
        }

        val disableAllBtn = MaterialButton(this).apply {
            text = "一键关闭全部"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(8)
            }
            setOnClickListener { runBatchAction(disable = true) }
        }

        val enableAllBtn = MaterialButton(
            this,
            null,
            com.google.android.material.R.attr.materialButtonOutlinedStyle
        ).apply {
            text = "一键恢复默认"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(8)
            }
            setOnClickListener { runBatchAction(disable = false) }
        }

        actionRow.addView(disableAllBtn)
        actionRow.addView(enableAllBtn)
        rootLayout.addView(actionRow)

        // 说明提示
        val noteView = TextView(this).apply {
            text = "说明：智能管理器 (sm_cn) 与安装器 (packageinstaller) 由 LSPosed 在运行时拦截安全检测，不会冻结其包体，避免影响正常安装软件与电池管理。"
            textSize = 12f
            setTextColor(Color.parseColor("#5F6368"))
            setPadding(dp(4), 0, dp(4), dp(12))
        }
        rootLayout.addView(noteView)

        // 组件列表容器
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootLayout.addView(listContainer)

        rootScroll.addView(rootLayout)
        setContentView(rootScroll)

        refreshAll()
    }

    private fun refreshAll() {
        thread {
            val rootOk = RootShell.hasRoot()
            val lsposedOk = isModuleActive()
            runOnUiThread {
                hasRootAccess = rootOk
                val lspLabel = if (lsposedOk) "已激活" else "未激活（请在 LSPosed 中勾选本模块及建议作用域）"
                val rootLabel = if (rootOk) "已授权" else "未授权（需要 Root 权限执行一键冻结）"
                statusText.text = "LSPosed 模块状态：$lspLabel\nRoot (su) 权限：$rootLabel"
                renderComponentList()
            }
        }
    }

    private fun renderComponentList() {
        listContainer.removeAllViews()
        for (item in SecurityComponents.disableTargets) {
            val installed = isPackageInstalled(item.packageName)
            val enabled = if (installed) isPackageEnabled(item.packageName) else false

            val card = createCard().apply {
                val lp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = dp(10)
                layoutParams = lp
            }

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(16), dp(14), dp(16), dp(14))
            }

            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                    marginEnd = dp(12)
                }
            }

            val title = TextView(this).apply {
                text = item.title
                textSize = 15f
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor("#1A1C1E"))
            }

            val pkgText = TextView(this).apply {
                val stateLabel = when {
                    !installed -> "未安装"
                    enabled -> "运行中"
                    else -> "已关闭"
                }
                text = "${item.packageName}  •  $stateLabel"
                textSize = 12f
                setTextColor(
                    when {
                        !installed -> Color.parseColor("#9AA0A6")
                        enabled -> Color.parseColor("#C5221F")
                        else -> Color.parseColor("#137333")
                    }
                )
                setPadding(0, dp(2), 0, dp(4))
            }

            val desc = TextView(this).apply {
                text = item.description
                textSize = 12f
                setTextColor(Color.parseColor("#5F6368"))
            }

            textCol.addView(title)
            textCol.addView(pkgText)
            textCol.addView(desc)

            val toggle = MaterialSwitch(this).apply {
                isEnabled = installed
                // 开关打开 = 已关闭（安全组件已禁用）
                isChecked = installed && !enabled
                setOnCheckedChangeListener { buttonView, isChecked ->
                    if (!buttonView.isPressed) return@setOnCheckedChangeListener
                    if (!hasRootAccess) {
                        buttonView.isChecked = !isChecked
                        Toast.makeText(this@MainActivity, "请先授予 Root 权限", Toast.LENGTH_SHORT).show()
                        return@setOnCheckedChangeListener
                    }
                    thread {
                        val res = RootShell.setPackageEnabled(item.packageName, enabled = !isChecked)
                        runOnUiThread {
                            if (!res.success) {
                                Toast.makeText(
                                    this@MainActivity,
                                    "操作失败：${res.stderr.ifEmpty { "请检查 Root 权限" }}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            renderComponentList()
                        }
                    }
                }
            }

            row.addView(textCol)
            row.addView(toggle)
            card.addView(row)
            listContainer.addView(card)
        }
    }

    private fun runBatchAction(disable: Boolean) {
        if (!hasRootAccess) {
            Toast.makeText(this, "未检测到 Root 权限，请在 Magisk/KernelSU 中授权", Toast.LENGTH_SHORT).show()
            return
        }
        val targets = SecurityComponents.disableTargets
            .filter { (!disable || it.disableByDefault) && isPackageInstalled(it.packageName) }
            .map { it.packageName }

        if (targets.isEmpty()) {
            Toast.makeText(this, "当前设备未检测到可操作的目标组件", Toast.LENGTH_SHORT).show()
            return
        }

        thread {
            val res = if (disable) RootShell.disableAll(targets) else RootShell.enableAll(targets)
            runOnUiThread {
                val msg = if (res.success) {
                    if (disable) "已一键关闭 ${targets.size} 项国行安全组件" else "已恢复 ${targets.size} 项组件"
                } else {
                    "部分操作失败：${res.stderr}"
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
                renderComponentList()
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, PackageManager.MATCH_DISABLED_COMPONENTS)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun isPackageEnabled(packageName: String): Boolean {
        return try {
            val state = packageManager.getApplicationEnabledSetting(packageName)
            state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED &&
                state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER &&
                state != PackageManager.COMPONENT_ENABLED_STATE_DISABLED_UNTIL_USED
        } catch (_: Exception) {
            true
        }
    }

    private fun createCard(): MaterialCardView {
        return MaterialCardView(this).apply {
            radius = dp(16).toFloat()
            cardElevation = dp(1).toFloat()
            setCardBackgroundColor(Color.WHITE)
            strokeWidth = dp(1)
            strokeColor = Color.parseColor("#E0E3E7")
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
