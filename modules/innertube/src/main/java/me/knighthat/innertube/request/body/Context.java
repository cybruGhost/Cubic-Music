package me.knighthat.innertube.request.body;

import lombok.Data;
import me.knighthat.innertube.Constants;
import me.knighthat.innertube.UserAgents;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@Data
public class Context {

    @NotNull
    public static final Context WEB_DEFAULT = new Context( Client.WEB );

    @NotNull
    public static final Context IOS_DEFAULT = new Context( Client.IOS );

    @NotNull
    public final Client client;

    @Data
    public static class Client {

        @NotNull
        public static final Client WEB = new Client(
                "WEB_REMIX",
                "1.20250416.01.00",
                "DESKTOP",
                Locale.getDefault().getLanguage(),
                Locale.getDefault().getCountry(),
                Constants.VISITOR_DATA,
                UserAgents.CHROME_WINDOWS,
                Constants.YOUTUBE_MUSIC_HOST,
                67
        );

        @NotNull
        public static final Client IOS = new Client(
                "IOS",
                "20.03.02",
                "MOBILE",
                Locale.getDefault().getLanguage(),
                Locale.getDefault().getCountry(),
                Constants.VISITOR_DATA,
                UserAgents.IOS,
                Constants.YOUTUBE_MUSIC_HOST,
                5
        );

        /**
         * Name of client to present to YouTube,
         * can be extracted by using web interface.
         * <p>
         * Go to <a href="https://music.youtube.com">...</a> with inspector
         * and look for any POST request to "music.youtube.com",
         * "clientName" should be present in request.
         */
        @NotNull
        public final String clientName;

        /**
         * Version co-responding to {@code clientName},
         * should be extracted at the same time with it
         * to prevent version mismatch.
         */
        @NotNull
        public final String clientVersion;

        /**
         * Platform sending the request, co-response to {@code clientName}.
         */
        @NotNull
        public final String platform;

        /**
         * 2-letter language code
         * <p>
         * I.E. `en` equals `English`
         */
        @NotNull
        public final String hl;

        /**
         * 2-letter region code
         * <p>
         * I.E. `US` equals `United States`
         */
        @NotNull
        public final String gl;

        /**
         * Unique string for YT tracking. Required in most scenario.
         * <p>
         * Use {@code Constants.VISITOR_DATA} for default value.
         */
        @NotNull
        public final String visitorData;

        /**
         * Browser identifier.
         * <p>
         * Use {@code UserAgents} for default values
         */
        @NotNull
        public final String userAgent;

        /**
         * Should be <a href="https://music.youtube.com">...</a> in most cases
         */
        @NotNull
        public final String referer;

        /**
         * {@code versionName} in numeric format. Must co-response to
         * {@code versionName}.
         * <p>
         * {@code 67} is numeric format of "{@code DESKTOP}"
         */
        public final int xClientName;
    }
}
