package com.iprody.common;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class DateRange {
    private Timestamp from;
    private Timestamp to;
}
