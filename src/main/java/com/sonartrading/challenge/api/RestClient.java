package com.sonartrading.challenge.api;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RestClient {

	private RestAPI restAPI;

	public RestClient(String baseURL) {
		restAPI = new Retrofit.Builder().baseUrl(baseURL).addConverterFactory(GsonConverterFactory.create()).build()
				.create(RestAPI.class);
	}

	public RestAPI getAPI() {
		return restAPI;
	}
}
