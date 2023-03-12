package com.rise.trading.options.passive.income.backtest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import com.rise.trading.options.Util;
import com.studerw.tda.client.HttpTdaClient;
import com.studerw.tda.model.history.Candle;
import com.studerw.tda.model.history.FrequencyType;
import com.studerw.tda.model.history.PriceHistReq;
import com.studerw.tda.model.history.PriceHistReq.Builder;
import com.studerw.tda.model.history.PriceHistory;

public class HistoricalPricesFetcher {

	private HttpTdaClient httpTdaClient = Util.getHttpTDAClient();

	public static void main(String args[]) throws Exception {
		HistoricalPricesFetcher fetcher = new HistoricalPricesFetcher();
		LocalDate date = LocalDate.parse("2005-01-01");
		for (int i = 0; i < 10; i++) {
			List<String> lines = fetcher.fetchHistoricalPricesFromEODHistoricalData("QQQQ", date);
			System.out.println(date + " -> " + lines.size());
			date = date.plusDays(1);
		}
	}

	public static void mainOld(String[] args) {
		LocalDate d = LocalDate.parse("2020-01-01");
		ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/Oslo");
		long epoch = d.atStartOfDay(zoneId).toEpochSecond() * 1000;
		Date dx = new Date(epoch);
		System.out.println(dx);
	}

	public HistoricalPricesForMultipleDays getHistoricalPrices(String ticker) {
		return getHistoricalPrices(ticker, ((e) -> {
			return true;
		}));
	}

	private HistoricalPricesForMultipleDays getHistoricalPrices(String ticker, DayParser parser) {
		String dir = DIRECTORY + "/" + ticker;
		File d = new File(dir);
		HistoricalPricesForMultipleDays hpmd = new HistoricalPricesForMultipleDays();
		hpmd.ticker = ticker;
		File f[] = d.listFiles();
		for (File x : f) {
			String name = x.getName();
			if (parser.canParse(name) == false) {
				continue;
			}
			HistoricalPricesForDay hpd = makeHistoricalPricesForDay(x, ticker);
			if (hpd == null) {
				continue;
			}

			
			hpmd.addHistoricalPricesForDay(x.getName(), hpd);

		}

		return hpmd;
	}

	public HistoricalPricesForMultipleDays getHistoricalPrices(String ticker, List<String> days) {

		return getHistoricalPrices(ticker, ((e) -> {
			boolean b = days.contains(e);
			return b;
		}));
	}

	interface DayParser {
		boolean canParse(String fileName);
	}

	String HISTORICAL_API = "https://eodhistoricaldata.com/api/intraday/{0}.US?api_token={1}&interval=1m&from={2}&to={3}";

	public List<String> fetchHistoricalPricesFromEODHistoricalData(String ticker, LocalDate date) throws Exception {

		LocalDate ld = date;
		System.out.println(ld);
		long start = seconds(ld);
		// long start = d.getTime();
		long end = start + (1000 * 60 * 60 * 24);
		// long start = getUnixTimeStamp(year, month, 1);
		// long end = getUnixTimeStamp(year, month+4, 1);
		String URL = MessageFormat.format(HISTORICAL_API, ticker, Util.getEODToken(), (start / 1000) + "",
				(end / 1000) + "");
		System.out.println(URL);
		return Util.readLinesFromURL(URL);
	}

