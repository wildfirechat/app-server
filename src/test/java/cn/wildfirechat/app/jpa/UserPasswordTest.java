package cn.wildfirechat.app.jpa;

import org.junit.Test;

import static org.junit.Assert.*;

public class UserPasswordTest {

    @Test
    public void testDefaultConstructor() {
        // When
        UserPassword userPassword = new UserPassword();

        // Then
        assertNull(userPassword.getUserId());
        assertNull(userPassword.getPassword());
        assertNull(userPassword.getSalt());
        assertNull(userPassword.getResetCode());
        assertEquals(0, userPassword.getTryCount());
        assertEquals(0L, userPassword.getResetCodeTime());
        assertEquals(0L, userPassword.getLastTryTime());
    }

    @Test
    public void testConstructorWithUserId() {
        // Given
        String userId = "user123";

        // When
        UserPassword userPassword = new UserPassword(userId);

        // Then
        assertEquals(userId, userPassword.getUserId());
        assertNull(userPassword.getPassword());
        assertEquals(0, userPassword.getTryCount());
    }

    @Test
    public void testConstructorWithAllParams() {
        // Given
        String userId = "user123";
        String password = "hashedPassword";
        String salt = "randomSalt";
        String resetCode = "123456";
        long resetCodeTime = System.currentTimeMillis();

        // When
        UserPassword userPassword = new UserPassword(userId, password, salt, resetCode, resetCodeTime);

        // Then
        assertEquals(userId, userPassword.getUserId());
        assertEquals(password, userPassword.getPassword());
        assertEquals(salt, userPassword.getSalt());
        assertEquals(resetCode, userPassword.getResetCode());
        assertEquals(resetCodeTime, userPassword.getResetCodeTime());
        assertEquals(0, userPassword.getTryCount());
    }

    @Test
    public void testSettersAndGetters() {
        // Given
        UserPassword userPassword = new UserPassword();

        // When
        userPassword.setUserId("user456");
        userPassword.setPassword("newPassword");
        userPassword.setSalt("newSalt");
        userPassword.setResetCode("654321");
        userPassword.setResetCodeTime(1234567890L);
        userPassword.setTryCount(5);
        userPassword.setLastTryTime(9876543210L);

        // Then
        assertEquals("user456", userPassword.getUserId());
        assertEquals("newPassword", userPassword.getPassword());
        assertEquals("newSalt", userPassword.getSalt());
        assertEquals("654321", userPassword.getResetCode());
        assertEquals(1234567890L, userPassword.getResetCodeTime());
        assertEquals(5, userPassword.getTryCount());
        assertEquals(9876543210L, userPassword.getLastTryTime());
    }

    @Test
    public void testTryCountIncrementation() {
        // Given
        UserPassword userPassword = new UserPassword("user", "pass", "salt");
        assertEquals(0, userPassword.getTryCount());

        // When - increment from constructor default (0)
        userPassword.setTryCount(userPassword.getTryCount() + 1);
        userPassword.setTryCount(userPassword.getTryCount() + 1);

        // Then
        assertEquals(2, userPassword.getTryCount());
    }
}
