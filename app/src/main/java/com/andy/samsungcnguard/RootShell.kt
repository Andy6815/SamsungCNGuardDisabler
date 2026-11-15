package com.andy.samsungcnguard

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

data class ShellResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String
)

object RootShell {

    fun hasRoot(): Boolean {
        val result = exec("id")
        return result.success && result.stdout.contains("uid=0")
    }

    fun exec(command: String): ShellResult {
        return execBatch(listOf(command))
    }

    fun execBatch(commands: List<String>): ShellResult {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                for (cmd in commands) {
                    os.writeBytes("$cmd\n")
                }
                os.writeBytes("exit\n")
                os.flush()
            }

            val stdout = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }
            val exitCode = process.waitFor()
            ShellResult(exitCode == 0, stdout.trim(), stderr.trim())
        } catch (e: Exception) {
            ShellResult(false, "", e.message ?: "Unknown error")
        } finally {
            process?.destroy()
        }
    }

    fun setPackageEnabled(packageName: String, enabled: Boolean): ShellResult {
        val cmd = if (enabled) {
            "pm enable $packageName"
        } else {
            "pm disable-user --user 0 $packageName"
        }
        return exec(cmd)
    }

    fun disableAll(packages: List<String>): ShellResult {
        val cmds = mutableListOf<String>()
        for (pkg in packages) {
            cmds.add("pm disable-user --user 0 $pkg 2>/dev/null || true")
        }
        cmds.addAll(SecurityComponents.disableSettingsCommands.map { "$it 2>/dev/null || true" })
        return execBatch(cmds)
    }

    fun enableAll(packages: List<String>): ShellResult {
        val cmds = packages.map { "pm enable $it 2>/dev/null || true" }
        return execBatch(cmds)
    }
}