	public long getUnixTimeStamp(int year, int month, int day) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DAY_OF_MONTH, day);
		cal.set(Calendar.MONTH, month);
		cal.set(Calendar.YEAR, year);

		Date givenDate = cal.getTime();
		long unixTimestamp = givenDate.getTime() / 1000L;
		System.out.println("Unix timestamp: " + unixTimestamp);
		return unixTimestamp;
	}

	private HistoricalPricesForDay makeHistoricalPricesForDay(File x, String ticker) {
		// System.out.println(x);
		File xx[] = x.listFiles();
		if (xx == null) {
			return null;
		}

		for (File file : xx) {
			if (file.getName().contentEquals("historical_prices.csv")) {
				return createHistoricalPricesForDay(file, ticker, new LineParser() {

					@Override
					public HistoricalPriceForMinute parseLine(String l) {
						return makeHistoricalPriceForMinute(l);
					}

				});
			}
			if (file.getName().contentEquals("historical_prices_eod.csv")) {
				return createHistoricalPricesForDay(file, ticker, new LineParser() {

					@Override
					public HistoricalPriceForMinute parseLine(String l) {
						return makeHistoricalPriceForMinuteEOD(l);
					}
				});
			}
		}
		return null;
	}

	DateTimeFormatter df = DateTimeFormatter.ofPattern("d-MM-yyyy");
	private HistoricalPricesForDay createHistoricalPricesForDay(File file, String ticker, LineParser lp) {
		HistoricalPricesForDay day = new HistoricalPricesForDay();
		day.ticker = ticker;
		day.day = LocalDate.parse(file.getParentFile().getName(), df);

		try {
			List<String> lines = Files.readAllLines(Paths.get(file.toURI()));

			if (lines.size() == 0) {
				// System.out.println(file.getAbsolutePath()+" has no content");
				return day;
			}
			lines.remove(0);
			for (String l : lines) {
				HistoricalPriceForMinute minute = lp.parseLine(l);
				if (minute == null)
					continue;
				day.addHistoricalPriceForMinute(minute);
			}
			return day;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}

	}

	private HistoricalPriceForMinute makeHistoricalPriceForMinute(String l) {
		String x[] = l.split(",");
		HistoricalPriceForMinute minute = new HistoricalPriceForMinute();
		minute.timestamp = Long.parseLong(x[0]);
		minute.d = new Date(minute.timestamp);
		minute.time = new java.sql.Time(minute.timestamp).toString();
		minute.open = Double.parseDouble(x[2]);
		minute.low = Double.parseDouble(x[3]);
		minute.high = Double.parseDouble(x[4]);
		minute.close = Double.parseDouble(x[5]);
		minute.volume = Double.parseDouble(x[6]);

		return minute;
	}

	private HistoricalPriceForMinute makeHistoricalPriceForMinuteEOD(String l) {
		try {
			String x[] = l.split(",");
			HistoricalPriceForMinute minute = new HistoricalPriceForMinute();
			minute.timestamp = Long.parseLong(x[0]) * 1000;
			minute.d = new Date(minute.timestamp);
			minute.time = new java.sql.Time(minute.timestamp).toString();
			minute.open = Double.parseDouble(x[3]);
			minute.high = Double.parseDouble(x[4]);
			minute.low = Double.parseDouble(x[5]);
			minute.close = Double.parseDouble(x[6]);
			minute.volume = Double.parseDouble(x[7]);

			return minute;
		} catch (RuntimeException e) {
			System.out.println(l + " -> " + e);
			// TODO Auto-generated catch block
			return null;
		}
	}

	interface LineParser {
		HistoricalPriceForMinute parseLine(String l);
	}

	public static void mainOldest(String[] args) throws Exception {
		// TODO Auto-generated method stub
		HistoricalPricesFetcher hp = new HistoricalPricesFetcher();

		// LocalDate date = LocalDate.now();
		LocalDate date = LocalDate.parse("2011-01-01");
		// LocalDate yesterday = date.minusDays(1);
		// hp.fetchHistoricalPrices("QQQ", seconds(date), System.out);
		String days[] = new String[] { "2021-01-03", "2021-01-04", "2021-01-05", "2021-01-06" };
		for (String d : days) {
			date = LocalDate.parse(d);
			// LocalDate yesterday = date.minusDays(1);
			hp.fetchHistoricalPrices("MSFT", seconds(date), System.out);
		}
		// hp.fetchAndPotentialStoreHistoricalPrices("QQQ", date);

	}

	public void fetchAndPotentialStoreHistoricalPrices(String ticker, LocalDate start) throws Exception {
		String dir = DIRECTORY + "/" + ticker;
		File d = new File(dir);
		if (d.exists() == false) {
			d.mkdirs();
		}
		File file = new File(d + "/" + toDate(start));
		if (file.exists() == false) {
			file.mkdirs();
		}
		file = new File(file, "historical_prices.csv");
		if (file.exists() == true) {
			return;
		}
		PrintStream out = new PrintStream(new FileOutputStream(file));
		Long seconds = seconds(start);
		fetchHistoricalPrices(ticker, seconds, out);
		out.flush();

	}

	public void fetchAndPotentialStoreHistoricalPricesFromEOD(String ticker, LocalDate start) throws Exception {
		String dir = DIRECTORY + "/" + ticker;
		File d = new File(dir);
		if (d.exists() == false) {
			d.mkdirs();
		}
		File file = new File(d + "/" + toDate(start));
		if (file.exists() == false) {
			file.mkdirs();
		}
		file = new File(file, "historical_prices_eod.csv");
		if (file.exists() == true) {
			return;
		}
		PrintStream out = new PrintStream(new FileOutputStream(file));
		// Long seconds = seconds(start);
		// fetchHistoricalPrices(ticker, seconds, out);
		List<String> lines = fetchHistoricalPricesFromEODHistoricalData(ticker, start);
		System.out.println(lines.size());
		for (String l : lines) {
			out.println(l);
		}
		out.flush();

	}

	public void fetchAndPotentialStoreHistoricalPrices(String ticker, LocalDate start, LocalDate end) throws Exception {
		LocalDate x = start;

		while (x.isBefore(end)) {
			// System.out.println(x);
			fetchAndPotentialStoreHistoricalPrices(ticker, x);
			x = x.plusDays(1);
		}
	}

	public void fetchAndPotentialStoreHistoricalPricesEOD(String ticker, LocalDate start, LocalDate end)
			throws Exception {
		LocalDate x = start;

		while (x.isBefore(end)) {
			// System.out.println(x);
			fetchAndPotentialStoreHistoricalPricesFromEOD(ticker, x);
			x = x.plusDays(1);
		}
	}

	public String toDate(LocalDate date) {

		DateTimeFormatter formatters = DateTimeFormatter.ofPattern("d-MM-uuuu");
		String text = date.format(formatters);
		return text;
	}

	static String DIRECTORY = "/Users/sivad/historical_prices";

	// static String EOD_HISTORICAL = "/Users/sivad/eod_historical";

	public static long seconds(LocalDate d) {
		ZoneId zoneId = ZoneId.systemDefault(); // or: ZoneId.of("Europe/Oslo");
		long epoch = d.atStartOfDay(zoneId).toEpochSecond();
		return epoch * 1000;
		// return Timestamp.valueOf(d).getTime();
	}

	public void fetchHistoricalPrices(String symbol, long startDate, PrintStream out) {

		PriceHistReq request = Builder.priceHistReq().withSymbol(symbol).withStartDate(startDate)
				.withEndDate(startDate + (1000 * 60 * 60 * 18)).withFrequencyType(FrequencyType.minute).withFrequency(1)
				// .withExtendedHours(false)
				.build();
		// System.out.println(request.toString());
		PriceHistory priceHistory = null;
		try {
			priceHistory = httpTdaClient.priceHistory(request);
			// return;
		} catch (Exception e) {
			System.out.println(startDate + " -> " + new java.util.Date(startDate) + " - > " + e.getMessage());
			return;
		}

		// System.out.println(priceHistory.getCandles().size());
		out.println("Long,Date,Open,Low,High,Close,Volume");
		System.out.println(
				startDate + " -> " + new java.util.Date(startDate) + " -> " + priceHistory.getCandles().size());
		int counter = 0;
		for (Candle c : priceHistory.getCandles()) {

			out.println(c.getDatetime() + "," + new Date(c.getDatetime()) + "," + c.getOpen() + "," + c.getLow() + ","
					+ c.getHigh() + "," + c.getClose() + "," + c.getVolume());
			counter++;
			// if (counter == 1)
			// break;
			// out.println(Util.toJSON(c));
		}
		/*
		 * assertThat(priceHistory).isNotNull();
		 * assertThat(priceHistory.getCandles().size()).isGreaterThan(1);
		 * assertThat(priceHistory.getSymbol()).isEqualTo("VGIAX");
		 * assertThat(priceHistory.isEmpty()).isFalse();
		 * LOGGER.debug(priceHistory.toString());
		 * 
		 * Candle candle = priceHistory.getCandles().get(0);
		 * LOGGER.debug(candle.toString());
		 * assertThat(candle.getOpen()).isGreaterThan(new BigDecimal("1.00"));
		 * assertThat(candle.getHigh()).isGreaterThan(new BigDecimal("1.00"));
		 * assertThat(candle.getLow()).isGreaterThan(new BigDecimal("1.00"));
		 * assertThat(candle.getClose()).isGreaterThan(new BigDecimal("1.00")); //should
		 * not have taken more than 20 seconds assertThat(candle.getDatetime() -
		 * now).isLessThan(20000);
		 */

	}

	public HistoricalPricesForDay getHistoricalPricesForDay(String ticker, LocalDate day) {
		DateTimeFormatter df = DateTimeFormatter.ofPattern("d-MM-yyyy");
		String dir = DIRECTORY + "/" + ticker + "/" + day.format(df);
		File d = new File(dir);
		return makeHistoricalPricesForDay(d, ticker);
		
	}

}
