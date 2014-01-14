package com.routon.calendar;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.Log;
import android.util.Xml;

public class CalendarXmlParser {

	private static final String ns = null;
	
	public List<Day> parse(int year, int month)
	{
		List<Day> month0 = new ArrayList<Day>();
		int []numDays = {31,30,31,30,31,30,31,31,30,31,30,31};
		for(int i = 0; i < numDays[month]; i++){
			month0.add(new Day());
		}
		return month0;
	}
	
	public List<Day> parse(InputStream stream) 
			throws XmlPullParserException, IOException
	{
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(stream, null);
            parser.nextTag();
            return readLjHdMessage(parser);
        } finally {
            stream.close();
        }
	}
	
	private List<Day> readLjHdMessage(XmlPullParser parser)
	{
//		List<Day> month = new ArrayList<Day>();
		List<Day> month = null;
		try {
			parser.require(XmlPullParser.START_TAG, ns, "jl-hd-message");

			while (parser.next() != XmlPullParser.END_TAG) {
			    if (parser.getEventType() != XmlPullParser.START_TAG) {
			        continue;
			    }
			    String name = parser.getName();
			    // Starts by looking for the day tag
			    if (name.equals("days")) {
			        month = readResultCode(parser);
			    } else {
			        skip(parser);
			    }
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return month;
	}

	private List<Day> readResultCode(XmlPullParser parser) 
			throws XmlPullParserException, IOException
	{
		Log.i("readResultCode", "readResultCode");
		List<Day> month = new ArrayList<Day>();
		
		parser.require(XmlPullParser.START_TAG, ns, "days");
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        // Starts by looking for the day tag
	        if (name.equals("day")) {
	            month.add(readDay(parser));
	        } else {
	            skip(parser);
	        }
	    }
		return month;
	}

	private void skip(XmlPullParser parser) throws XmlPullParserException, IOException {
	    if (parser.getEventType() != XmlPullParser.START_TAG) {
	        throw new IllegalStateException();
	    }
	    int depth = 1;
	    while (depth != 0) {
	        switch (parser.next()) {
	        case XmlPullParser.END_TAG:
	            depth--;
	            break;
	        case XmlPullParser.START_TAG:
	            depth++;
	            break;
	        }
	    }
	 }

	private Day readDay(XmlPullParser parser) throws XmlPullParserException, IOException
	{
		parser.require(XmlPullParser.START_TAG, ns, "day");
		
		String number = parser.getAttributeValue(null, "number");
		String shengxiao = parser.getAttributeValue(null, "shengxiao");
		String lunaryear = parser.getAttributeValue(null, "lunar-year");
		String lunaryearganzhi = parser.getAttributeValue(null, "lunar-year-ganzhi");
		String lunarmonth = parser.getAttributeValue(null, "lunar-month");
		String lunarmonthganzhi = parser.getAttributeValue(null, "lunar-month-ganzhi");
		String lunarday = parser.getAttributeValue(null, "lunar-day");
		String lunardayganzhi = parser.getAttributeValue(null, "lunar-day-ganzhi");
		String week = parser.getAttributeValue(null, "week");
		String holiday = parser.getAttributeValue(null, "holiday");
		
		String lunarfestival = null;
		String solarterm = null;
		String solarfestival = null;
		String suit = null;
		String avoid = null;
		
		
	    while (parser.next() != XmlPullParser.END_TAG) {
	        if (parser.getEventType() != XmlPullParser.START_TAG) {
	            continue;
	        }
	        String name = parser.getName();
	        if (name.equals("lunar-festival")) {
	            lunarfestival = readLunarFestival(parser);
	        } 
	        else if (name.equals("solar-term")) {
	            solarterm = readSolarterm(parser);
	        } 
	        else if (name.equals("solar-festival")) {
	            solarfestival = readSolarfestival(parser);
	        } 
	        else if(name.equalsIgnoreCase("suit")) {
	        	suit = readSuit(parser);
	        } 
	        else if (name.equalsIgnoreCase("avoid")) {
	        	avoid = readAvoid(parser);
	        } 
	        else {
	            skip(parser);
	        }
	    }
		
		return new Day(number, shengxiao, lunaryear, lunaryearganzhi,
				        lunarmonth, lunarmonthganzhi,
				        lunarday, lunardayganzhi,
				        week, holiday, lunarfestival,
				        solarterm, solarfestival,
				        suit, avoid);
	}

	private String readAvoid(XmlPullParser parser) {
        String avoid = null;
	    try {
	    	
			parser.require(XmlPullParser.START_TAG, ns, "avoid");
			avoid = readText(parser);
			parser.require(XmlPullParser.END_TAG, ns, "avoid");
			
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return avoid;
	}

	private String readSuit(XmlPullParser parser) {
		// TODO Auto-generated method stub
	    String suit = null;
		try{
			
	    parser.require(XmlPullParser.START_TAG, ns, "suit");
	    suit = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "suit");
	    
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return suit;
	}

	private String readSolarfestival(XmlPullParser parser) {
		// TODO Auto-generated method stub
		String solarfestival = null;
		try{
	    parser.require(XmlPullParser.START_TAG, ns, "solar-festival");
	    solarfestival = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "solar-festival");
	    
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	    return solarfestival;
	}

	private String readSolarterm(XmlPullParser parser) {
		// TODO Auto-generated method stub
		String solarterm = null;
		try{
	    parser.require(XmlPullParser.START_TAG, ns, "solar-term");
	    solarterm = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "solar-term");
	    
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return solarterm;
	}

	private String readLunarFestival(XmlPullParser parser) {
		// TODO Auto-generated method stub
		String lunarfestival = null;
		try{
	    parser.require(XmlPullParser.START_TAG, ns, "lunar-festival");
	    lunarfestival = readText(parser);
	    parser.require(XmlPullParser.END_TAG, ns, "lunar-festival");
	    
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    return lunarfestival;
	}
	
	private String readText(XmlPullParser parser) throws IOException, XmlPullParserException {
	    String result = "";
	    if (parser.next() == XmlPullParser.TEXT) {
	        result = parser.getText();
	        parser.nextTag();
	    }
	    return result;
	}

}


