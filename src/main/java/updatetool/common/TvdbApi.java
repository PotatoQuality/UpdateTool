package updatetool.common;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Map;
import org.tinylog.Logger;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import updatetool.exceptions.ApiCallFailedException;

public class TvdbApi extends AbstractApi {
    private static final String BASE_URL = "https://api.thetvdb.com";
    private final String authToken;
    private final Gson gson = new Gson();

    public class UnmarshalTvdb {
        public final String Error = null;
        private final Object data = null;
        
        private boolean isSeries() {
            return data instanceof LinkedTreeMap;
        }
        
        private boolean isEpisode() {
            return data instanceof ArrayList;
        }
        
        @SuppressWarnings("rawtypes")
        public String getImdbId() {
            if(data == null) return null;
            if(isSeries()) return (String) ((LinkedTreeMap) data).get("imdbId");
            if(isEpisode()) return (String) ((LinkedTreeMap) ((ArrayList) data).get(0)).get("imdbId");
            return null;
        }
    }
    
    public TvdbApi(String key) throws ApiCallFailedException {
        super();
        authToken = "Bearer " + auth(key);
    }

    private class Token { String token; };
    
    private String auth(String key) throws ApiCallFailedException {
        try {
            var response = send(
                        postJson(BASE_URL + "/login", gson.toJson(Map.of(
                                "apikey", key)
                                ))
                        );
            if(response.statusCode() != 200) {
                Logger.error("TVDB authorization failed with code {}", response.statusCode());
                Logger.error("This could be due to the TVDB API having issues at the moment or your credentials being wrong.");
                Logger.error("This is the received response:");
                Logger.error(response.body());
                Logger.error("===================================================");
                throw new ApiCallFailedException("TVDB API authorization failed.");
            }
            return new Gson().fromJson(response.body(), Token.class).token;
        } catch (IOException | InterruptedException e) {
            throw Utility.rethrow(e);
        }
    }
    
    public HttpResponse<String> seriesImdbId(String tvdbId) {
        try {
            return send(HttpRequest.newBuilder(new URI(String.format("%s/series/%s", BASE_URL, tvdbId)))
                    .GET()
                    .header("Authorization", authToken)
                    .build());
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw Utility.rethrow(e);
        }
    }
    
    public HttpResponse<String> episodeImdbId(String[] parts) {
        try {
            return send(HttpRequest.newBuilder(new URI(String.format("%s/series/%s/episodes/query?airedSeason=%s&airedEpisode=%s", BASE_URL, parts[0], parts[1], parts[2])))
                    .GET()
                    .header("Authorization", authToken)
                    .build());
        } catch (IOException | InterruptedException | URISyntaxException e) {
            throw Utility.rethrow(e);
        }
    }
    
    @Override
    public HttpResponse<String> testApi() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String keysWhere() {
        throw new UnsupportedOperationException();
    }

}
