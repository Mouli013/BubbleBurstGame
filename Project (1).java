import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;
import java.util.Hashtable;

public class Project {
  private static final int Rounds = 10;
  private static final int Radius = 30;
  private static final int Box_Size = 50;
  private static final int Neighborhood = 18;
  private StartFrame startFrame;
  private GameFrame gameField;
  private Random random = new Random();

  public Project() {
    startFrame = new StartFrame();
    gameField = new GameFrame(startFrame);
    startFrame.setVisible(true);
  }

  public class StartFrame extends JFrame {
    private JSlider slider;

    public StartFrame() {
      setPreferredSize(new Dimension(400, 400));
      setTitle("Bubble Game");
      setLayout(new FlowLayout());
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
      JButton startButton = new JButton("Start");
      JButton restartButton = new JButton("Restart");
      buttonPanel.add(startButton);
      buttonPanel.add(restartButton);

      JPanel sliderPanel = new JPanel();
      sliderPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

      slider = new JSlider(JSlider.HORIZONTAL, 4, 6, 4);
      Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
      labelTable.put(4, new JLabel("Easy 4"));
      labelTable.put(5, new JLabel("Medium 5"));
      labelTable.put(6, new JLabel("Hard 6"));
      slider.setLabelTable(labelTable);
      slider.setPaintLabels(true);
      slider.setMajorTickSpacing(1);
      slider.setPaintTicks(true);
      slider.setPaintLabels(true);
      sliderPanel.add(slider);

      add(buttonPanel);
      add(sliderPanel);

      startButton.addActionListener(e -> gameField.startGame(slider.getValue()));
      restartButton.addActionListener(e -> gameField.restartGame(slider.getValue()));

      pack();
      setLocationRelativeTo(null);
    }
  }

  public class GameFrame extends JFrame {
    private GamePanel gamePanel;
    private int level;
    private int currentRound = 1;

    public GameFrame(StartFrame startFrame) {
      setTitle("Playing Field");
      gamePanel = new GamePanel();
      add(gamePanel);
      pack();
      setLocationRelativeTo(null);
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      setVisible(false);
    }

    public void restartGame(int level) {
      this.level = level;
      currentRound = 1;
      gamePanel.restartGame();
      setVisible(true);
      startFrame.setVisible(false);
    }

    public void startGame(int level) {
      this.level = level;
      gamePanel.startNewRound();
      setVisible(true);
      startFrame.setVisible(false);
    }

    class GamePanel extends JPanel {
      private ArrayList<Bubble> bubbles = new ArrayList<>();
      private boolean checkBubbles = true;
      private Timer hopper;
      private final int TIMER_DELAY = 1500;
      private Timer roundTimer;
      private int roundTimeLeft; 
      private JLabel timerLabel;

      public GamePanel() {
        setPreferredSize(new Dimension(400, 400));
        setBackground(Color.WHITE);
        hopper = new Timer(TIMER_DELAY, e -> updateBubbles());
        hopper.start();

        setLayout(new BorderLayout()); 
        timerLabel = new JLabel("Time left: 15", SwingConstants.RIGHT); 
        timerLabel.setForeground(Color.GREEN);
        add(timerLabel, BorderLayout.NORTH);

        roundTimer = new Timer(1000, e -> {
            if (roundTimeLeft > 0) {
                roundTimeLeft--;
                timerLabel.setText("Time left: " + roundTimeLeft);
            } else {
                roundTimer.stop();
                gameOver("You Lost Buddy ! Game over.");
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (checkBubbles) {
                    Point point = e.getPoint();
                    if (BoundaryCheck(point) && bubbles.stream().noneMatch(b -> b.contains(point) || b.BoundingBox(point))) {
                        placeBubble(point);
                    } else {
                      JOptionPane.showMessageDialog(GamePanel.this, "Oops! Bubble overlap detected, please choose another location.");
                    }
                } else {
                    if (bubbles.stream().noneMatch(b -> b.contains(e.getPoint()) || b.BoundingBox(e.getPoint()))) {
                        roundTimer.stop();
                        gameOver("Done Wrong Buddy Sorry. Game over.");
                    } else {
                        burstBubble(e.getPoint());
                    }
                }
            }
        });
      }

      private void gameOver(String message) {
          roundTimer.stop();
          JOptionPane.showMessageDialog(this, message);
          resetGame();
          GameFrame.this.setVisible(false);
          startFrame.setVisible(true);
      }

      public void resetGame() {
          bubbles.clear();
          currentRound = 1;
          checkBubbles = true;
          roundTimeLeft = 15; 
          timerLabel.setText("Time left: " + roundTimeLeft);
          repaint();
      }

      private boolean BoundaryCheck(Point point) {
        int offset = Radius / 2;
        return point.x >= offset && point.y >= offset && point.x <= getWidth() - offset
            && point.y <= getHeight() - offset;
      }


      public void startNewRound() {
        checkBubbles = true;
        for (Bubble bubble : bubbles) {
          bubble.isBurst = false;
          bubble.neighborhoodSize = Box_Size + Neighborhood * (currentRound - 1);
        }
        repaint();
      }

      private void updateBubbles() {
          if (!checkBubbles) {
              for (Bubble bubble : bubbles) {
                  bubble.repositionLocal(getWidth(),getHeight()); 
              }
              repaint();
          }
      }

      public void restartGame() {
        currentRound = 1;
        for (Bubble bubble : bubbles) {
          bubble.neighborhoodSize = Box_Size;
        }
        startNewRound();
      }

