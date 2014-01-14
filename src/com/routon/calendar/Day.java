package com.routon.calendar;

/*
 * Day object
 */
public class Day{
	public String number;
	public String shengxiao;
	public String lunaryear;
	public String lunaryearganzhi;
	public String lunarmonth;
	public String lunarmonthganzhi;
	public String lunarday;
	public String lunardayganzhi;
	public String week;
	public String holiday;
	public String lunarfestival;
	public String solarterm;
	public String solarfestival;
	public String suit;
	public String avoid;
	public Day(String number,
				String shengxiao,
				String lunaryear,
				String lunaryearganzhi,
				String lunarmonth,
				String lunarmonthganzhi,
				String lunarday,
				String lunardayganzhi,
				String week,
				String holiday,
				String lunarfestival,
				String solarterm,
				String solarfestival,
				String suit,
				String avoid)
	{
			this.number = number;
			this.shengxiao = shengxiao;
			this.lunaryear = lunaryear;
			this.lunaryearganzhi = lunaryearganzhi;
			this.lunarmonth = lunarmonth;
			this.lunarmonthganzhi = lunarmonthganzhi;
			this.lunarday = lunarday;
			this.lunardayganzhi = lunardayganzhi;
			this.week = week;
			this.holiday = holiday;
			this.lunarfestival = lunarfestival;
			this.solarterm = solarterm;
			this.solarfestival = solarfestival;
			this.suit = suit;
			this.avoid = avoid;
	}
	public Day(){
		number 			 = "";
		shengxiao 		 = "";
		lunaryear 		 = "";
		lunaryearganzhi  = "";
		lunarmonth 		 = "";
		lunarmonthganzhi = "";
		lunarday 		 = "";
		lunardayganzhi 	 = "";
		week 			 = "0";
		holiday 		 = "-1";
		lunarfestival 	 = "";
		solarterm 		 = "";
		solarfestival 	 = "";
		suit 			 = "";
		avoid 			 = "";
	}
	

}
