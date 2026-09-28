package com.example.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.Formatter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addFormatterForFieldType(LocalDateTime.class, new Formatter<LocalDateTime>() {
            @Override
            public LocalDateTime parse(String text, Locale locale) throws ParseException {
                if (text == null || text.trim().isEmpty()) {
                    return null;
                }
                String s = text.trim();
                String[] patterns = {
                    "dd/MM/yyyy HH:mm",
                    "dd/MM/yyyy'T'HH:mm",
                    "dd/MM/yyyy HH:mm:ss",
                    "yyyy-MM-dd'T'HH:mm",
                    "yyyy-MM-dd'T'HH:mm:ss",
                    "yyyy-MM-dd HH:mm"
                };
                for (String p : patterns) {
                    try {
                        return LocalDateTime.parse(s, DateTimeFormatter.ofPattern(p));
                    } catch (Exception ignored) {
                    }
                }
                try {
                    return LocalDateTime.parse(s);
                } catch (Exception e) {
                    throw new ParseException("Định dạng ngày giờ không hợp lệ: " + text, 0);
                }
            }

            @Override
            public String print(LocalDateTime object, Locale locale) {
                if (object == null) {
                    return "";
                }
                return object.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
        });
    }
}
