package org.jpos.gl.dbtpl;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UserIdGeneratorImplUnitTest {
    @TestTemplate
    @ExtendWith(UserIdGeneratorTestInvocationContextProvider.class)
    public void whenUserIdRequested_thenUserIdIsReturnedInCorrectFormat(UserIdGeneratorTestCase testCase) {
        UserIdGenerator userIdGenerator = new UserIdGeneratorImpl(testCase.isFeatureEnabled());

        String actualUserId = userIdGenerator.generate(testCase.getFirstName(), testCase.getLastName());

        assertEquals(testCase.getExpectedUserId(), actualUserId);
        //assertThat(actualUserId).isEqualTo(testCase.getExpectedUserId());
    }
}
