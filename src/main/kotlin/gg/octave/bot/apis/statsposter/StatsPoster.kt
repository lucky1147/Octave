package gg.octave.bot.apis.statsposter

import gg.octave.bot.Launcher
import gg.octave.bot.apis.statsposter.websites.*
import gg.octave.bot.db.Database
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class StatsPoster(botId: String) {
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    val websites = listOf(
        BotsForDiscord(botId, Launcher.credentials.botsForDiscord ?: ""),
        BotsGg(botId, Launcher.credentials.botsGg ?: ""),
        BotsOnDiscord(botId, Launcher.credentials.botsOnDiscord ?: ""),
        TopGg(botId, Launcher.credentials.topGg ?: "")
    )

    fun update(count: Long) {
        for (website in websites.filter(Website::canPost)) {
            website.update(count)
                .thenApply { it.body()?.close() }
                .exceptionally {
                    log.error("Updating server count failed for ${website.name}: ", it)
                    return@exceptionally null
                }
        }
    }

    fun postEvery(time: Long, unit: TimeUnit) {
        scheduler.scheduleWithFixedDelay({
            var sum = 0L;
            Database.getDefaultJedisPool().resource.use { jedis ->
                for(x in 0 until Launcher.credentials.totalShards) {
                    val json = jedis.hget("stats", x.toString())
                    val guilds = JSONObject(json).getLong("guild_count")
                    sum += guilds
                }
            }

            update(sum)
        }, time, time, unit)
    }

    companion object {
        private val log = LoggerFactory.getLogger(StatsPoster::class.java)
    }
}
