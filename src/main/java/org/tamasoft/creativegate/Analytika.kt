package org.tamasoft.creativegate

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.lang.reflect.Method

/**
 * Událost pro CiviaAnalytics bez závislosti na něm: plugin se hledá za běhu a volá přes reflexi.
 * Když neběží, volání nic neudělá. Nikdy nevyhazuje, sběr nesmí ovlivnit hru.
 * Kotlin verze šablony Analytika.java. Nikdy sem nepatří text od hráče, tedy ani jméno sítě brány.
 */
object Analytika {

    private var plugin: Plugin? = null
    private var track: Method? = null
    private var bezMetody = false
    private var zalogovano = 0L

    fun track(hrac: Player, udalost: String, vararg vlastnosti: Pair<String, Any>) {
        try {
            val p = Bukkit.getPluginManager().getPlugin("CiviaAnalytics")
            if (p == null || !p.isEnabled) return
            if (p !== plugin) { // po reloadu je to nová instance
                plugin = p
                track = null
                bezMetody = false
            }
            if (bezMetody) return
            val m = track ?: p.javaClass.getMethod("track", Player::class.java, String::class.java, Map::class.java).also { track = it }
            m.invoke(p, hrac, udalost, linkedMapOf(*vlastnosti))
        } catch (e: NoSuchMethodException) {
            bezMetody = true
            Bukkit.getLogger().warning("[Analytika] CiviaAnalytics nemá metodu track, události se neposílají.")
        } catch (e: Exception) {
            hlas(udalost, e)
        } catch (e: LinkageError) {
            hlas(udalost, e)
        }
    }

    /** přechodná chyba jednoho volání nesmí vypnout ostatní; do logu nejvýš jednou za minutu */
    private fun hlas(udalost: String, e: Throwable) {
        val ted = System.currentTimeMillis()
        if (ted - zalogovano > 60_000L) {
            zalogovano = ted
            Bukkit.getLogger().warning("[Analytika] Událost $udalost selhala: $e")
        }
    }
}
