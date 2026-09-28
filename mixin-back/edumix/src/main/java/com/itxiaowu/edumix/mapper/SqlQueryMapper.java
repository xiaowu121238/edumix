package com.itxiaowu.edumix.mapper;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/**
 * 通用 SQL 数据访问入口。
 *
 * <p>用于承接暂时不适合单表 MyBatis-Plus Mapper 的多表统计和事务 SQL，使 ServiceImpl 不直接依赖数据库连接实现。
 * 后续某一模块查询增多时，应继续拆分为独立的领域 Mapper。
 */
@Repository
@RequiredArgsConstructor
public class SqlQueryMapper {
  private final JdbcTemplate jdbc;

  public int update(String sql, Object... args) {
    return jdbc.update(sql, args);
  }

  public int update(PreparedStatementCreator statementCreator, KeyHolder keyHolder) {
    return jdbc.update(statementCreator, keyHolder);
  }

  public <T> T queryForObject(String sql, Class<T> requiredType, Object... args) {
    return jdbc.queryForObject(sql, requiredType, args);
  }

  public <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) {
    return jdbc.queryForObject(sql, rowMapper, args);
  }

  public Map<String, Object> queryForMap(String sql, Object... args) {
    return jdbc.queryForMap(sql, args);
  }

  public <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) {
    return jdbc.queryForList(sql, elementType, args);
  }

  public List<Map<String, Object>> queryForList(String sql, Object... args) {
    return jdbc.queryForList(sql, args);
  }

  public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
    return jdbc.query(sql, rowMapper, args);
  }
}
