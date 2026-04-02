package code.ticketreservationapp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Example JUnit5 test with Mockito demonstrating modern testing practices.
 * 
 * Key features:
 * - @ExtendWith(MockitoExtension.class) enables Mockito in JUnit5
 * - @Mock creates mock objects
 * - @BeforeEach runs before each test method
 * - @DisplayName provides readable test names
 * - @ParameterizedTest allows testing with multiple inputs
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Example JUnit5 Test Suite")
public class ExampleJUnit5Test {

    @Mock
    private SampleService mockService;

    private Calculator calculator;

    @BeforeEach
    void setUp() {
        // Initialize test objects before each test
        calculator = new Calculator();
    }

    @Test
    @DisplayName("Should add two numbers correctly")
    void testAddition() {
        int result = calculator.add(2, 3);
        assertEquals(5, result, "2 + 3 should equal 5");
    }

    @Test
    @DisplayName("Should subtract two numbers correctly")
    void testSubtraction() {
        int result = calculator.subtract(5, 3);
        assertEquals(2, result, "5 - 3 should equal 2");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 5, 10, 100})
    @DisplayName("Should verify positive numbers")
    void testPositiveNumbers(int number) {
        assertTrue(number > 0, "Number should be positive");
    }

    @Test
    @DisplayName("Should mock service behavior")
    void testMockService() {
        // Arrange: Set up mock behavior
        when(mockService.getValue()).thenReturn("Mocked Value");

        // Act: Call the mock
        String result = mockService.getValue();

        // Assert: Verify behavior
        assertEquals("Mocked Value", result);
        verify(mockService, times(1)).getValue();
    }

    @Test
    @DisplayName("Should throw exception for invalid operation")
    void testException() {
        assertThrows(ArithmeticException.class, () -> {
            calculator.divide(10, 0);
        }, "Division by zero should throw ArithmeticException");
    }

    // Simple Calculator class for testing
    static class Calculator {
        public int add(int a, int b) {
            return a + b;
        }

        public int subtract(int a, int b) {
            return a - b;
        }

        public int divide(int a, int b) {
            if (b == 0) {
                throw new ArithmeticException("Cannot divide by zero");
            }
            return a / b;
        }
    }

    // Sample service interface for mocking demonstration
    interface SampleService {
        String getValue();
    }
}
