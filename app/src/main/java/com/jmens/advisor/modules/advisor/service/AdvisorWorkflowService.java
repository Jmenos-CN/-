package com.jmens.advisor.modules.advisor.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.springframework.stereotype.Service;

@Service
public class AdvisorWorkflowService {

  @FunctionalInterface
  public interface AgentRunner {
    SingleAgentAnalysis run(String stockCode);
  }

  private final List<AgentRunner> agentRunners;
  private final Executor executor;

  public AdvisorWorkflowService(List<AgentRunner> agentRunners) {
    this.agentRunners = agentRunners;
    this.executor = Executors.newVirtualThreadPerTaskExecutor();
  }

  public List<SingleAgentAnalysis> runAgents(String stockCode) {
    return agentRunners.stream()
        .map(runner -> CompletableFuture.supplyAsync(() -> runner.run(stockCode), executor))
        .map(CompletableFuture::join)
        .sorted(Comparator.comparing(item -> item.role().ordinal()))
        .toList();
  }
}
