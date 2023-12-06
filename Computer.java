import java.util.Timer;
import java.util.TimerTask;

public class Computer extends Player {

    private final Object lock;
    private Model model;

    public Computer(Model model, ShotsQueue shotsQueue, boolean isGameRunning) {
        super(shotsQueue, isGameRunning);
        this.model = model;
        lock = model.getLock();
    }

    public void doAction() {
        synchronized (lock) {
            waitForTurn();
            produceShot();
        }
    }

    public void waitForTurn() {
        try {
            lock.wait();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void produceShot() {
        Shot shot = generateShot();
        ShotsQueue shots = getShotsQueue();
        try {
            shots.add(shot);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private Shot generateShot() {
        return new Shot(0, 0, PlayerType.COMPUTER); //logic of computer's shot generation
    }
}
