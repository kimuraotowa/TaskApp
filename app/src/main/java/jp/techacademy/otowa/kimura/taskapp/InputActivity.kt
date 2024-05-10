package jp.techacademy.otowa.kimura.taskapp

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.realm.kotlin.Realm
import io.realm.kotlin.RealmConfiguration
import io.realm.kotlin.ext.query
import jp.techacademy.otowa.kimura.taskapp.databinding.ActivityInputBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*


class InputActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInputBinding

    private lateinit var realm: Realm
    private lateinit var task: Task
    private var calendar = Calendar.getInstance(Locale.JAPANESE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // アクションバーの設定
        setSupportActionBar(binding.toolbar)
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }

        // ボタンのイベントリスナーの設定
        binding.content.dateButton.setOnClickListener(dateClickListener)
        binding.content.timeButton.setOnClickListener(timeClickListener)
        binding.content.doneButton.setOnClickListener(doneClickListener)

        // EXTRA_TASKからTaskのidを取得
        val intent = intent
        val taskId = intent.getIntExtra(EXTRA_TASK, -1)

        // Realmデータベースとの接続を開く
        val config = RealmConfiguration.create(schema = setOf(Task::class))
        realm = Realm.open(config)

        // タスクを取得または初期化
        initTask(taskId)
    }

    override fun onDestroy() {
        super.onDestroy()

        // Realmデータベースとの接続を閉じる
        realm.close()
    }

    /**
     * 日付選択ボタン
     */
    private val dateClickListener = View.OnClickListener {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(year, month, day)
                setDateTimeButtonText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    /**
     * 時刻選択ボタン
     */
    private val timeClickListener = View.OnClickListener {
        val timePickerDialog = TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                setDateTimeButtonText()
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        )
        timePickerDialog.show()
    }

    /**
     * 決定ボタン
     */
    private val doneClickListener = View.OnClickListener {
        CoroutineScope(Dispatchers.Default).launch {
            addTask()
            finish()
        }
    }

    /**
     * タスクを取得または初期化
     */
    private fun initTask(taskId: Int) {
        // 引数のtaskIdに合致するタスクを検索
        val findTask = realm.query<Task>("id==$taskId").first().find()

        if (findTask == null) {
            // 新規作成の場合
            task = Task()
            task.id = -1

            // 日付の初期値を1日後に設定
            calendar.add(Calendar.DAY_OF_MONTH, 1)

        } else {
            // 更新の場合
            task = findTask

            // taskの日時をcalendarに反映
            val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)
            calendar.time = simpleDateFormat.parse(task.date) as Date

            // taskの値を画面項目に反映
            binding.content.titleEditText.setText(task.title)
            binding.content.contentEditText.setText(task.contents)
        }

        // 日付と時刻のボタンの表示を設定
        setDateTimeButtonText()
    }

    /**
     * タスクの登録または更新を行う
     */
    private suspend fun addTask() {
        // 日付型オブジェクトを文字列に変換用
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.JAPANESE)

        // 登録（更新）する値を取得
        val title = binding.content.titleEditText.text.toString()
        val content = binding.content.contentEditText.text.toString()
        val date = simpleDateFormat.format(calendar.time)
        val category = binding.content.categoryEditText.text.toString()

        if (task.id == -1) {
            // 登録

            // 最大のid+1をセット
            task.id = (realm.query<Task>().max("id", Int::class).find() ?: -1) + 1
            // 画面項目の値で更新
            task.title = title
            task.contents = content
            task.date = date

            // 登録処理
            realm.writeBlocking {
                copyToRealm(task)
            }
        } else {
            // 更新
            realm.write {
                findLatest(task)?.apply {
                    // 画面項目の値で更新
                    this.title = title
                    this.contents = content
                    this.date = date
                    this.category = category
                }
            }
        }

        // タスクの日時にアラームを設定
        val intent = Intent(applicationContext, TaskAlarmReceiver::class.java)
        intent.putExtra(EXTRA_TASK, task.id)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        alarmManager.setAlarmClock(AlarmClockInfo(calendar.timeInMillis, null), pendingIntent)
    }

    /**
     * 日付と時刻のボタンの表示を設定する
     */
    private fun setDateTimeButtonText() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.JAPANESE)
        binding.content.dateButton.text = dateFormat.format(calendar.time)

        val timeFormat = SimpleDateFormat("HH:mm", Locale.JAPANESE)
        binding.content.timeButton.text = timeFormat.format(calendar.time)

    }
}