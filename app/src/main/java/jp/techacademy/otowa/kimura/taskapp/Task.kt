package jp.techacademy.otowa.kimura.taskapp

import java.io.Serializable
import io.realm.kotlin.types.RealmObject
import io.realm.kotlin.types.annotations.PrimaryKey

//Serializableインターフェイスを実装することで生成したオブジェクトをシリアライズできるようになります.
// シリアライズとはデータを丸ごとファイルに保存したり、TaskAppでいうと別のActivityに渡すこと
open class Task : RealmObject,Serializable{
    @PrimaryKey
    var id = 0

    var title =""
    var contents = ""
    var date = ""
    var category = ""
}