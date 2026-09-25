package com.itxiaowu.edumix.mapper;

import com.itxiaowu.edumix.entity.CaseFavorite;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/** 案例收藏数据访问接口，负责收藏关系查询、创建和删除。 */
public interface CaseFavoriteMapper {
  @Insert("INSERT IGNORE INTO case_favorite(user_id, case_id) VALUES(#{userId}, #{caseId})")
  int add(Long userId, Long caseId);

  @Delete("DELETE FROM case_favorite WHERE user_id=#{userId} AND case_id=#{caseId}")
  int remove(Long userId, Long caseId);

  @Select("SELECT COUNT(*) FROM case_favorite WHERE user_id=#{userId} AND case_id=#{caseId}")
  int exists(Long userId, Long caseId);

  @Select(
      "SELECT user_id, case_id, create_time FROM case_favorite WHERE user_id=#{userId} ORDER BY create_time DESC")
  List<CaseFavorite> findByUser(Long userId);
}
