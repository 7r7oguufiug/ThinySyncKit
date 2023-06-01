package com.hi.dhl.action

import com.hi.dhl.action.base.AbstractAnAction
import com.hi.dhl.action.listener.BuildProcessListener
import com.hi.dhl.common.R
import com.hi.dhl.console.CommandManager
import com.hi.dhl.utils.LogUtils
import com.hi.dhl.utils.MessagesUtils
import com.hi.dhl.utils.StringUtils
import com.hi.dhl.utils.TimeUtils
import com.intellij.execution.process.ProcessEvent
import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/**
 * 远程编译 CI 模式
 */
class PublishProjectAnAction : AbstractAnAction(R.String.ui.actionPublishProject) {

    override fun afterActionPerformed(project: Project) {
        var extraCommand = ""
        if (remoteMachineInfo.isSelectGradlew()) {
            extraCommand = "./gradlew "
        }
        if (remoteMachineInfo.remotePublishCommand.isNullOrEmpty()) {
            val warringTitle = StringUtils.getMessage("sync.init.warring.title")
            MessagesUtils.showMessageWarnDialog(
                warringTitle, StringUtils.getMessage("sync.service.empry.command")
            )
            return
        }

        extraCommand += remoteMachineInfo.remotePublishCommand.toString()
        val commands = StringBuilder()

        //Publish 不同步结果回来
        CommandManager.publishAndroid(
            commands,
            extraCommand,
            projectBasePath,
            remoteMachineInfo
        )
        var startTime: Long = 0
        val consoleTitle = "${R.String.projectTitle} [ ${remoteMachineInfo.remotePublishCommand} ]"

        execSyncRunnerConsole(project, projectBasePath, commands.toString(), consoleTitle, object :
            BuildProcessListener {
            override fun onStart(time: Long) {
                startTime = time
            }

            override fun onStop(processEvent: ProcessEvent, endTime: Long) {
                val execTime = TimeUtils.formatTime(
                    startTime = startTime,
                    endTime = endTime
                )
                when (processEvent.exitCode) {
                    0 -> {
                        val content = "BUILD SUCCESSFUL in $execTime"
                        LogUtils.log(
                            content,
                            NotificationDisplayType.BALLOON,
                            NotificationType.INFORMATION,
                            project
                        )
                    }

                    else -> {
                        var errorTip = "BUILD FAILED in ${execTime}, error code ${processEvent.exitCode}"
                        if (!processEvent.text.isNullOrEmpty()) {
                            errorTip += ", ${processEvent.text}"
                        }
                        LogUtils.log(
                            errorTip,
                            NotificationDisplayType.BALLOON,
                            NotificationType.WARNING,
                            project
                        )
                    }
                }
            }
        })
    }
}