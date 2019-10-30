package org.noear.solon.boot.nettyhttp;

import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import org.noear.solon.core.XContext;
import org.noear.solon.core.XFile;
import org.noear.solon.core.XMap;

import java.io.*;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.netty.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty.handler.codec.http.HttpHeaderNames.SET_COOKIE;


public class NettyHttpContext extends XContext {

    private final HttpRequestParser _request_parse;
    private final FullHttpRequest _request;
    private final FullHttpResponse _response;
    public NettyHttpContext(FullHttpRequest request, FullHttpResponse response) {
        _request = request;
        _response = response;
        try {
            _request_parse = new HttpRequestParser(request).parse();
        }catch (Exception ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Object request() {
        return _request;
    }

    @Override
    public String ip() {
        return header("ip");
    }

    @Override
    public String method() {
        return _request.method().name();
    }

    @Override
    public String protocol() {
        return _request.protocolVersion().toString();
    }

    private URI _uri;
    @Override
    public URI uri() {
        if(_uri == null){
            _uri = URI.create(url());
        }
        return _uri;
    }

    @Override
    public String path() {
        return uri().getPath();
    }

    @Override
    public String url() {
        return _request.uri();
    }

    @Override
    public long contentLength() {
        return _request.content().nioBufferCount();
    }

    @Override
    public String contentType() {
        return header("Content-Type");
    }

    @Override
    public String body() throws IOException {
        return _request.content().toString();
    }

    @Override
    public InputStream bodyAsStream() throws IOException {
        return new ByteArrayInputStream(_request.content().array());
    }

    @Override
    public String[] paramValues(String key) {
        List<String> tmp = _request_parse.parmMap.get(key);
        if(tmp == null){
            return null;
        }else{
            return tmp.toArray(new String[tmp.size()]);
        }
    }

    @Override
    public String param(String key) {
        return paramMap().get(key);
    }

    @Override
    public String param(String key, String def) {
        String tmp = paramMap().get(key);
        if(tmp == null) {
            return def;
        }else{
            return tmp;
        }
    }

    private XMap _paramMap;
    @Override
    public XMap paramMap() {
        if(_paramMap == null){
            _paramMap = new XMap();

            _request_parse.parmMap.forEach((k,l)->{
                if(l.size() > 0){
                    _paramMap.put(k,l.get(0));
                }
            });
        }

        return _paramMap;
    }

    @Override
    public void paramSet(String key, String val) {
        paramMap().put(key,val);
    }

    @Override
    public List<XFile> files(String key) throws Exception {
        return _request_parse.fileMap.get(key);
    }

    @Override
    public String cookie(String key) {
        return cookieMap().get(key);
    }

    @Override
    public String cookie(String key, String def) {
        String temp = cookieMap().get(key);
        if(temp == null){
            return  def;
        }else{
            return temp;
        }
    }

    private XMap _cookieMap;
    @Override
    public XMap cookieMap() {
        if(_cookieMap == null){
            _cookieMap = new XMap();

            String _cookieMapStr = _request.headers().get(COOKIE);
            if (_cookieMapStr != null) {
                Set<Cookie> tmp = ServerCookieDecoder.LAX.decode(_cookieMapStr);

                for(Cookie c1 :tmp){
                    _cookieMap.put(c1.name(),c1.value());
                }
            }
        }

        return _cookieMap;
    }

    @Override
    public String header(String key) {
        return _request.headers().get(key);
    }

    @Override
    public String header(String key, String def) {
        String tmp = _request.headers().get(key);
        if(tmp == null){
            return def;
        }else{
            return  tmp;
        }
    }

    private XMap _headerMap;
    @Override
    public XMap headerMap() {
        if(_headerMap == null) {
            _headerMap = new XMap();
            HttpHeaders headers = _request.headers();

            for(Map.Entry<String, String> kv : headers){
                _headerMap.put(kv.getKey(), kv.getValue());
            }
        }

        return _headerMap;
    }

    @Override
    public Object response() {
        return _response;
    }

    @Override
    public void charset(String charset) {
        _charset = charset;
    }
    private String _charset = "UTF-8";

    @Override
    public void contentType(String contentType) {
        if (_charset != null) {
            if (contentType.indexOf(";") < 0) {
                _response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType + ";charset=" + _charset);
                return;
            }

        }

        _response.headers().set(HttpHeaderNames.CONTENT_TYPE, contentType);
    }

    @Override
    public OutputStream outputStream() throws IOException{
        return _outputStream;
    }

    @Override
    public void output(String str) {
        try {
            OutputStream out = _outputStream;

            out.write(str.getBytes(_charset));
            out.flush();
        }catch (Throwable ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void output(InputStream stream) {
        try {
            OutputStream out = _outputStream;

            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = stream.read(buff, 0, 100)) > 0) {
                out.write(buff, 0, rc);
            }

            out.flush();
        }catch (Throwable ex){
            throw new RuntimeException(ex);
        }
    }

    protected ByteArrayOutputStream _outputStream = new ByteArrayOutputStream();

    @Override
    public void headerSet(String key, String val) {
        _response.headers().set(key,val);
    }

    @Override
    public void cookieSet(String key, String val, String domain, String path, int maxAge) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append("=").append(val).append(";");

        if (path != null) {
            sb.append("path=").append(path).append(";");
        }

        sb.append("max-age=").append(maxAge).append(";");

        if (domain != null) {
            sb.append("domain=").append(domain.toLowerCase()).append(";");
        }

        _response.headers().add(SET_COOKIE, sb.toString());
    }

    @Override
    public void cookieRemove(String key) {
        StringBuilder sb = new StringBuilder();
        sb.append(key).append("=;");
        sb.append("max-age=0;");

        _response.headers().add(SET_COOKIE, sb.toString());
    }

    @Override
    public void redirect(String url) {
        redirect(url,302);
    }

    @Override
    public void redirect(String url, int code) {
        try {
            headerSet("Location", url);
            status(code);
        }catch (Throwable ex){
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int status() {
        return _status;
    }

    private int _status = 200;
    @Override
    public void status(int status) {
        _status = status;
        _response.setStatus(HttpResponseStatus.valueOf(status));
    }

    @Override
    protected void commit() throws IOException{
        _response.content().writeBytes(_outputStream.toByteArray());
    }
}