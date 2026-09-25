/**
 * HTTP 接口层。
 *
 * <p>负责接收和校验前端参数、读取当前登录用户、声明角色权限并返回统一响应。账号认证、课程体系、 教学资源、教学案例、审核、积分、AI、私信和消息中心分别由对应 Controller
 * 对外提供接口； 事务和状态流转统一交给 Service，避免在控制器中重复业务规则。
 */
package com.itxiaowu.edumix.controller;
