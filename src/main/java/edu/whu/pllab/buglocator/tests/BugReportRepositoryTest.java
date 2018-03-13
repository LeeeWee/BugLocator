package edu.whu.pllab.buglocator.tests;

import edu.whu.pllab.buglocator.Property;
import edu.whu.pllab.buglocator.common.BugReportRepository;

public class BugReportRepositoryTest {
	public static void main(String[] args) {
		Property property = Property.loadInstance();
		BugReportRepository brRepo = new BugReportRepository();
	}
}
