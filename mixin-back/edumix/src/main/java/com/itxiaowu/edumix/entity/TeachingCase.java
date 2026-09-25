package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 教学案例实体，保存富文本帖子内容、课程归属、访问方式及审核状态。 */
@Data
@TableName("teaching_case")
public class TeachingCase {
  @TableId private Long id;
  private String caseNo;
  private String caseName;
  private String summary;
  private String teachingObjective;
  private String keyPoints;
  private String difficultPoints;
  private String teachingProcess;
  private String teachingReflection;
  private String content;
  private String subject;
  private String coverUrl;
  private Long creatorId;
  private Long courseId;
  private String visibility;
  private String accessType;
  private Integer pointsPrice;
  private String auditStatus;
  private Integer excellent;
  private Integer referenceCount;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
