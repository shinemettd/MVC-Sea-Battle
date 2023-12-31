import javax.swing.ImageIcon;
import java.awt.*;

public class GameLogic implements Runnable {

    private Model model;
    private Thread thread;
    private ShotsQueue shotsQueue;
    private final Object lock;
    private boolean isRunning;
    private int userShipsNumber;
    private int computerShipsNumber;
    private final int SHIPS_NUMBER = 10;
    private final int SHIP = 1;
    private final int HIT_SHOT = 2;
    private final int MISS_SHOT = 3;
    private final int DESTROYED = 4;

    public GameLogic(Model model, ShotsQueue shotsQueue) {
        this.model = model;
        this.shotsQueue = shotsQueue;
        thread = new Thread(this);
        lock = model.getLock();
        userShipsNumber = SHIPS_NUMBER;
        computerShipsNumber = SHIPS_NUMBER;
    }

    public void start() {
        thread.start();
    }

    public void stop() {
        isRunning = false;
    }

    public void run() {
        isRunning = true;
        while (isRunning) {
            handleAction();
        }
    }

    public boolean isGameOver() {
        return userShipsNumber == 0 || computerShipsNumber == 0;
    }

    public int getUserShipsNumber() {
        return userShipsNumber;
    }

    public int getComputerShipsNumber() {
        return computerShipsNumber;
    }

    public void updateShipsNumber() {
        userShipsNumber = SHIPS_NUMBER;
        computerShipsNumber = SHIPS_NUMBER;
    }

    private void handleAction() {
        Shot shot = consumeShot();
        synchronized (lock) {
            if (shot != null) {
                if (shot.getPlayerType() == PlayerType.USER) {
                    handleUserAction(shot);
                } else {
                    handleComputerAction(shot);
                }
                model.viewerUpdate();
            }
        }
    }

    private Shot consumeShot() {
        try {
            return shotsQueue.remove();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void handleUserAction(Shot shot) {
        boolean isShipHit = processUserShot(shot);
        model.setUserTurn(isShipHit);
        lock.notify();
    }

    private void handleComputerAction(Shot shot) {
        boolean isShipHit = processComputerShot(shot);
        model.setUserTurn(!isShipHit);
        if (isShipHit && !isGameOver()) {
            lock.notifyAll();
        }
    }

    private boolean processUserShot(Shot shot) {
        Cell[][] computerBoard = model.getEnemyBoardArray();
        boolean isHit = processShot(computerBoard, shot);

        Cell shottedCell = computerBoard[shot.getY()][shot.getX()];
        if (isHit) {
            setHitImage(shottedCell, true);
        }

        Ship ship = shottedCell.getShip();
        if (ship != null) {
            checkIfShipDestroyed(ship, true);
        }
        return isHit;
    }

    private boolean processComputerShot(Shot shot) {
        Cell[][] userBoard = model.getUserBoardArray();
        boolean isHit = processShot(userBoard, shot);

        Cell shottedCell = userBoard[shot.getY()][shot.getX()];
        if (isHit) {
            setHitImage(shottedCell, false);
        }

        Ship ship = shottedCell.getShip();
        if (ship != null) {
            checkIfShipDestroyed(ship, false);
        }
        return isHit;
    }

    private boolean processShot(Cell[][] board, Shot shot) {
        Cell shottedCell = board[shot.getY()][shot.getX()];
        hideIfVisible(shottedCell);

        if (shottedCell.getValue() == SHIP) {
            shottedCell.setValue(HIT_SHOT);
            model.getShotSound().play();
            model.getSuccessShotSound().play();
            return true;
        } else {
            shottedCell.setValue(MISS_SHOT);
            model.getShotSound().play();
            model.getWaterShotSound().play();
            return false;
        }
    }

    private void setHitImage(Cell cell, boolean isUser) {
        if (isUser) {
            cell.setImage(new ImageIcon("images/ship_shot.png").getImage());
            return;
        }
        String imagePath = cell.getImagePath();
        String sharpedImagePath = imagePath.substring(0, imagePath.length() - 4) + "-sharped.png";
        cell.setImage(new ImageIcon(sharpedImagePath).getImage());
    }

    private void hideIfVisible(Cell cell) {
        if (cell.isVisible()) {
            cell.setVisible(false);
        }
    }

    private void checkIfShipDestroyed(Ship ship, boolean isUserTurn) {
        Cell[] shipCells = ship.getCells();
        int i = 1;
        if (isShipDestroyed(shipCells)) {
            for (Cell cell : shipCells) {
                cell.setValue(DESTROYED);
                cell.setImage(new ImageIcon("images/ship-" + i + "-sinked.png").getImage());
                if (i < 3) {
                    i++;
                }
            }
            model.getKilledShipSound().play();
            if (isUserTurn) {
                computerShipsNumber--;
            } else {
                userShipsNumber--;
            }
        }
    }

    private boolean isShipDestroyed(Cell[] cells) {
        for (Cell cell : cells) {
            if (cell.getValue() == 1) {
                return false;
            }
        }
        return true;
    }

}
