/*
 * Copyright 2015-2018 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder.request;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.testkit.ExecutionResults;
import org.opentest4j.TestAbortedException;

/**
 * Tests for discovery and execution of standard test cases for the
 * {@link JupiterTestEngine}.
 *
 * @since 5.0
 */
class StandardTestClassTests extends AbstractJupiterTestEngineTests {

	@Test
	void standardTestClassIsCorrectlyDiscovered() {
		LauncherDiscoveryRequest request = request().selectors(selectClass(MyStandardTestCase.class)).build();
		TestDescriptor engineDescriptor = discoverTests(request);
		assertEquals(1 /*class*/ + 6 /*methods*/, engineDescriptor.getDescendants().size(),
			"# resolved test descriptors");
	}

	@Test
	void moreThanOneTestClassIsCorrectlyDiscovered() {
		LauncherDiscoveryRequest request = request().selectors(selectClass(SecondOfTwoTestCases.class)).build();
		TestDescriptor engineDescriptor = discoverTests(request);
		assertEquals(1 /*class*/ + 3 /*methods*/, engineDescriptor.getDescendants().size(),
			"# resolved test descriptors");
	}

	@Test
	void moreThanOneTestClassIsExecuted() {
		LauncherDiscoveryRequest request = request().selectors(selectClass(FirstOfTwoTestCases.class),
			selectClass(SecondOfTwoTestCases.class)).build();

		ExecutionResults executionResults = executeTests(request).getExecutionResults();

		assertEquals(6, executionResults.getTestStartedCount(), "# tests started");
		assertEquals(5, executionResults.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(1, executionResults.getTestFailedCount(), "# tests failed");

		assertEquals(3, executionResults.getContainerStartedCount(), "# containers started");
		assertEquals(3, executionResults.getContainerFinishedCount(), "# containers finished");
	}

	@Test
	void allTestsInClassAreRunWithBeforeEachAndAfterEachMethods() {
		ExecutionResults executionResults = executeTestsForClass(MyStandardTestCase.class).getExecutionResults();

		assertEquals(2, executionResults.getContainerStartedCount(), "# containers started");
		assertEquals(2, executionResults.getContainerFinishedCount(), "# containers finished");

		assertEquals(6, executionResults.getTestStartedCount(), "# tests started");
		assertEquals(2, executionResults.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(3, executionResults.getTestAbortedCount(), "# tests aborted");
		assertEquals(1, executionResults.getTestFailedCount(), "# tests failed");

		assertEquals(6, MyStandardTestCase.countBefore1, "# before1 calls");
		assertEquals(6, MyStandardTestCase.countBefore2, "# before2 calls");
		assertEquals(6, MyStandardTestCase.countAfter, "# after each calls");
	}

	@Test
	void testsFailWhenBeforeEachFails() {
		ExecutionResults executionResults = executeTestsForClass(TestCaseWithFailingBefore.class).getExecutionResults();

		assertEquals(2, executionResults.getTestStartedCount(), "# tests started");
		assertEquals(0, executionResults.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(2, executionResults.getTestFailedCount(), "# tests failed");

		assertEquals(2, executionResults.getContainerStartedCount(), "# containers started");
		assertEquals(2, executionResults.getContainerFinishedCount(), "# containers finished");

		assertEquals(2, TestCaseWithFailingBefore.countBefore, "# before each calls");
	}

	@Test
	void testsFailWhenAfterEachFails() {
		ExecutionResults executionResults = executeTestsForClass(TestCaseWithFailingAfter.class).getExecutionResults();

		assertEquals(1, executionResults.getTestStartedCount(), "# tests started");
		assertEquals(0, executionResults.getTestSuccessfulCount(), "# tests succeeded");
		assertEquals(1, executionResults.getTestFailedCount(), "# tests failed");

		assertEquals(2, executionResults.getContainerStartedCount(), "# containers started");
		assertEquals(2, executionResults.getContainerFinishedCount(), "# containers finished");

		assertTrue(TestCaseWithFailingAfter.testExecuted, "test executed?");
	}

	static class MyStandardTestCase {

		static int countBefore1 = 0;
		static int countBefore2 = 0;
		static int countAfter = 0;

		@BeforeEach
		void before1() {
			countBefore1++;
		}

		@BeforeEach
		void before2() {
			countBefore2++;
		}

		@AfterEach
		void after() {
			countAfter++;
		}

		@Test
		void succeedingTest1() {
			assertTrue(true);
		}

		@Test
		void succeedingTest2() {
			assertTrue(true);
		}

		@Test
		void failingTest() {
			fail("always fails");
		}

		@Test
		@DisplayName("Test aborted via the OTA's TestAbortedException")
		void testAbortedOpenTest4J() {
			throw new TestAbortedException("aborted!");
		}

		@Test
		@DisplayName("Test aborted via JUnit 4's AssumptionViolatedException")
		void testAbortedJUnit4() {
			throw new org.junit.AssumptionViolatedException("aborted!");
		}

		@Test
		@DisplayName("Test aborted via JUnit 4's legacy, deprecated AssumptionViolatedException")
		@SuppressWarnings("deprecation")
		void testAbortedJUnit4Legacy() {
			throw new org.junit.internal.AssumptionViolatedException("aborted!");
		}

	}

	static class FirstOfTwoTestCases {

		@Test
		void succeedingTest1() {
			assertTrue(true);
		}

		@Test
		void succeedingTest2() {
			assertTrue(true);
		}

		@Test
		void failingTest() {
			fail("always fails");
		}

	}

	static class SecondOfTwoTestCases {

		@Test
		void succeedingTest1() {
			assertTrue(true);
		}

		@Test
		void succeedingTest2() {
			assertTrue(true);
		}

		@Test
		void succeedingTest3() {
			assertTrue(true);
		}

	}

	static class TestCaseWithFailingBefore {

		static int countBefore = 0;

		@BeforeEach
		void before() {
			countBefore++;
			throw new RuntimeException("Problem during setup");
		}

		@Test
		void test1() {
		}

		@Test
		void test2() {
		}

	}

	static class TestCaseWithFailingAfter {

		static boolean testExecuted = false;

		@AfterEach
		void after() {
			throw new RuntimeException("Problem during 'after'");
		}

		@Test
		void test1() {
			testExecuted = true;
		}

	}

}
