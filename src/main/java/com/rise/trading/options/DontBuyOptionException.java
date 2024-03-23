package com.rise.trading.options;

public class DontBuyOptionException extends RuntimeException{
	
	public DontBuyOptionException(String s) {
		super(s);
	}
	
	public DontBuyOptionException() {
		super();
	}

}
