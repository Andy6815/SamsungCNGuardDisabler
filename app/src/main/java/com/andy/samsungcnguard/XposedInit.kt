package com.andy.samsungcnguard

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XC_MethodReplacement
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

class XposedInit : IXposedHookLoadPackage {

    companion object {
        private const val TAG = "[SamsungCNGuard]"
        private const val SELF_PKG = "com.andy.samsungcnguard"
    }

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        when (lpparam.packageName) {
            SELF_PKG -> hookSelf(lpparam)
            "android" -> hookSystemFramework(lpparam)
            "com.samsung.android.rampart" -> hookAutoBlocker(lpparam)
            "com.samsung.android.sm.devicesecurity.tcm",
            "com.samsung.android.sm.devicesecurity" -> hookDeviceSecurity(lpparam)
            "com.samsung.android.sm_cn" -> hookSmartManagerCn(lpparam)
            "com.samsung.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.packageinstaller" -> hookPackageInstaller(lpparam)
        }
    }

    private fun hookSelf(lpparam: XC_LoadPackage.LoadPackageParam) {
        runCatching {
            XposedHelpers.findAndHookMethod(
                "$SELF_PKG.MainActivity",
                lpparam.classLoader,
                "isModuleActive",
                XC_MethodReplacement.returnConstant(true)
            )
        }
    }

    /**
     * 系统框架层 Hook：
     * 1. 屏蔽国行安装安全检测相关的 CSC 特性开关
     * 2. 防止 One UI 自动重新启用已被禁用的自动拦截程序 (rampart) 等组件
     */
    private fun hookSystemFramework(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookCscFeature(lpparam.classLoader)
        hookPackageReEnableGuard(lpparam.classLoader)
    }

    private fun hookCscFeature(classLoader: ClassLoader) {
        val cscClass = XposedHelpers.findClassIfExists(
            "com.samsung.android.feature.SemCscFeature",
            classLoader
        ) ?: return

        val blockedBooleanKeys = setOf(
            "CscFeature_Common_SupportChinaAppSecurity",
            "CscFeature_SmartManager_ConfigDashboard",
            "CscFeature_Setting_SupportAutoBlocker"
        )

        runCatching {
            XposedBridge.hookAllMethods(cscClass, "getBoolean", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val key = param.args.firstOrNull() as? String ?: return
                    if (key in blockedBooleanKeys || key.contains("ChinaAppSecurity", ignoreCase = true)) {
                        param.result = false
                    }
                }
            })
        }.onFailure {
            XposedBridge.log("$TAG SemCscFeature.getBoolean hook failed: ${it.message}")
        }
    }

    private fun hookPackageReEnableGuard(classLoader: ClassLoader) {
        // 阻止系统后台服务私自把已禁用的国行安全组件重新设为 ENABLED (1)
        val pmsClass = XposedHelpers.findClassIfExists(
            "com.android.server.pm.PackageManagerService",
            classLoader
        ) ?: return

        runCatching {
            XposedBridge.hookAllMethods(pmsClass, "setApplicationEnabledSetting", object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val pkg = param.args.getOrNull(0) as? String ?: return
                    val newState = param.args.getOrNull(1) as? Int ?: return
                    val callingPkg = param.args.lastOrNull() as? String
                    // COMPONENT_ENABLED_STATE_ENABLED == 1, COMPONENT_ENABLED_STATE_DEFAULT == 0
                    if (pkg in SecurityComponents.protectedPackages &&
                        (newState == 1 || newState == 0) &&
                        callingPkg != SELF_PKG &&
                        callingPkg != "com.android.shell"
                    ) {
                        XposedBridge.log("$TAG Blocked auto re-enable of $pkg by $callingPkg")
                        param.result = null
                    }
                }
            })
        }.onFailure {
            XposedBridge.log("$TAG PMS guard hook skipped: ${it.message}")
        }
    }

    /**
     * Hook 自动拦截程序 (com.samsung.android.rampart)
     */
    private fun hookAutoBlocker(lpparam: XC_LoadPackage.LoadPackageParam) {
        val keywords = listOf("isAutoBlocker", "isMaximumRestriction", "isRampartEnabled", "isBlocked")
        hookBooleanMethodsByPrefix(lpparam.classLoader, "com.samsung.android.rampart", keywords, false)
    }

    /**
     * Hook 腾讯设备安全 / McAfee 设备安全组件，使其直接返回安全状态并跳过扫描
     */
    private fun hookDeviceSecurity(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedBridge.log("$TAG Loaded in ${lpparam.packageName}, neutralizing security scanner hooks")
        hookCscFeature(lpparam.classLoader)
    }

    /**
     * Hook 国行智能管理器 (com.samsung.android.sm_cn)，屏蔽安全检测与风险提示
     */
    private fun hookSmartManagerCn(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookCscFeature(lpparam.classLoader)
    }

    /**
     * Hook 系统安装器 (com.samsung.android.packageinstaller / google / aosp)
     * 绕过国行安装器的腾讯安全云端检测、风险应用拦截与广告提示
     */
    private fun hookPackageInstaller(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookCscFeature(lpparam.classLoader)

        val candidateClasses = listOf(
            "com.samsung.android.packageinstaller.util.SecurityCheckUtil",
            "com.samsung.android.packageinstaller.security.SecurityCheckHelper",
            "com.android.packageinstaller.sec.SecurityCheckManager",
            "com.android.packageinstaller.InstallStaging"
        )

        for (clsName in candidateClasses) {
            val cls = XposedHelpers.findClassIfExists(clsName, lpparam.classLoader) ?: continue
            for (method in cls.declaredMethods) {
                if (method.returnType == Boolean::class.javaPrimitiveType ||
                    method.returnType == java.lang.Boolean::class.java
                ) {
                    val name = method.name.lowercase()
                    if (name.contains("security") || name.contains("scan") || name.contains("risk") || name.contains("china")) {
                        runCatching {
                            XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(false))
                            XposedBridge.log("$TAG Hooked $clsName.${method.name} -> false")
                        }
                    }
                }
            }
        }
    }

    private fun hookBooleanMethodsByPrefix(
        classLoader: ClassLoader,
        packagePrefix: String,
        methodPrefixes: List<String>,
        returnValue: Boolean
    ) {
        // 仅在已知入口存在时安全 Hook，避免遍历全部类导致启动耗时
        val knownClasses = listOf(
            "$packagePrefix.RampartManager",
            "$packagePrefix.AutoBlockerManager",
            "$packagePrefix.util.AutoBlockerUtil",
            "$packagePrefix.provider.RampartProvider"
        )
        for (clsName in knownClasses) {
            val cls = XposedHelpers.findClassIfExists(clsName, classLoader) ?: continue
            for (method in cls.declaredMethods) {
                if ((method.returnType == Boolean::class.javaPrimitiveType ||
                            method.returnType == java.lang.Boolean::class.java) &&
                    methodPrefixes.any { method.name.startsWith(it, ignoreCase = true) }
                ) {
                    runCatching {
                        XposedBridge.hookMethod(method, XC_MethodReplacement.returnConstant(returnValue))
                        XposedBridge.log("$TAG Hooked $clsName.${method.name} -> $returnValue")
                    }
                }
            }
        }
    }
}
