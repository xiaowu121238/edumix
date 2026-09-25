package com.itxiaowu.edumix.dto;

/** AI 服务配置状态，用于前端展示当前模型是否可用。 */
public record AiStatusResponse(boolean configured, String model, String message) {}
