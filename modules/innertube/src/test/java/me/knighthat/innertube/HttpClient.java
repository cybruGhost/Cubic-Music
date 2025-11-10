package me.knighthat.innertube;

import com.google.gson.Gson;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.jetbrains.annotations.NotNull;

public interface HttpClient {

    HttpLoggingInterceptor INTERCEPTOR = new HttpLoggingInterceptor().setLevel( HttpLoggingInterceptor.Level.BODY );
    @NotNull
    OkHttpClient CLIENT =  new OkHttpClient.Builder().addInterceptor( INTERCEPTOR ).build();
    @NotNull
    Gson JSON = new Gson();

    default Request makeRequest( String endpoint, me.knighthat.innertube.request.body.RequestBody body ) {
        RequestBody reqBody = RequestBody.create( JSON.toJson( body ), me.knighthat.Constants.APPLICATION_JSON );

        HttpUrl url = HttpUrl.parse( me.knighthat.innertube.Constants.YOUTUBE_MUSIC_HOST + "/" + endpoint )
                             .newBuilder()
                             .addQueryParameter( "prettyPrint", "false" )
                             .build();

        return new Request.Builder()
                          .url( url )
                          .addHeader( "Content-Type", me.knighthat.Constants.APPLICATION_JSON.type() )
                          .addHeader( "User-Agent", UserAgents.CHROME_WINDOWS )
                          .post( reqBody )
                          .build();
    }

    default boolean post( Request request ) {
        try (
                Response response = CLIENT.newCall( request ).execute()
        ) {
            return response.isSuccessful();
        } catch ( Exception e ) {
            e.printStackTrace();
            return false;
        }
    }

    default boolean post( String endpoint, me.knighthat.innertube.request.body.RequestBody body ) {
        Request request = makeRequest( endpoint, body );
        return post( request );
    }
}
