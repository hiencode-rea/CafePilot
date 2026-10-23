import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpPrincipal;
import com.sun.net.httpserver.HttpContext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Dependency-light contract tests for API behavior that does not require SQL Server. */
public final class ApiContractTest {
    private static int passed;

    public static void main(String[] args) throws Exception {
        queryWithoutQueryStringReturnsNull();
        queryParameterIsRead();
        queryParameterIsUrlDecodedAsUtf8();
        missingQueryParameterReturnsNull();
        firstDuplicateQueryParameterWins();
        loginRejectsGet();
        ordersRejectGet();
        attendanceRejectsGet();
        kitchenQueueRejectsPostOnCollectionRoute();

        System.out.println("Passed " + passed + " API contract tests.");
    }

    private static void queryWithoutQueryStringReturnsNull() {
        equal(null, HttpUtil.layQueryParam(exchange("GET", "/api/ban"), "trangthai"),
                "query absent");
    }

    private static void queryParameterIsRead() {
        equal("Trong", HttpUtil.layQueryParam(exchange("GET", "/api/ban?trangthai=Trong"), "trangthai"),
                "query value");
    }

    private static void queryParameterIsUrlDecodedAsUtf8() {
        equal("Bàn trống", HttpUtil.layQueryParam(
                exchange("GET", "/api/ban?trangthai=B%C3%A0n+tr%E1%BB%91ng"), "trangthai"),
                "UTF-8 URL decoding");
    }

    private static void missingQueryParameterReturnsNull() {
        equal(null, HttpUtil.layQueryParam(exchange("GET", "/api/ban?khac=1"), "trangthai"),
                "query key absent");
    }

    private static void firstDuplicateQueryParameterWins() {
        equal("Trong", HttpUtil.layQueryParam(
                exchange("GET", "/api/ban?trangthai=Trong&trangthai=Ban"), "trangthai"),
                "duplicate query key behavior");
    }

    private static void loginRejectsGet() throws Exception {
        assertMethodRejected(new DangNhapHandler(), "GET", "/api/dangnhap", 405, "Chỉ hỗ trợ POST");
    }

    private static void ordersRejectGet() throws Exception {
        assertMethodRejected(new DonHangHandler(), "GET", "/api/donhang", 405, "Chi ho tro POST");
    }

    private static void attendanceRejectsGet() throws Exception {
        assertMethodRejected(new ChamCongHandler(), "GET", "/api/chamcong/vao", 405, "Chỉ hỗ trợ POST");
    }

    private static void kitchenQueueRejectsPostOnCollectionRoute() throws Exception {
        TestExchange exchange = exchange("POST", "/api/xulymon");
        new XuLyMonHandler().handle(exchange);
        assertResponse(exchange, 404, "Đường dẫn không hợp lệ");
    }

    private static void assertMethodRejected(com.sun.net.httpserver.HttpHandler handler,
                                             String method, String path, int expectedStatus,
                                             String expectedMessage) throws Exception {
        TestExchange exchange = exchange(method, path);
        handler.handle(exchange);
        assertResponse(exchange, expectedStatus, expectedMessage);
    }

    private static void assertResponse(TestExchange exchange, int expectedStatus, String expectedMessage) {
        equal(expectedStatus, exchange.responseCode, "HTTP response status");
        String contentType = exchange.responseHeaders.getFirst("Content-Type");
        if (contentType == null || !contentType.startsWith("application/json")) {
            throw new AssertionError("Expected JSON Content-Type but got: " + contentType);
        }
        String body = exchange.responseBody.toString(StandardCharsets.UTF_8);
        if (!body.contains(expectedMessage)) {
            throw new AssertionError("Expected response to contain '" + expectedMessage + "' but got: " + body);
        }
        passed++;
        System.out.println("PASS " + expectedStatus + " " + expectedMessage);
    }

    private static void equal(Object expected, Object actual, String description) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(description + ": expected <" + expected + "> but got <" + actual + ">");
        }
        passed++;
        System.out.println("PASS " + description);
    }

    private static TestExchange exchange(String method, String pathAndQuery) {
        return new TestExchange(method, URI.create(pathAndQuery));
    }

    private static final class TestExchange extends HttpExchange {
        private final Headers requestHeaders = new Headers();
        private final Headers responseHeaders = new Headers();
        private final InputStream requestBody = new ByteArrayInputStream(new byte[0]);
        private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
        private final Map<String, Object> attributes = new HashMap<>();
        private final String method;
        private final URI uri;
        private int responseCode = -1;

        private TestExchange(String method, URI uri) {
            this.method = method;
            this.uri = uri;
        }

        @Override public Headers getRequestHeaders() { return requestHeaders; }
        @Override public Headers getResponseHeaders() { return responseHeaders; }
        @Override public URI getRequestURI() { return uri; }
        @Override public String getRequestMethod() { return method; }
        @Override public HttpContext getHttpContext() { return null; }
        @Override public void close() { }
        @Override public InputStream getRequestBody() { return requestBody; }
        @Override public OutputStream getResponseBody() { return responseBody; }
        @Override public void sendResponseHeaders(int code, long length) { responseCode = code; }
        @Override public InetSocketAddress getRemoteAddress() { return new InetSocketAddress("127.0.0.1", 12345); }
        @Override public int getResponseCode() { return responseCode; }
        @Override public InetSocketAddress getLocalAddress() { return new InetSocketAddress("127.0.0.1", 8080); }
        @Override public String getProtocol() { return "HTTP/1.1"; }
        @Override public Object getAttribute(String name) { return attributes.get(name); }
        @Override public void setAttribute(String name, Object value) { attributes.put(name, value); }
        @Override public void setStreams(InputStream input, OutputStream output) { }
        @Override public HttpPrincipal getPrincipal() { return null; }
    }
}