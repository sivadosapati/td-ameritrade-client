# TD Ameritrade Java Client
![TDA_LOGO](https://github.com/studerw/td-ameritrade-client/blob/master/td_logo.png)

![travisci-passing](https://api.travis-ci.org/studerw/td-ameritrade-client.svg?branch=old-xml-api)
[![APL v2](https://img.shields.io/badge/license-Apache%202-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)

----
[API Documentation](http://td-ameritrade-client.studerw.com.s3-website-us-east-1.amazonaws.com/).

Java rest client for TD Ameritrade Api. Uses [OKHttp 3](https://github.com/square/okhttp) under the hood.

Uses the new [TDA OAuth2 API](https://developer.tdameritrade.com/).

I'm happy to collaborate contractually or OSS with other devs. 

## 2019 

See the [old-xml-api](https://github.com/studerw/td-ameritrade-client/tree/old-xml-api) branch for the previous project based on the soon-to-be-deprecated TDA XML API.

Sometime in-between the beginning of this project (based on TDA's older XML API) and now, TDA released a restful [API](https://developer.tdameritrade.com/). 
Unfortunately the old API is being [deprecated in 2020](https://apiforums.tdameritrade.com/tda-board/ubbthreads.php) and so the
original source code for this project has been moved to the [old-xml-api](https://github.com/studerw/td-ameritrade-client/tree/old-xml-api) branch.

## Build

To build the jar, checkout the source and run:

```bash
mvn clean install
```

## Usage
Until the project is finished, you will need to have built this locally in order to put the necessary jars in your local Maven repo.
Once we have a 1.0.0 version, it will be submitted to Maven Central. 

Add the following to your Maven build file:

```
  <dependency>
    <groupId>com.studerw.tda</groupId>
    <artifactId>td-ameritrade-client</artifactId>
    <version>2.0-SNAPSHOT</version>
  </dependency>
```
----
You need to obtain a valid TDA Developer *refresh token* every 90 days. See TDA's [Simple Auth for Local Apps](https://developer.tdameritrade.com/content/simple-auth-local-apps).

```
  TdaClient tdaClient = new HttpTdaClient();
  final Quote quote = tdaClient.fetchQuote("msft");
  EquityQuote equityQuote = (EquityQuote) quote;
  System.out.println("Current price of MSFT: " + equityQuote.getAskPrice());
```

In java, you will get a `Quote` pojo. All of the response objects extend the base `Quote`
which can then be casted to its actual type.

## DateTime Handling
Most TDA dates and times are returned as longs (i.e. milliseconds since the epoch UTC).
An easy way to convert them to Java's new DateTime is via the following:

```java
long someDateTime = ...
ZonedDateTime dateTime = Instant.ofEpochMilli(someDateTime).atZone(ZoneId.systemDefault());
```
Or you could use the deprecated _java.util.Date_.

```java
long someDateTime = ...
Date date = new Date(someDateTime);
```

To convert a long to human readable ISO 8601 String, use the following:
```java
long currentTime = System.currentTimeMillis();
String formattedDate = FormatUtils.epochToStr(currentTime);
System.out.println(formattedDate) //   2019-09-13T19:59-04:00[America/New_York]
```

## Error Handling

Before the call is even made, validator or other exceptions can be thrown. Usually you won't have to catch these in your program, they'll be helpful
when testing, though.

Once the call is made, the TDA server returns 200 success responses **even if the call was not successful**, for example you've sent an invalid request type 
or some other issue. Often this means the body is an empty JSON string.

The rules are this within the Client:

* Most validation exception before the call was made will throw unchecked `IllegalArgumentException`.

* All non 200 HTTP responses throw unchecked `RuntimeExceptions` since there is no way to recover, usually.

*  Responses that are completely empty but should have returned a full json body throw a `RunTimeException` also.

* If there is an error parsing the JSON into a Java pojo, the `RuntimeException` wrapping the `IOException` from Jackson will be thrown.
 
The only exception to this rule is if we cannot login - either due to bad credentials, locked account, or otherwise.
When this occurs, an `IllegalStateException` is thrown. This is explicitly signalled by a 401 response code.  


## Integration Tests
Integration tests do require a Client App ID user and refresh token, though are not needed to build the jar.

To run integration tests, you will need to rename this file *src/test/resources/my-test.properties.changeme* to *my-test.properties* and fill in the 
necessary TDA properties.

Then run the following command.

```
mvn failsafe:integration-test
```

## Login Parameters
The client only requires a Client ID and Refresh Token. The refresh token expires every 90 days.
See the [Simple Auth for Local Apps](https://developer.tdameritrade.com/content/simple-auth-local-apps) for help.

