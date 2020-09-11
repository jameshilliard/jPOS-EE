package org.jpos.gl.dbtpl;


import org.junit.jupiter.api.extension.*;
import io.zonky.test.db.postgres.junit5.EmbeddedPostgresExtension;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class DatabaseTestInvocationContextProvider implements TestTemplateInvocationContextProvider {
    @Override
    public boolean supportsTestTemplate(ExtensionContext extensionContext) {
        return true;
    }
    @Override
    public Stream<TestTemplateInvocationContext> provideTestTemplateInvocationContexts(
            ExtensionContext extensionContext) {
        return Stream.of(
                dbContext(new DatabaseTestCase("h2")),
                dbContext(new DatabaseTestCase("postgres"))
        );
    }
    private TestTemplateInvocationContext dbContext(
            DatabaseTestCase dbTestCase) {
        return new TestTemplateInvocationContext() {
            @Override
            public String getDisplayName(int invocationIndex) {
                return dbTestCase.getDriver();
            }

            @Override
            public List<Extension> getAdditionalExtensions() {
                return asList(
                        new GenericTypedParameterResolver(dbTestCase),
                        //EmbeddedPostgresExtension.singleInstance(),
                        new BeforeTestExecutionCallback() {
                            @Override
                            public void beforeTestExecution(ExtensionContext extensionContext) {
                                System.out.println("BeforeTestExecutionCallback:Disabled context");
                            }
                        },
                        new AfterTestExecutionCallback() {
                            @Override
                            public void afterTestExecution(ExtensionContext extensionContext) {
                                System.out.println("AfterTestExecutionCallback:Disabled context");
                            }
                        }
                );
            }
        };
    }
}
