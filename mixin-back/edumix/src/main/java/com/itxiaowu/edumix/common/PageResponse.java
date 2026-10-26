package com.itxiaowu.edumix.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import java.util.function.Function;

/** 通用分页响应，前端可根据总条数和总页数绘制分页控件。 */
public record PageResponse<T>(List<T> records, long total, long page, long size, long pages) {

  /** 将数据库实体分页结果转换为接口 DTO，同时保留分页元数据。 */
  public static <S, T> PageResponse<T> from(IPage<S> source, Function<S, T> converter) {
    return new PageResponse<>(
        source.getRecords().stream().map(converter).toList(),
        source.getTotal(),
        source.getCurrent(),
        source.getSize(),
        source.getPages());
  }
}
