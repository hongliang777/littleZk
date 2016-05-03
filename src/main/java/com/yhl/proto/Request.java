/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yhl.proto;

import java.nio.ByteBuffer;
import java.util.List;

import com.yhl.KeeperException;
import com.yhl.ServerCnxn;
import com.yhl.TxnHeader;
import com.yhl.record.Id;
import com.yhl.record.Record;
import com.yhl.util.ZooDefs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This is the structure that represents a request moving through a chain of
 * RequestProcessors. There are various pieces of information that is tacked
 * onto the request as it is processed.
 */
public class Request {
    private static final Logger LOG = LoggerFactory.getLogger(Request.class);

    public final static Request requestOfDeath = new Request(null, 0, 0, 0,
            null, null);

    /**
     * @param cnxn
     * @param sessionId
     * @param xid
     * @param type
     * @param bb
     */
    public Request(ServerCnxn cnxn, long sessionId, int xid, int type,
            ByteBuffer bb, List<Id> authInfo) {
        this.cnxn = cnxn;
        this.sessionId = sessionId;
        this.cxid = xid;
        this.type = type;
        this.request = bb;
        this.authInfo = authInfo;
    }

    public final long sessionId;

    public final int cxid;

    public final int type;

    public final ByteBuffer request;

    public final ServerCnxn cnxn;

    public TxnHeader hdr;

    public Record txn;

    public long zxid = -1;

    public final List<Id> authInfo;

    public final long createTime = System.currentTimeMillis();
    
    private Object owner;
    
    private KeeperException e;

    public Object getOwner() {
        return owner;
    }
    
    public void setOwner(Object owner) {
        this.owner = owner;
    }

    /**
     * is the packet type a valid packet in zookeeper
     * 
     * @param type
     *                the type of the packet
     * @return true if a valid packet, false if not
     */
    public static boolean isValid(int type) {
        // make sure this is always synchronized with Zoodefs!!
        switch (type) {
        case ZooDefs.OpCode.notification:
            return false;
        case ZooDefs.OpCode.create:
        case ZooDefs.OpCode.delete:
        case ZooDefs.OpCode.createSession:
        case ZooDefs.OpCode.exists:
        case ZooDefs.OpCode.getData:
        case ZooDefs.OpCode.check:
        case ZooDefs.OpCode.multi:
        case ZooDefs.OpCode.setData:
        case ZooDefs.OpCode.sync:
        case ZooDefs.OpCode.getACL:
        case ZooDefs.OpCode.setACL:
        case ZooDefs.OpCode.getChildren:
        case ZooDefs.OpCode.getChildren2:
        case ZooDefs.OpCode.ping:
        case ZooDefs.OpCode.closeSession:
        case ZooDefs.OpCode.setWatches:
            return true;
        default:
            return false;
        }
    }

    public static boolean isQuorum(int type) {
        switch (type) {
        case ZooDefs.OpCode.exists:
        case ZooDefs.OpCode.getACL:
        case ZooDefs.OpCode.getChildren:
        case ZooDefs.OpCode.getChildren2:
        case ZooDefs.OpCode.getData:
            return false;
        case ZooDefs.OpCode.error:
        case ZooDefs.OpCode.closeSession:
        case ZooDefs.OpCode.create:
        case ZooDefs.OpCode.createSession:
        case ZooDefs.OpCode.delete:
        case ZooDefs.OpCode.setACL:
        case ZooDefs.OpCode.setData:
        case ZooDefs.OpCode.check:
        case ZooDefs.OpCode.multi:
            return true;
        default:
            return false;
        }
    }
    
    static String op2String(int op) {
        switch (op) {
        case ZooDefs.OpCode.notification:
            return "notification";
        case ZooDefs.OpCode.create:
            return "create";
        case ZooDefs.OpCode.setWatches:
            return "setWatches";
        case ZooDefs.OpCode.delete:
            return "delete";
        case ZooDefs.OpCode.exists:
            return "exists";
        case ZooDefs.OpCode.getData:
            return "getData";
        case ZooDefs.OpCode.check:
            return "check";
        case ZooDefs.OpCode.multi:
            return "multi";
        case ZooDefs.OpCode.setData:
            return "setData";
        case ZooDefs.OpCode.sync:
              return "sync:";
        case ZooDefs.OpCode.getACL:
            return "getACL";
        case ZooDefs.OpCode.setACL:
            return "setACL";
        case ZooDefs.OpCode.getChildren:
            return "getChildren";
        case ZooDefs.OpCode.getChildren2:
            return "getChildren2";
        case ZooDefs.OpCode.ping:
            return "ping";
        case ZooDefs.OpCode.createSession:
            return "createSession";
        case ZooDefs.OpCode.closeSession:
            return "closeSession";
        case ZooDefs.OpCode.error:
            return "error";
        default:
            return "unknown " + op;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("sessionid:0x").append(Long.toHexString(sessionId))
            .append(" type:").append(op2String(type))
            .append(" cxid:0x").append(Long.toHexString(cxid))
            .append(" zxid:0x").append(Long.toHexString(hdr == null ?
                    -2 : hdr.getZxid()))
            .append(" txntype:").append(hdr == null ?
                    "unknown" : "" + hdr.getType());

        // best effort to print the path assoc with this request
        String path = "n/a";
        if (type != ZooDefs.OpCode.createSession
                && type != ZooDefs.OpCode.setWatches
                && type != ZooDefs.OpCode.closeSession
                && request != null
                && request.remaining() >= 4)
        {
            try {
                // make sure we don't mess with request itself
                ByteBuffer rbuf = request.asReadOnlyBuffer();
                rbuf.clear();
                int pathLen = rbuf.getInt();
                // sanity check
                if (pathLen >= 0
                        && pathLen < 4096
                        && rbuf.remaining() >= pathLen)
                {
                    byte b[] = new byte[pathLen];
                    rbuf.get(b);
                    path = new String(b);
                }
            } catch (Exception e) {
                // ignore - can't find the path, will output "n/a" instead
            }
        }
        sb.append(" reqpath:").append(path);

        return sb.toString();
    }

    public void setException(KeeperException e) {
        this.e = e;
    }
	
    public KeeperException getException() {
        return e;
    }
}
