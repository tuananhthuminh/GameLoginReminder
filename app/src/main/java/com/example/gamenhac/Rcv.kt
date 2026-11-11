package com.example.gamenhac

import android.app.*
import android.content.*
import java.time.LocalDate

class Rcv : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action == Intent.ACTION_BOOT_COMPLETED) { Sched.all(c); return }
        val id = i.getLongExtra("id", 0)
        val title: String; val text: String
        if (i.getStringExtra("t") == "g") {
            val g = Store.games(c).find { it.id == id } ?: return
            Sched.game(c, g)
            if (g.last == LocalDate.now().toString()) return
            title = "Đăng nhập ${g.name}"; text = "Hôm nay bạn chưa đăng nhập!"
        } else {
            val e = Store.events(c).find { it.id == id } ?: return
            val gn = Store.games(c).find { it.id == e.gid }?.name ?: ""
            title = "Sắp kết thúc: ${e.title}"; text = "$gn • còn khoảng ${i.getStringExtra("l")}"
        }
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("r", "Nhắc nhở", NotificationManager.IMPORTANCE_HIGH))
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        nm.notify((id % 1_000_000).toInt() + (if (i.getStringExtra("t") == "g") 0 else 1_000_000),
            Notification.Builder(c, "r").setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build())
    }
}
