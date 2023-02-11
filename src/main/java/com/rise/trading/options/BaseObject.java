package com.rise.trading.options;

import java.io.Serializable;

public class BaseObject {

	public String toString() {
		if (this instanceof ToJSONString) {
			return Util.toJSON(this);
		}
		return super.toString();
	}
}
