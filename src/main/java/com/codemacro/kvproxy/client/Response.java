package com.codemacro.kvproxy.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 2017/5/20.
 */
public class Response {
  private final static Logger logger = LoggerFactory.getLogger(Response.class.getName());
  private final static String[] SIMPLE_MSGS = {"STORED", "EXISTS", "DELETED", "NOT_FOUND", "NOT_STORED",
    "END"};
  private final static Map<String, MemcacheStatus> STATUS_MAP = new HashMap<>();
  static {
    STATUS_MAP.put("STORED", MemcacheStatus.STORED);
    STATUS_MAP.put("EXISTS", MemcacheStatus.EXISTS);
    STATUS_MAP.put("DELETED", MemcacheStatus.DELETED);
    STATUS_MAP.put("NOT_FOUND", MemcacheStatus.NOT_FOUND);
    STATUS_MAP.put("NOT_STORED", MemcacheStatus.NOT_STORED);
  }
  private enum State { MSG, VALUE_DATA };
  private State state;
  private String msg;
  private Map<String, byte[]> values;
  private LineParser lineParser;
  private ValueParser valueParser;

  private static class LineParser {
    private StringBuilder line;

    public LineParser() {
      reset();
    }

    public void reset() {
      line = new StringBuilder();
    }

    public String get() { return line.toString(); }

    public boolean parse(final ByteBuffer buf) {
      char c0 = line.length() > 0 ? line.charAt(line.length() - 1) : 0;
      boolean got = false;
      while (!got && buf.hasRemaining()) {
        char c = (char) buf.get();
        line.append(c);
        if (c0 == '\r' && c == '\n') {
          line.delete(line.length() - 2, line.length()); // strip \r\n
          got = true;
        }
        c0 = c;
      }
      return got;
    }
  }

  private static class ValueParser {
    private int total;
    private int size;
    private byte[] data;
    private String key;

    public void reset(String key, int total) {
      this.key = key;
      data = new byte[total];
      this.total = total;
      this.size = 0;
    }

    public String getKey() { return key; }
    public byte[] getData() { return data; }

    public boolean parse(final ByteBuffer buf) {
      int toRead = total - size;
      if (toRead > 0) {
        toRead = buf.remaining() >= toRead ? toRead : buf.remaining();
        buf.get(data, size, toRead);
        size += toRead;
      }
      while (total - size > -2 && buf.hasRemaining()) {
        size += 1;
        buf.get();
      }
      return total - size == -2;
    }
  }

  public Response() {
    state = State.MSG;
    values = new HashMap<>();
    lineParser = new LineParser();
    valueParser = new ValueParser();
  }

  public MemcacheStatus getStatus() {
    MemcacheStatus s = STATUS_MAP.get(msg);
    return s == null ? MemcacheStatus.UNKNOWN : s;
  }

  public byte[] getValue(String k) { return values.get(k); }

  public boolean decode(final ByteBuffer buf) {
    if (state == State.MSG) {
      if (!lineParser.parse(buf)) return false;
      msg = lineParser.get();
      if (checkMsg()) return true;
      lineParser.reset();
      state = State.VALUE_DATA;
      startParseValue();
    }
    if (state == State.VALUE_DATA) {
      parseValues(buf);
    }
    return false;
  }

  private void startParseValue() {
    String[] secs = msg.split(" ");
    String key = secs[1];
    int bytes = Integer.parseInt(secs[3]);
    valueParser.reset(key, bytes);
  }

  private void parseValues(final ByteBuffer buf) {
    if (valueParser.parse(buf)) {
      state = State.MSG;
      lineParser.reset();
      values.put(valueParser.getKey(), valueParser.getData());
    }
  }

  private boolean checkMsg() {
    String s = msg.split(" ")[0];
    for (String m : SIMPLE_MSGS) {
      if (s.equals(m)) return true;
    }
    if (!s.equals("VALUE")) {
      logger.error("not supported msg found:{}", s);
      return true;
    }
    return false;
  }
}
