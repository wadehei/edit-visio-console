package com.visualbuilder.common.core.result;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

@Data
public class PageResult<T> implements Serializable {
    private List<T> list;
    private Pagination pagination;

    @Data
    public static class Pagination implements Serializable {
        private long page;
        private long pageSize;
        private long total;
        private long totalPages;
    }

    public static <T> PageResult<T> of(List<T> list, long total, long page, long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        Pagination pagination = new Pagination();
        pagination.setPage(page);
        pagination.setPageSize(pageSize);
        pagination.setTotal(total);
        pagination.setTotalPages((total + pageSize - 1) / pageSize);
        result.setPagination(pagination);
        return result;
    }
}
