package com.example.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object AppIconHelper {

    const val ICON_NEW = "new"
    const val ICON_CLASSIC = "classic"

    fun applyLauncherIcon(context: Context, iconStyle: String) {
        try {
            val pm = context.packageManager
            val pkg = context.packageName

            val aliasNew = ComponentName(pkg, "$pkg.MainActivityNew")
            val aliasClassic = ComponentName(pkg, "$pkg.MainActivityClassic")

            val useNew = iconStyle == ICON_NEW

            val targetEnable = if (useNew) aliasNew else aliasClassic
            val targetDisable = if (useNew) aliasClassic else aliasNew

            val enableState = pm.getComponentEnabledSetting(targetEnable)
            if (enableState != PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
                pm.setComponentEnabledSetting(
                    targetEnable,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }

            val disableState = pm.getComponentEnabledSetting(targetDisable)
            if (disableState != PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                pm.setComponentEnabledSetting(
                    targetDisable,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
