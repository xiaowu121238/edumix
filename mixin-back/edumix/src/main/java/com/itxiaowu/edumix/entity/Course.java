package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** 课程实体，保存课程基础信息及知识、能力、素质三个维度的教学目标。 */
@Data
@TableName("course")
public class Course {
  @TableId private Long id;
  private String courseCode;
  private String courseName;
  private String description;
  private String knowledgeObjective;
  private String abilityObjective;
  private String qualityObjective;
  private String coverUrl;
  private String college;
  private String semester;
  private Long ownerId;
  private Integer status;
  @TableLogic private Integer deleted;
  private LocalDateTime createTime;
  private LocalDateTime updateTime;
}
