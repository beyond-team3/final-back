package com.monsoon.seedflowplus.domain.statistics.billing.repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class H2DateFormatFunctions {

    private H2DateFormatFunctions() {
    }

    public static String dateFormat(Date date, String pattern) {
        if (date == null) {
            return null;
        }

        LocalDate localDate = date.toLocalDate();
        String javaPattern = pattern
                .replace("%Y", "yyyy")
                .replace("%m", "MM")
                .replace("%d", "dd");

        return localDate.format(DateTimeFormatter.ofPattern(javaPattern));
    }
}
