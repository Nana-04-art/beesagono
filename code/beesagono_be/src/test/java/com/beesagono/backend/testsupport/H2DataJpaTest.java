package com.beesagono.backend.testsupport;

import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation designed for JPA repository tests using H2 in-memory.
 *
 * It is equivalent to writing @DataJpaTest plus the @TestPropertySource block
 * containing H2 datasource properties, but in a single line. This is necessary
 * because, in Spring Boot 4.0.8, the automatic DataSource replacement mechanism
 * of @AutoConfigureTestDatabase (included in @DataJpaTest) does not write the
 * "spring.test.database.replace" property when the value is the default (ANY)
 * — @PropertyMapping(skip = ON_DEFAULT_VALUE) — meaning the actual DataSource
 * (MySQL) is never automatically replaced. Explicitly setting the H2
 * properties here bypasses the issue.
 *
 * Usage:
 * 
 * @H2DataJpaTest
 *                class MyRepositoryTest { ... }
 *
 *                If the bug is fixed in a future version of Spring Boot,
 *                you will only need to update or remove this annotation
 *                in one place.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@DataJpaTest
@TestPropertySource(properties = {
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.jpa.show-sql=true",
                "spring.jpa.properties.hibernate.format_sql=true",
                "logging.level.org.hibernate.SQL=DEBUG",
                "logging.level.org.hibernate.tool.schema=DEBUG",
                "logging.level.org.hibernate.type.descriptor.sql=TRACE"
})
public @interface H2DataJpaTest {
}