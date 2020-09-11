package org.jpos.gl.dbtpl;

public interface UserIdGenerator {
    String generate(String firstName, String lastName);
}
