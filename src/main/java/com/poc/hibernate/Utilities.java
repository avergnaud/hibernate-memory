package com.poc.hibernate;

import org.apache.commons.lang.RandomStringUtils;

public class Utilities {
	public static String generatedRandomString(){
	    return RandomStringUtils.random(20000, true, true);
	}
}