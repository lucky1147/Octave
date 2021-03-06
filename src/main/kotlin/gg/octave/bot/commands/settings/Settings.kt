package gg.octave.bot.commands.settings

import gg.octave.bot.db.guilds.GuildData
import gg.octave.bot.utils.extensions.DEFAULT_SUBCOMMAND
import gg.octave.bot.utils.extensions.config
import gg.octave.bot.utils.extensions.data
import gg.octave.bot.utils.extensions.premiumGuild
import gg.octave.bot.utils.toDuration
import me.devoxin.flight.api.Context
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Greedy
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.entities.Cog
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import java.time.Duration

class Settings : Cog {
    @Command(aliases = ["setting", "set", "config", "configuration", "configure", "opts", "options"],
        description = "Change music settings.", userPermissions = [Permission.MANAGE_SERVER])
    fun settings(ctx: Context) = DEFAULT_SUBCOMMAND(ctx)

    @SubCommand(description = "Resets the settings for the guild.")
    fun reset(ctx: Context) {
        ctx.data.apply { reset(); save() }
        ctx.send {
            setColor(0x9570D3)
            setTitle("Settings")
            setDescription("The settings for this server have been reset.")
        }
    }

    @SubCommand(aliases = ["autodel"], description = "Toggle whether the bot auto-deletes its responses.")
    fun autodelete(ctx: Context, toggle: Boolean) {
        ctx.data.let {
            it.command.isAutoDelete = toggle
            it.save()
        }

        val send = if (!toggle) "The bot will no longer automatically delete messages after 10 seconds."
        else "The bot will now delete messages after 10 seconds."

        ctx.send(send)
    }

    @SubCommand(aliases = ["ta"], description = "Toggles music announcements.")
    fun announcements(ctx: Context, toggle: Boolean) {
        ctx.data.let {
            it.music.announce = toggle
            it.save()
        }

        val send = if (toggle) "Announcements for music enabled." else "Announcements for music disabled."
        ctx.send(send)
    }

    @SubCommand(description = "Toggles whether only DJs can use the bot.")
    fun djonly(ctx: Context, toggle: Boolean) {
        ctx.data.let {
            it.command.isDjOnlyMode = toggle
            it.save()
        }

        val send = if (toggle) "Enabled DJ-only mode." else "Disabled DJ-only mode."
        ctx.send(send)
    }

    @SubCommand(aliases = ["djrequirement", "rdj"], description = "Set whether DJ-only commands can be used by all.")
    fun requiredj(ctx: Context, toggle: Boolean) { // toggle = false (no)
        ctx.data.let {
            it.music.isDisableDj = !toggle
            it.save()
        }

        val send = if (!toggle) "DJ commands can be now run by everyone." else "DJ commands now require the DJ role."
        ctx.send(send)
    }

    @SubCommand(aliases = ["votequeue", "vp", "vq"], description = "Toggle whether voting is enabled for track queueing.")
    fun voteplay(ctx: Context, toggle: Boolean) {
        ctx.data.let {
            it.music.isVotePlay = toggle
            it.save()
        }

        val send = if (toggle) "Enabled vote-play." else "Disabled vote-play."
        ctx.send(send)
    }

    @SubCommand(aliases = ["vc"], description = "Toggles a voice-channel as a dedicated music channel.")
    fun voicechannel(ctx: Context, channel: VoiceChannel) {
        val data = ctx.data

        if (channel.id in data.music.channels) {
            data.music.channels.remove(channel.id)
            data.save()
            return ctx.send("${channel.name} is no longer a designated music channel.")
        }

        if (channel == ctx.guild!!.afkChannel) {
            return ctx.send("`${channel.name}` is the AFK channel, you can't play music there.")
        }

        data.music.channels.add(channel.id)
        data.save()
        ctx.send("`${channel.name}` is now a designated music channel.")
    }

    @SubCommand(aliases = ["ac"], description = "Set the music announcement channel. Omit to reset.")
    fun announcementchannel(ctx: Context, textChannel: TextChannel?) {
        ctx.data.let {
            it.music.announcementChannel = textChannel?.id
            it.save()
        }

        val out = textChannel?.let { "Successfully set music announcement channel to ${it.asMention}" }
            ?: "Successfully reset the music announcement channel."

        ctx.send(out)
    }

