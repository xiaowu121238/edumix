package com.itxiaowu.edumix.mapper;

import com.itxiaowu.edumix.entity.ResourceFavorite;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 资源收藏数据访问接口，负责收藏关系和个人收藏列表查询。 */
public interface ResourceFavoriteMapper {
  @Insert(
      "INSERT IGNORE INTO resource_favorite(user_id, resource_id) VALUES(#{userId}, #{resourceId})")
  int add(Long userId, Long resourceId);

  @Delete("DELETE FROM resource_favorite WHERE user_id=#{userId} AND resource_id=#{resourceId}")
  int remove(Long userId, Long resourceId);

  @Select(
      "SELECT COUNT(*) FROM resource_favorite WHERE user_id=#{userId} AND resource_id=#{resourceId}")
  int exists(Long userId, Long resourceId);

  @Select("SELECT user_id, resource_id, create_time FROM resource_favorite WHERE user_id=#{userId}")
  List<ResourceFavorite> findByUser(Long userId);

  /** 只查询当前分页中已收藏的资源，避免每翻一页都加载用户的全部收藏记录。 */
  @Select({
    "<script>",
    "SELECT resource_id FROM resource_favorite WHERE user_id=#{userId} AND resource_id IN",
    "<foreach collection='resourceIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
    "</script>"
  })
  List<Long> findIdsInPage(
      @Param("userId") Long userId, @Param("resourceIds") List<Long> resourceIds);
}
