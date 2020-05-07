/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2020 jPOS Software SRL
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

package org.jpos.binlog;

import org.jpos.iso.ISOUtil;
import org.jpos.util.TPS;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.commons.lang3.SystemUtils.IS_OS_MAC_OSX;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;
import static org.junit.jupiter.api.Assertions.assertEquals;


@TestMethodOrder(OrderAnnotation.class)
public class BinLogTest implements Runnable {
    public static File dir;
    private AtomicLong cnt = new AtomicLong();

    @BeforeEach
    public void before () {
        // Skip Tests for BinLog if on MS Windows/OSX due to severe lock contention performance issues
        Assumptions.assumeFalse(IS_OS_WINDOWS || IS_OS_MAC_OSX);
    }
    
    @BeforeAll
    public static void setup () throws IOException {
        dir = File.createTempFile("binlog-", "");
        dir.delete();
        System.out.println ("TEMP=" + dir);
        // dir = new File("/tmp/binlog");
    }
    @Test
    @Order(1)
    public void test000_Write() throws IOException {
        try (BinLogWriter w = new BinLogWriter(dir)) { }
        for (int i=0; i<10; i++) {
            new Thread(this).start();
        }
        try (BinLogReader bl = new BinLogReader(dir)) {
            int i = 0;
            while (bl.hasNext(10000L)) {
                i++;
                byte[] b = bl.next().get();
                if ((i % 1000) == 0)
                    System.out.println(i + " " + new String(b));
            }
            assertEquals(100000, i, "Invalid number of entries");
        }
    }

    public void run() {
        TPS tps = new TPS();
        try (BinLogWriter bl = new BinLogWriter(dir)) {
            for (int i = 1; i <= 10000; i++) {
                long l = cnt.incrementAndGet();
                if (i % 5000 == 0) {
                    bl.cutover();
                }
                bl.add(ISOUtil.zeropad(l, 12).getBytes());
                tps.tick();
            }
            tps.dump(System.out, "");
        } catch (Throwable e) {
            e.printStackTrace(System.err);
        }
    }

    @AfterAll
    public static void cleanup() throws IOException {
        if (dir.listFiles() != null) {
            for (File f : dir.listFiles()) {
                if (f.toString().endsWith(".dat")) {
                    System.out.println ("Deleting " + f.toString());
                    f.delete();
                }
            }
        }
        System.out.println ("Deleting " + dir);
        dir.delete();
    }
}