    @SubCommand(aliases = ["djr", "dr"], description = "Sets the DJ role. Omit to reset.")
    fun djrole(ctx: Context, role: Role?) {
        ctx.data.let {
            it.command.djRole = role?.id
            it.save()
        }

        val out = role?.let { "Successfully set the DJ role to ${it.asMention}" }
            ?: "Successfully reset the DJ role to default."

        ctx.send(out)
    }

    @SubCommand(aliases = ["sl"], description = "Set the maximum song length. \"reset\" to reset.")
    fun songlength(ctx: Context, content: String) {
        val data = ctx.data

        if (content == "reset") {
            data.music.maxSongLength = 0
            data.save()
            return ctx.send("Song length limit reset.")
        }

        val duration = try {
            content.toDuration()
        } catch (e: RuntimeException) {
            return ctx.send("Wrong duration specified: Expected something like `40 minutes`")
        }

        val premiumGuild = ctx.premiumGuild
        val durationLimit = premiumGuild?.songLengthQuota ?: ctx.config.durationLimit.toMillis()

        if (duration.toMillis() > durationLimit) {
            return ctx.send("This is too much. The limit is ${ctx.config.durationLimitText}.")
        }

        if (duration.toMinutes() < 1) {
            return ctx.send("That's too little. It has to be more than 1 minute.")
        }

        data.music.maxSongLength = duration.toMillis()
        data.save()
        ctx.send("Successfully set song length limit to $content.")
    }

    @SubCommand(aliases = ["qs"], description = "Sets the maximum queue size for the server. Omit to reset.")
    fun queuesize(ctx: Context, limit: Int?) {
        val data = ctx.data

        if (limit == null) {
            data.music.maxQueueSize = 0
            data.save()
            return ctx.send("Queue limit reset.")
        }

        val premiumGuild = ctx.premiumGuild
        val totalLimit = premiumGuild?.queueSizeQuota ?: ctx.config.queueLimit
        val qLimit = limit.takeIf { it in 2..totalLimit }
            ?: return ctx.send("The limit needs to be between 1-$totalLimit.")

        ctx.data.let {
            it.music.maxQueueSize = qLimit
            it.save()
        }

        ctx.send("Successfully set queue limit to $qLimit.")
    }

    @SubCommand(aliases = ["votequeuecooldown", "vqc", "vpc"], description = "Sets the vote-play cooldown.")
    fun voteplaycooldown(ctx: Context, @Greedy duration: String) = durationParseCommand(ctx, duration,
        { music.votePlayCooldown = it }, ctx.config.votePlayCooldown, ctx.config.votePlayCooldownText, "vote-play cooldown")

    @SubCommand(aliases = ["votequeueduration", "vqd", "vpd"], description = "Sets the vote-play duration.")
    fun voteplayduration(ctx: Context, @Greedy duration: String) = durationParseCommand(ctx, duration,
        { music.votePlayDuration = it }, ctx.config.votePlayDuration, ctx.config.votePlayDurationText, "vote-play duration")

    @SubCommand(aliases = ["vsd"], description = "Sets the vote-skip duration.")
    fun voteskipduration(ctx: Context, @Greedy duration: String) = durationParseCommand(ctx, duration,
        { music.voteSkipDuration = it }, ctx.config.voteSkipDuration, ctx.config.voteSkipDurationText, "vote-skip duration")

    @SubCommand(aliases = ["vsc"], description = "Sets the vote-skip cooldown.")
    fun voteskipcooldown(ctx: Context, @Greedy duration: String) = durationParseCommand(ctx, duration,
        { music.voteSkipCooldown = it }, ctx.config.voteSkipCooldown, ctx.config.voteSkipCooldownText, "vote-skip cooldown")

    private fun durationParseCommand(ctx: Context, duration: String, setter: GuildData.(Long) -> Unit,
                                     limit: Duration, limitText: String, property: String) {
        if (duration == "reset") {
            ctx.data.let {
                it.setter(0)
                it.save()
            }

            return ctx.send("Reset $property.")
        }

        val amount = try {
            duration.toDuration()
        } catch (e: RuntimeException) {
            return ctx.send("Wrong duration specified: Expected something like `40 minutes`")
        }

        if (amount > limit) {
            return ctx.send("This is too much. The limit is $limitText.")
        }

        if (amount.toSeconds() < 10) {
            return ctx.send("Has to be more than 10 seconds.")
        }

        ctx.data.let {
            it.setter(amount.toMillis())
            it.save()
        }

        ctx.send("Successfully set $property to $duration.")
    }
}
