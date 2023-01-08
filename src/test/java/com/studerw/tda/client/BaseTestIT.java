package com.studerw.tda.client;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import com.rise.trading.options.Util;

public abstract class BaseTestIT {

	static protected HttpTdaClient httpTdaClient;
	static protected Properties props = null;

	// @BeforeClass
	public static void beforeClassOld() {
		try (InputStream in = BaseTestIT.class.getClassLoader().getResourceAsStream("my-test.properties")) {
			props = new Properties();
			props.load(in);
		} catch (IOException e) {
			throw new IllegalStateException(
					"Could not load default properties from classpath at com.studerw.my-test.properties");
		}

		httpTdaClient = new HttpTdaClient(props);
	}

	// @BeforeClass
	public static void beforeClassOlder() {
		try {
			props = new Properties();
			String path = System.getProperty("user.dir");
			props.load(new FileInputStream(path + "/td-ameritrade/tda.properties"));
			httpTdaClient = new HttpTdaClient(props);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@BeforeClass
	public static void beforeClass() {
		httpTdaClient = Util.getHttpTDAClient();
	}

	String getAccountId() {
		return Util.getAccessProperties().getProperty("tda.account.id");
	}

	@AfterClass
	public static void afterClass() {
		httpTdaClient = null;
	}

}