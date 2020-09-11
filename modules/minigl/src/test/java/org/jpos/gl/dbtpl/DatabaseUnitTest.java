package org.jpos.gl.dbtpl;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

public class DatabaseUnitTest {
    @TempDir
    public File configDir;

    @TestTemplate
    @ExtendWith(DatabaseTestInvocationContextProvider.class)
    public void whenStuffHappens(DatabaseTestCase testCase) throws IOException {
        System.out.println(testCase.getDB(configDir));
    }
}
