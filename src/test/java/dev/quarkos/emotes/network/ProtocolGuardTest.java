package dev.quarkos.emotes.network;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ProtocolGuardTest {
    @Test void boundsIdentityAndRateLimit() {
        var guard=new ProtocolGuard();Object a=new Object(),b=new Object();
        assertTrue(guard.accept(a,"test:wave",0));assertTrue(guard.accept(a,"",1));assertFalse(guard.accept(a,"",2));assertFalse(guard.accept(a,"test:wave",100_000_000));
        assertTrue(guard.accept(b,"test:wave",100_000_000));assertTrue(guard.accept(a,"",150_000_000));
        assertFalse(guard.accept(a,"bad id",999_000_000));assertFalse(guard.accept(a,"x:"+"a".repeat(128),999_000_000));
        assertTrue(guard.accept(a,"test:wave",999_000_000));
    }
}
