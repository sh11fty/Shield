package com.guardian.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock

/** Two independent heartbeats so a killed process is revived: a 15 min persisted job and a 5 min alarm chain. */
object Scheduler {
    private const val JOB_ID = 4711

    fun scheduleAll(c: Context) {
        val ctx = c.applicationContext
        runCatching {
            val js = ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            if (js.getPendingJob(JOB_ID) == null) {
                js.schedule(
                    JobInfo.Builder(JOB_ID, ComponentName(ctx, EnforceJobService::class.java))
                        .setPeriodic(15 * 60 * 1000L)
                        .setPersisted(true)
                        .build()
                )
            }
        }
        scheduleAlarm(ctx)
    }

    fun scheduleAlarm(c: Context) {
        val ctx = c.applicationContext
        runCatching {
            val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                ctx, 2, Intent(ctx, AlarmReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 5 * 60 * 1000L, pi)
        }
    }
}

class EnforceJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            try { Enforcer.apply(applicationContext, "job") } catch (_: Throwable) { }
            jobFinished(params, false)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true
}

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        Scheduler.scheduleAlarm(context)
        Thread {
            try { Enforcer.apply(context, "alarm") } catch (_: Throwable) { }
            pending.finish()
        }.start()
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Scheduler.scheduleAll(context)
        Enforcer.applyAsync(context, intent.action ?: "boot")
    }
}
