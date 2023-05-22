package bguspl.set.ex;

import java.util.LinkedList;
import java.util.Random;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    public Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    public Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    public final boolean human;

    /**
     * True iff game should be terminated.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;


    private final Dealer dealer;

    final int numOfActions = 3;

    public LinkedList<Integer> actions = new LinkedList<>();

    int state = 0;
    
    int setChecked = 1;

    Object locked = new Object();

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop

            // if (actions.size() == numOfActions){
            //     // synchronized(dealer){
            //     //     dealer.notify();
            //     // }
            //     try {
            //         synchronized(this){
            //             this.wait();
            //         }
            //     } catch (InterruptedException e) {
            //         // TODO Auto-generated catch block
            //         e.printStackTrace();
            //     }
            // }

            {
                try {
                    if (setChecked == 0){
                        synchronized(locked){
                            locked.wait();
                        }
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            
            if (setChecked == 1){
                synchronized(locked){
                    locked.notifyAll();   
                }
            }
            //env.logger.info("state  " + state);
            if (state == -1){
                penalty();
            }
            else if (state == 1){
                point();
            }
            state = 0;
            // synchronized(aiThread){
            //     aiThread.notify();
            // }

        }

        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very, very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                Random rnd = new Random();
                int randomSlot = rnd.nextInt((env.config.tableSize- 1) - 0 + 1);
                keyPressed(randomSlot);
                while(actions.size()==numOfActions){
                    try {
                        synchronized (aiThread) { aiThread.wait(); }
                    } catch (InterruptedException ignored) {}
                }
                synchronized(this){
                    this.notifyAll();
                }

            }
            env.logger.info("thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        synchronized(table){
        if (table.slotToCard[slot] != null){
            if (actions.contains(slot)){
                // if (actions.size() == numOfActions){
                //     synchronized(actions){
                //         actions.notify();   
                //     }
                // }
                table.removeToken(id, slot);
                actions.remove((Integer)slot);
            }
        
            else {
                if(actions.size() < numOfActions){
                    table.placeToken(id, slot);
                    actions.add(slot);
                    env.logger.info("actions size of " + Thread.currentThread().getName() + " is " + actions.size());
                    if(actions.size() == numOfActions){
                        int[] cards = new int[numOfActions+1];
                        for(int i = 0 ; i < numOfActions ; i ++){
                            int sloty = actions.get(i);
                            while (table.slotToCard[sloty] == null){
                                try {
                                    Thread.sleep(env.config.pointFreezeMillis/10);
                                } catch (InterruptedException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                            cards[i] = table.slotToCard[sloty];
                        }
                        cards[numOfActions] = id;
            
                        table.sets.offer(cards);
                        setChecked = 0;
                    
                        env.logger.info("tread " + Thread.currentThread().getName() + " set added");
                        env.logger.info("num of sets to check " + table.sets.size());
                    }
                }
            }
        }
        }
    }
    

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        // TODO implement
        for (int i = 0; i<actions.size(); i++){
            table.removeToken(id, actions.get(i));
        } 
        env.ui.setFreeze(id, env.config.pointFreezeMillis);
        try {
            Thread.sleep(env.config.pointFreezeMillis);
            //aiThread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       
        actions.clear();
        env.ui.setFreeze(id, 0);

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public synchronized void penalty() {
        env.ui.setFreeze(id, env.config.penaltyFreezeMillis);
        try {
            Thread.sleep(env.config.penaltyFreezeMillis);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        for(int i=0; i<actions.size(); i++){
            table.removeToken(id, actions.get(i));
        }
        actions.clear();
        env.ui.setFreeze(id, 0);
        
    }

    public int score() {
        return score;
    }
}
