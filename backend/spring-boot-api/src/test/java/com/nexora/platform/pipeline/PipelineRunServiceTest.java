package com.nexora.platform.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nexora.platform.databricks.DatabricksJobsClient;
import com.nexora.platform.databricks.DatabricksJobsClient.RunStatus;
import com.nexora.platform.databricks.DatabricksProperties;

@ExtendWith(MockitoExtension.class)
class PipelineRunServiceTest {

    @Mock
    private DatabricksJobsClient jobsClient;
    @Mock
    private PipelineRunRepository repository;

    private final DatabricksProperties properties = new DatabricksProperties("host", "token", 42L, "warehouse", 60000);

    private PipelineRunService service() {
        return new PipelineRunService(jobsClient, properties, repository);
    }

    @Test
    void triggerRunStartsTheConfiguredJobAndPersistsItAsPending() {
        when(jobsClient.runNow(42L)).thenReturn(777L);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PipelineRunDto result = service().triggerRun("angular-ui");

        assertThat(result.jobId()).isEqualTo(42L);
        assertThat(result.databricksRunId()).isEqualTo(777L);
        assertThat(result.lifecycleState()).isEqualTo("PENDING");
        assertThat(result.triggeredBy()).isEqualTo("angular-ui");
    }

    @Test
    void refreshRunPollsDatabricksWhenTheStoredRunIsNotYetTerminal() {
        PipelineRun run = new PipelineRun(42L, 777L, "RUNNING", "angular-ui", Instant.now());
        when(repository.findById(1L)).thenReturn(Optional.of(run));
        when(jobsClient.getRunStatus(777L)).thenReturn(new RunStatus("TERMINATED", "SUCCESS", null));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PipelineRunDto result = service().refreshRun(1L);

        assertThat(result.lifecycleState()).isEqualTo("TERMINATED");
        assertThat(result.resultState()).isEqualTo("SUCCESS");
        assertThat(result.completedAt()).isNotNull();
    }

    @Test
    void refreshRunSkipsTheDatabricksCallWhenTheStoredRunIsAlreadyTerminal() {
        PipelineRun run = new PipelineRun(42L, 777L, "TERMINATED", "angular-ui", Instant.now());
        when(repository.findById(1L)).thenReturn(Optional.of(run));

        service().refreshRun(1L);

        verify(jobsClient, never()).getRunStatus(anyLong());
        verify(repository, never()).save(any());
    }

    @Test
    void refreshRunThrowsForAnUnknownRunId() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(java.util.NoSuchElementException.class,
                () -> service().refreshRun(99L));
    }
}
