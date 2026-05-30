package com.mlbb.scrim.data.model

import org.junit.Assert.*
import org.junit.Test

class AuthResultTest {

    @Test
    fun `AuthResult Idle is singleton`() {
        val idle1 = AuthResult.Idle
        val idle2 = AuthResult.Idle
        assertSame(idle1, idle2)
    }

    @Test
    fun `AuthResult Success is singleton`() {
        val success1 = AuthResult.Success
        val success2 = AuthResult.Success
        assertSame(success1, success2)
    }

    @Test
    fun `AuthResult Loading is singleton`() {
        val loading1 = AuthResult.Loading
        val loading2 = AuthResult.Loading
        assertSame(loading1, loading2)
    }

    @Test
    fun `AuthResult Error stores message`() {
        val error = AuthResult.Error("Test error message")
        assertEquals("Test error message", error.message)
    }

    @Test
    fun `AuthResult Error equality`() {
        val error1 = AuthResult.Error("same")
        val error2 = AuthResult.Error("same")
        assertEquals(error1, error2)
    }

    @Test
    fun `AuthResult EmailNotVerified stores email`() {
        val ev = AuthResult.EmailNotVerified("test@example.com")
        assertEquals("test@example.com", ev.email)
    }

    @Test
    fun `AuthResult EmailNotVerified equality`() {
        val ev1 = AuthResult.EmailNotVerified("a@b.com")
        val ev2 = AuthResult.EmailNotVerified("a@b.com")
        assertEquals(ev1, ev2)
    }

    @Test
    fun `AuthResult types are not equal to each other`() {
        assertNotEquals(AuthResult.Idle, AuthResult.Success)
        assertNotEquals(AuthResult.Success, AuthResult.Loading)
        assertNotEquals(AuthResult.Error("x"), AuthResult.Error("y"))
    }
}
