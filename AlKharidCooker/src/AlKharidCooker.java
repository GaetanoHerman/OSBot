import org.osbot.rs07.api.model.NPC;
import org.osbot.rs07.api.model.RS2Object;
import org.osbot.rs07.api.ui.RS2Widget;
import org.osbot.rs07.api.ui.Skill;
import org.osbot.rs07.api.util.Utilities;
import org.osbot.rs07.script.Script;
import org.osbot.rs07.script.ScriptManifest;
import org.osbot.rs07.utility.ConditionalSleep;


import java.awt.*;
import java.text.DecimalFormat;


@ScriptManifest(name = "RogueDenCooker", author = "GaetanoH", version = 1.0, info = "", logo = "")
public class AlKharidCooker extends Script {

    private int cookingXP = 0;
    private long startTime;

    public enum State {
        COOKING, BANKING;
    }

    public State getState(){
        if(!bank.isOpen() && inventory.contains("Raw dark crab")){
            return State.COOKING;
        }

        if(!inventory.contains("Raw dark crab")){
            return State.BANKING;
        }
        return null;
    }

    @Override
    public void onStart() {
        log("Thanks for using my Rogue's Den Cooker, please start with enough food in the bank");
        startTime = System.currentTimeMillis();
        getExperienceTracker().start(Skill.COOKING);
    }

    @Override
    public void onExit() {
    }

    @Override
    public int onLoop() throws InterruptedException {

        switch(getState()){
            case COOKING:
                RS2Object fire = getObjects().closest("Fire");
                RS2Widget widget = getWidgets().get(307, 2);
                if(!isCooking()){
                    if(widget == null){
                        getInventory().getItem("Raw dark crab").interact("Use");
                        if(fire.isVisible() &&  myPlayer().isVisible()){
                            fire.interact("Use");
                            new ConditionalSleep(1500){
                                @Override
                                public boolean condition() throws InterruptedException {
                                    return widget != null;
                                }
                            }.sleep();
                        }
                    } else {
                        widget.interact("Cook All");
                    }
                }
                break;
            case BANKING:
                NPC emeraldBenedict = getNpcs().closest("Emerald Benedict");
                if(emeraldBenedict != null){
                    if(!bank.isOpen()){
                        emeraldBenedict.interact("Bank");
                        sleep(1500);
                        if(bank.isOpen()){
                            bank.depositAll();
                            bank.withdraw("Raw dark crab", 28);
                            bank.close();
                            sleep(random(300,400));
                        }
                    }
                }
                break;
        }

        return 100;
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
        Point mP = getMouse().getPosition();

        g.drawLine(mP.x - 5, mP.y + 5, mP.x + 5, mP.y - 5);
        g.drawLine(mP.x + 5, mP.y + 5, mP.x - 5, mP.y - 5);

        long runTime = System.currentTimeMillis() - startTime;

        cookingXP = getExperienceTracker().getGainedXPPerHour(Skill.COOKING);

        g.drawString("Time running: " + formatTime(runTime), 10, 290);
        g.drawString("Experience/h: " + String.valueOf(cookingXP), 10, 310);
        g.drawString("Made by GaetanoH", 10, 330);

    }

    public boolean isCooking() {
        boolean isCooking = false;
        Timer timer = new Timer(1800);
        while (timer.isRunning() && !isCooking) {
            isCooking = myPlayer().getAnimation() != -1 ? true : isCooking;
        }
        return isCooking;
    }


    public class Timer {
        private long period;
        private long start;
        public Timer(long period) {
            this.period = period;
            this.start = System.currentTimeMillis();
        }
        public long getElapsed() {
            return System.currentTimeMillis() - this.start;
        }
        public long getRemaining() {
            return this.period - this.getElapsed();
        }
        public boolean isRunning() {
            return this.getElapsed() <= this.period;
        }
        public void setPeriod(long period) {
            this.period = period;
        }
        public void reset() {
            this.start = System.currentTimeMillis();
        }
        public String format(long milliSeconds) {
            long secs = milliSeconds / 1000L;
            return String.format("%02d:%02d:%02d", secs / 3600L,
                    secs % 3600L / 60L, secs % 60L);
        }
    }
}