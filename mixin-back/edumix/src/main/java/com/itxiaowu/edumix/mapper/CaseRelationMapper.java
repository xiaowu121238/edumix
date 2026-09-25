package com.itxiaowu.edumix.mapper;

import java.util.List;
import org.apache.ibatis.annotations.*;

public interface CaseRelationMapper {
  @Select("SELECT knowledge_point_id FROM case_knowledge_point WHERE case_id=#{caseId}")
  List<Long> pointIds(Long caseId);

  @Select("SELECT resource_id FROM case_resource WHERE case_id=#{caseId} ORDER BY sort_order")
  List<Long> resourceIds(Long caseId);

  @Insert(
      "INSERT INTO case_knowledge_point(case_id,knowledge_point_id) VALUES(#{caseId},#{pointId})")
  int addPoint(Long caseId, Long pointId);

  @Insert(
      "INSERT INTO case_resource(case_id,resource_id,sort_order) VALUES(#{caseId},#{resourceId},#{sortOrder})")
  int addResource(Long caseId, Long resourceId, Integer sortOrder);

  @Delete("DELETE FROM case_knowledge_point WHERE case_id=#{caseId}")
  int deletePoints(Long caseId);

  @Delete("DELETE FROM case_resource WHERE case_id=#{caseId}")
  int deleteResources(Long caseId);
}
