package com.codemacro.kvproxy.memcache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2017/4/10.
 */
public class ProtocolParser {
  private static final Logger logger = LoggerFactory.getLogger(ProtocolParser.class.getName());
  private static final int STATE_COMMAND = 0;
  private static final int STATE_DATA = 1;
  private static final int STATE_DONE = 2;
  private static final String[] STORE_CMDS = {"set", "add", "replace", "prepend", "append", "cas"};
  private static final String[] OTHER_CMDS = {"stats", "version", "quit"};
  private static final class Buffer {
    public ByteBuffer buf;
    public int pos;
    public Buffer(ByteBuffer buf, int pos) {
      this.buf = buf;
      this.pos = pos;
    }
  }
  private List<Buffer> buffers = new ArrayList<Buffer>();
  private StringBuilder cmdline;
  private int state;
  private String command;
  private List<String> keys = new ArrayList<String>();
  private int flag;
  private int time;
  private int bytes;
  private int size;
  private long value;
  private boolean noreply = false;

  public static class ParseException extends RuntimeException {
    public ParseException(String msg) { super(msg); }
  }

  ProtocolParser() {
    reset();
  }

  public void reset() {
    state = STATE_COMMAND;
    buffers.clear();
    cmdline = new StringBuilder();
    keys.clear();
    flag = 0;
    bytes = size = 0;
    time = 0;
    noreply = false;
    value = 0;
  }

  public boolean consume(final ByteBuffer buf) throws ParseException {
    if (state == STATE_COMMAND) {
      char c0 = cmdline.length() > 0 ? cmdline.charAt(cmdline.length() - 1) : 0;
      boolean got_cmd = false;
      while (!got_cmd && buf.hasRemaining()) {
        char c = (char) buf.get();
        cmdline.append(c);
        if (c0 == '\r' && c == '\n') {
          cmdline.delete(cmdline.length() - 2, cmdline.length()); // strip \r\n
          got_cmd = true;
        }
        c0 = c;
      }
      if (!got_cmd) return false;
      if (!parseCmdLine()) {
        state = STATE_DATA;
        expect(bytes > 0, "bytes > 0");
        size = 0;
      } else {
        state = STATE_DONE;
      }
    }
    if (state == STATE_DATA) {
      buffers.add(new Buffer(buf.duplicate(), buf.position()));
      int require = bytes + 2 - size;
      if (require > buf.remaining()) {
        size += buf.remaining();
        buf.position(buf.position() + buf.remaining());
      } else {
        size = bytes;
        state = STATE_DONE;
        buf.position(buf.position() + require);
      }
    }
    return state == STATE_DONE;
  }

  public String getCommand() {
    return command;
  }

  public int getBytes() {
    return bytes;
  }

  public int getFlag() {
    return flag;
  }

  public int getTime() {
    return time;
  }

  public long getValue() {
    return value;
  }

  public boolean isStoreCmd() {
    for (String c : STORE_CMDS) {
      if (c.equals(command)) return true;
    }
    return false;
  }

  public boolean isRetrieveCmd() {
    return command.equals("get") || command.equals("gets");
  }

  public boolean isIncDecCmd() {
    return command.equals("incr") || command.equals("decr");
  }

  public boolean isOtherCmd() {
    for (String c : OTHER_CMDS) {
      if (c.equals(command)) return true;
    }
    return false;
  }

  public List<String> getKeys() {
    return keys;
  }

  public String getKey() {
    return keys.get(0);
  }

  public boolean isNoreply() {
    return noreply;
  }

  public byte[] cloneData() throws ParseException {
    byte[] data = new byte[bytes];
    int left = bytes;
    for (int i = 0; i < buffers.size(); ++i) {
      final Buffer buf0 = buffers.get(i);
      final ByteBuffer buf = buf0.buf;
      buf.position(buf0.pos);
      if (left > 0) {
        int toRead = buf.remaining() < left ? buf.remaining() : left;
        buf.get(data, bytes - left, toRead);
        left -= toRead;
      }
      if (left <= 0) { // to skip \r\n
        if (buf.remaining() > 0 && left == 0) {
          expect(buf.get() == '\r', "end \\r"); left --;
        }
        if (buf.remaining() > 0 && left == -1) {
          expect(buf.get() == '\n', "end \\n"); left --;
        }
      }
    }
    return data;
  }

  private void expect(boolean exp, String msg) throws ParseException {
    if (!exp) throw new ParseException(msg);
  }

  private boolean parseCmdLine() throws ParseException {
    String line = cmdline.toString();
    if (line.isEmpty()) throw new ParseException("empty command line");
    String[] parts = line.split(" ");
    command = parts[0];
    if (isStoreCmd()) {
      expect(parts.length == 5 || parts.length == 6, "valid storage command");
      keys.add(parts[1]);
      flag = Integer.parseInt(parts[2]);
      time = Integer.parseInt(parts[3]);
      bytes = Integer.parseInt(parts[4]);
      if (parts.length == 6) {
        expect(parts[5].equals("noreply"), "noreply flag");
        noreply = true;
      }
      return false;
    } else if (isRetrieveCmd()) {
      expect(parts.length >= 2, "valid retrieve command");
      for (int i = 1; i < parts.length; ++i) {
        keys.add(parts[i]);
      }
      return true;
    } else if (command.equals("delete")) {
      expect(parts.length >= 2, "valid delete command");
      keys.add(parts[1]);
      time = parts.length >= 3 ? Integer.parseInt(parts[2]) : 0;
      if (parts.length == 4) {
        expect(parts[3].equals("noreply"), "noreply flag");
        noreply = true;
      }
      return true;
    } else if (isIncDecCmd()) {
      expect(parts.length >= 3, "valid inc/dec command");
      keys.add(parts[1]);
      value = Long.parseLong(parts[2]);
      if (parts.length == 4) {
        expect(parts[3].equals("noreply"), "noreply flag");
        noreply = true;
      }
      return true;
    } else if (command.equals("flush_all")) {
      if (parts.length == 3) {
        expect(parts[2].equals("noreply"), "noreply flag");
        noreply = true;
      }
      return true;
    } else if (isOtherCmd()) {
      return true;
    } else {
      throw new ParseException("unknown command:" + command);
    }
  }

}

