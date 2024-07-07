package job.postings;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import com.rise.trading.options.Util;

public class iLaborCrawler {
	public static void main(String[] args) {
		// Set the path to the chromedriver executable
		System.setProperty("webdriver.chrome.driver", "/Users/sivad/Downloads/chromedriver");

		// Initialize WebDriver
		WebDriver driver = null;

		try {
			// Navigate to the initial website
			driver = new ChromeDriver();
			System.out.println("Driver is created");
			driver.get("https://vendor.ilabor360.com/login");

			// Perform necessary interactions to keep the session active
			performInteractions(driver);

			// Make requests to different URLs and extract data
			String dataFromUrl1 = requestData(driver, "https://vendor.ilabor360.com/showRequisitionViewList?searchTerm=&page=1&pageSize=500&skip=0&take=500");
			

			// Process the extracted data
			processExtractedData(dataFromUrl1);
		}catch(Exception e) {
			e.printStackTrace();
		}
		finally {
			// Close the driver
			driver.quit();
		}
	}

	private static void performInteractions(WebDriver driver) {
		// Perform necessary interactions to keep the session active
		// Example: Logging in
		//WebElement loginButton = driver.findElement(By.id("loginButton"));
		//loginButton.click();

		System.out.println("Entering login information");
		WebElement usernameField = driver.findElement(By.id("email"));
		usernameField.sendKeys(Util.getILaborUserName());

		WebElement passwordField = driver.findElement(By.id("password"));
		passwordField.sendKeys(Util.getILaborPassword());

		WebElement submitButton = driver.findElement(By.name("submit"));
		submitButton.click();
		System.out.println("Submit Button is clicked");

		// Wait for login to complete, if necessary
		try {
			Thread.sleep(5000); // Adjust the wait time as needed
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String requestData(WebDriver driver, String url) {
		System.out.println("Fetching data from requistions");
		// Navigate to the specified URL
		driver.get(url);

		// Extract data from the URL
		// Assuming the data you need is in an element with a specific ID
		//WebElement dataElement = driver.findElement(By.id("dataElementId"));
		//return dataElement.getText();
		return driver.getPageSource();
	}

	private static void processExtractedData(String data1) {
		// Process the extracted data
		// For example, printing the data
		System.out.println(data1);
	

		// Perform other operations with the data
		// ...
	}
}
