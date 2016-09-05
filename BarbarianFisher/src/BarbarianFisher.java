import org.osbot.rs07.api.map.Area;
import org.osbot.rs07.api.map.Position;
import org.osbot.rs07.api.map.constants.Banks;
import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.ui.Message;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.api.ui.Tab;
import org.osbot.rs07.event.WalkingEvent;
import org.osbot.rs07.event.WebWalkEvent;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.Condition;
import org.osbot.rs07.utility.ConditionalSleep;

import javax.swing.*;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;

@ScriptManifest(name = "Barbarian Fisher", author = "GaetanoH", version = 1.06, info = "Start in Edgeville bank or at the fishing spot with enough feathers and a fly fishing rod in inventory!", logo = "")
public class BarbarianFisher extends Script {

    long startTime;
    int fishCaught = 0;
    long fishingxpHour = 0;
    long fishingXP = 0;
    String state = "Starting up bot";
    long runTime;
    Timer timer = null;
    String[] listQuestions = new String[]{"Level", "Levels", "Lvl","level", "levels", "lvl"};
    Area fishingArea = new Area(3102, 3439, 3111, 3421);

    //Setting variables
    boolean powerfishing = false;
    boolean sayAnswer;
    boolean started = false;
    int random = -1;

    //GUI variables
    JFrame gui;
    JCheckBox checkbox;
    JCheckBox antiBan;

    private enum State{
        FISHING,
        BANKING,
        POWERFISHING
    }
    
    public State getState(){
        if(getInventory().isFull() && powerfishing != true)
            return State.BANKING;
        if(getInventory().isFull() && powerfishing == true)
            return State.POWERFISHING;
        else
            return State.FISHING;
    }
    
    private void createGUI(){
        final int GUI_WIDTH = 350, GUI_HEIGHT = 100;
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        
        final int gX = (int) (screenSize.getWidth() / 2) - (GUI_WIDTH / 2);
        final int gY = (int) (screenSize.getHeight() / 2) - (GUI_HEIGHT / 2);
        
        gui = new JFrame("Choose option: ");
        
        gui.setBounds(gX, gY, GUI_WIDTH, GUI_HEIGHT);
        
        gui.setResizable(false);
        
        JPanel panel = new JPanel();
        
        gui.add(panel);

        checkbox = new JCheckBox("Powerfish?");
        panel.add(checkbox);

        antiBan = new JCheckBox("Use antiban? (Lowers XP per hour)");
        panel.add(antiBan);
        
        JButton startButton = new JButton("Start");
        startButton.addActionListener(e -> {
            started  = true;
            gui.setVisible(false);
        });
        panel.add(startButton);
    
        gui.setVisible(true);
    }
    
    @Override
    public void onStart() throws InterruptedException {
        createGUI();
        startTime = System.currentTimeMillis();
        getExperienceTracker().start(Skill.FISHING);
        log("Update v1.06: More info on paint about antiban");
        log("Start this script either in Edgeville or at the Fishing spot!");
        log("Have enough feathers in inventory and a fly fishing rod.");
        log("Please leave a like at the forum post, and post any bugs there with a screenshot of the console log, thanks in advance!");
    }
    
    @Override
    public void onExit() throws InterruptedException {
        if(gui != null) {
            gui.setVisible(false);
            gui.dispose();
        }
        
        log("You caught " + fishCaught + " fishes, gained " + fishingXP + " Fishing XP, leveled " + getExperienceTracker().getGainedLevels(Skill.FISHING)+ "and that all in " + formatTime(runTime));
        log("Thank you for using my script, leave feedback and ideas at the thread.");
    }
    
    
    @Override
    public int onLoop() throws InterruptedException {
        powerfishing = checkbox.isSelected();
        if(started){
            switch (getState()){
                case BANKING:
                    if(!Banks.EDGEVILLE.contains(myPosition())){
                        state = "Walking to Edgeville Bank";
                        WebWalkEvent toBank = new WebWalkEvent(Banks.EDGEVILLE.getRandomPosition());
                        toBank.setBreakCondition(new Condition() {
                            @Override
                            public boolean evaluate() {
                                return Banks.EDGEVILLE.getRandomPosition().distance(myPlayer()) <= 7;
                            }
                        });
                        toBank.execute();
                    }
                    
                    if(!getBank().isOpen()){
                        getBank().open();
                        new ConditionalSleep(5000) {
                            @Override
                            public boolean condition() throws InterruptedException {
                                return getBank().isOpen();
                            }
                        }.sleep();
                    } else {
                        if(!getInventory().isEmptyExcept("Fly fishing rod", "Feather")){
                            state = "Banking...";
                            getBank().depositAllExcept("Fly fishing rod", "Feather");
                            if(!getInventory().contains("Raw salmon", "Raw trout")){
                                getBank().close();
                                sleep(random(200, 300));
                            }
                        }
                    }
                    break;
                    
                case FISHING:
                    getCamera().toTop();
                    if(!fishingArea.contains(myPosition())){
                        state = "Walking to fishing spot...";
                        WebWalkEvent toSpot = new WebWalkEvent(fishingArea);
                        toSpot.setBreakCondition(new Condition() {
                            @Override
                            public boolean evaluate() {
                                return fishingArea.getRandomPosition().distance(myPlayer()) <= 7;
                            }
                        });
                        toSpot.execute();
                    } else {
                        if(getTabs().open(Tab.INVENTORY)){
                            if(getInventory().contains("Feather")){
                                NPC fishingSpot = getNpcs().closest(true, "Fishing spot");
                                if(fishingSpot != null && fishingSpot.hasAction("Lure") && fishingSpot.exists()){
                                    if(!myPlayer().isAnimating() && !myPlayer().isMoving()){
                                        fishingSpot.interact("Lure");
                                        new ConditionalSleep(2000) {
                                            @Override
                                            public boolean condition() throws InterruptedException {
                                                return myPlayer().isAnimating() || !getDialogues().clickContinue();
                                            }
                                        }.sleep();
                                    } else {
                                        state = "Fishing...";
                                        if(antiBan.isSelected()){
                                            antiBan();
                                        }
                                    }
                                }
                            } else {
                                state = "Stopping script! You don't have feathers in inventory!";
                                getLogoutTab().logOut();
                                log("Stopping script, you don't have feathers in inventory");
                                getBot().getScriptExecutor().stop();
                            }
                        }
                    }
                    break;
                case POWERFISHING:
                    if(getInventory().contains("Raw salmon", "Raw trout")){
                        state = "Dropping all the fishes...";
                        getInventory().dropAllExcept("Feather", "Fly fishing rod");
                        new ConditionalSleep(5000) {
                            @Override
                            public boolean condition() throws InterruptedException {
                                return getInventory().contains("Raw salmon", "Raw trout");
                            }
                        }.sleep();
                    }
            }
        }
        return random(400, 500);
        
    }
    
