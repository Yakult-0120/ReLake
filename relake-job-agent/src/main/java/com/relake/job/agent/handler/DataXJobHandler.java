package com.relake.job.agent.handler;

import com.relake.common.dto.DataXConfigDTO;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * DataX 同步任务 XXL-JOB Handler
 * <p>
 * 由 XXL-JOB Admin 调度触发，从 Integration 内部 API 获取完整 DataX 配置，
 * 通过 ProcessBuilder 执行 DataX 命令，将 stdout 输出到 XXL-JOB 日志。
 */
@Component
public class DataXJobHandler {

    @Value("${datax.home:/opt/datax}")
    private String dataxHome;

    @Value("${relake.integration.url:http://host.docker.internal:8083}")
    private String integrationUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    @XxlJob("dataxSync")
    public void execute() {
        String param = XxlJobHelper.getJobParam();
        Long taskId = Long.parseLong(param);
        XxlJobHelper.log("DataX 任务启动: taskId={}", taskId);

        DataXConfigDTO config;
        try {
            // 1. 回调 Integration 内部 API 获取完整 DataX 配置
            String url = integrationUrl + "/internal/tasks/" + taskId + "/datax-config";
            config = restTemplate.getForObject(url, DataXConfigDTO.class);
            if (config == null || config.getJobJson() == null) {
                XxlJobHelper.handleFail("获取 DataX 配置失败: taskId=" + taskId);
                return;
            }
            XxlJobHelper.log("DataX 配置获取成功: taskId={}, command={}, args={}",
                    taskId, config.getCommand(), config.getArgs());
        } catch (Exception e) {
            XxlJobHelper.handleFail("获取 DataX 配置异常: taskId=" + taskId + ", error=" + e.getMessage());
            return;
        }

        Path configFile = null;
        try {
            // 2. 写 DataX JSON 配置文件到工作目录
            String workDir = config.getWorkingDir() != null ? config.getWorkingDir() : dataxHome;
            Path workPath = Paths.get(workDir);
            String configFileName = "datax-job-" + taskId + ".json";
            configFile = workPath.resolve(configFileName);
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, config.getJobJson());
            XxlJobHelper.log("DataX 配置文件已写入: {}", configFile.toAbsolutePath());

            // 3. 执行 DataX 命令（args 是空格分隔的字符串，需拆分为独立参数）
            List<String> cmd = new ArrayList<>();
            cmd.add(config.getCommand());
            for (String arg : config.getArgs().split(" ")) {
                arg = arg.trim();
                if (!arg.isEmpty()) {
                    cmd.add(arg);
                }
            }
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(workPath.toFile());
            pb.redirectErrorStream(true);

            Process process = pb.start();
            XxlJobHelper.log("DataX 进程已启动: taskId={}", taskId);

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    XxlJobHelper.log(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                XxlJobHelper.handleSuccess("DataX 同步完成: taskId=" + taskId);
            } else {
                XxlJobHelper.handleFail("DataX 执行失败: taskId=" + taskId + ", exitCode=" + exitCode);
            }
        } catch (Exception e) {
            XxlJobHelper.handleFail("DataX 执行异常: taskId=" + taskId + ", error=" + e.getMessage());
        } finally {
            // 清理配置文件
            if (configFile != null) {
                try {
                    Files.deleteIfExists(configFile);
                } catch (Exception ignored) {
                }
            }
        }
    }
}
