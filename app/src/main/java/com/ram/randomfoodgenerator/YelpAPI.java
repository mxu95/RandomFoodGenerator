package com.ram.randomfoodgenerator;

import java.util.ArrayList;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

public class YelpAPI {
    private static final String API_HOST = "api.yelp.com";
    private static final String SEARCH_PATH = "/v2/search";

    OAuthService service;
    Token accessToken;

    /**
     * Setup the Yelp API OAuth credentials.
     *
     * @param consumerKey Consumer key
     * @param consumerSecret Consumer secret
     * @param token Token
     * @param tokenSecret Token secret
     */
    public YelpAPI(String consumerKey, String consumerSecret, String token, String tokenSecret) {
        this.service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(consumerKey).apiSecret(consumerSecret).build();
        this.accessToken = new Token(token, tokenSecret);
    }

    private String searchForBusinessesByLocation(String location) {
        OAuthRequest request = createOAuthRequest(SEARCH_PATH);
        request.addQuerystringParameter("term", "food");
        request.addQuerystringParameter("limit", "20");
        request.addQuerystringParameter("sort", "1");
        request.addQuerystringParameter("location", location);
        return sendRequestAndGetResponse(request);
    }

    private String searchForBusinessesByLocation(Double latitude, Double longitude) {
        OAuthRequest request = createOAuthRequest(SEARCH_PATH);
        request.addQuerystringParameter("term", "food");
        request.addQuerystringParameter("limit", "20");
        request.addQuerystringParameter("sort", "1");
        request.addQuerystringParameter("ll", String.valueOf(latitude) + "," + String.valueOf(longitude));
        return sendRequestAndGetResponse(request);
    }

    /**
     * Creates and returns an {@link OAuthRequest} based on the API endpoint specified.
     *
     * @param path API endpoint to be queried
     * @return <tt>OAuthRequest</tt>
     */
    private OAuthRequest createOAuthRequest(String path) {
        OAuthRequest request = new OAuthRequest(Verb.GET, "http://" + API_HOST + path);
        return request;
    }

    /**
     * Sends an {@link OAuthRequest} and returns the {@link Response} body.
     *
     * @param request {@link OAuthRequest} corresponding to the API request
     * @return <tt>String</tt> body of API response
     */
    private String sendRequestAndGetResponse(OAuthRequest request) {
        this.service.signRequest(this.accessToken, request);
        Response response = request.send();
        return response.getBody();
    }

    static ArrayList<Restaurant> queryAPI(YelpAPI yelpApi, Double latitude, Double longitude) {
        return parseJSON(yelpApi.searchForBusinessesByLocation(latitude, longitude));
    }

    static ArrayList<Restaurant> queryAPI(YelpAPI yelpApi, String location) {
        return parseJSON(yelpApi.searchForBusinessesByLocation(location));
    }

    static ArrayList<Restaurant> parseJSON(String searchResponseJSON) {
        ArrayList<Restaurant> parsedRestaurants = new ArrayList<>();

        JSONParser parser = new JSONParser();
        JSONObject response = null;
        try {
            response = (JSONObject) parser.parse(searchResponseJSON);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }

        JSONArray businesses = (JSONArray) response.get("businesses");

		/*
		 * Parses the JSON to retrieve the name, rating, URL, phone, and address
		 * and populates the ArrayList parsedRestaurants
		 */
        for(int i = 0; i < businesses.size() - 1; i++) {
            JSONObject business = (JSONObject) businesses.get(i);

            parsedRestaurants.add(new Restaurant(business.get("name").toString(),
                    ((JSONObject) business.get("location")).get("address").toString(),
                    business.get("url").toString(), business.get("image_url").toString(),
                    business.get("display_phone").toString(), business.get("rating_img_url").toString(),
                    business.get("mobile_url").toString(), business.get("city").toString(),
                    business.get("postal_code").toString(), business.get("state_code").toString(),
                    (Double) business.get("rating"), (Double) ((JSONObject)((JSONObject) business.get("location")).get("coordinate")).get("latitude"),
                    (Double) ((JSONObject)((JSONObject) business.get("location")).get("coordinate")).get("longitude")));
        }

        return parsedRestaurants;
    }
}
