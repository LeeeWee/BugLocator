package edu.whu.pllab.buglocator.tests;

public class SwitchTest {

	public static void main(String[] args) {
		int a = 13;
		switch (a = 10) {
		default:
            System.out.println("Error!");
        case 1:
        case 2:
        case 3:
        case 4:
        case 5:
            System.out.println("Spring");
            break;
        case 6:
        case 7:
        case 8:
            System.out.println("Summer");
            break;
        case 9:
        case 10:
        case 11:
        case 12:
            System.out.println("Fall");
            break;
        }
	}
	
	
}
