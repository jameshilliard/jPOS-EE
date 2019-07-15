/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2012 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.space;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.BinaryJedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;

public class JedisSpace<K,V> implements Space<K,V> {
    protected String redisHost;
    protected int redisPort;
    JedisPool pool;
    byte[] lpushSha;
    byte[] lpushtSha;
    byte[] rpushSha;
    byte[] rpushtSha;
    byte[] lremSha;
    byte[] lpopSha;
    byte[] blpopSha;
    byte[] lgetSha;
    byte[] lgetallSha;
    byte[] existSha;

    static final Map<String,Space> spaceRegistrar =
            new HashMap<String,Space>();

    public JedisSpace(String spaceName) {
        redisHost = "localhost";
        redisPort = 6379;
        initPool();
    }

    public JedisSpace(String spaceName, String host) {
        redisHost = host;
        redisPort = 6379;
        initPool();
    }

    public JedisSpace(String spaceName, int port) {
        redisHost = "localhost";
        redisPort = port;
        initPool();
    }

    private void initPool() {
        pool = new JedisPool(new JedisPoolConfig(), redisHost, redisPort);
        try (Jedis jedis = pool.getResource()) {
            jedis.configSet("notify-keyspace-events", "Kl");
        }
    }

    public synchronized static JedisSpace getSpace (String name, String path)
    {
        JedisSpace sp = (JedisSpace) spaceRegistrar.get(name);
        if (sp == null) {
            sp = new JedisSpace(name, path);
            spaceRegistrar.put (name, sp);
        }
        return sp;
    }

    public static JedisSpace getSpace (String name) {
        return getSpace (name, name);
    }

    private byte[] serialize(Object obj) {
        if (obj == null) {
            throw new NullPointerException();
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream os = new ObjectOutputStream(baos);
            os.writeObject(obj);
            obj = baos.toByteArray();
        } catch (IOException e) {
            throw new SpaceError (e);
        }
        return (byte[]) obj;
    }

