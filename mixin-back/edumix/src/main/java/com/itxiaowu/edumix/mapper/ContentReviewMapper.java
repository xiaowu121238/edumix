package com.itxiaowu.edumix.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.itxiaowu.edumix.entity.ContentReview;
import org.apache.ibatis.annotations.Mapper;

/** 统一内容审核记录的数据访问接口。 */
@Mapper
public interface ContentReviewMapper extends BaseMapper<ContentReview> {}
