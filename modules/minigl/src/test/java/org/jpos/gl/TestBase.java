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

package org.jpos.gl;

import org.jpos.ee.DB;
import org.jpos.gl.dbtpl.DatabaseTestCase;
import org.jpos.gl.dbtpl.DatabaseTestInvocationContextProvider;
import org.jpos.gl.tools.Export;
import org.jpos.gl.tools.Import;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
public abstract class TestBase {
    protected static GLSession gls;
    protected static long start;
    protected static long checkpoint;

    @TempDir
    public static File configDir;

    @BeforeAll
    @ExtendWith(DatabaseTestInvocationContextProvider.class)
    public static void setUpBase () throws Exception {
        try {
            String userName = System.getProperty("user.name");
            System.setProperty("user.name", "travis");
            new Import().parse("../test-classes/testdata.xml");
            new Export().export(System.out);
            System.setProperty("user.name", userName);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        start = checkpoint = System.currentTimeMillis();
    }
    @AfterAll
    @ExtendWith(DatabaseTestInvocationContextProvider.class)
    public static void tearDownBase () throws Exception {
        if (gls != null) {
            gls.close();
        }
    }


    @ExtendWith(DatabaseTestInvocationContextProvider.class)
    @TestTemplate
    public static void getGls (DatabaseTestCase testCase) throws Exception {
        DB db = testCase.getDB(configDir);
        if (gls == null) {
            gls = new GLSession(db, "bob");
        }
    }

    public void start () {
        start = checkpoint = System.currentTimeMillis();
    }
    public void start (String s) {
        System.out.println ("     start: " + s + " ("
            + Runtime.getRuntime().totalMemory() + ")"
        );
        start = checkpoint = System.currentTimeMillis();
    }
    public void checkPoint (String s) {
        long now = System.currentTimeMillis();
        System.out.println ("checkpoint: " + s + " [" + (now - checkpoint) + "] (" 
            + Runtime.getRuntime().totalMemory() + ")"
        );
        checkpoint = now;
    }
    public void end (String s) {
        long now = System.currentTimeMillis();
        System.out.println ("       end: " + s + " [" + (now - start) + "] (" 
            + Runtime.getRuntime().totalMemory() + ")"
        );
        start = checkpoint = now;
    }
}