    private Object deserialize(Object obj) {
        ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) obj);
        try {
            ObjectInputStream is = new ObjectInputStream (bais);
            return is.readObject();
        } catch (Exception e) {
            throw new SpaceError (e);
        }
    }

    private void lpush(K k, V v) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = ARGV[1]\n" +
            "local seq = redis.call(\"incr\", \"s:\" .. key)\n" +
            "redis.call(\"set\", \"v:\" .. key .. \":\" .. seq, obj)\n" +
            "redis.call(\"lpush\", \"k:\" .. key, seq)\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (lpushSha == null) {
                lpushSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            jedis.evalsha(lpushSha, 1, key.getBytes(), serialize(v));
        }
    }

    private void lpush(K k, V v, long t) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = ARGV[1]\n" +
            "local timeout = ARGV[2]\n" +
            "local seq = redis.call(\"incr\", \"s:\" .. key)\n" +
            "redis.call(\"psetex\", \"v:\" .. key .. \":\" .. seq, timeout, obj)\n" +
            "redis.call(\"lpush\", \"k:\" .. key, seq)\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (lpushtSha == null) {
                lpushtSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            byte[] timeout = Long.toString(t).getBytes();
            jedis.evalsha(lpushtSha, 1, key.getBytes(), serialize(v), timeout);
        }
    }

    private void rpush(K k, V v) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = ARGV[1]\n" +
            "local seq = redis.call(\"incr\", \"s:\" .. key)\n" +
            "redis.call(\"set\", \"v:\" .. key .. \":\" .. seq, obj)\n" +
            "redis.call(\"rpush\", \"k:\" .. key, seq)\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (rpushSha == null) {
                rpushSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            jedis.evalsha(rpushSha, 1, key.getBytes(), serialize(v));
        }
    }

    private void rpush(K k, V v, long t) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = ARGV[1]\n" +
            "local timeout = ARGV[2]\n" +
            "local seq = redis.call(\"incr\", \"s:\" .. key)\n" +
            "redis.call(\"psetex\", \"v:\" .. key .. \":\" .. seq, timeout, obj)\n" +
            "redis.call(\"rpush\", \"k:\" .. key, seq)\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (rpushtSha == null) {
                rpushtSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            byte[] timeout = Long.toString(t).getBytes();
            jedis.evalsha(rpushtSha, 1, key.getBytes(), serialize(v), timeout);
        }
    }

    private boolean lrem(K k, V v) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = ARGV[1]\n" +
            "local index = 0\n" +
            "while true do\n" +
            "   local seq = redis.call(\"lindex\", \"k:\" .. key, index)\n" +
            "   if not seq then\n" +
            "       return false\n" +
            "   end\n" +
            "   local testobj = redis.call(\"get\", \"v:\" .. key .. \":\" .. seq)\n" +
            "   if obj == testobj then\n" +
            "       redis.call(\"lrem\", \"k:\" .. key, 1, seq)\n" +
            "       redis.call(\"del\", \"v:\" .. key .. \":\" .. seq)\n" +
            "       return true\n" +
            "   end\n" +
            "   index = index + 1\n" +
            "end\n" +
            "return false\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (lremSha == null) {
                lremSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            Object obj = jedis.evalsha(lremSha, 1, key.getBytes(), serialize(v));
            if (obj != null) {
                return true;
            } else {
                return false;
            }
        }
    }

    private V lpop(K k) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = nil\n" +
            "while not obj do\n" +
            "   local seq = redis.call(\"lpop\", \"k:\" .. key)\n" +
            "   if not seq then\n" +
            "       return false\n" +
            "   end\n" +
            "   obj = redis.call(\"get\", \"v:\" .. key .. \":\" .. seq)\n" +
            "   redis.call(\"del\", \"v:\" .. key .. \":\" .. seq)\n" +
            "end\n" +
            "return obj\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (lpopSha == null) {
                lpopSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            Object obj = jedis.evalsha(lpopSha, 1, key.getBytes());
            if (obj != null) {
                return (V) deserialize(obj);
            } else {
                return null;
            }
        }
    }

    private V blpop(K k) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = nil\n" +
            "while not obj do\n" +
            "   local seq = redis.call(\"lpop\", \"k:\" .. key)\n" +
            "   if not seq then\n" +
            "       return false\n" +
            "   end\n" +
            "   obj = redis.call(\"get\", \"v:\" .. key .. \":\" .. seq)\n" +
            "   redis.call(\"del\", \"v:\" .. key .. \":\" .. seq)\n" +
            "end\n" +
            "return obj\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (blpopSha == null) {
                blpopSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            final Object[] obj = new Object[1];
            obj[0] = jedis.evalsha(blpopSha, 1, key.getBytes());
            if (obj[0] == null) {
                try (Jedis jedissub = pool.getResource()) {
                    jedissub.psubscribe(new JedisPubSub() {
                        public void onPMessage(String pattern, String channel, String message) {
                            if (message.equals("rpush") || message.equals("lpush")) {
                                obj[0] = jedis.evalsha(blpopSha, 1, key.getBytes());
                                if (obj[0] != null) {
                                    punsubscribe();
                                }
                            }
                        }

                        public void onPSubscribe(String channel, int subscribedChannels) {
                            obj[0] = jedis.evalsha(blpopSha, 1, key.getBytes());
                            if (obj[0] != null) {
                                punsubscribe();
                            }
                        }
                    }, "__keyspace@0__:k:" + key);
                }
            }
            return (V) deserialize(obj[0]);
        }
    }

    private V lget(K k) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = nil\n" +
            "while not obj do\n" +
            "   local seq = redis.call(\"lindex\", \"k:\" .. key, 0)\n" +
            "   if not seq then\n" +
            "       return false\n" +
            "   end\n" +
            "   obj = redis.call(\"get\", \"v:\" .. key .. \":\" .. seq)\n" +
            "   if not obj then\n" +
            "       redis.call(\"lpop\", \"k:\" .. key)\n" +
            "   end\n" +
            "end\n" +
            "return obj\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (lgetSha == null) {
                lgetSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            Object obj = jedis.evalsha(lgetSha, 1, key.getBytes());
            if (obj != null) {
                return (V) deserialize(obj);
            } else {
                return null;
            }
        }
    }

    private List<Object> lgetall(K k) {
        String script =
            "local key = KEYS[1]\n" +
            "local objlist = {}\n" +
            "local seqlist = redis.call(\"lrange\", \"k:\" .. key, 0, -1)\n" +
            "for _, seq in ipairs(seqlist) do\n" +
            "   local obj = redis.call(\"get\", \"v:\" .. key .. \":\" .. seq)\n" +
            "   if obj then\n" +
            "       table.insert(objlist, obj)\n" +
            "   end\n" +
            "end\n" +
            "return objlist\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (lgetallSha == null) {
                lgetallSha = jedis.scriptLoad(script.getBytes());
            }
            String key = "{" + k.toString() + "}";
            List<byte[]> responses = (List<byte[]>) jedis.evalsha(lgetallSha, 1, key.getBytes());
            List<Object> objlist = new ArrayList<Object>();
            for (byte[] response : responses) {
                objlist.add(deserialize(response));
            }
            return objlist;
        }
    }

    private boolean exists(K[] keylist) {
        String script =
            "local key = KEYS[1]\n" +
            "local obj = nil\n" +
            "while not obj do\n" +
            "   local seq = redis.call(\"lindex\", \"k:\" .. key, 0)\n" +
            "   if not seq then\n" +
            "       return false\n" +
            "   end\n" +
            "   obj = redis.call(\"get\", \"v:\" .. key .. \":\" .. seq)\n" +
            "   if not obj then\n" +
            "       redis.call(\"lpop\", \"k:\" .. key)\n" +
            "   end\n" +
            "end\n" +
            "return true\n";
        try (BinaryJedis jedis = pool.getResource()) {
            if (existSha == null) {
                existSha = jedis.scriptLoad(script.getBytes());
            }
            for (K k : keylist) {
                String key = "{" + k.toString() + "}";
                Object exists = jedis.evalsha(existSha, 1, key.getBytes());
                if (exists != null) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean bexists(K[] keys) {
        final Object[] obj = new Object[1];
        obj[0] = exists(keys);
        if ((Boolean)obj[0]) {
            return true;
        }
        List<String> subkeys = new ArrayList<String>();
        for (K key : keys) {
            subkeys.add("__keyspace@0__:k:{" + key + "}");
        }
        try (Jedis jedissub = pool.getResource()) {
            jedissub.psubscribe(new JedisPubSub() {
                public void onPMessage(String pattern, String channel, String message) {
                    if (message.equals("rpush") || message.equals("lpush")) {
                        obj[0] = exists(keys);
                        if ((Boolean)obj[0]) {
                            punsubscribe();
                        }
                    }
                }

                public void onPSubscribe(String channel, int subscribedChannels) {
                    obj[0] = exists(keys);
                    if ((Boolean)obj[0]) {
                        punsubscribe();
                    }
                }
            }, subkeys.toArray(new String[0]));
        }
        return (Boolean) obj[0];
    }

    public void out(K k, V v) {
        rpush(k, v);
    }
    public void out(K k, V v, long l) {
        rpush(k, v, l);
    }
    public V in(K key) {
        return blpop(key);
    }
    public V in(K key, long timeout) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<String> future = executor.submit(new Callable() {
            public V call() throws Exception {
                return blpop(key);
            }
        });
        Object obj;
        try {
            obj = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            obj = null;
        } catch (InterruptedException e) {
            obj = null;
        } catch (ExecutionException e) {
            obj = null;
        }
        executor.shutdownNow();
        return (V) obj;
    }
    public V inp(Object key) {
        Template tmpl;
        if (key instanceof Template) {
            tmpl = (Template) key;
            key  = tmpl.getKey();
            List<Object> objlist = lgetall((K)key);
            for (Object obj : objlist) {
                if (tmpl.equals(obj)) {
                    if (lrem((K)key, (V)obj)) {
                        return (V) obj;
                    }
                }
            }
            return null;
        }
        return lpop((K)key);
    }

    public V rdp(Object key) {
        Template tmpl;
        if (key instanceof Template) {
            tmpl = (Template) key;
            key  = tmpl.getKey();
            List<Object> objlist = lgetall((K)key);
            for (Object obj : objlist) {
                if (tmpl.equals(obj)) {
                    return (V) obj;
                }
            }
            return null;
        }
        return lget((K)key);
    }

    public void nrd(K key) {

    }

    public V nrd(K key, long timeout) {
        return null;
    }

    public V rd(K key) {
        Object obj;
        while ((obj = rdp (key)) == null) {
            try {
                synchronized(this) {
                    this.wait();
                }
            } catch (InterruptedException ignored) { }
        }
        return (V) obj;
    }

    public V rd(K key, long timeout) {
        Object obj;
        long now = System.currentTimeMillis();
        long end = now + timeout;
        while ((obj = rdp (key)) == null &&
                (now = System.currentTimeMillis()) < end)
        {
            try {
                synchronized(this) {
                    this.wait(end - now);
                }
            } catch (InterruptedException ignored) { }
        }
        return (V) obj;
    }

    public void push(K k, V v) {
        lpush(k, v);
    }

    public void push(K k, V v, long l) {
        lpush(k, v, l);
    }

    public boolean existAny(K[] keys) {
        return exists(keys);
    }

    public boolean existAny(K[] keys, long timeout) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> future = executor.submit(new Callable() {
            public Object call() throws Exception {
                return bexists(keys);
            }
        });
        Boolean obj;
        try {
            obj = future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            obj = false;
        } catch (InterruptedException e) {
            obj = false;
        } catch (ExecutionException e) {
            obj = false;
        }
        executor.shutdownNow();
        return obj;
    }

    public void put(K key, V value) {
        while (inp (key) != null)
            ; // NOPMD
        out (key, value);
    }

    public void put(K key, V value, long timeout) {
        while (inp (key) != null)
            ; // NOPMD
        out (key, value, timeout);
    }
}
