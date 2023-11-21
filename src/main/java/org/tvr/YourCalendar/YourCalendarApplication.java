package org.tvr.YourCalendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@SpringBootApplication
@ServletComponentScan
@EnableScheduling
public class YourCalendarApplication {
	public static void main(String[] args) {
		SpringApplication.run(YourCalendarApplication.class, args);
	}

}
