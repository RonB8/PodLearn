package com.example.podlearn.data.remote.rss

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Minimal, tolerant RSS 2.0 / podcast feed parser built on Android's built-in XmlPullParser -
 * no extra dependency needed. Namespace-unaware: tag names are matched by their local part
 * (e.g. "itunes:duration" -> "duration"), and the "itunes:image" href-attribute tag is matched
 * by its raw prefixed name since it collides with the local name of the plain <image> element.
 */
object RssParser {

    fun parse(input: InputStream): RssFeed {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(input, null)

        var channelTitle = ""
        var channelDescription: String? = null
        var channelImageUrl: String? = null
        val items = mutableListOf<RssItem>()

        var inItem = false
        var inChannelImage = false
        var currentTag: String? = null

        var itemTitle = ""
        var itemGuid: String? = null
        var itemPubDateEpochMs: Long? = null
        var itemAudioUrl: String? = null
        var itemDurationSec: Long? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val rawName = parser.name
                    val name = localName(rawName)
                    currentTag = name

                    when {
                        rawName == "itunes:image" -> {
                            if (!inItem && channelImageUrl == null) {
                                channelImageUrl = parser.getAttributeValue(null, "href")
                            }
                        }
                        name == "image" -> if (!inItem) inChannelImage = true
                        name == "item" -> {
                            inItem = true
                            itemTitle = ""
                            itemGuid = null
                            itemPubDateEpochMs = null
                            itemAudioUrl = null
                            itemDurationSec = null
                        }
                        name == "enclosure" && inItem -> {
                            itemAudioUrl = parser.getAttributeValue(null, "url")
                        }
                    }
                }

                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        when (currentTag) {
                            "title" -> if (inItem) itemTitle = text else if (!inChannelImage) channelTitle = text
                            "description" -> if (!inItem) channelDescription = text
                            "guid" -> if (inItem) itemGuid = text
                            "pubDate" -> if (inItem) itemPubDateEpochMs = parseRfc822Date(text)
                            "duration" -> if (inItem) itemDurationSec = parseItunesDuration(text)
                            "url" -> if (inChannelImage) channelImageUrl = text
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    when (localName(parser.name)) {
                        "item" -> {
                            inItem = false
                            items += RssItem(itemGuid, itemTitle, itemPubDateEpochMs, itemAudioUrl, itemDurationSec)
                        }
                        "image" -> inChannelImage = false
                    }
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        return RssFeed(
            title = channelTitle,
            imageUrl = channelImageUrl,
            description = channelDescription,
            items = items,
        )
    }

    private fun localName(name: String): String = name.substringAfterLast(':')

    private fun parseRfc822Date(text: String): Long? = try {
        ZonedDateTime.parse(text, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }

    private fun parseItunesDuration(text: String): Long? {
        val parts = text.split(":").mapNotNull { it.trim().toLongOrNull() }
        return when (parts.size) {
            0 -> null
            1 -> parts[0]
            2 -> parts[0] * 60 + parts[1]
            else -> parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    }
}
