package com.example.gamenhac

import android.Manifest
import android.app.*
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import java.time.YearMonth
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        Sched.all(this)
        setContent { MaterialTheme { App() } }
    }
}

fun fmt(t: Long) = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault()).format(Date(t))
fun remain(end: Long): String {
    val ms = end - System.currentTimeMillis()
    return if (ms <= 0) "Đã kết thúc" else "Còn ${ms / 86_400_000} ngày ${(ms / 3_600_000) % 24} giờ"
}
fun pickTime(c: Context, h: Int, m: Int, ok: (Int, Int) -> Unit) =
    TimePickerDialog(c, { _, a, b -> ok(a, b) }, h, m, true).show()
fun pickDateTime(c: Context, ok: (Long) -> Unit) {
    val n = Calendar.getInstance()
    DatePickerDialog(c, { _, y, mo, d ->
        pickTime(c, 23, 59) { h, mi -> ok(Calendar.getInstance().apply { set(y, mo, d, h, mi, 0) }.timeInMillis) }
    }, n.get(Calendar.YEAR), n.get(Calendar.MONTH), n.get(Calendar.DAY_OF_MONTH)).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    val c = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var games by remember { mutableStateOf(Store.games(c)) }
    var evs by remember { mutableStateOf(Store.events(c)) }
    var dlg by remember { mutableStateOf(false) }
    var hist by remember { mutableStateOf(Store.hist(c)) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    val today = LocalDate.now().toString()
    fun saveG(l: List<Game>) { games = l; Store.saveGames(c, l); Sched.all(c) }
    fun mark(g: Game, on: Boolean) {
        val s = (hist[g.id] ?: emptySet()).let { if (on) it + today else it - today }
        hist = hist + (g.id to s); Store.saveHist(c, hist)
        saveG(games.map { if (it.id == g.id) it.copy(last = if (on) today else "") else it })
    }
    fun saveE(l: List<Ev>) { evs = l; Store.saveEvents(c, l); Sched.all(c) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Nhắc đăng nhập game") }) },
        floatingActionButton = { if (tab < 2) FloatingActionButton({ dlg = true }) { Text("+") } },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(tab == 0, { tab = 0 }, { Text("🎮") }, label = { Text("Game") })
                NavigationBarItem(tab == 1, { tab = 1 }, { Text("⏰") }, label = { Text("Sự kiện") })
                NavigationBarItem(tab == 2, { tab = 2 }, { Text("🏆") }, label = { Text("Thành tích") })
            }
        }
    ) { p ->
        LazyColumn(Modifier.padding(p).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tab == 0) {
                if (games.isEmpty()) item { Text("Chưa có game. Bấm + để thêm.") }
                items(games) { g ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium)
                            Text("Nhắc lúc %02d:%02d mỗi ngày".format(g.h, g.m))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (g.last == today) { Text("✅ Đã đăng nhập hôm nay", Modifier.weight(1f)); TextButton({ mark(g, false) }) { Text("Hoàn tác") } }
                                else Button({ mark(g, true) }, Modifier.weight(1f)) { Text("Đã đăng nhập") }
                                TextButton({ saveG(games - g); saveE(evs.filter { it.gid != g.id }) }) { Text("Xóa") }
                            }
                        }
                    }
                }
            } else if (tab == 1) {
                if (evs.isEmpty()) item { Text("Chưa có sự kiện. Bấm + để thêm.") }
                items(evs.sortedBy { it.end }) { e ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.title, style = MaterialTheme.typography.titleMedium)
                                Text(games.find { it.id == e.gid }?.name ?: "")
                                Text("Kết thúc: ${fmt(e.end)}")
                                Text(remain(e.end), color = if (e.end < System.currentTimeMillis()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary)
                            }
                            TextButton({ saveE(evs - e) }) { Text("Xóa") }
                        }
                    }
                }
            } else {
                item {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton({ month = month.minusMonths(1) }) { Text("◀") }
                        Text("Tháng ${month.monthValue}/${month.year}", style = MaterialTheme.typography.titleMedium)
                        TextButton({ month = month.plusMonths(1) }) { Text("▶") }
                    }
                }
                if (games.isEmpty()) item { Text("Chưa có game.") }
                items(games) { g ->
                    val d = (hist[g.id] ?: emptySet()) + (if (g.last.isNotBlank()) setOf(g.last) else emptySet())
                    val (cur, best) = streak(d)
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(g.name, style = MaterialTheme.typography.titleMedium)
                            Text("🔥 Chuỗi hiện tại: $cur ngày • Kỷ lục: $best ngày • Tổng: ${d.size} ngày")
                            Spacer(Modifier.height(8.dp))
                            Row { listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN").forEach { Box(Modifier.weight(1f), Alignment.Center) { Text(it, style = MaterialTheme.typography.labelSmall) } } }
                            val off = month.atDay(1).dayOfWeek.value - 1
                            val len = month.lengthOfMonth()
                            for (r in 0 until (off + len + 6) / 7) Row {
                                for (col in 0..6) {
                                    val day = r * 7 + col - off + 1
                                    Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), Alignment.Center) {
                                        if (day in 1..len) {
                                            val on = month.atDay(day).toString() in d
                                            Box(Modifier.fillMaxSize().background(if (on) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape), Alignment.Center) {
                                                Text("$day", style = MaterialTheme.typography.labelSmall, color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (dlg) {
        var name by remember { mutableStateOf("") }
        var h by remember { mutableStateOf(20) }
        var m by remember { mutableStateOf(0) }
        var gid by remember { mutableStateOf(games.firstOrNull()?.id ?: 0L) }
        var end by remember { mutableStateOf(0L) }
        AlertDialog(
            onDismissRequest = { dlg = false },
            title = { Text(if (tab == 0) "Thêm game" else "Thêm sự kiện") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (tab == 1) {
                        if (games.isEmpty()) Text("Hãy thêm game trước.")
                        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            games.forEach { FilterChip(gid == it.id, { gid = it.id }, { Text(it.name) }) }
                        }
                    }
                    OutlinedTextField(name, { name = it }, label = { Text(if (tab == 0) "Tên game" else "Tên sự kiện") })
                    if (tab == 0) OutlinedButton({ pickTime(c, h, m) { a, b -> h = a; m = b } }) { Text("Giờ nhắc: %02d:%02d".format(h, m)) }
                    else OutlinedButton({ pickDateTime(c) { end = it } }) { Text(if (end == 0L) "Chọn ngày giờ kết thúc" else fmt(end)) }
                }
            },
            confirmButton = {
                TextButton({
                    if (name.isNotBlank()) {
                        if (tab == 0) { saveG(games + Game(Store.newId(), name.trim(), h, m, "")); dlg = false }
                        else if (end > 0 && gid != 0L) { saveE(evs + Ev(Store.newId(), gid, name.trim(), end)); dlg = false }
                    }
                }) { Text("Lưu") }
            },
            dismissButton = { TextButton({ dlg = false }) { Text("Hủy") } }
        )
    }
}
