package com.jmens.advisor.modules.advisor.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jmens.advisor.modules.advisor.service.AdvisorWorkflowService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdvisorControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    AdvisorWorkflowService workflowService = new AdvisorWorkflowService(List.of());
    mockMvc = MockMvcBuilders.standaloneSetup(new AdvisorController(workflowService)).build();
  }

  @Test
  void analyzeReturnsSuccess() throws Exception {
    mockMvc.perform(post("/api/advisor/analyze")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"query\":\"帮我分析600519\",\"analysisType\":\"full\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.success").value(true));
  }
}
