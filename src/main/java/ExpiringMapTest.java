import java.util.concurrent.TimeUnit;

import net.jodah.expiringmap.ExpiringMap;

public class ExpiringMapTest {

	public static void main(String[] args) throws Exception {
		// Map<String, String> map = new SelfExpiringHashMap<String, String>(1000 * 2 *
		// 1);
		ExpiringMap<String, String> map = ExpiringMap.builder().expiration(10, TimeUnit.SECONDS) // Set expiration time
				.build();
		map.put("siva", "siva");
		int counter = 0;
		for (;;) {
			Thread.sleep(1000);
			System.out.println(counter++ + "." + map.get("siva"));
		}

	}

}
