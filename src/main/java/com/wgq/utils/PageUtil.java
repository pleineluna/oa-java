package com.wgq.utils;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 分页工具，对会议模块使用。其他模块均使用mybatis-plus的分页插件。
 */
@Data
public class PageUtil implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * 总记录数
     */
    private long totalCount;
    /**
     * 每页记录数
     */
    private int pageSize;
    /**
     * 总页数
     */
    private int totalPage;
    /**
     * 当前页数
     */
    private int pageIndex;
    /**
     * 列表数据
     */
    private List list;

    public PageUtil(List list, long totalCount, int pageIndex, int pageSize) {
        this.list = list;
        this.totalCount = totalCount;
        this.pageSize = pageSize;
        this.pageIndex = pageIndex;
        this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
    }

}
