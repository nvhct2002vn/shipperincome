package com.example.shipperincome.widget

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.unit.ColorProvider as GlanceColor
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.shipperincome.MainActivity
import com.example.shipperincome.utils.DateWithShortWeekday
import com.example.shipperincome.utils.formatCompactCurrency
import com.example.shipperincome.utils.formatCurrency
import com.example.shipperincome.ui.viewmodel.GoalProgress
import com.example.shipperincome.ui.viewmodel.RankProgress
import com.example.shipperincome.utils.groupDigits
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Widget man hinh chinh: thu nhap hom nay, tong tuan va nhip do rank. Cham vao de mo app. */
class QuickEarnWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = withContext(Dispatchers.IO) { WidgetData.load(context) }
        provideContent { WidgetContent(data) }
    }

    companion object {
        private const val TAG = "QuickEarnWidget"
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        /** Goi sau moi lan luu ca/diem/cai dat de widget cap nhat ngay, khong doi chu ky 30 phut. */
        fun refresh(context: Context) {
            val app = context.applicationContext
            scope.launch {
                try {
                    QuickEarnWidget().updateAll(app)
                } catch (e: Exception) {
                    Log.w(TAG, "Cannot update widget", e)
                }
            }
        }
    }
}

class QuickEarnWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickEarnWidget()
}

// Nen xanh dam giong phan dau trang trong app; widget co mau co dinh nen sang/toi dung chung.
private val Background = fixed(Color(0xFF0F3D2E))
private val OnBackground = fixed(Color.White)
private val OnBackgroundDim = fixed(Color(0xB3FFFFFF))
private val Accent = fixed(Color(0xFF8AD6B4))
private val Warning = fixed(Color(0xFFFFB77C))
private val Track = fixed(Color(0x33FFFFFF))

private fun fixed(color: Color) = ColorProvider(day = color, night = color)

@Composable
private fun WidgetContent(data: WidgetData) {
    // Glance chi ve toi da 10 con trong mot Column/Row (thua bi cat), nen moi khoi tu boc trong Column rieng.
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .cornerRadius(20.dp)
            .background(Background)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("QuickEarn", style = TextStyle(color = Accent, fontSize = 13.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.defaultWeight())
            Text(
                data.today.format(DateWithShortWeekday).replaceFirstChar { it.uppercase() },
                style = TextStyle(color = OnBackgroundDim, fontSize = 12.sp)
            )
        }
        // Khoang cach giua cac khoi gian deu theo chieu cao widget (4x2, 4x3...), khong don het vao mot cho.
        Spacer(GlanceModifier.defaultWeight())
        // Ca thuong nhap vao cuoi ngay nen widget xem tong tuan/thang (va muc tieu), khong xem rieng hom nay.
        PeriodBlock("Tuần này", data.weekNet, data.weekGoal)
        Spacer(GlanceModifier.defaultWeight())
        PeriodBlock("Tháng ${data.today.monthValue}", data.monthNet, data.monthGoal)
        if (data.weekGoal == null && data.monthGoal == null) {
            // Chua dat muc tieu thi nhac cho de biet widget co the hien tien do.
            Text(
                "Đặt mục tiêu tuần/tháng trong Cài đặt để xem tiến độ ở đây",
                style = TextStyle(color = OnBackgroundDim, fontSize = 11.sp),
                maxLines = 2,
                modifier = GlanceModifier.padding(top = 4.dp)
            )
        }
        data.rank?.let { rank ->
            Spacer(GlanceModifier.defaultWeight())
            val good = rank.isDone || rank.onTrack
            StatBlock(
                label = "Rank",
                value = points(rank.currentPoints),
                suffix = " / ${points(rank.targetPoints)}",
                progress = rank.fraction,
                caption = when {
                    rank.isDone -> "Đủ chỉ tiêu rồi!"
                    rank.daysRemaining == 0 -> "Hết chu kỳ · thiếu ${points(rank.remainingPoints)}"
                    else -> "Cần ${points(rank.pointsPerDayNeeded)}/ngày · còn ${rank.daysRemaining} ngày"
                },
                accent = if (good) Accent else Warning,
                highlightCaption = true
            )
        }
    }
}

/** Tong thuc nhan cua tuan/thang; co muc tieu thi them thanh tien do va so can moi ngay. */
@Composable
private fun PeriodBlock(label: String, net: Double, goal: GoalProgress?) {
    StatBlock(
        label = label,
        value = formatCurrency(net),
        suffix = goal?.let { " / ${formatCompactCurrency(it.target)}" },
        progress = goal?.fraction,
        caption = goal?.let {
            when {
                it.isDone -> "${it.percent}% · Đã đạt mục tiêu!"
                it.daysRemaining == 0 -> "${it.percent}% · thiếu ${formatCompactCurrency(it.remaining)}"
                else -> "${it.percent}% · cần ${formatCompactCurrency(it.perDayNeeded)}/ngày · còn ${it.daysRemaining} ngày"
            }
        },
        accent = Accent,
        highlightCaption = goal?.isDone == true
    )
}

/** Mot khoi thong nhat: nhan trai, so lon phai; tuy chon thanh tien do va dong chu thich. */
@Composable
private fun StatBlock(
    label: String,
    value: String,
    suffix: String?,
    progress: Float?,
    caption: String?,
    accent: GlanceColor,
    highlightCaption: Boolean
) {
    Column(modifier = GlanceModifier.fillMaxWidth()) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = TextStyle(color = OnBackgroundDim, fontSize = 13.sp))
            Spacer(GlanceModifier.defaultWeight())
            Text(value, style = TextStyle(color = OnBackground, fontSize = 19.sp, fontWeight = FontWeight.Bold))
            if (suffix != null) {
                Text(suffix, style = TextStyle(color = OnBackgroundDim, fontSize = 12.sp))
            }
        }
        if (progress != null) {
            Spacer(GlanceModifier.height(5.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = accent,
                backgroundColor = Track
            )
        }
        if (caption != null) {
            Spacer(GlanceModifier.height(3.dp))
            Text(
                caption,
                style = TextStyle(color = if (highlightCaption) accent else OnBackgroundDim, fontSize = 11.sp),
                maxLines = 1
            )
        }
    }
}

private fun points(value: Int): String = groupDigits(value.toString())
