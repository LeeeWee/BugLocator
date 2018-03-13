package edu.whu.pllab.buglocator.tests;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTest {

	public static void main(String[] args) {
		long timestamp = 1364400000L;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = new Date(timestamp * 1000);
		System.out.println(sdf.format(date));
	}
	
}
