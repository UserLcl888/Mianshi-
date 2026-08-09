package com.interview.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> {
    private List<T> list;
    private long page;
    private long size;
    private long total;
    private boolean hasMore;

    public static <T> PageResult<T> of(long page, long size, long total, List<T> list) {
        return new PageResult<>(list, page, size, total, page * size < total);
    }
}