      private void placeBubble(Point point) {
        if (bubbles.size() < level) {
          int initialNeighborhoodSize = Box_Size + Neighborhood * (currentRound - 1);
          Bubble newBubble = new Bubble(point, randomColor(), initialNeighborhoodSize);
          for (Bubble bubble : bubbles) {
              if (newBubble.overlaps(bubble)) {
                  JOptionPane.showMessageDialog(this, "Oops! Bubble overlap detected, please choose another location.");
                  return;
              }
          }
          bubbles.add(newBubble);
          if (bubbles.size() == level) {
            checkBubbles = false;
            JOptionPane.showMessageDialog(GamePanel.this, "Round " + currentRound + " begins! show your Madness on bubbles.");
            roundTimeLeft = Math.max(15 - (currentRound - 1), 5); 
            timerLabel.setText("Time left: " + roundTimeLeft);
            roundTimer.start();
          }
          repaint();
        }
      }

      private void burstBubble(Point clickPoint) {
        bubbles.removeIf(bubble -> bubble.contains(clickPoint));
        if (bubbles.isEmpty()) {
          if (currentRound < Rounds) {
            currentRound++;
            increaseNeighborhoodSize();
            reposition_global();
          } else {
            JOptionPane.showMessageDialog(GamePanel.this, "Game Over! Your a Champion Buddy");
            GameFrame.this.setVisible(false);
            startFrame.setVisible(true);
          }
        }
        repaint();
      }

      private Color randomColor() {
        return new Color(random.nextInt(256), random.nextInt(256), random.nextInt(256));
      }

      private void reposition_global() {
        bubbles.clear();
        while (bubbles.size() < level) {
          int x = Radius + random.nextInt(getWidth() - 2 * Radius);
          int y = Radius + random.nextInt(getHeight() - 2 * Radius);
          Point point = new Point(x, y);
          boolean overlaps = bubbles.stream().anyMatch(b -> b.overlaps(point));
          if(!overlaps){
          bubbles.add(new Bubble(point, randomColor(), Box_Size + Neighborhood * (currentRound - 1)));
        }
        }
        roundTimeLeft = Math.max(15 - (currentRound - 1), 5); 
        timerLabel.setText("Time left: " + roundTimeLeft);
        roundTimer.start();
        checkBubbles = false;
        JOptionPane.showMessageDialog(this, "Round " + currentRound + " begins! show your Madness on bubbles.");
        repaint();
      }

      
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Bubble bubble : bubbles) {

          g.setColor(bubble.color);
          g.fillOval(bubble.origin.x - Radius / 2, bubble.origin.y - Radius / 2, Radius, Radius);

          if (!bubble.isBurst) {
            g.setColor(Color.RED);

            int boxX = bubble.neighborhoodCenter.x - bubble.neighborhoodSize / 2;
            int boxY = bubble.neighborhoodCenter.y - bubble.neighborhoodSize / 2;
            g.drawRect(boxX, boxY, bubble.neighborhoodSize, bubble.neighborhoodSize);
          }
        }
        g.setColor(Color.BLUE);
        g.drawString("Round: " + currentRound, 10, 20);
      }

      private void increaseNeighborhoodSize() {
        for (Bubble bubble : bubbles) {
          bubble.neighborhoodSize += Neighborhood * 2;
        }
      }

    }
  }

  class Bubble {
    Point origin;
    Point neighborhoodCenter;
    Color color;
    boolean isBurst = false;
    int neighborhoodSize;

    public Bubble(Point origin, Color color, int neighborhoodSize) {
      this.origin = origin;
      this.color = color;
      this.neighborhoodSize = neighborhoodSize;
      this.neighborhoodCenter = new Point(origin);
    }

    public boolean BoundingBox(Point p) {
        int boxX = neighborhoodCenter.x - neighborhoodSize / 2;
        int boxY = neighborhoodCenter.y - neighborhoodSize / 2;
        return p.x >= boxX && p.x <= (boxX + neighborhoodSize) &&
               p.y >= boxY && p.y <= (boxY + neighborhoodSize);
    }

    public void repositionLocal(int panelWidth, int panelHeight) {

      int minX = Math.max(neighborhoodCenter.x - neighborhoodSize / 2, 0);
      int maxX = Math.min(neighborhoodCenter.x + neighborhoodSize / 2, panelWidth);
      int minY = Math.max(neighborhoodCenter.y - neighborhoodSize / 2, 0);
      int maxY = Math.min(neighborhoodCenter.y + neighborhoodSize / 2, panelHeight);

      minX += Radius / 2;
      maxX -= Radius / 2;
      minY += Radius / 2;
      maxY -= Radius / 2;

      if (maxX > minX && maxY > minY) {
        origin = new Point(minX + random.nextInt(maxX - minX), minY + random.nextInt(maxY - minY));
      }
    }

    public boolean overlaps(Bubble other) {
        double dx = this.neighborhoodCenter.x - other.neighborhoodCenter.x;
        double dy = this.neighborhoodCenter.y - other.neighborhoodCenter.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < (this.neighborhoodSize / 2) + (other.neighborhoodSize / 2);
    }

    public boolean overlaps(Point point) {
        double dx = this.neighborhoodCenter.x - point.x;
        double dy = this.neighborhoodCenter.y - point.y;
        double distance = Math.sqrt(dx * dx + dy * dy);
        return distance < Radius / 2; 
    }

    public boolean contains(Point p) {
      double dx = Math.abs(p.x - origin.x);
      double dy = Math.abs(p.y - origin.y);
      return Math.sqrt(dx * dx + dy * dy) <= Radius / 2;
    }
  }
}