package com.example.gamenhac

import android.app.*
import android.content.*
import android.os.Build
import org.json.*
import java.util.Calendar
import java.time.LocalDate

data class Game(val id: Long, val name: String, val h: Int, val m: Int, val last: String)
data class Ev(val id: Long, val gid: Long, val title: String, val end: Long)

object Store {
    private fun sp(c: Context) = c.getSharedPreferences("d", 0)
    fun newId() = (System.currentTimeMillis() / 1000) % 100_000_000
    fun games(c: Context): List<Game> {
        val a = JSONArray(sp(c).getString("g", "[]"))
        return List(a.length()) { a.getJSONObject(it).let { o -> Game(o.getLong("id"), o.getString("n"), o.getInt("h"), o.getInt("m"), o.getString("l")) } }
    }
    fun saveGames(c: Context, l: List<Game>) = sp(c).edit().putString("g",
        JSONArray(l.map { JSONObject().put("id", it.id).put("n", it.name).put("h", it.h).put("m", it.m).put("l", it.last) }).toString()).apply()
    fun events(c: Context): List<Ev> {
        val a = JSONArray(sp(c).getString("e", "[]"))
        return List(a.length()) { a.getJSONObject(it).let { o -> Ev(o.getLong("id"), o.getLong("g"), o.getString("t"), o.getLong("e")) } }
    }
    fun hist(c: Context): Map<Long, Set<String>> {
        val o = JSONObject(sp(c).getString("h", "{}"))
        return o.keys().asSequence().associate { k ->
            val a = o.getJSONArray(k); k.toLong() to List(a.length()) { a.getString(it) }.toSet()
        }
    }
    fun saveHist(c: Context, m: Map<Long, Set<String>>) {
        val o = JSONObject()
        m.forEach { (k, v) -> o.put(k.toString(), JSONArray(v.toList())) }
        sp(c).edit().putString("h", o.toString()).apply()
    }
    fun saveEvents(c: Context, l: List<Ev>) = sp(c).edit().putString("e",
        JSONArray(l.map { JSONObject().put("id", it.id).put("g", it.gid).put("t", it.title).put("e", it.end) }).toString()).apply()
}

object Sched {
    private fun pi(c: Context, code: Int, t: String, id: Long, l: String = ""): PendingIntent {
        val i = Intent(c, Rcv::class.java).putExtra("t", t).putExtra("id", id).putExtra("l", l)
        return PendingIntent.getBroadcast(c, code, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
    private fun arm(c: Context, at: Long, p: PendingIntent) {
        val a = c.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT < 31 || a.canScheduleExactAlarms()) a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
        else a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, p)
    }
    fun game(c: Context, g: Game) {
        val t = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, g.h); set(Calendar.MINUTE, g.m); set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        arm(c, t.timeInMillis, pi(c, (g.id * 10).toInt(), "g", g.id))
    }
    fun all(c: Context) {
        Store.games(c).forEach { game(c, it) }
        Store.events(c).forEach { e ->
            listOf(Triple(1, 24L, "24 giờ"), Triple(2, 2L, "2 giờ")).forEach { (k, h, label) ->
                val at = e.end - h * 3_600_000
                if (at > System.currentTimeMillis()) arm(c, at, pi(c, (e.id * 10 + k).toInt(), "e", e.id, label))
            }
        }
    }
}

fun streak(d: Set<String>): Pair<Int, Int> {
    val ds = d.map { LocalDate.parse(it) }.toSortedSet()
    var best = 0; var run = 0; var prev: LocalDate? = null
    for (x in ds) { run = if (prev != null && prev.plusDays(1) == x) run + 1 else 1; best = maxOf(best, run); prev = x }
    var cur = 0
    var day = if (LocalDate.now() in ds) LocalDate.now() else LocalDate.now().minusDays(1)
    while (day in ds) { cur++; day = day.minusDays(1) }
    return cur to best
}
