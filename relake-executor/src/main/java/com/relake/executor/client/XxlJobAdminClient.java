package com.relake.executor.client;

import com.relake.executor.dto.XxlJobLogDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * XXL-JOB Admin REST API 客户端
 * <p>
 * 通过 cookie 认证方式调用 XXL-JOB Admin 内部 API：
 * 创建任务、触发执行、查询状态/日志、停止执行。
 */
@Slf4j
@Component
public class XxlJobAdminClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String adminUrl;
    private final int jobGroupId;
    private final String username;
    private final String password;
    private volatile String cookie;

    public XxlJobAdminClient(
            @Value("${xxl.job.admin.url:http://localhost:8086/xxl-job-admin}") String adminUrl,
            @Value("${xxl.job.admin.username:admin}") String username,
            @Value("${xxl.job.admin.password:123456}") String password,
            @Value("${xxl.job.executor.group-id:1}") int jobGroupId) {
        this.adminUrl = adminUrl;
        this.jobGroupId = jobGroupId;
        this.username = username;
        this.password = password;
        login();
    }

    // ──────── 认证 ────────

    /**
     * 确保已认证，若 cookie 为空则重新登录
     */
    private synchronized void ensureAuthenticated() {
        if (cookie == null) {
            login();
        }
    }

    private void login() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("userName", username);
            params.add("password", password);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    adminUrl + "/login",
                    new HttpEntity<>(params, headers),
                    String.class);

            List<String> cookies = resp.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies != null && !cookies.isEmpty()) {
                this.cookie = cookies.get(0).split(";")[0];
                log.info("XXL-JOB Admin 登录成功, cookie={}", this.cookie);
            } else {
                log.warn("XXL-JOB Admin 登录未获取到 cookie, 响应头: {}", resp.getHeaders());
            }
        } catch (Exception e) {
            log.error("XXL-JOB Admin 登录失败: url={}, error={}", adminUrl, e.getMessage());
        }
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        if (cookie != null) {
            headers.set(HttpHeaders.COOKIE, cookie);
        }
        return headers;
    }

    // ──────── 任务管理 ────────

    /**
     * 创建 XXL-JOB 任务
     *
     * @param jobDesc          任务描述
     * @param executorHandler  Handler 名称（如 "dataxSync"）
     * @param executorParam    默认执行参数
     * @param scheduleConf     Cron 表达式（如 "0 0 2 * * ?"）
     * @return XXL-JOB 任务 ID
     */
    public int addJob(String jobDesc, String executorHandler,
                      String executorParam, String scheduleConf) {
        ensureAuthenticated();
        try {
            HttpHeaders headers = authHeaders();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("jobGroup", String.valueOf(jobGroupId));
            params.add("jobDesc", jobDesc);
            params.add("author", "ReLake");
            params.add("scheduleType", "CRON");
            params.add("scheduleConf", scheduleConf);
            params.add("misfireStrategy", "DO_NOTHING");
            params.add("executorRouteStrategy", "FIRST");
            params.add("executorHandler", executorHandler);
            params.add("executorParam", executorParam);
            params.add("executorBlockStrategy", "SERIAL_EXECUTION");
            params.add("executorTimeout", "0");
            params.add("executorFailRetryCount", "0");
            params.add("glueType", "BEAN");

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    adminUrl + "/jobinfo/add",
                    HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    new ParameterizedTypeReference<>() {});

            Map<String, Object> body = resp.getBody();
            if (body != null && "200".equals(String.valueOf(body.get("code")))) {
                // content 即 xxl_job_info.id
                Object content = body.get("content");
                // XXL-JOB 2.4.0 content 可能是数字或字符串
                int jobId = Integer.parseInt(String.valueOf(content));
                log.info("XXL-JOB 任务创建成功: handler={}, jobDesc={}, xxlJobId={}",
                        executorHandler, jobDesc, jobId);
                return jobId;
            }
            log.error("XXL-JOB 任务创建返回异常: body={}", body);
        } catch (Exception e) {
            log.error("XXL-JOB 任务创建失败: handler={}, error={}", executorHandler, e.getMessage());
            throw new RuntimeException("XXL-JOB 任务创建失败", e);
        }
        return -1;
    }

    /**
     * 根据 jobDesc 查找或创建 XXL-JOB 任务
     */
    public int findOrCreateJob(String jobDesc, String executorHandler,
                               String executorParam, String scheduleConf) {
        // 简化实现：直接创建。XXL-JOB Admin 不会对同名任务去重，
        // 但在 ReLake 侧通过 taskJobMap 保证每个 taskId 只创建一次。
        return addJob(jobDesc, executorHandler, executorParam, scheduleConf);
    }

    // ──────── 任务触发 ────────

    /**
     * 触发一次任务执行
     */
    public void triggerJob(int jobId, String executorParam) {
        ensureAuthenticated();
        try {
            HttpHeaders headers = authHeaders();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("id", String.valueOf(jobId));
            params.add("executorParam", executorParam);

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    adminUrl + "/jobinfo/trigger",
                    HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    new ParameterizedTypeReference<>() {});

            log.info("XXL-JOB 任务触发: jobId={}, resp={}", jobId, resp.getBody());
        } catch (Exception e) {
            log.error("XXL-JOB 任务触发失败: jobId={}, error={}", jobId, e.getMessage());
            throw new RuntimeException("XXL-JOB 任务触发失败", e);
        }
    }

    // ──────── 日志查询 ────────

    /**
     * 获取最近一次执行日志（含 handleCode 和日志内容）
     */
    public XxlJobLogDTO getLastLog(int jobId) {
        ensureAuthenticated();
        try {
            HttpHeaders headers = authHeaders();
            String url = adminUrl + "/joblog/pageList"
                    + "?jobGroup=" + jobGroupId
                    + "&jobId=" + jobId
                    + "&logStatus=-1"
                    + "&start=0&length=1";
            RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                    .headers(headers)
                    .build();
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(request,
                    new ParameterizedTypeReference<>() {});
            Map<String, Object> body = resp.getBody();
            if (body == null) {
                log.warn("XXL-JOB pageList 返回空 body: jobId={}", jobId);
                return null;
            }
            // pageList 返回格式: {"recordsFiltered":1, "data":[...], "recordsTotal":1}
            Object dataObj = body.get("data");
            if (!(dataObj instanceof List && !((List<?>) dataObj).isEmpty())) {
                log.warn("XXL-JOB pageList 返回空 data: jobId={}, body={}", jobId, body);
                return null;
            }
            Map<String, Object> record = (Map<String, Object>) ((List<?>) dataObj).get(0);

            XxlJobLogDTO dto = new XxlJobLogDTO();
            dto.setId(toInt(record.get("id")));
            dto.setJobId(jobId);
            dto.setTriggerCode(toInt(record.get("triggerCode")));
            dto.setTriggerMsg((String) record.get("triggerMsg"));
            dto.setHandleCode(toInt(record.get("handleCode")));
            dto.setHandleMsg((String) record.get("handleMsg"));
            dto.setExecutorAddress((String) record.get("executorAddress"));
            return dto;
        } catch (Exception e) {
            log.error("查询 XXL-JOB 日志列表失败: jobId={}, error={}", jobId, e.getMessage());
            // 可能是 cookie 过期，重置后下次重试
            resetCookie();
        }
        return null;
    }

    private synchronized void resetCookie() {
        this.cookie = null;
    }

    /**
     * 获取最近一次执行日志 ID
     */
    public Integer getLastLogId(int jobId) {
        XxlJobLogDTO log = getLastLog(jobId);
        return log != null ? log.getId() : null;
    }

    /**
     * 获取执行日志内容（用于查看完整 DataX 输出）
     */
    public String getJobLogContent(int logId) {
        ensureAuthenticated();
        try {
            HttpHeaders headers = authHeaders();
            String url = adminUrl + "/joblog/logDetailCat"
                    + "?logId=" + logId
                    + "&fromLineNum=0";
            RequestEntity<Void> request = RequestEntity.get(URI.create(url))
                    .headers(headers)
                    .build();
            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(request,
                    new ParameterizedTypeReference<>() {});
            Map<String, Object> body = resp.getBody();
            if (body == null || !"200".equals(String.valueOf(body.get("code")))) {
                return null;
            }
            Map<String, Object> content = (Map<String, Object>) body.get("content");
            if (content == null) {
                return null;
            }
            Object logContent = content.get("logContent");
            return logContent instanceof String ? (String) logContent : null;
        } catch (Exception e) {
            log.error("查询 XXL-JOB 日志详情失败: logId={}, error={}", logId, e.getMessage());
            resetCookie();
        }
        return null;
    }

    private static int toInt(Object val) {
        if (val == null) return 0;
        if (val instanceof Number) return ((Number) val).intValue();
        return Integer.parseInt(String.valueOf(val));
    }

    // ──────── 任务停止 ────────

    /**
     * 杀掉正在运行的任务执行
     */
    public void killJob(int jobId) {
        ensureAuthenticated();
        try {
            // 先查最近日志 ID
            Integer logId = getLastLogId(jobId);
            if (logId == null) {
                log.warn("XXL-JOB kill 失败: 未找到 jobId={} 的日志记录", jobId);
                return;
            }

            HttpHeaders headers = authHeaders();
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("id", String.valueOf(logId));

            ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                    adminUrl + "/joblog/logKill",
                    HttpMethod.POST,
                    new HttpEntity<>(params, headers),
                    new ParameterizedTypeReference<>() {});

            log.info("XXL-JOB 任务终止: jobId={}, logId={}, resp={}", jobId, logId, resp.getBody());
        } catch (Exception e) {
            log.error("XXL-JOB 任务终止失败: jobId={}, error={}", jobId, e.getMessage());
        }
    }
}
