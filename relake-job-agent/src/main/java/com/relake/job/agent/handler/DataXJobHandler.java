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
                XxlJobHelper.log("DataX 执行成功: taskId={}, exitCode={}", taskId, exitCode);

                // 4. MinIO 目标：将 staging 文件上传至对象存储
                String storageType = config.getTargetStorageType();
                XxlJobHelper.log("目标存储类型: targetStorageType={}, stagingPath={}",
                        storageType, config.getStagingPath());
                boolean isMinio = "MINIO".equalsIgnoreCase(storageType)
                        || "S3".equalsIgnoreCase(storageType);
                boolean uploaded = false;
                if (isMinio) {
                    uploaded = uploadToMinio(config, taskId);
                }

                // 5. MinIO staging 上传成功才清理临时文件，失败保留待排查
                if (isMinio && config.getStagingPath() != null) {
                    if (uploaded) {
                        cleanStaging(config.getStagingPath());
                    } else {
                        XxlJobHelper.log("MinIO 上传失败，staging 文件保留在: {}", config.getStagingPath());
                    }
                }

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

    private void cleanStaging(String stagingPath) {
        try {
            Path stagingDir = Paths.get(stagingPath);
            if (Files.exists(stagingDir)) {
                Files.walk(stagingDir)
                        .sorted(java.util.Comparator.reverseOrder())
                        .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
                XxlJobHelper.log("staging 目录已清理: {}", stagingPath);
            }
        } catch (Exception e) {
            XxlJobHelper.log("清理 staging 失败: {} ({})", stagingPath, e.getMessage());
        }
    }

    /**
     * 通过 MinIO Client (mc) 将 staging 目录上传到 MinIO bucket
     *
     * @return true=上传成功, false=失败
     */
    private boolean uploadToMinio(DataXConfigDTO config, Long taskId) {
        // 检查 mc 是否已安装
        try {
            Process versionCheck = new ProcessBuilder("mc", "--version").start();
            if (versionCheck.waitFor() != 0) {
                XxlJobHelper.log("MinIO 上传跳过: mc 未安装（容器内无 mc 二进制，请检查 Dockerfile 构建日志）");
                return false;
            }
        } catch (Exception e) {
            XxlJobHelper.log("MinIO 上传跳过: mc 不可用 ({})", e.getMessage());
            return false;
        }

        try {
            String endpoint = config.getMinioEndpoint();
            String accessKey = config.getMinioAccessKey();
            String secretKey = config.getMinioSecretKey();
            String bucket = config.getMinioBucket();
            String stagingPath = config.getStagingPath();

            if (endpoint == null || bucket == null || stagingPath == null) {
                XxlJobHelper.log("MinIO 上传跳过: 配置不完整, endpoint={}, bucket={}, stagingPath={}",
                        endpoint, bucket, stagingPath);
                return false;
            }

            // 检查 staging 目录是否存在且有文件
            Path stagingDir = Paths.get(stagingPath);
            if (!Files.exists(stagingDir) || !Files.isDirectory(stagingDir)) {
                XxlJobHelper.log("MinIO 上传跳过: staging 目录不存在={}", stagingPath);
                return false;
            }
            long fileCount = 0;
            try (var s = Files.list(stagingDir)) { fileCount = s.count(); }
            if (fileCount == 0) {
                XxlJobHelper.log("MinIO 上传跳过: staging 目录为空={}", stagingPath);
                return false;
            }
            XxlJobHelper.log("staging 目录就绪: path={}, files={}", stagingPath, fileCount);

            // 1. 配置 mc alias（捕获 stderr 以便排查）
            XxlJobHelper.log("mc alias set: endpoint={}, accessKey={}, secretKey=***", endpoint, accessKey);
            ProcessBuilder aliasPb = new ProcessBuilder(
                    "mc", "alias", "set", "minio-target", endpoint, accessKey, secretKey);
            aliasPb.redirectErrorStream(true);
            Process aliasProcess = aliasPb.start();
            String aliasOutput = new String(aliasProcess.getInputStream().readAllBytes()).trim();
            int aliasExit = aliasProcess.waitFor();
            if (aliasExit != 0) {
                XxlJobHelper.log("mc alias set 失败: exitCode={}, output={}", aliasExit, aliasOutput);
                return false;
            }
            XxlJobHelper.log("mc alias 配置成功: endpoint={}, bucket={}", endpoint, bucket);

            // 2. 上传 staging 目录到 MinIO bucket
            String targetPath = "minio-target/" + bucket + "/" + taskId + "/";
            ProcessBuilder cpPb = new ProcessBuilder(
                    "mc", "cp", "-r", stagingPath + "/", targetPath);
            cpPb.redirectErrorStream(true);
            Process cpProcess = cpPb.start();
            String cpOutput;
            try (var in = cpProcess.getInputStream()) {
                cpOutput = new String(in.readAllBytes()).trim();
            }
            int cpExit = cpProcess.waitFor();
            if (!cpOutput.isBlank()) {
                XxlJobHelper.log("[mc upload] {}", cpOutput);
            }
            if (cpExit == 0) {
                XxlJobHelper.log("MinIO 上传成功: stagingPath={} -> {}/{}",
                        stagingPath, bucket, taskId);
                return true;
            } else {
                XxlJobHelper.log("MinIO 上传失败: exitCode={}, output={}", cpExit, cpOutput);
                return false;
            }
        } catch (Exception e) {
            XxlJobHelper.log("MinIO 上传异常: {}", e.getMessage());
            return false;
        }
    }
}
