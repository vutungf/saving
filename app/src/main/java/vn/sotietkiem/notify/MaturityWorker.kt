package vn.sotietkiem.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import vn.sotietkiem.MainActivity
import vn.sotietkiem.R
import vn.sotietkiem.data.Repository
import vn.sotietkiem.domain.Calc
import vn.sotietkiem.util.Fmt
import java.util.concurrent.TimeUnit

/** Kiểm tra mỗi ngày: nhắc trước 3 ngày và vào ngày đáo hạn. Chạy hoàn toàn trên máy. */
class MaturityWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val repo = Repository(applicationContext)
        if (!repo.remindersEnabled) return Result.success()
        val notified = repo.notified.toMutableSet()
        val open = repo.load().filter { !it.closed }
        val liveIds = open.map { it.id }.toSet()
        notified.retainAll { it.substringBefore('|') in liveIds }

        for (a in open) {
            val left = Calc.daysLeft(a)
            val (key, text) = when {
                left <= 0 -> "${a.id}|due" to "Đã đến ngày đáo hạn (${Fmt.date(Calc.maturity(a))}). Nhận về ${Fmt.vnd(Calc.total(a))}."
                left <= REMIND_DAYS -> "${a.id}|soon" to "Còn $left ngày nữa đáo hạn (${Fmt.date(Calc.maturity(a))})."
                else -> continue
            }
            if (key in notified) continue
            if (show(applicationContext, a.id.hashCode(), "${a.name} · ${a.bankName}", text)) notified += key
        }
        repo.notified = notified
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "maturity"
        private const val WORK = "maturity-check"
        const val REMIND_DAYS = 3

        fun schedule(ctx: Context) {
            createChannel(ctx)
            val req = PeriodicWorkRequestBuilder<MaturityWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
        }

        private fun createChannel(ctx: Context) {
            val ch = NotificationChannel(CHANNEL, "Nhắc đáo hạn", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Báo trước khi sổ tiết kiệm đến ngày đáo hạn"
            }
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }

        private fun show(ctx: Context, id: Int, title: String, text: String): Boolean {
            if (Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) return false
            val pi = PendingIntent.getActivity(
                ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val n = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .build()
            return runCatching { NotificationManagerCompat.from(ctx).notify(id, n); true }.getOrDefault(false)
        }
    }
}
