package com.rise.trading.options;

import com.studerw.tda.client.HttpTdaClient;

public class BaseHandler {

	public HttpTdaClient getClient() {
		return Util.getHttpTDAClient();
	}
	
	public String toJSON(Object o) {
		return Util.toJSON(o);
	}

}
