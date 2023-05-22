package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        //env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        for (int i = 0; i<players.length; i++){
            Thread threadPlayer = new Thread(players[i],"player number " + players[i].id);
            threadPlayer.start();
        }
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
        }
        announceWinners();
        env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis + 1000;
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            removeCardsFromTable();
            placeCardsOnTable();
        }
        reshuffleTime = System.currentTimeMillis();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        for (int i=0; i<players.length; i++){
            players[i].terminate();
        }
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    public synchronized void removeCardsFromTable() {
        // TODO implement
        while(!table.sets.isEmpty()){
            env.logger.info("Thread " + Thread.currentThread().getName() + " checking set.");
            int[] cards = table.sets.poll();
            int playerId = cards[3];
            int [] cardsOnly = new int[3];
            for (int i=0; i<cardsOnly.length; i++){
                cardsOnly[i] = cards[i];
            }
            boolean stillOnTable = true;
            for(int i = 0 ;i<cardsOnly.length;i++){
                if(table.cardToSlot[cardsOnly[i]] == null){
                    stillOnTable = false;
                }
            }
            if(stillOnTable){
                if(env.util.testSet(cardsOnly)){
                    env.logger.info("Thread " + Thread.currentThread().getName() + " say : this is set!!!!!.");
                    players[playerId].setChecked = 1;
                    players[playerId].state = 1;
                    env.logger.info("Thread " + Thread.currentThread().getName() +" change state to " + players[playerId].state);
                    env.logger.info("Thread " + Thread.currentThread().getName() +" change setCheck to " + players[playerId].setChecked);
                    for(int i=0; i<cardsOnly.length; i++){
                        table.removeCard(table.cardToSlot[cardsOnly[i]]);
                        deck.remove((Integer)cardsOnly[i]);
                    }
                    updateTimerDisplay(true);
                    synchronized(players[playerId].locked){
                        players[playerId].locked.notifyAll();
                        //if(!players[playerId].human){
                        //    synchronized(players[playerId].aiThread){
                        //        players[playerId].aiThread.notifyAll();
                        //    }
                        //}
                    }
                }
                else{
                    env.logger.info("Thread " + Thread.currentThread().getName() + " say : this is not set!!!!!.");
                    players[playerId].state = -1;
                    players[playerId].setChecked = 1;
                    env.logger.info("Thread " + Thread.currentThread().getName() +" change state to " + players[playerId].state);
                    env.logger.info("Thread " + Thread.currentThread().getName() +" change setCheck to " + players[playerId].setChecked);
                    synchronized(players[playerId].locked){
                        players[playerId].locked.notifyAll();;
                        if(!players[playerId].human){
                            synchronized(players[playerId].aiThread){
                                players[playerId].aiThread.notifyAll();
                            }
                        }
                    }
                }
                synchronized(players[playerId].locked){
                    players[playerId].locked.notifyAll();;
                    if(!players[playerId].human){
                        synchronized(players[playerId].aiThread){
                            players[playerId].aiThread.notifyAll();
                        }
                    }
                }
            }
            synchronized(players[playerId].locked){
                players[playerId].locked.notifyAll();;
                if(!players[playerId].human){
                    synchronized(players[playerId].aiThread){
                        players[playerId].aiThread.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    public synchronized void placeCardsOnTable() {
        // TODO implement
        Random rnd = new Random();
        int randomCard;
        while (table.countCards() < env.config.tableSize){
            int emptySlot = -1;
            for(int i = 0 ; i < table.slotToCard.length & emptySlot == -1 ; i ++){
                if(table.slotToCard[i] == null){
                    emptySlot = i;
                }
            }
            env.logger.info("Thread " + Thread.currentThread().getName() +" found empty slot " + emptySlot);
            if(deck.size() > 0){
                randomCard = rnd.nextInt((deck.size() - 1) - 0 + 1);
                if(table.cardToSlot[deck.get(randomCard)] == null){
                    table.placeCard(deck.get(randomCard), emptySlot);
                }
            }
            else{
                terminate();
            }
        }
        for ( int i = 0; i<players.length; i++){
            synchronized(players[i].locked){
                players[i].locked.notifyAll();;
                if(!players[i].human){
                    synchronized(players[i].aiThread){
                        players[i].aiThread.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        // TODO implement
        if(reshuffleTime - System.currentTimeMillis() > env.config.turnTimeoutWarningMillis){
            try {
                Thread.sleep(env.config.tableDelayMillis);
            } catch (InterruptedException ignored) {}
        }
        else{
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {}
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        //env.ui.setCountdown(env.config.turnTimeoutMillis, env.config.turnTimeoutMillis <= env.config.turnTimeoutWarningMillis);
        if (reset){
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
        }
        env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), reshuffleTime - System.currentTimeMillis() <= env.config.turnTimeoutWarningMillis);

    }

    /**
     * Returns all the cards from the table to the deck.
     */
    public void removeAllCardsFromTable() {
        // TODO implement
        for(int i = 0; i<table.slotToCard.length; i++){
            if(table.slotToCard[i] != null){
                table.removeCard(i);
            }
        }
        for(int i = 0;i<players.length;i++)
        {
            synchronized(players[i].locked){
                players[i].locked.notifyAll();
                if(!players[i].human & players[i].aiThread != null){
                    synchronized(players[i].aiThread){
                        players[i].aiThread.notifyAll();
                    }
                }
            }
        }
    }
    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int maxScore = 0;
        ArrayList<Integer> winners = new ArrayList<Integer>();
        //compute the max score
        for (int i = 0; i<players.length; i++){
            if(players[i].score() > maxScore){
                maxScore = players[i].score();
            }
        }
        //add all winners to list
        for (int i = 0; i<players.length; i++){
            if (players[i].score() == maxScore){
                winners.add(players[i].id);
            }
        }
        int[] wiPlayers = new int[winners.size()];
        for (int i = 0; i<winners.size(); i++){
            wiPlayers[i] = winners.get(i);
        }
        env.ui.announceWinner(wiPlayers);

    }
}
