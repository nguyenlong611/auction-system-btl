import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CalculatorTest {
    @Test
    public void testAdd1() {
        Calculator calculator = new Calculator();
        int actualResult = calculator.add(2, 3);
        int expectedResult = 5;
        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testDivide1() {
        Calculator calculator = new Calculator();
        int actualResult = calculator.divide(6, 3);
        int expectedResult = 2;
        Assertions.assertEquals(expectedResult, actualResult);
    }

    @Test
    public void testDivide2() {
        Calculator calculator = new Calculator();
        Assertions.assertThrows(IllegalArgumentException.class, () -> calculator.divide(3, 0));
    }
}
