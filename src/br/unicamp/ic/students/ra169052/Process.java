package br.unicamp.ic.students.ra169052;

public class Process {
    private static int PEER_COUNT = 5;

    public Network network;
    //my clock (starts with value = 1)
    public Clock clock;
    //queue of messages
    public Message[] queue;

    public Process(){
        queue = new Message[PEER_COUNT];
        //at the start, pid 0 has access to the critical region
        queue[0] = new Message(new Clock(0, 0), Message.Action.REQUEST);
    }

    /**
     * Request access to critical region
     */
    private void requestAccess(){
        Message message = new Message(clock, Message.Action.REQUEST);
        network.broadcastExcept(message, clock.pid);
        clock.increment();
    }

    /**
     * Do work on the critical region
     */
    private void doProcess(){
        sleepFor(1000);
    }

    /**
     * Notify peers that this process has exited the critical region
     */
    private void release(){
        Message message = new Message(clock, Message.Action.RELEASE);
        network.broadcastExcept(message, clock.pid);
        queue[clock.pid] = message;
        clock.increment();
    }

    /**
     * Starts thread for handling incoming messages
     */
    public void startMessengerThread(){
        new Thread(() -> {
            while(true) {
                //try to get a message for this replica
                Message message = network.getMessageFor(clock.pid);
                if(message != null){
                    handleMessage(message);
                }

                sleepFor(500);
            }
        }).start();
    }

    /**
     * Handles incoming message
     * @param message
     */
    private void handleMessage(Message message) {
        int senderPid = message.clock.pid;
        updateClock(message.clock);

        switch (message.action){
            case REQUEST:
                queue[senderPid] = message;
                Message reply = new Message(clock, Message.Action.ACK);
                network.sendTo(reply, senderPid);
                break;
            case ACK:
                Message sendersLastMessage = queue[senderPid];
                //do not substitute last message if it was a REQUEST
                if(sendersLastMessage != null && sendersLastMessage.action != Message.Action.REQUEST){
                    queue[senderPid] = message;
                }

                //if i have requested access, test if it's my turn
                Message myLastMessage = queue[clock.pid];
                if(myLastMessage != null && myLastMessage.action == Message.Action.REQUEST){
                    //find lowest clock
                    Clock lowestClock = queue[0].clock;
                    for(int i = 1; i < PEER_COUNT; i++){
                        Message queueMessage = queue[i];
                        if(queueMessage != null){
                            lowestClock = Clock.GetLowestClock(clock, queueMessage.clock);
                        }
                    }

                    //test if the lowest clock is mine
                    if(lowestClock.pid == clock.pid){
                        //work on critical region
                        doProcess();
                        //release critical region
                        release();
                    }
                }
                break;
            case RELEASE:
                queue[senderPid] = message;
                break;
        }
    }

    /**
     * Update clock based on another a message timestamp
     * @param remoteClock
     */
    private void updateClock(Clock remoteClock) {
        clock.value = Math.max(clock.value, remoteClock.value) + 1;
    }

    /**
     * Makes thread sleep for around milis ms
     * @param milis
     */
    private void sleepFor(int milis){
        try {
            Thread.sleep(milis + (int) (milis * 0.5 * Math.random()));
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public static void main(String[] args) {

    }
}
