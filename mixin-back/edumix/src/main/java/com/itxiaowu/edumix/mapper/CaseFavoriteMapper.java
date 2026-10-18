package com.itxiaowu.edumix.mapper;

import com.itxiaowu.edumix.entity.CaseFavorite;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
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

  /** 只查询当前分页中已收藏的案例，避免分页查询退化为全量收藏加载。 */
  @Select({
    "<script>",
    "SELECT case_id FROM case_favorite WHERE user_id=#{userId} AND case_id IN",
    "<foreach collection='caseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
    "</script>"
  })
  List<Long> findIdsInPage(@Param("userId") Long userId, @Param("caseIds") List<Long> caseIds);
}
