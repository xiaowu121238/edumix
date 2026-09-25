/**
 * 安全认证模块。
 *
 * <p>负责从数据库加载账号与角色、签发和解析 JWT、过滤每次请求、维护注销令牌黑名单， 并向控制器和服务提供可信的当前登录用户身份。
 */
package com.itxiaowu.edumix.security;
