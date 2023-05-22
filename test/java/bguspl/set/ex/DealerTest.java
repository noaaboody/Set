package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DealerTest {

    Dealer dealer;
    Player[] players;
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Logger logger;

    void assertInvariants() {
        assertTrue(players.length > 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, ""), ui, util);
        players = new Player[2];
        for(int i = 0; i<players.length; i++){
            players[i] = new Player(env, dealer, table, i, false);
        }
        table = new Table(env);
        dealer = new Dealer(env, table, players);
        assertInvariants();
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    private void fillAllSlots() {
        for (int i = 0; i < table.slotToCard.length; ++i) {
            table.slotToCard[i] = i;
            table.cardToSlot[i] = i;
        }
    }

    @Test
    void placeCardsOnTable() {
        int expected = 12;
        dealer.placeCardsOnTable();
        assertEquals(expected, table.countCards());
    }
    
    @Test
    void removeAllCardsFromTable() {
        int expected = 0;
        dealer.removeAllCardsFromTable();
        assertEquals(expected, table.countCards());
    }
}