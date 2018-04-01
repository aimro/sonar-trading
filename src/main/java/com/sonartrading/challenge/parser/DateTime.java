package com.sonartrading.challenge.parser;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

public class DateTime {

	public static final DateTimeFormatter DATE_TIME_FORMATTER = new DateTimeFormatterBuilder()
		    // date/time
		    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
		    // offset (hh:mm - "+00:00" when it's zero)
		    .optionalStart().appendOffset("+HH:MM", "+00:00").optionalEnd()
		    // offset (hhmm - "+0000" when it's zero)
		    .optionalStart().appendOffset("+HHMM", "+0000").optionalEnd()
		    // offset (hh - "Z" when it's zero)
		    .optionalStart().appendOffset("+HH", "Z").optionalEnd()
		    // create formatter
		    .toFormatter();
}
