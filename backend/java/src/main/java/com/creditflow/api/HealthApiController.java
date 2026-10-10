package com.creditflow.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class HealthApiController {
  private final JdbcTemplate jdbc;
  public HealthApiController(JdbcTemplate jdbc) { this.jdbc=jdbc; }
  @GetMapping("/api/health") public Map<String,String> health() { jdbc.queryForObject("SELECT 1", Integer.class); return Map.of("status","UP","database","CONNECTED"); }
}
