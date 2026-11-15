package com.andy.samsungcnguard

data class SecurityComponent(
    val packageName: String,
    val title: String,
    val description: String,
    val category: Category,
    val disableByDefault: Boolean = true
) {
    enum class Category(val label: String) {
        CORE_SECURITY("核心安全与杀毒引擎"),
        POLICY_AND_KNOX("安全策略与远程控制"),
        CLOUD_CONFIG("云端策略下发")
    }
}

object SecurityComponents {

    /**
     * 可通过 pm disable-user --user 0 安全禁用的三星国行安全组件列表
     * 注意：智能管理器 (com.samsung.android.sm_cn) 和 安装器 (com.samsung.android.packageinstaller)
     * 不能直接 pm disable（否则影响电池设置或无法安装 APK），它们由 XposedInit 在运行时 Hook 绕过检测。
     */
    val disableTargets: List<SecurityComponent> = listOf(
        SecurityComponent(
            packageName = "com.samsung.android.sm.devicesecurity.tcm",
            title = "设备安全（腾讯安全引擎）",
            description = "国行智能管理器内置的腾讯病毒查杀与安装扫描引擎",
            category = SecurityComponent.Category.CORE_SECURITY
        ),
        SecurityComponent(
            packageName = "com.samsung.android.sm.devicesecurity",
            title = "设备安全（McAfee 引擎）",
            description = "部分版本固件附带的 McAfee 设备保护扫描组件",
            category = SecurityComponent.Category.CORE_SECURITY
        ),
        SecurityComponent(
            packageName = "com.samsung.android.rampart",
            title = "自动拦截程序（Auto Blocker）",
            description = "拦截第三方安装包、ADB 命令及 USB 调试的高权限安全组件",
            category = SecurityComponent.Category.CORE_SECURITY
        ),
        SecurityComponent(
            packageName = "com.samsung.android.spdclient",
            title = "安全策略更新（SPD Client）",
            description = "定期从三星服务器拉取 SELinux 与安全拦截策略",
            category = SecurityComponent.Category.POLICY_AND_KNOX
        ),
        SecurityComponent(
            packageName = "com.policydm",
            title = "安全策略更新（PolicyDM）",
            description = "旧版/兼容层安全策略下发组件",
            category = SecurityComponent.Category.POLICY_AND_KNOX
        ),
        SecurityComponent(
            packageName = "com.samsung.android.sm.policy",
            title = "智能管理器安全策略",
            description = "国行智能管理器权限与后台安全规则策略组件",
            category = SecurityComponent.Category.POLICY_AND_KNOX
        ),
        SecurityComponent(
            packageName = "com.samsung.klmsagent",
            title = "Knox 许可证代理（KLMS）",
            description = "Knox 授权校验与企业安全策略代理",
            category = SecurityComponent.Category.POLICY_AND_KNOX
        ),
        SecurityComponent(
            packageName = "com.samsung.android.knox.attestation",
            title = "Knox 完整性认证",
            description = "向系统及应用提供 Knox 设备完整性校验状态",
            category = SecurityComponent.Category.POLICY_AND_KNOX
        ),
        SecurityComponent(
            packageName = "com.samsung.android.app.omcagent",
            title = "OMC 运营商/国行配置代理",
            description = "负责云端下发国行 CSC 特性与区域限制策略（如无需系统更新配置可关闭）",
            category = SecurityComponent.Category.CLOUD_CONFIG,
            disableByDefault = false
        )
    )

    val protectedPackages: Set<String> = disableTargets.map { it.packageName }.toSet()

    /**
     * 关闭系统设置中与自动拦截程序/安装检测相关的开关
     */
    val disableSettingsCommands: List<String> = listOf(
        "settings put global rampart_blocked_adb_cmd 0",
        "settings put secure rampart_blocked_adb_cmd 0",
        "settings put global auto_blocker_enabled 0",
        "settings put secure auto_blocker_enabled 0",
        "settings put global maximum_restrictions_enabled 0",
        "settings put secure maximum_restrictions_enabled 0"
    )
}
