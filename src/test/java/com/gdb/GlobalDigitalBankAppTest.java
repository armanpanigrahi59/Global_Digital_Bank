package com.gdb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.gdb.domain.AccountType;
import com.gdb.logging.DatabaseLogDestination;
import com.gdb.logging.FileLogDestination;
import com.gdb.logging.LogDestination;
import com.gdb.logging.MemoryLogDestination;
import com.gdb.service.AccountService;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

class GlobalDigitalBankAppTest {

    @Test
    void chooseTheLogDestinationByName() {
        assertTrue(GlobalDigitalBankApp.createDestination("file", Paths.get("x.log")) instanceof FileLogDestination);
        assertTrue(GlobalDigitalBankApp.createDestination("MEMORY", null) instanceof MemoryLogDestination);
        assertTrue(GlobalDigitalBankApp.createDestination(" db ", null) instanceof DatabaseLogDestination);
    }

    @Test
    void unknownDestinationIsRejected() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> GlobalDigitalBankApp.createDestination("cloud", null));
        assertEquals("Unknown log destination 'cloud'. Use file, memory or db.", e.getMessage());
    }

    @Test
    void wiredServiceWorksEndToEnd() throws Exception {
        LogDestination memory = new MemoryLogDestination();
        AccountService service = GlobalDigitalBankApp.createService(memory);
        String no = service.openAccount(AccountType.SAVINGS, "Asha", 30, 1000, "1234", 0).getAccountNumber();
        service.deposit(no, 50);
        assertEquals(1050.0, service.getBalance(no, "1234"));
        assertEquals(1, memory.readAll().size());
    }
}
