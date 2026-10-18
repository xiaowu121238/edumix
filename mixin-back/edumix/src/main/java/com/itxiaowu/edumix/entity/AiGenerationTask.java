package com.itxiaowu.edumix.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/** AI 生成任务实体，记录调用场景、输入摘要、生成状态和结果。 */
@Data
@TableName("ai_generation_task")
public class AiGenerationTask {
  @TableId private Long id;
  private String taskNo;
  private Long userId;
  private Long courseId;
  private String taskType;
  private String requirementText;
  private String inputJson;
  private String resultJson;
  private String status;
  private String errorMessage;
  private LocalDateTime startedTime;
  private LocalDateTime finishedTime;
  private LocalDateTime createTime;
}
