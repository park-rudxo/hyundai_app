package io.changyeoga.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Bundle
import android.provider.CalendarContract
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Receives text without uploading it. Calendar save remains in the user's calendar UI. */
class SharedPlanActivity:ComponentActivity() {
    private lateinit var titleInput:EditText
    private lateinit var placeInput:EditText
    private lateinit var notesInput:EditText
    private var selectedTime=0L
    override fun onSaveInstanceState(outState:Bundle) {
        if(::titleInput.isInitialized) {
            outState.putString("title",titleInput.text.toString());outState.putString("place",placeInput.text.toString());outState.putString("notes",notesInput.text.toString());outState.putLong("time",selectedTime)
        }
        super.onSaveInstanceState(outState)
    }
    override fun onCreate(state:Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(state)
        WindowCompat.setDecorFitsSystemWindows(window,false)
        val prefs=getSharedPreferences("shared-plan",MODE_PRIVATE)
        val shared=if(intent.action==Intent.ACTION_SEND && intent.type=="text/plain") intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()?.take(10000).orEmpty() else ""
        val layout=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(0xFFFAFAF8.toInt()) }
        val scroll=ScrollView(this).apply { addView(layout) };setContentView(scroll)
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightStatusBars=true
        WindowCompat.getInsetsController(window,window.decorView).isAppearanceLightNavigationBars=true
        val pad=(24*resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(scroll) { v,ins -> val b=ins.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime());v.setPadding(pad+b.left,b.top+pad,pad+b.right,b.bottom+pad);ins }
        fun label(text:String)=TextView(this).apply { this.text=text;cText(14f,true);setPadding(0,pad,pad/4,pad/2);layout.addView(this) }
        label(if(shared.isNotBlank()) "공유받은 글 확인" else "일정과 공유 메모").apply { cText(26f) }
        label("내용을 확인하고 저장하세요. 날짜·장소는 직접 정해 주세요.")
        fun field(hint:String,value:String,multi:Boolean=false)=EditText(this).apply { id=View.generateViewId();label(hint).labelFor=id;this.hint=hint;setText(value);setSingleLine(!multi);cField();layout.addView(this) }
        val title=field("일정 제목",if(shared.isNotBlank()) shared.lineSequence().firstOrNull().orEmpty().take(80) else prefs.getString("title",intent.getStringExtra("title").orEmpty()).orEmpty())
        val place=field("장소 · 선택",if(shared.isNotBlank()) "" else intent.getStringExtra("place") ?: prefs.getString("place","").orEmpty())
        val notes=field("공유받은 글 또는 메모",shared.ifBlank { prefs.getString("notes","").orEmpty() },true)
        titleInput=title;placeInput=place;notesInput=notes
        state?.let { title.setText(it.getString("title"));place.setText(it.getString("place"));notes.setText(it.getString("notes")) }
        notes.minLines=4
        val date=Calendar.getInstance().apply { timeInMillis=state?.getLong("time") ?: prefs.getLong("time",System.currentTimeMillis()) }
        val dateButton=Button(this).apply { cAction() }
        fun updateDate(){selectedTime=date.timeInMillis;dateButton.text=SimpleDateFormat("yyyy년 M월 d일 HH:mm",Locale.KOREA).format(date.time)}
        updateDate();layout.addView(dateButton)
        dateButton.setOnClickListener {
            DatePickerDialog(this,{_,y,m,d -> date.set(y,m,d);TimePickerDialog(this,{_,h,min -> date.set(Calendar.HOUR_OF_DAY,h);date.set(Calendar.MINUTE,min);date.set(Calendar.SECOND,0);updateDate()},date.get(Calendar.HOUR_OF_DAY),date.get(Calendar.MINUTE),true).show()},date.get(Calendar.YEAR),date.get(Calendar.MONTH),date.get(Calendar.DAY_OF_MONTH)).show()
        }
        label("날짜와 시간을 눌러 바꿀 수 있어요.")
        val status=label("")
        fun action(text:String,run:()->Unit) { layout.addView(Button(this).apply { this.text=text;cAction(end=true);setOnClickListener { run() } }) }
        fun save():Boolean {
            if(title.text.isBlank()){status.text="제목을 입력해 주세요.";return false}
            val ok=prefs.edit().putString("title",title.text.toString().take(200)).putString("place",place.text.toString().take(500)).putString("notes",notes.text.toString().take(10000)).putLong("time",date.timeInMillis).commit()
            status.text=if(ok) "이 휴대폰에 저장했어요." else "저장하지 못했어요. 다시 시도해 주세요."
            return ok
        }
        action("메모 저장") { save() }
        action("Google 캘린더에서 일정 작성 →") {
            if(save()) {
                val event=Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
                    .putExtra(CalendarContract.Events.TITLE,title.text.toString())
                    .putExtra(CalendarContract.Events.EVENT_LOCATION,place.text.toString())
                    .putExtra(CalendarContract.Events.DESCRIPTION,notes.text.toString())
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME,date.timeInMillis)
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME,date.timeInMillis+3600000)
                try { startActivity(Intent(event).setPackage("com.google.android.calendar"));status.text="캘린더에서 저장을 눌러야 일정이 등록돼요." }
                catch(_:android.content.ActivityNotFoundException) {
                    try { startActivity(event);status.text="사용할 캘린더를 선택하고 저장해 주세요." }
                    catch(_:android.content.ActivityNotFoundException) {status.text="일정을 작성할 캘린더 앱이 없어요. Google 캘린더를 설치해 주세요."}
                }
            }
        }
        action("외출 준비 화면으로") { startActivity(Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish() }
    }
}
