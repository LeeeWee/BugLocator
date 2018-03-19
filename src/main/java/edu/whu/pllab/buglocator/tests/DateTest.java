package edu.whu.pllab.buglocator.tests;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DateTest {

	public static void main(String[] args) throws ParseException {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	    String str1 = "2012-02-12 12:12:12";
	    String str2 = "2012-01-18 12:12:13";
	    Date date1 = sdf.parse(str1);
	    Date date2 = sdf.parse(str2);
	    Calendar bef = Calendar.getInstance();
	    Calendar aft = Calendar.getInstance();
	    bef.setTime(date1);
	    aft.setTime(date2);
	    int result = aft.get(Calendar.MONTH) - bef.get(Calendar.MONTH);
	    int month = (aft.get(Calendar.YEAR) - bef.get(Calendar.YEAR)) * 12;
	    System.out.println(result);
	    System.out.println(month);
	    System.out.println(Math.abs(month + result));   
	    
	    Double monthDurationTime = ((double)(date1.getTime() - date2.getTime())) / 1000.0 / 3600.0 / 24.0 / 30.0;
	    System.out.println(monthDurationTime.intValue());
	    System.out.println(monthDurationTime);
		
		long timestamp = 1364400000L;
		Date date = new Date(timestamp * 1000);
		System.out.println(sdf.format(date));
	}
	
}
