package xyz.gnarbot.gnar.music

import com.github.natanbc.lavadsp.karaoke.KaraokePcmAudioFilter
import com.github.natanbc.lavadsp.timescale.TimescalePcmAudioFilter
import com.github.natanbc.lavadsp.tremolo.TremoloPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.AudioFilter
import com.sedmelluq.discord.lavaplayer.filter.FilterChainBuilder
import com.sedmelluq.discord.lavaplayer.filter.FloatPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.UniversalPcmAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.converter.ToFloatAudioFilter
import com.sedmelluq.discord.lavaplayer.filter.equalizer.Equalizer
import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import xyz.gnarbot.gnar.music.filters.*

class DSPFilter(private val player: AudioPlayer) {
    // Equalizer properties
    var bassBoost = BoostSetting.OFF
        set(value) {
            field = value
            applyFilters()
        }

    // Karaoke properties
    var karaokeEnable = false
        set(value) {
            field = value
            applyFilters()
        }
    var kLevel = 1.0f
        set(value) {
            field = value
            applyFilters()
        }
    var kFilterBand = 220f
        set(value) {
            field = value
            applyFilters()
        }
    var kFilterWidth = 100f
        set(value) {
            field = value
            applyFilters()
        }

    // Timescale properties
    val timescaleEnable: Boolean
        get() = tsSpeed != 1.0 || tsPitch != 1.0 || tsRate != 1.0

    var tsSpeed = 1.0
        set(value) {
            field = value
            applyFilters()
        }
    var tsPitch = 1.0
        set(value) {
            field = value
            applyFilters()
        }
    var tsRate = 1.0
        set(value) {
            field = value
            applyFilters()
        }

    // Tremolo properties
    var tremoloEnable = false
        set(value) {
            field = value
            applyFilters()
        }
    var tDepth = 0.5f
        set(value) {
            field = value
            applyFilters()
        }
    var tFrequency = 2f
        set(value) {
            field = value
            applyFilters()
        }

    fun buildFilters(configs: List<FilterConfig<*>>, format: AudioDataFormat,
                     output: UniversalPcmAudioFilter): List<AudioFilter> {
        if (configs.isEmpty()) {
            println("No configs!")
            return emptyList()
        }

        val filters = mutableListOf<FloatPcmAudioFilter>()

        for (filter in configs) { // Last filter writes to output.
            val pipeTo = if (filters.isEmpty()) "Output" else filters.last()::class.simpleName
            println("${filter::class.simpleName} piping to $pipeTo")
            val built = if (filters.isEmpty()) { // First (read: last) filter
                filter.build(output, format)
            } else {
                filter.build(filters.last(), format)
            }
            filters.add(built)
        }

        return filters.reversed()
    }

    fun applyFilters() {
        player.setFilterFactory { _, format, output ->
            val filterConfigs = mutableListOf<FilterConfig<*>>()

            if (karaokeEnable) {
                val config = KaraokeFilter().configure {
                    level = kLevel
                    filterBand = kFilterBand
                    filterWidth = kFilterWidth
                }
                filterConfigs.add(config)
            }

            if (timescaleEnable) {
                val config = TimescaleFilter().configure {
                    pitch = tsPitch
                    speed = tsSpeed
                    rate = tsRate
                }
                filterConfigs.add(config)
            }

            if (tremoloEnable) {
                val config = TremoloFilter().configure {
                    depth = tDepth
                    frequency = tFrequency
                }
                filterConfigs.add(config)
            }

            if (bassBoost != BoostSetting.OFF) {
                val config = EqualizerFilter().configure {
                    setGain(0, bassBoost.band1)
                    setGain(1, bassBoost.band2)
                }
                filterConfigs.add(config)
            }

            return@setFilterFactory buildFilters(filterConfigs, format, output)
        }
    }

    fun clearFilters() {
        player.setFilterFactory(null)
    }

}