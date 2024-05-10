package jp.techacademy.otowa.kimura.taskapp

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.graphics.BitmapFactory
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val notificationManager =
            context!!.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // SDKバージョンが26以上の場合、チャネルを設定する必要がある
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "default",
                "Channel name",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = "Channel description"
            notificationManager.createNotificationChannel(channel)
        }

        // 通知の設定を行う
        val builder = NotificationCompat.Builder(context, "default")
        builder.setSmallIcon(R.drawable.small_icon)
        builder.setLargeIcon(BitmapFactory.decodeResource(context.resources, R.drawable.large_icon))
        builder.setWhen(System.currentTimeMillis())
        builder.setDefaults(Notification.DEFAULT_ALL)
        builder.setAutoCancel(true)

        // EXTRA_TASKからTaskのidを取得して、 idからTaskのインスタンスを取得する
        val taskId = intent!!.getIntExtra(EXTRA_TASK, -1)
        // Realmデータベースとの接続を開く
        val config = RealmConfiguration.create(schema = setOf(Task::class))
        val realm = Realm.open(config)
        // 引数のtaskIdに合致するタスクを検索
        var task = realm.query<Task>("id==$taskId").first().find()

        // タスクが見つからなかった場合の処理
        if (task == null) {
            task = Task()
            task.title = "task(id:$taskId) not found"
            Log.e("TaskApp", task.title)
        }

        // タスクの情報を設定する
        builder.setTicker(task.title)   // 5.0以降は表示されない
        builder.setContentTitle(task.title)
        builder.setContentText(task.contents)

        // 通知をタップしたらアプリを起動するようにする
        val startAppIntent = Intent(context, MainActivity::class.java)
        startAppIntent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT)
        val pendingIntent =
            PendingIntent.getActivity(context, 0, startAppIntent, PendingIntent.FLAG_IMMUTABLE)
        builder.setContentIntent(pendingIntent)

        // 通知を表示する
        notificationManager.notify(task.id, builder.build())

        // taskオブジェクトが無効になってしまうため、Realmデータベースとの接続は最後に閉じる
        realm.close()
    }
}
