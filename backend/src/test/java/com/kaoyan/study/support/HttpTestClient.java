package com.kaoyan.study.support;

import com.jayway.jsonpath.JsonPath;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * 集成测试用的极简 HTTP 客户端。
 *
 * <p>带 Cookie 管理，因此可以真实验证登录后的会话 Cookie 与 CSRF Cookie 行为——
 * 这两者在 MockMvc 中会被 Spring Session 与容器会话的差异掩盖。
 */
public final class HttpTestClient {

    private final HttpClient client;
    private final String baseUrl;

    /** 响应快照。 */
    public record Response(int status, String body) {

        /** 读取字符串字段。 */
        public String string(String path) {
            return JsonPath.read(body, path);
        }

        /** 读取布尔字段。 */
        public boolean bool(String path) {
            return JsonPath.read(body, path);
        }

        /** 读取数字字段（JSON 数字会解析成 Integer/Double 等，统一按 Number 取）。 */
        public Number number(String path) {
            return JsonPath.read(body, path);
        }

        /** 读取任意字段，字段缺失或为 null 时返回 null。 */
        public Object value(String path) {
            try {
                return JsonPath.read(body, path);
            } catch (com.jayway.jsonpath.PathNotFoundException ex) {
                return null;
            }
        }
    }

    public HttpTestClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    public Response get(String path) {
        return send(builder(path).GET().build());
    }

    public Response post(String path, String jsonBody, String csrfToken) {
        return sendWithBody("POST", path, jsonBody, csrfToken);
    }

    public Response put(String path, String jsonBody, String csrfToken) {
        return sendWithBody("PUT", path, jsonBody, csrfToken);
    }

    private Response sendWithBody(String method, String path, String jsonBody, String csrfToken) {
        HttpRequest.Builder builder = builder(path)
                .header("Content-Type", "application/json");
        if (csrfToken != null) {
            builder.header("X-XSRF-TOKEN", csrfToken);
        }
        HttpRequest.BodyPublisher publisher = HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8);
        return send(builder.method(method, publisher).build());
    }

    /** 取一次 CSRF 令牌（同时把 Cookie 存入 Cookie 管理器）。 */
    public String fetchCsrfToken() {
        Response response = get("/api/auth/csrf");
        if (response.status() != 200) {
            throw new IllegalStateException("获取 CSRF 令牌失败：" + response.status());
        }
        return response.string("$.token");
    }

    private HttpRequest.Builder builder(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path));
    }

    private Response send(HttpRequest request) {
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Response(response.statusCode(), response.body());
        } catch (Exception e) {
            throw new IllegalStateException("请求失败：" + request.uri(), e);
        }
    }
}