    public String formatTime(long ms){
        
        long s = ms / 1000, m = s / 60, h = m / 60, d = h / 24;
        s %= 60; m %= 60; h %= 24;
        
        return d > 0 ? String.format("%02d:%02d:%02d:%02d", d, h, m, s) :
        h > 0 ? String.format("%02d:%02d:%02d", h, m, s) :
        String.format("%02d:%02d", m, s);
    }
    
    @Override
    public void onPaint(Graphics2D g) {
        
        if(started){
            Point mP = getMouse().getPosition();

            g.setColor(Color.ORANGE);
            
            g.drawLine(mP.x - 5, mP.y + 5, mP.x + 5, mP.y - 5);
            g.drawLine(mP.x + 5, mP.y + 5, mP.x - 5, mP.y - 5);
            
            runTime = System.currentTimeMillis() - startTime;
            
            fishingXP = getExperienceTracker().getGainedXP(Skill.FISHING);
            fishingxpHour = getExperienceTracker().getGainedXPPerHour(Skill.FISHING);
            
            
            g.setFont(new Font("Serif", Font.BOLD, 12));
            g.drawString("Time running: " + formatTime(runTime), 10, 270);
            g.drawString("Fish caught: " + fishCaught, 10, 285);
            g.drawString("Status: " + state, 10, 300);
            g.drawString("Fishing XP (hour): " + String.valueOf(fishingXP) + " (" +String.valueOf(fishingxpHour) + ")", 10, 315);
            g.drawString("Made by GaetanoH", 10, 330);
        }
    }

    public void antiBan() throws InterruptedException {
        if(timer == null){
            timer = new Timer();
        }

        if(random == -1) {
            random = random(2, 187);
        }

        if(timer.duration().getSeconds() > random){
            timer.reset();
        }

        if(random == timer.duration().getSeconds()){
            int dice  = random(1, 6);

            switch (dice){
                case 1:
                    getMouse().moveOutsideScreen();
                    state = "Moving mouse out of screen";
                    sleep(random(200, 300));
                    break;
                case 2:
                    int x = random(1, 3);
                    switch (x){
                        case 1:
                            getMouse().moveRandomly();
                            state = "Moving mouse randomly";
                            break;
                        case 2:
                            getMouse().moveSlightly();
                            state = "Moving mouse slightly";
                            break;
                        case 3:
                            getMouse().moveVerySlightly();
                            state = "Moving mouse very slightly";
                            break;
                    }
                case 3:
                    getSkills().hoverSkill(Skill.FISHING);
                    state = "Hovering skills";
                    sleep(random(400, 800));
                    getTabs().open(Tab.INVENTORY);
                    break;
                case 4:
                    Tab[] tabs = Tab.values();
                    List<Tab> tabList = Arrays.asList(tabs);
                    state = "Opening random tab";
                    getTabs().open(tabList.get(random(0, tabList.size() - 1)));
                case 5:
                    getCamera().toPosition(myPlayer().getArea(6).getRandomPosition());
                    state = "Moving camera to random position";
                    sleep(random(100, 500));
                    getCamera().toEntity(getObjects().closest(fishingArea, "Fishing spot"));
                case 6:
                    if(sayAnswer){
                        state = "Responding to question in chat";
                        getKeyboard().typeString(Integer.toString(getSkills().getStatic(Skill.FISHING)), true);
                        sayAnswer = false;
                    }
            }
            random = -1;
            timer = null;
        }
    }

    @Override
    public void onMessage(Message c){
        if(c.getType() == Message.MessageType.GAME){
            String t = c.getMessage().toLowerCase();
            try {
                if(t.contains("you catch a"))
                    fishCaught++;
            } catch(Exception e){
                e.printStackTrace();
            }
        }

        if(c.getType() == Message.MessageType.PLAYER){
            String txt = c.getMessage().toLowerCase();

            if(Arrays.asList(listQuestions).contains(txt)){
                sayAnswer = true;
            }
        }
    }

    public class Timer {

        private Instant start;

        private static final String formatter = "%02d";

        public Timer() {
            reset();
        }

        public void reset() {
            start = Instant.now();
            log("resetting");
        }

        public Duration duration() {
            return Duration.between(start, Instant.now());
        }

        @Override
        public String toString() {
            Duration duration = duration();
            return String.format(formatter, duration.toHours()) + ":" + String.format(formatter, duration.toMinutes() % 60) + ":" + String.format(formatter, duration.getSeconds() % 60);
        }

    }
}